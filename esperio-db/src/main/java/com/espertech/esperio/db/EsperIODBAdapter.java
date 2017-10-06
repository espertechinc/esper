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
package com.espertech.esperio.db;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.epl.db.DatabaseConfigServiceImpl;
import com.espertech.esper.epl.db.DatabaseConnectionFactory;
import com.espertech.esper.util.SQLTypeMapUtil;
import com.espertech.esperio.db.config.*;
import com.espertech.esperio.db.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class EsperIODBAdapter {
    private final static Logger log = LoggerFactory.getLogger(EsperIODBAdapter.class);

    private final ConfigurationDBAdapter config;
    private final String engineURI;

    private DatabaseConfigServiceImpl databaseConfigSvc;
    private ExecutorServices executorFactory;

    /**
     * Quickstart constructor.
     *
     * @param config    configuration
     * @param engineURI engine uri
     */
    public EsperIODBAdapter(ConfigurationDBAdapter config, String engineURI) {
        this.config = config;
        this.engineURI = engineURI;
    }

    /**
     * Re-initialize DDS endpoint.
     */
    public void initialize() {
    }

    /**
     * Start the DDS endpoint.
     */
    public synchronized void start() {
        if (log.isInfoEnabled()) {
            log.info("Starting EsperIO DB Adapter for engine URI '" + engineURI + "'");
        }

        EPServiceProviderSPI engineSPI = (EPServiceProviderSPI) EPServiceProviderManager.getProvider(engineURI);

        databaseConfigSvc = new DatabaseConfigServiceImpl(config.getJdbcConnections(), null, null, engineSPI.getEngineImportService());
        executorFactory = new ExecutorServices(engineSPI, config.getExecutors());

        // Handle Upserts
        for (UpsertQuery upsert : config.getUpsertQueries()) {
            RunnableUpsertFactory upsertFactory = getUpsertFactory(engineSPI, upsert, databaseConfigSvc);

            try {
                EsperIODBBaseSubscription subs = new EsperIODBBaseSubscription(upsertFactory, executorFactory.getConfiguredExecutor(upsert.getExecutorName()));
                subs.seteventTypeName(upsert.getStream());
                subs.setSubscriptionName(upsertFactory.getContext().getName());
                subs.registerAdapter(engineSPI);
            } catch (Throwable t) {
                log.error("Error starting Upsert query '" + upsertFactory.getContext().getName() + "'" + t.getMessage(), t);
            }
        }

        // Handle DML
        for (DMLQuery dml : config.getDmlQueries()) {
            RunnableDMLFactory dmlFactory = getDMLFactory(engineSPI, dml, databaseConfigSvc);

            try {
                EsperIODBBaseSubscription subs = new EsperIODBBaseSubscription(dmlFactory, executorFactory.getConfiguredExecutor(dml.getExecutorName()));
                subs.seteventTypeName(dml.getStream());
                subs.setSubscriptionName(dmlFactory.getContext().getName());
                subs.registerAdapter(engineSPI);
            } catch (Throwable t) {
                log.error("Error starting DML query '" + dmlFactory.getContext().getName() + "'" + t.getMessage(), t);
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Completed starting EsperIO DB Adapter for engine URI '" + engineURI + "'.");
        }
    }

    private RunnableUpsertFactory getUpsertFactory(EPServiceProviderSPI engineSPI, UpsertQuery upsert, DatabaseConfigServiceImpl databaseConfigSvc) {
        String upsertName = upsert.getName();
        if (upsertName == null) {
            upsertName = "Upsert against table '" + upsert.getTableName() + "'";
        }
        final String finalUpsertName = upsertName;

        EventType eventType = engineSPI.getEventAdapterService().getExistsTypeByName(upsert.getStream());
        if (eventType == null) {
            throw new ConfigurationException("Event type by name '" + upsert.getStream() + "' not found");
        }

        try {
            DatabaseConnectionFactory connectionFactory = databaseConfigSvc.getConnectionFactory(upsert.getConnection());

            String[] keys = new String[upsert.getKeys().size()];
            int[] keyTypes = new int[upsert.getKeys().size()];
            EventPropertyGetter[] keyGetters = new EventPropertyGetter[upsert.getKeys().size()];

            int index = 0;
            for (Column key : upsert.getKeys()) {
                keys[index] = key.getColumn();
                keyTypes[index] = SQLTypeMapUtil.getSQLTypeByName(key.getType());
                keyGetters[index] = eventType.getGetter(key.getProperty());
                if (keyGetters[index] == null) {
                    throw new ConfigurationException("Property name '" + key.getProperty() + "' not found for type '" + eventType + "'");
                }
                index++;
            }

            String[] values = new String[upsert.getValues().size()];
            int[] valueTypes = new int[upsert.getValues().size()];
            EventPropertyGetter[] valueGetters = new EventPropertyGetter[upsert.getValues().size()];

            index = 0;
            for (Column value : upsert.getValues()) {
                values[index] = value.getColumn();
                valueTypes[index] = SQLTypeMapUtil.getSQLTypeByName(value.getType());
                valueGetters[index] = eventType.getGetter(value.getProperty());

                if (valueGetters[index] == null) {
                    throw new ConfigurationException("Property name '" + value.getProperty() + "' not found for type '" + eventType + "'");
                }
                index++;
            }

            StoreExceptionHandler handler = new StoreExceptionHandler() {
                public void handle(String message, SQLException ex) {
                    log.error("Error executing '" + finalUpsertName + "'");
                }
            };

            MultiKeyMultiValueTable table = new MultiKeyMultiValueTable(upsert.getTableName(), keys, keyTypes,
                    values, valueTypes, handler);

            RunnableUpsertContext context = new RunnableUpsertContext(upsertName, connectionFactory, table, keyGetters, valueGetters, upsert.getRetry(), upsert.getRetryIntervalSec());
            return new RunnableUpsertFactory(context);
        } catch (ConfigurationException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new ConfigurationException("Error configuring " + upsertName + " :" + t.getMessage());
        }
    }

    private RunnableDMLFactory getDMLFactory(EPServiceProviderSPI engineSPI, DMLQuery dmlQuery, DatabaseConfigServiceImpl databaseConfigSvc) {
        String dmlName = dmlQuery.getName();
        if (dmlName == null) {
            dmlName = "DML '" + dmlQuery.getSql();
        }
        final String finalDmlName = dmlName;

        EventType eventType = engineSPI.getEventAdapterService().getExistsTypeByName(dmlQuery.getStream());
        if (eventType == null) {
            throw new ConfigurationException("Event type by name '" + dmlQuery.getStream() + "' not found");
        }

        try {
            DatabaseConnectionFactory connectionFactory = databaseConfigSvc.getConnectionFactory(dmlQuery.getConnection());

            Map<Integer, BindingEntry> bindings = new HashMap<Integer, BindingEntry>();
            for (BindingParameter theParams : dmlQuery.getBindings()) {
                EventPropertyGetter valueGetter = eventType.getGetter(theParams.getPropertyName());
                bindings.put(theParams.getPosition(), new BindingEntry(valueGetter));
            }

            StoreExceptionHandler handler = new StoreExceptionHandler() {
                public void handle(String message, SQLException ex) {
                    log.error("Error executing '" + finalDmlName + "'");
                }
            };

            DMLStatement dmlStmt = new DMLStatement(handler, dmlQuery.getSql(), bindings);

            RunnableDMLContext context = new RunnableDMLContext(dmlName, connectionFactory, dmlStmt, dmlQuery.getRetry(), dmlQuery.getRetryIntervalSec());
            return new RunnableDMLFactory(context);
        } catch (ConfigurationException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new ConfigurationException("Error configuring " + dmlName + " :" + t.getMessage());
        }
    }

    /**
     * Destroy the adapter.
     */
    public synchronized void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying Esper DB Adapter");
        }

        executorFactory.destroy();
    }
}
