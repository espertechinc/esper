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
package com.espertech.esper.common.internal.epl.historical.database.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewableBase;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigException;
import com.espertech.esper.common.internal.epl.historical.execstrategy.PollExecStrategy;

public class HistoricalEventViewableDatabase extends HistoricalEventViewableBase {

    public HistoricalEventViewableDatabase(HistoricalEventViewableDatabaseFactory factory, PollExecStrategy pollExecStrategy, AgentInstanceContext agentInstanceContext) {
        super(factory, pollExecStrategy, agentInstanceContext);
        try {
            this.dataCache = agentInstanceContext.getDatabaseConfigService().getDataCache(factory.databaseName,
                    agentInstanceContext, factory.getStreamNumber(), factory.getScheduleCallbackId());
        } catch (DatabaseConfigException e) {
            throw new EPException("Failed to obtain cache: " + e.getMessage(), e);
        }
    }
}
