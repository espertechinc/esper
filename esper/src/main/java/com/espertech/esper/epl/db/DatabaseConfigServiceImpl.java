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

import com.espertech.esper.client.ConfigurationDBRef;
import com.espertech.esper.client.ConfigurationDataCache;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.schedule.ScheduleBucket;
import com.espertech.esper.schedule.SchedulingService;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation provides database instance services such as connection factory and
 * connection settings.
 */
public class DatabaseConfigServiceImpl implements DatabaseConfigService {
    private final Map<String, ConfigurationDBRef> mapDatabaseRef;
    private final Map<String, DatabaseConnectionFactory> connectionFactories;
    private final SchedulingService schedulingService;
    private final ScheduleBucket scheduleBucket;
    private final EngineImportService engineImportService;

    /**
     * Ctor.
     *
     * @param mapDatabaseRef    is a map of database name and database configuration entries
     * @param schedulingService is for scheduling callbacks for a cache
     * @param scheduleBucket    is a system bucket for all scheduling callbacks for caches
     * @param engineImportService engine imports
     */
    public DatabaseConfigServiceImpl(Map<String, ConfigurationDBRef> mapDatabaseRef,
                                     SchedulingService schedulingService,
                                     ScheduleBucket scheduleBucket,
                                     EngineImportService engineImportService) {
        this.mapDatabaseRef = mapDatabaseRef;
        this.connectionFactories = new HashMap<String, DatabaseConnectionFactory>();
        this.schedulingService = schedulingService;
        this.scheduleBucket = scheduleBucket;
        this.engineImportService = engineImportService;
    }

    public ConnectionCache getConnectionCache(String databaseName, String preparedStatementText) throws DatabaseConfigException {
        ConfigurationDBRef config = mapDatabaseRef.get(databaseName);
        if (config == null) {
            throw new DatabaseConfigException("Cannot locate configuration information for database '" + databaseName + '\'');
        }

        DatabaseConnectionFactory connectionFactory = getConnectionFactory(databaseName);

        boolean retain = config.getConnectionLifecycleEnum().equals(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        if (retain) {
            return new ConnectionCacheImpl(connectionFactory, preparedStatementText);
        } else {
            return new ConnectionNoCacheImpl(connectionFactory, preparedStatementText);
        }
    }

    public DatabaseConnectionFactory getConnectionFactory(String databaseName) throws DatabaseConfigException {
        // check if we already have a reference
        DatabaseConnectionFactory factory = connectionFactories.get(databaseName);
        if (factory != null) {
            return factory;
        }

        ConfigurationDBRef config = mapDatabaseRef.get(databaseName);
        if (config == null) {
            throw new DatabaseConfigException("Cannot locate configuration information for database '" + databaseName + '\'');
        }

        ConfigurationDBRef.ConnectionSettings settings = config.getConnectionSettings();
        if (config.getConnectionFactoryDesc() instanceof ConfigurationDBRef.DriverManagerConnection) {
            ConfigurationDBRef.DriverManagerConnection dmConfig = (ConfigurationDBRef.DriverManagerConnection) config.getConnectionFactoryDesc();
            factory = new DatabaseDMConnFactory(dmConfig, settings, engineImportService);
        } else if (config.getConnectionFactoryDesc() instanceof ConfigurationDBRef.DataSourceConnection) {
            ConfigurationDBRef.DataSourceConnection dsConfig = (ConfigurationDBRef.DataSourceConnection) config.getConnectionFactoryDesc();
            factory = new DatabaseDSConnFactory(dsConfig, settings);
        } else if (config.getConnectionFactoryDesc() instanceof ConfigurationDBRef.DataSourceFactory) {
            ConfigurationDBRef.DataSourceFactory dsConfig = (ConfigurationDBRef.DataSourceFactory) config.getConnectionFactoryDesc();
            factory = new DatabaseDSFactoryConnFactory(dsConfig, settings, engineImportService);
        } else if (config.getConnectionFactoryDesc() == null) {
            throw new DatabaseConfigException("No connection factory setting provided in configuration");
        } else {
            throw new DatabaseConfigException("Unknown connection factory setting provided in configuration");
        }

        connectionFactories.put(databaseName, factory);

        return factory;
    }

    public DataCache getDataCache(String databaseName, StatementContext statementContext, EPStatementAgentInstanceHandle epStatementAgentInstanceHandle, DataCacheFactory dataCacheFactory, int streamNumber) throws DatabaseConfigException {
        ConfigurationDBRef config = mapDatabaseRef.get(databaseName);
        if (config == null) {
            throw new DatabaseConfigException("Cannot locate configuration information for database '" + databaseName + '\'');
        }

        ConfigurationDataCache dataCacheDesc = config.getDataCacheDesc();
        return dataCacheFactory.getDataCache(dataCacheDesc, statementContext, epStatementAgentInstanceHandle, schedulingService, scheduleBucket, streamNumber);
    }

    public ColumnSettings getQuerySetting(String databaseName) throws DatabaseConfigException {
        ConfigurationDBRef config = mapDatabaseRef.get(databaseName);
        if (config == null) {
            throw new DatabaseConfigException("Cannot locate configuration information for database '" + databaseName + '\'');
        }
        return new ColumnSettings(config.getMetadataRetrievalEnum(), config.getColumnChangeCase(), config.getSqlTypesMapping());
    }
}
