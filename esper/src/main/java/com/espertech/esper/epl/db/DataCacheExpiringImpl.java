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
package com.espertech.esper.epl.db;

import com.espertech.esper.client.ConfigurationCacheReferenceType;
import com.espertech.esper.collection.apachecommons.ReferenceMap;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.schedule.SchedulingService;

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
public class DataCacheExpiringImpl implements DataCache, ScheduleHandleCallback {
    private final double maxAgeSec;
    private final double purgeIntervalSec;
    private final SchedulingService schedulingService;
    private final long scheduleSlot;
    private final Map<Object, Item> cache;
    private final EPStatementAgentInstanceHandle epStatementAgentInstanceHandle;
    private final TimeAbacus timeAbacus;

    private boolean isScheduled;

    /**
     * Ctor.
     *
     * @param maxAgeSec                      is the maximum age in seconds
     * @param purgeIntervalSec               is the purge interval in seconds
     * @param cacheReferenceType             indicates whether hard, soft or weak references are used in the cache
     * @param schedulingService              is a service for call backs at a scheduled time, for purging
     * @param scheduleSlot                   slot for scheduling callbacks for this cache
     * @param epStatementAgentInstanceHandle is the statements-own handle for use in registering callbacks with services
     * @param timeAbacus                     time abacus
     */
    public DataCacheExpiringImpl(double maxAgeSec,
                                 double purgeIntervalSec,
                                 ConfigurationCacheReferenceType cacheReferenceType,
                                 SchedulingService schedulingService,
                                 long scheduleSlot,
                                 EPStatementAgentInstanceHandle epStatementAgentInstanceHandle,
                                 TimeAbacus timeAbacus) {
        this.maxAgeSec = maxAgeSec;
        this.purgeIntervalSec = purgeIntervalSec;
        this.schedulingService = schedulingService;
        this.scheduleSlot = scheduleSlot;
        this.timeAbacus = timeAbacus;

        if (cacheReferenceType == ConfigurationCacheReferenceType.HARD) {
            this.cache = new HashMap<Object, Item>();
        } else if (cacheReferenceType == ConfigurationCacheReferenceType.SOFT) {
            this.cache = new ReferenceMap(ReferenceMap.SOFT, ReferenceMap.SOFT);
        } else {
            this.cache = new WeakHashMap<Object, Item>();
        }

        this.epStatementAgentInstanceHandle = epStatementAgentInstanceHandle;
    }

    public EventTable[] getCached(Object[] methodParams, int numLookupKeys) {
        Object key = DataCacheUtil.getLookupKey(methodParams, numLookupKeys);
        Item item = cache.get(key);
        if (item == null) {
            return null;
        }

        long now = schedulingService.getTime();
        long maxAgeMSec = timeAbacus.deltaForSecondsDouble(maxAgeSec);
        if ((now - item.getTime()) > maxAgeMSec) {
            cache.remove(key);
            return null;
        }

        return item.getData();
    }

    public void put(Object[] methodParams, int numLookupKeys, EventTable[] rows) {
        Object key = DataCacheUtil.getLookupKey(methodParams, numLookupKeys);
        long now = schedulingService.getTime();
        Item item = new Item(rows, now);
        cache.put(key, item);

        if (!isScheduled) {
            EPStatementHandleCallback callback = new EPStatementHandleCallback(epStatementAgentInstanceHandle, this);
            schedulingService.add(timeAbacus.deltaForSecondsDouble(purgeIntervalSec), callback, scheduleSlot);
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

    public void scheduledTrigger(EngineLevelExtensionServicesContext engineLevelExtensionServicesContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qHistoricalScheduledEval();
        }
        // purge expired
        long now = schedulingService.getTime();
        Iterator<Object> it = cache.keySet().iterator();
        long maxAgeMSec = timeAbacus.deltaForSecondsDouble(maxAgeSec);
        for (; it.hasNext(); ) {
            Item item = cache.get(it.next());
            if ((now - item.getTime()) > maxAgeMSec) {
                it.remove();
            }
        }

        isScheduled = false;
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aHistoricalScheduledEval();
        }
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
