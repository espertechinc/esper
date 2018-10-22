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

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonCache;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.historical.database.core.ColumnSettings;
import com.espertech.esper.common.internal.epl.historical.database.core.ConnectionCache;
import com.espertech.esper.common.internal.epl.historical.database.core.ConnectionCacheImpl;
import com.espertech.esper.common.internal.epl.historical.database.core.ConnectionCacheNoCacheImpl;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCache;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation provides database instance services such as connection factory and
 * connection settings.
 */
public class DatabaseConfigServiceImpl implements DatabaseConfigServiceCompileTime, DatabaseConfigServiceRuntime {
    private final Map<String, ConfigurationCommonDBRef> mapDatabaseRef;
    private final Map<String, DatabaseConnectionFactory> connectionFactories;
    private final ClasspathImportService classpathImportService;

    /**
     * Ctor.
     *
     * @param mapDatabaseRef         is a map of database name and database configuration entries
     * @param classpathImportService imports
     */
    public DatabaseConfigServiceImpl(Map<String, ConfigurationCommonDBRef> mapDatabaseRef,
                                     ClasspathImportService classpathImportService) {
        this.mapDatabaseRef = mapDatabaseRef;
        this.connectionFactories = new HashMap<String, DatabaseConnectionFactory>();
        this.classpathImportService = classpathImportService;
    }

    public DatabaseConnectionFactory getConnectionFactory(String databaseName) throws DatabaseConfigException {
        // check if we already have a reference
        DatabaseConnectionFactory factory = connectionFactories.get(databaseName);
        if (factory != null) {
            return factory;
        }

        ConfigurationCommonDBRef config = mapDatabaseRef.get(databaseName);
        if (config == null) {
            throw new DatabaseConfigException("Cannot locate configuration information for database '" + databaseName + '\'');
        }

        ConfigurationCommonDBRef.ConnectionSettings settings = config.getConnectionSettings();
        if (config.getConnectionFactoryDesc() instanceof ConfigurationCommonDBRef.DriverManagerConnection) {
            ConfigurationCommonDBRef.DriverManagerConnection dmConfig = (ConfigurationCommonDBRef.DriverManagerConnection) config.getConnectionFactoryDesc();
            factory = new DatabaseDMConnFactory(dmConfig, settings, classpathImportService);
        } else if (config.getConnectionFactoryDesc() instanceof ConfigurationCommonDBRef.DataSourceConnection) {
            ConfigurationCommonDBRef.DataSourceConnection dsConfig = (ConfigurationCommonDBRef.DataSourceConnection) config.getConnectionFactoryDesc();
            factory = new DatabaseDSConnFactory(dsConfig, settings);
        } else if (config.getConnectionFactoryDesc() instanceof ConfigurationCommonDBRef.DataSourceFactory) {
            ConfigurationCommonDBRef.DataSourceFactory dsConfig = (ConfigurationCommonDBRef.DataSourceFactory) config.getConnectionFactoryDesc();
            factory = new DatabaseDSFactoryConnFactory(dsConfig, settings, classpathImportService);
        } else if (config.getConnectionFactoryDesc() == null) {
            throw new DatabaseConfigException("No connection factory setting provided in configuration");
        } else {
            throw new DatabaseConfigException("Unknown connection factory setting provided in configuration");
        }

        connectionFactories.put(databaseName, factory);

        return factory;
    }

    public ColumnSettings getQuerySetting(String databaseName) throws DatabaseConfigException {
        ConfigurationCommonDBRef config = mapDatabaseRef.get(databaseName);
        if (config == null) {
            throw new DatabaseConfigException("Cannot locate configuration information for database '" + databaseName + '\'');
        }
        return new ColumnSettings(config.getMetadataRetrievalEnum(), config.getColumnChangeCase(), config.getSqlTypesMapping());
    }

    public HistoricalDataCache getDataCache(String databaseName, AgentInstanceContext agentInstanceContext, int streamNumber, int scheduleCallbackId) throws DatabaseConfigException {
        ConfigurationCommonDBRef config = mapDatabaseRef.get(databaseName);
        if (config == null) {
            throw new DatabaseConfigException("Cannot locate configuration information for database '" + databaseName + '\'');
        }

        ConfigurationCommonCache dataCacheDesc = config.getDataCacheDesc();
        return agentInstanceContext.getHistoricalDataCacheFactory().getDataCache(dataCacheDesc, agentInstanceContext, streamNumber, scheduleCallbackId);
    }

    public ConnectionCache getConnectionCache(String databaseName, String preparedStatementText) throws DatabaseConfigException {
        ConfigurationCommonDBRef config = mapDatabaseRef.get(databaseName);
        if (config == null) {
            throw new DatabaseConfigException("Cannot locate configuration information for database '" + databaseName + '\'');
        }

        DatabaseConnectionFactory connectionFactory = getConnectionFactory(databaseName);

        boolean retain = config.getConnectionLifecycleEnum().equals(ConfigurationCommonDBRef.ConnectionLifecycleEnum.RETAIN);
        if (retain) {
            return new ConnectionCacheImpl(connectionFactory, preparedStatementText);
        } else {
            return new ConnectionCacheNoCacheImpl(connectionFactory, preparedStatementText);
        }
    }
}
