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
package com.espertech.esper.client;

import java.io.Serializable;

/**
 * Holds configuration information for data caches for use in method invocations in the from-clause.
 */
public class ConfigurationMethodRef implements Serializable {
    private ConfigurationDataCache dataCacheDesc;
    private static final long serialVersionUID = -9178934436665140004L;

    /**
     * Configures a LRU cache of the given size for the method invocation.
     *
     * @param size is the maximum number of entries before method invocation results are evicted
     */
    public void setLRUCache(int size) {
        dataCacheDesc = new ConfigurationLRUCache(size);
    }

    /**
     * Configures an expiry-time cache of the given maximum age in seconds and purge interval in seconds.
     * <p>
     * Specifies the cache reference type to be weak references. Weak reference cache entries become
     * eligible for garbage collection and are removed from cache when the garbage collection requires so.
     *
     * @param maxAgeSeconds        is the maximum number of seconds before a method invocation result is considered stale (also known as time-to-live)
     * @param purgeIntervalSeconds is the interval at which the engine purges stale data from the cache
     */
    public void setExpiryTimeCache(double maxAgeSeconds, double purgeIntervalSeconds) {
        dataCacheDesc = new ConfigurationExpiryTimeCache(maxAgeSeconds, purgeIntervalSeconds, ConfigurationCacheReferenceType.getDefault());
    }

    /**
     * Configures an expiry-time cache of the given maximum age in seconds and purge interval in seconds. Also allows
     * setting the reference type indicating whether garbage collection may remove entries from cache.
     *
     * @param maxAgeSeconds        is the maximum number of seconds before a method invocation result is considered stale (also known as time-to-live)
     * @param purgeIntervalSeconds is the interval at which the engine purges stale data from the cache
     * @param cacheReferenceType   specifies the reference type to use
     */
    public void setExpiryTimeCache(double maxAgeSeconds, double purgeIntervalSeconds, ConfigurationCacheReferenceType cacheReferenceType) {
        dataCacheDesc = new ConfigurationExpiryTimeCache(maxAgeSeconds, purgeIntervalSeconds, cacheReferenceType);
    }

    /**
     * Return a method invocation result data cache descriptor.
     *
     * @return cache descriptor
     */
    public ConfigurationDataCache getDataCacheDesc() {
        return dataCacheDesc;
    }
}
