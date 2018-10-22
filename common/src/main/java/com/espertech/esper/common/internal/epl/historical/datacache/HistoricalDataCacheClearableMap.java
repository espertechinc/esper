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

import com.espertech.esper.common.internal.epl.index.base.EventTable;

import java.util.HashMap;
import java.util.Map;

/**
 * For use in iteration over historical joins, a {@link HistoricalDataCache} implementation
 * that serves to hold EventBean rows generated during a join evaluation
 * involving historical streams stable for the same cache lookup keys.
 */
public class HistoricalDataCacheClearableMap implements HistoricalDataCache {
    private Map<Object, EventTable[]> cache;

    /**
     * Ctor.
     */
    public HistoricalDataCacheClearableMap() {
        this.cache = new HashMap<Object, EventTable[]>();
    }

    public EventTable[] getCached(Object methodParams) {
        Object key = methodParams;
        return cache.get(key);
    }

    public void put(Object methodParams, EventTable[] rows) {
        Object key = methodParams;
        cache.put(key, rows);
    }

    public boolean isActive() {
        return false;
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        cache.clear();
    }

    public void destroy() {
    }
}
