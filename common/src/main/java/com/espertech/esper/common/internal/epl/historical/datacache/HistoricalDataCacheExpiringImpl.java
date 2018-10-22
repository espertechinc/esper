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
package com.espertech.esper.common.internal.epl.historical.datacache;

import com.espertech.esper.common.client.util.CacheReferenceType;
import com.espertech.esper.common.internal.collection.apachecommons.ReferenceMap;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.schedule.SchedulingService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Implements an expiry-time cache that evicts data when data becomes stale
 * after a given number of seconds.
 * <p>
 * The cache reference type indicates which backing Map is used: Weak type uses the WeakHashMap,
 * Soft type uses the apache commons ReferenceMap, and Hard type simply uses a HashMap.
 */
public class HistoricalDataCacheExpiringImpl implements HistoricalDataCache, ScheduleHandleCallback {
    private final static String NAME_AUDITPROVIDER_SCHEDULE = "historical data-cache";

    private final double maxAgeSec;
    private final double purgeIntervalSec;
    private final AgentInstanceContext agentInstanceContext;
    private final long scheduleSlot;
    private final Map<Object, Item> cache;

    private boolean isScheduled;

    /**
     * Ctor.
     *
     * @param maxAgeSec            is the maximum age in seconds
     * @param purgeIntervalSec     is the purge interval in seconds
     * @param cacheReferenceType   indicates whether hard, soft or weak references are used in the cache
     * @param agentInstanceContext agent instance context
     * @param scheduleSlot         slot for scheduling callbacks for this cache
     */
    public HistoricalDataCacheExpiringImpl(double maxAgeSec,
                                           double purgeIntervalSec,
                                           CacheReferenceType cacheReferenceType,
                                           AgentInstanceContext agentInstanceContext,
                                           long scheduleSlot) {
        this.maxAgeSec = maxAgeSec;
        this.purgeIntervalSec = purgeIntervalSec;
        this.agentInstanceContext = agentInstanceContext;
        this.scheduleSlot = scheduleSlot;

        if (cacheReferenceType == CacheReferenceType.HARD) {
            this.cache = new HashMap<>();
        } else if (cacheReferenceType == CacheReferenceType.SOFT) {
            this.cache = new ReferenceMap(ReferenceMap.SOFT, ReferenceMap.SOFT);
        } else {
            this.cache = new WeakHashMap<>();
        }
    }

    public EventTable[] getCached(Object methodParams) {
        Object key = methodParams;
        Item item = cache.get(key);
        if (item == null) {
            return null;
        }

        long now = agentInstanceContext.getSchedulingService().getTime();
        long maxAgeMSec = agentInstanceContext.getClasspathImportServiceRuntime().getTimeAbacus().deltaForSecondsDouble(maxAgeSec);
        if ((now - item.getTime()) > maxAgeMSec) {
            cache.remove(key);
            return null;
        }

        return item.getData();
    }

    public void put(Object methodParams, EventTable[] rows) {
        SchedulingService schedulingService = agentInstanceContext.getSchedulingService();
        TimeAbacus timeAbacus = agentInstanceContext.getClasspathImportServiceRuntime().getTimeAbacus();

        Object key = methodParams;
        long now = schedulingService.getTime();
        Item item = new Item(rows, now);
        cache.put(key, item);

        if (!isScheduled) {
            EPStatementHandleCallbackSchedule callback = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), this);
            long timeDelta = timeAbacus.deltaForSecondsDouble(purgeIntervalSec);
            agentInstanceContext.getAuditProvider().scheduleAdd(timeDelta, agentInstanceContext, callback, ScheduleObjectType.historicaldatacache, NAME_AUDITPROVIDER_SCHEDULE);
            schedulingService.add(timeDelta, callback, scheduleSlot);
            isScheduled = true;
        }
    }

    /**
     * Returns the maximum age in milliseconds.
     *
     * @return millisecon max age
     */
    protected double getMaxAgeSec() {
        return maxAgeSec;
    }

    public double getPurgeIntervalSec() {
        return purgeIntervalSec;
    }

    public boolean isActive() {
        return true;
    }

    /**
     * Returns the current cache size.
     *
     * @return cache size
     */
    protected long getSize() {
        return cache.size();
    }

    public void scheduledTrigger() {
        agentInstanceContext.getInstrumentationProvider().qHistoricalScheduledEval();

        // purge expired
        agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.historicaldatacache, NAME_AUDITPROVIDER_SCHEDULE);
        long now = agentInstanceContext.getSchedulingService().getTime();
        Iterator<Object> it = cache.keySet().iterator();
        long maxAgeMSec = agentInstanceContext.getClasspathImportServiceRuntime().getTimeAbacus().deltaForSecondsDouble(maxAgeSec);
        for (; it.hasNext(); ) {
            Item item = cache.get(it.next());
            if ((now - item.getTime()) > maxAgeMSec) {
                it.remove();
            }
        }

        isScheduled = false;

        agentInstanceContext.getInstrumentationProvider().aHistoricalScheduledEval();
    }

    public void destroy() {
    }

    private static class Item {
        private EventTable[] data;
        private long time;

        public Item(EventTable[] data, long time) {
            this.data = data;
            this.time = time;
        }

        public EventTable[] getData() {
            return data;
        }

        public long getTime() {
            return time;
        }
    }
}
