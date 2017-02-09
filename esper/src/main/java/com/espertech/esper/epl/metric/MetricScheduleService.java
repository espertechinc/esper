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
package com.espertech.esper.epl.metric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Scheduling for metrics execution is handles=d by this service.
 */
public final class MetricScheduleService implements MetricTimeSource {
    private static final Logger log = LoggerFactory.getLogger(MetricScheduleService.class);

    private final SortedMap<Long, List<MetricExec>> timeHandleMap;

    // Current time - used for evaluation as well as for adding new handles
    private volatile Long currentTime;

    private volatile Long nearestTime;

    /**
     * Constructor.
     */
    public MetricScheduleService() {
        this.timeHandleMap = new TreeMap<Long, List<MetricExec>>();
    }

    public long getCurrentTime() {
        return currentTime;
    }

    /**
     * Clears the schedule.
     */
    public void clear() {
        log.debug("Clearing scheduling service");
        timeHandleMap.clear();
        nearestTime = null;
    }

    /**
     * Sets current time.
     *
     * @param currentTime to set
     */
    public synchronized final void setTime(long currentTime) {
        this.currentTime = currentTime;
    }

    /**
     * Adds an execution to the schedule.
     *
     * @param afterMSec offset to add at
     * @param execution execution to add
     */
    public synchronized final void add(long afterMSec, MetricExec execution) {
        if (execution == null) {
            throw new IllegalArgumentException("Unexpected parameters : null execution");
        }
        long triggerOnTime = currentTime + afterMSec;
        List<MetricExec> handleSet = timeHandleMap.get(triggerOnTime);
        if (handleSet == null) {
            handleSet = new ArrayList<MetricExec>();
            timeHandleMap.put(triggerOnTime, handleSet);
        }
        handleSet.add(execution);

        nearestTime = timeHandleMap.firstKey();
    }

    /**
     * Evaluate the schedule and populates executions, if any.
     *
     * @param handles to populate
     */
    public synchronized final void evaluate(Collection<MetricExec> handles) {
        SortedMap<Long, List<MetricExec>> headMap = timeHandleMap.headMap(currentTime + 1);

        // First determine all triggers to shoot
        List<Long> removeKeys = new LinkedList<Long>();
        for (Map.Entry<Long, List<MetricExec>> entry : headMap.entrySet()) {
            Long key = entry.getKey();
            List<MetricExec> value = entry.getValue();
            removeKeys.add(key);
            for (MetricExec handle : value) {
                handles.add(handle);
            }
        }

        // Remove all triggered msec values
        for (Long key : removeKeys) {
            timeHandleMap.remove(key);
        }

        if (!timeHandleMap.isEmpty()) {
            nearestTime = timeHandleMap.firstKey();
        } else {
            nearestTime = null;
        }
    }

    /**
     * Returns nearest scheduled time.
     *
     * @return nearest scheduled time, or null if none/empty schedule.
     */
    public Long getNearestTime() {
        return nearestTime;
    }

    /**
     * Remove from schedule an execution.
     *
     * @param metricExec to remove
     */
    public void remove(MetricExec metricExec) {
        for (Map.Entry<Long, List<MetricExec>> entry : timeHandleMap.entrySet()) {
            entry.getValue().remove(metricExec);
        }
    }
}
