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
package com.espertech.esper.common.internal.epl.historical.database.connection;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.historical.database.core.ConnectionCache;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCache;

/**
 * Service providing database connection factory and configuration information
 * for use with historical data polling.
 */
public interface DatabaseConfigServiceRuntime extends DatabaseConfigService {
    /**
     * Returns a new cache implementation for this database.
     *
     * @param databaseName         is the name of the database to return a new cache implementation for for
     * @param agentInstanceContext agent instance context
     * @param streamNumber         stream number
     * @param scheduleCallbackId   callback id
     * @return cache implementation
     * @throws DatabaseConfigException is thrown to indicate database configuration errors
     */
    HistoricalDataCache getDataCache(String databaseName, AgentInstanceContext agentInstanceContext, int streamNumber, int scheduleCallbackId) throws DatabaseConfigException;

    ConnectionCache getConnectionCache(String databaseName, String preparedStatementText) throws DatabaseConfigException;
}
