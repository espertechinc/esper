/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.db;

import com.espertech.esper.epl.join.table.EventTable;

import java.util.HashMap;
import java.util.Map;

/**
 * For use in iteration over historical joins, a {@link DataCache} implementation
 * that serves to hold EventBean rows generated during a join evaluation
 * involving historical streams stable for the same cache lookup keys.
 */
public class DataCacheClearableMap implements DataCache
{
    private Map<Object, EventTable[]> cache;

    /**
     * Ctor.
     */
    public DataCacheClearableMap()
    {
        this.cache = new HashMap<Object, EventTable[]>();
    }

    public EventTable[] getCached(Object[] lookupKeys)
    {
        Object key = DataCacheUtil.getLookupKey(lookupKeys);
        return cache.get(key);
    }

    public void put(Object[] lookupKeys, EventTable[] rows)
    {
        Object key = DataCacheUtil.getLookupKey(lookupKeys);
        cache.put(key, rows);
    }

    public boolean isActive()
    {
        return false;
    }

    /**
     * Clears the cache.
     */
    public void clear()
    {
        cache.clear();
    }

    public void destroy() {
    }
}
