/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.schedule;

import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.metrics.jmx.JmxGetter;
import com.espertech.esper.timer.TimeSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implements the schedule service by simply keeping a sorted set of long millisecond
 * values and a set of handles for each.
 * <p>
 * Synchronized since statement creation and event evaluation by multiple (event send) threads
 * can lead to callbacks added/removed asynchronously.
 */
public final class SchedulingServiceImpl implements SchedulingServiceSPI {
    // Map of time and handle
    private final SortedMap<Long, SortedMap<Long, ScheduleHandle>> timeHandleMap;

    // Map of handle and handle list for faster removal
    private final Map<ScheduleHandle, SortedMap<Long, ScheduleHandle>> handleSetMap;

    // Current time - used for evaluation as well as for adding new handles
    private volatile long currentTime;

    /**
     * Constructor.
     *
     * @param timeSourceService time source provider
     */
    public SchedulingServiceImpl(TimeSourceService timeSourceService) {
        this.timeHandleMap = new TreeMap<Long, SortedMap<Long, ScheduleHandle>>();
        this.handleSetMap = new HashMap<ScheduleHandle, SortedMap<Long, ScheduleHandle>>();
        // initialize time to just before now as there is a check for duplicate external time events
        this.currentTime = timeSourceService.getTimeMillis() - 1;
    }

    public void destroy() {
        log.debug("Destroying scheduling service");
        handleSetMap.clear();
        timeHandleMap.clear();
    }

    public long getTime() {
        // note that this.currentTime is volatile
        return this.currentTime;
    }

    public synchronized final void setTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public synchronized final void add(long afterTime, ScheduleHandle handle, long slot)
            throws ScheduleServiceException {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qScheduleAdd(currentTime, afterTime, handle, slot);
        }
        if (handleSetMap.containsKey(handle)) {
            remove(handle, slot);
        }

        long triggerOnTime = currentTime + afterTime;
        addTrigger(slot, handle, triggerOnTime);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aScheduleAdd();
        }
    }

    public synchronized final void remove(ScheduleHandle handle, long slot) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qScheduleRemove(handle, slot);
        }
        SortedMap<Long, ScheduleHandle> handleSet = handleSetMap.get(handle);
        if (handleSet == null) {
            // If it already has been removed then that's fine;
            // Such could be the case when 2 timers fireStatementStopped at the same time, and one stops the other
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aScheduleRemove();
            }
            return;
        }
        handleSet.remove(slot);
        handleSetMap.remove(handle);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aScheduleRemove();
        }
    }

    public synchronized final void evaluate(Collection<ScheduleHandle> handles) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qScheduleEval(currentTime);
        }
        // Get the values on or before the current time - to get those that are exactly on the
        // current time we just add one to the current time for getting the head map
        SortedMap<Long, SortedMap<Long, ScheduleHandle>> headMap = timeHandleMap.headMap(currentTime + 1);

        if (headMap.isEmpty()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aScheduleEval(Collections.<ScheduleHandle>emptyList());
            }
            return;
        }

        // First determine all triggers to shoot
        List<Long> removeKeys = new ArrayList<Long>();
        for (Map.Entry<Long, SortedMap<Long, ScheduleHandle>> entry : headMap.entrySet()) {
            Long key = entry.getKey();
            SortedMap<Long, ScheduleHandle> value = entry.getValue();
            removeKeys.add(key);
            for (ScheduleHandle handle : value.values()) {
                handles.add(handle);
            }
        }

        // Next remove all handles
        for (Map.Entry<Long, SortedMap<Long, ScheduleHandle>> entry : headMap.entrySet()) {
            for (ScheduleHandle handle : entry.getValue().values()) {
                handleSetMap.remove(handle);
            }
        }

        // Remove all triggered msec values
        for (Long key : removeKeys) {
            timeHandleMap.remove(key);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aScheduleEval(handles);
        }
    }

    public ScheduleSet take(Set<Integer> statementIds) {
        List<ScheduleSetEntry> list = new ArrayList<ScheduleSetEntry>();
        long currentTime = getTime();
        for (Map.Entry<Long, SortedMap<Long, ScheduleHandle>> schedule : timeHandleMap.entrySet()) {
            for (Map.Entry<Long, ScheduleHandle> entry : schedule.getValue().entrySet()) {
                if (statementIds.contains(entry.getValue().getStatementId())) {
                    long relative = schedule.getKey() - currentTime;
                    list.add(new ScheduleSetEntry(relative, entry.getKey(), entry.getValue()));
                }
            }
        }

        for (ScheduleSetEntry entry : list) {
            remove(entry.getHandle(), entry.getScheduleSlot());
        }

        return new ScheduleSet(list);
    }

    public void apply(ScheduleSet scheduleSet) {
        for (ScheduleSetEntry entry : scheduleSet.getList()) {
            add(entry.getTime(), entry.getHandle(), entry.getScheduleSlot());
        }
    }

    public void init() {
        // no action required
    }

    private void addTrigger(long slot, ScheduleHandle handle, long triggerTime) {
        SortedMap<Long, ScheduleHandle> handleSet = timeHandleMap.get(triggerTime);
        if (handleSet == null) {
            handleSet = new TreeMap<Long, ScheduleHandle>();
            timeHandleMap.put(triggerTime, handleSet);
        }
        handleSet.put(slot, handle);
        handleSetMap.put(handle, handleSet);
    }

    @JmxGetter(name = "TimeHandleCount", description = "Number of outstanding time evaluations")
    public int getTimeHandleCount() {
        return timeHandleMap.size();
    }

    @JmxGetter(name = "FurthestTimeHandle", description = "Furthest outstanding time evaluation")
    public String getFurthestTimeHandleDate() {
        Long handle = getFurthestTimeHandle();
        if (handle != null) {
            return DateTime.print(handle);
        }
        return null;
    }

    @JmxGetter(name = "NearestTimeHandle", description = "Nearest outstanding time evaluation")
    public String getNearestTimeHandleDate() {
        Long handle = getNearestTimeHandle();
        if (handle != null) {
            return DateTime.print(handle);
        }
        return null;
    }

    public Long getFurthestTimeHandle() {
        if (!timeHandleMap.isEmpty()) {
            return timeHandleMap.lastKey();
        }
        return null;
    }

    public int getScheduleHandleCount() {
        return handleSetMap.size();
    }

    public boolean isScheduled(ScheduleHandle handle) {
        return handleSetMap.containsKey(handle);
    }

    @Override
    public synchronized Long getNearestTimeHandle() {
        if (timeHandleMap.isEmpty()) {
            return null;
        }
        for (Map.Entry<Long, SortedMap<Long, ScheduleHandle>> entry : timeHandleMap.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            return entry.getKey();
        }
        return null;
    }

    public void visitSchedules(ScheduleVisitor visitor) {
        ScheduleVisit visit = new ScheduleVisit();
        for (Map.Entry<Long, SortedMap<Long, ScheduleHandle>> entry : timeHandleMap.entrySet()) {
            visit.setTimestamp(entry.getKey());

            for (Map.Entry<Long, ScheduleHandle> inner : entry.getValue().entrySet()) {
                visit.setStatementId(inner.getValue().getStatementId());
                visit.setAgentInstanceId(inner.getValue().getAgentInstanceId());
                visitor.visit(visit);
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(SchedulingServiceImpl.class);
}
