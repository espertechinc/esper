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

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonCache;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonCacheExpiryTime;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonCacheLRU;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;

/**
 * Factory for data caches for use caching database query results and method invocation results.
 */
public class HistoricalDataCacheFactory {

    /**
     * Creates a cache implementation for the strategy as defined by the cache descriptor.
     *
     * @param cacheDesc            cache descriptor
     * @param agentInstanceContext agent instance context
     * @param streamNum            stream number
     * @param scheduleCallbackId   callback id
     * @return data cache implementation
     */
    public HistoricalDataCache getDataCache(ConfigurationCommonCache cacheDesc,
                                            AgentInstanceContext agentInstanceContext,
                                            int streamNum,
                                            int scheduleCallbackId) {
        if (cacheDesc == null) {
            return new HistoricalDataCacheNullImpl();
        }

        if (cacheDesc instanceof ConfigurationCommonCacheLRU) {
            ConfigurationCommonCacheLRU lruCache = (ConfigurationCommonCacheLRU) cacheDesc;
            return new HistoricalDataCacheLRUImpl(lruCache.getSize());
        }

        if (cacheDesc instanceof ConfigurationCommonCacheExpiryTime) {
            ConfigurationCommonCacheExpiryTime expCache = (ConfigurationCommonCacheExpiryTime) cacheDesc;
            return makeTimeCache(expCache, agentInstanceContext, streamNum, scheduleCallbackId);
        }

        throw new IllegalStateException("Cache implementation class not configured");
    }

    protected HistoricalDataCache makeTimeCache(ConfigurationCommonCacheExpiryTime expCache, AgentInstanceContext agentInstanceContext, int streamNum, int scheduleCallbackId) {
        return new HistoricalDataCacheExpiringImpl(expCache.getMaxAgeSeconds(), expCache.getPurgeIntervalSeconds(), expCache.getCacheReferenceType(),
                agentInstanceContext, agentInstanceContext.getScheduleBucket().allocateSlot());
    }
}
