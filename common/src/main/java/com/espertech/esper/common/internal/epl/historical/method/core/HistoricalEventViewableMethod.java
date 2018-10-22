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
package com.espertech.esper.common.internal.epl.historical.method.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonCache;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonMethodRef;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewableBase;
import com.espertech.esper.common.internal.epl.historical.execstrategy.PollExecStrategy;

public class HistoricalEventViewableMethod extends HistoricalEventViewableBase {

    public HistoricalEventViewableMethod(HistoricalEventViewableMethodFactory factory, PollExecStrategy pollExecStrategy, AgentInstanceContext agentInstanceContext) {
        super(factory, pollExecStrategy, agentInstanceContext);

        try {
            ConfigurationCommonMethodRef configCache = agentInstanceContext.getClasspathImportServiceRuntime().getConfigurationMethodRef(factory.getConfigurationName());
            ConfigurationCommonCache dataCacheDesc = configCache != null ? configCache.getDataCacheDesc() : null;
            this.dataCache = agentInstanceContext.getHistoricalDataCacheFactory().getDataCache(dataCacheDesc, agentInstanceContext, factory.getStreamNumber(), factory.getScheduleCallbackId());
        } catch (Throwable t) {
            throw new EPException("Failed to obtain cache: " + t.getMessage(), t);
        }
    }
}
