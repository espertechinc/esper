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

/**
 * Implementations serve as caches for historical or reference data retrieved
 * via lookup keys consisting or one or more rows represented by a list of events.
 */
public interface DataCache {
    /**
     * Ask the cache if the keyed value is cached, returning a list or rows if the key is in the cache,
     * or returning null to indicate no such key cached. Zero rows may also be cached.
     *
     * @param methodParams is the keys to look up in the cache
     * @param numLookupKeys number of method param keys, from the start, that are for cache lookup
     * @return a list of rows that can be empty is the key was found in the cache, or null if
     * the key is not found in the cache
     */
    public EventTable[] getCached(Object[] methodParams, int numLookupKeys);

    /**
     * Puts into the cache a key and a list of rows, or an empty list if zero rows.
     * <p>
     * The put method is designed to be called when the cache does not contain a key as
     * determined by the get method. Implementations typically simply overwrite
     * any keys put into the cache that already existed in the cache.
     * @param methodParams is the keys to the cache entry
     * @param numLookupKeys number of method param keys, from the start, that are for cache lookup
     * @param rows       is a number of rows
     */
    public void put(Object[] methodParams, int numLookupKeys, EventTable[] rows);

    /**
     * Returns true if the cache is active and currently caching, or false if the cache is inactive and not currently caching
     *
     * @return true for caching enabled, false for no caching taking place
     */
    public boolean isActive();

    void destroy();
}
