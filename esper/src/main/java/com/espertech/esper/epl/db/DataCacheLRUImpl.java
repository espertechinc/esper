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

import com.espertech.esper.epl.join.table.EventTable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Query result data cache implementation that uses a least-recently-used algorithm
 * to store and evict query results.
 */
public class DataCacheLRUImpl implements DataCache {
    private final int cacheSize;
    private static final float HASH_TABLE_LOAD_FACTOR = 0.75f;
    private final LinkedHashMap<Object, EventTable[]> cache;

    /**
     * Ctor.
     *
     * @param cacheSize is the maximum cache size
     */
    public DataCacheLRUImpl(int cacheSize) {
        this.cacheSize = cacheSize;
        int hashTableCapacity = (int) Math.ceil(cacheSize / HASH_TABLE_LOAD_FACTOR) + 1;
        this.cache = new LinkedHashMap<Object, EventTable[]>(hashTableCapacity, HASH_TABLE_LOAD_FACTOR, true) {
            private static final long serialVersionUID = 1;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Object, EventTable[]> eldest) {
                return size() > DataCacheLRUImpl.this.cacheSize;
            }
        };
    }

    /**
     * Retrieves an entry from the cache.
     * The retrieved entry becomes the MRU (most recently used) entry.
     *
     * @param methodParams the key whose associated value is to be returned.
     * @return the value associated to this key, or null if no value with this key exists in the cache.
     */
    public EventTable[] getCached(Object[] methodParams, int numInputParameters) {
        Object key = DataCacheUtil.getLookupKey(methodParams, numInputParameters);
        return cache.get(key);
    }

    /**
     * Adds an entry to this cache.
     * If the cache is full, the LRU (least recently used) entry is dropped.
     *  @param methodParams  the keys with which the specified value is to be associated.
     * @param rows a value to be associated with the specified key.
     */
    public synchronized void put(Object[] methodParams, int numLookupKeys, EventTable[] rows) {
        Object key = DataCacheUtil.getLookupKey(methodParams, numLookupKeys);
        cache.put(key, rows);
    }

    /**
     * Returns the maximum cache size.
     *
     * @return maximum cache size
     */
    public int getCacheSize() {
        return cacheSize;
    }

    public boolean isActive() {
        return true;
    }

    public void destroy() {
    }
}
