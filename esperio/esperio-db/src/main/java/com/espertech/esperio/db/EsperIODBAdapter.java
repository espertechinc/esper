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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceImpl;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConnectionFactory;
import com.espertech.esper.common.internal.util.SQLTypeMapUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import com.espertech.esperio.db.config.*;
import com.espertech.esperio.db.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EsperIODBAdapter {
    private final static Logger log = LoggerFactory.getLogger(EsperIODBAdapter.class);

    private final ConfigurationDBAdapter config;
    private final String runtimeURI;
    private List<String> deployments = new ArrayList<>();

    private DatabaseConfigServiceImpl databaseConfigSvc;
    private ExecutorServices executorFactory;

    /**
     * Quickstart constructor.
     *
     * @param config     configuration
     * @param runtimeURI runtime URI
     */
    public EsperIODBAdapter(ConfigurationDBAdapter config, String runtimeURI) {
        this.config = config;
        this.runtimeURI = runtimeURI;
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
            log.info("Starting EsperIO DB Adapter for runtime URI '" + runtimeURI + "'");
        }

        EPRuntimeSPI runtime = (EPRuntimeSPI) EPRuntimeProvider.getRuntime(runtimeURI);

        databaseConfigSvc = new DatabaseConfigServiceImpl(config.getJdbcConnections(), runtime.getServicesContext().getClasspathImportServiceRuntime());
        executorFactory = new ExecutorServices(runtime, config.getExecutors());

        // Handle Upserts
        for (UpsertQuery upsert : config.getUpsertQueries()) {
            RunnableUpsertFactory upsertFactory = getUpsertFactory(runtime, upsert, databaseConfigSvc);

            try {
                EsperIODBUpdateListener subs = new EsperIODBUpdateListener(upsertFactory, executorFactory.getConfiguredExecutor(upsert.getExecutorName()));
                EPDeployment deployment = compileDeploySubscription(runtime, upsert.getStream(), upsertFactory.getContext().getName());
                deployments.add(deployment.getDeploymentId());
                deployment.getStatements()[0].addListener(subs);
            } catch (Throwable t) {
                log.error("Error starting Upsert query '" + upsertFactory.getContext().getName() + "'" + t.getMessage(), t);
            }
        }

        // Handle DML
        for (DMLQuery dml : config.getDmlQueries()) {
            RunnableDMLFactory dmlFactory = getDMLFactory(runtime, dml, databaseConfigSvc);

            try {
                EsperIODBUpdateListener subs = new EsperIODBUpdateListener(dmlFactory, executorFactory.getConfiguredExecutor(dml.getExecutorName()));
                EPDeployment deployment = compileDeploySubscription(runtime, dml.getStream(), dmlFactory.getContext().getName());
                deployments.add(deployment.getDeploymentId());
                deployment.getStatements()[0].addListener(subs);
            } catch (Throwable t) {
                log.error("Error starting DML query '" + dmlFactory.getContext().getName() + "'" + t.getMessage(), t);
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Completed starting EsperIO DB Adapter for runtime URI '" + runtimeURI + "'.");
        }
    }

    private EPDeployment compileDeploySubscription(EPRuntimeSPI runtime, String eventTypeName, String name) {
        try {
            String epl = "@name('" + name + "') select * from " + eventTypeName;
            CompilerArguments args = new CompilerArguments(runtime.getConfigurationDeepCopy());
            args.getPath().add(runtime.getRuntimePath());
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            return runtime.getDeploymentService().deploy(compiled);
        } catch (Exception ex) {
            throw new EPException("Failed to compile and deploy subscription: " + ex.getMessage(), ex);
        }
    }

    private RunnableUpsertFactory getUpsertFactory(EPRuntimeSPI runtime, UpsertQuery upsert, DatabaseConfigServiceImpl databaseConfigSvc) {
        String upsertName = upsert.getName();
        if (upsertName == null) {
            upsertName = "Upsert against table '" + upsert.getTableName() + "'";
        }
        final String finalUpsertName = upsertName;

        EventType eventType = runtime.getServicesContext().getEventTypeRepositoryBus().getNameToTypeMap().get(upsert.getStream());
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

    private RunnableDMLFactory getDMLFactory(EPRuntimeSPI runtime, DMLQuery dmlQuery, DatabaseConfigServiceImpl databaseConfigSvc) {
        String dmlName = dmlQuery.getName();
        if (dmlName == null) {
            dmlName = "DML '" + dmlQuery.getSql();
        }
        final String finalDmlName = dmlName;

        EventType eventType = runtime.getServicesContext().getEventTypeRepositoryBus().getNameToTypeMap().get(dmlQuery.getStream());
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

        EPRuntimeSPI runtime = (EPRuntimeSPI) EPRuntimeProvider.getRuntime(runtimeURI);
        for (String deployment : deployments) {
            try {
                runtime.getDeploymentService().undeploy(deployment);
            } catch (EPUndeployException e) {
                throw new EPException("Failed to undeploy: " + e.getMessage(), e);
            }
        }

        executorFactory.destroy();
    }
}
