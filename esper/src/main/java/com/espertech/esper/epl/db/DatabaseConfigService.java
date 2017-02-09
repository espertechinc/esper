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

import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.StatementContext;

/**
 * Service providing database connection factory and configuration information
 * for use with historical data polling.
 */
public interface DatabaseConfigService {
    /**
     * Returns a connection factory for a configured database.
     *
     * @param databaseName is the name of the database
     * @return is a connection factory to use to get connections to the database
     * @throws DatabaseConfigException is thrown to indicate database configuration errors
     */
    public DatabaseConnectionFactory getConnectionFactory(String databaseName) throws DatabaseConfigException;

    /**
     * Returns the column metadata settings for the database.
     *
     * @param databaseName is the database name
     * @return indicators for change case, metadata retrieval strategy and others
     * @throws DatabaseConfigException if the name was not configured
     */
    public ColumnSettings getQuerySetting(String databaseName) throws DatabaseConfigException;

    /**
     * Returns true to indicate a setting to retain connections between lookups.
     *
     * @param databaseName          is the name of the database
     * @param preparedStatementText is the sql text
     * @return a cache implementation to cache connection and prepared statements
     * @throws DatabaseConfigException is thrown to indicate database configuration errors
     */
    public ConnectionCache getConnectionCache(String databaseName, String preparedStatementText) throws DatabaseConfigException;

    /**
     * Returns a new cache implementation for this database.
     *
     * @param databaseName                   is the name of the database to return a new cache implementation for for
     * @param statementContext               statement context
     * @param epStatementAgentInstanceHandle is the statements-own handle for use in registering callbacks with services
     * @param dataCacheFactory               factory for cache
     * @param streamNumber                   stream number
     * @return cache implementation
     * @throws DatabaseConfigException is thrown to indicate database configuration errors
     */
    public DataCache getDataCache(String databaseName, StatementContext statementContext, EPStatementAgentInstanceHandle epStatementAgentInstanceHandle, DataCacheFactory dataCacheFactory, int streamNumber) throws DatabaseConfigException;
}
