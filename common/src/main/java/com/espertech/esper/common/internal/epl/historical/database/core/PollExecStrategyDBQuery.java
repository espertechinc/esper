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
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.type.SQLColumnValueContext;
import com.espertech.esper.common.client.hook.type.SQLInputParameterContext;
import com.espertech.esper.common.client.hook.type.SQLOutputRowValueContext;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.historical.execstrategy.PollExecStrategy;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.util.DatabaseTypeBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Date;
import java.util.*;

/**
 * Viewable providing historical data from a database.
 */
public class PollExecStrategyDBQuery implements PollExecStrategy {
    private static final Logger JDBC_PERF_LOG = LoggerFactory.getLogger(AuditPath.JDBC_LOG);
    private static final Logger log = LoggerFactory.getLogger(PollExecStrategyDBQuery.class);

    private final HistoricalEventViewableDatabaseFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final ConnectionCache connectionCache;
    private Pair<Connection, PreparedStatement> resources;

    public PollExecStrategyDBQuery(HistoricalEventViewableDatabaseFactory factory, AgentInstanceContext agentInstanceContext, ConnectionCache connectionCache) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;
        this.connectionCache = connectionCache;
    }

    public void start() {
        resources = connectionCache.getConnection();
    }

    public void done() {
        connectionCache.doneWith(resources);
    }

    public void destroy() {
        connectionCache.destroy();
    }

    public List<EventBean> poll(Object lookupValues, AgentInstanceContext agentInstanceContext) {
        List<EventBean> result;
        try {
            result = execute(resources.getSecond(), lookupValues);
        } catch (EPException ex) {
            connectionCache.doneWith(resources);
            throw ex;
        }

        return result;
    }

    private synchronized List<EventBean> execute(PreparedStatement preparedStatement,
                                                 Object lookupValuePerStream) {
        boolean hasJDBCLogging = factory.enableJDBCLogging && JDBC_PERF_LOG.isInfoEnabled();

        // set parameters
        SQLInputParameterContext inputParameterContext = null;
        if (factory.columnTypeConversionHook != null) {
            inputParameterContext = new SQLInputParameterContext();
        }

        int count = 1;
        Object[] parameters = null;
        if (hasJDBCLogging) {
            parameters = new Object[factory.inputParameters.length];
        }
        Object[] mk = factory.inputParameters.length == 1 ? null : (Object[]) lookupValuePerStream;
        for (int i = 0; i < factory.inputParameters.length; i++) {
            try {
                Object parameter;
                if (mk == null) {
                    parameter = lookupValuePerStream;
                } else {
                    parameter = mk[i];
                }

                if (factory.columnTypeConversionHook != null) {
                    inputParameterContext.setParameterNumber(i + 1);
                    inputParameterContext.setParameterValue(parameter);
                    parameter = factory.columnTypeConversionHook.getParameterValue(inputParameterContext);
                }

                setObject(preparedStatement, count, parameter);
                if (parameters != null) {
                    parameters[i] = parameter;
                }
            } catch (SQLException ex) {
                throw new EPException("Error setting parameter " + count, ex);
            }

            count++;
        }

        // execute
        ResultSet resultSet;
        if (hasJDBCLogging) {
            long startTimeNS = System.nanoTime();
            long startTimeMS = System.currentTimeMillis();
            try {
                resultSet = preparedStatement.executeQuery();
            } catch (SQLException ex) {
                throw new EPException("Error executing statement '" + factory.preparedStatementText + '\'', ex);
            }
            long endTimeNS = System.nanoTime();
            long endTimeMS = System.currentTimeMillis();
            JDBC_PERF_LOG.info("Statement '" + factory.preparedStatementText + "' delta nanosec " + (endTimeNS - startTimeNS) +
                " delta msec " + (endTimeMS - startTimeMS) +
                " parameters " + Arrays.toString(parameters));
        } else {
            try {
                resultSet = preparedStatement.executeQuery();
            } catch (SQLException ex) {
                throw new EPException("Error executing statement '" + factory.preparedStatementText + '\'', ex);
            }
        }

        // generate events for result set
        List<EventBean> rows = new LinkedList<EventBean>();
        try {
            SQLColumnValueContext valueContext = null;
            if (factory.columnTypeConversionHook != null) {
                valueContext = new SQLColumnValueContext();
            }

            SQLOutputRowValueContext rowContext = null;
            if (factory.outputRowConversionHook != null) {
                rowContext = new SQLOutputRowValueContext();
            }

            int rowNum = 0;
            while (resultSet.next()) {
                int colNum = 1;
                Map<String, Object> row = new HashMap<String, Object>();
                for (Map.Entry<String, DBOutputTypeDesc> entry : factory.outputTypes.entrySet()) {
                    String columnName = entry.getKey();

                    Object value;
                    DatabaseTypeBinding binding = entry.getValue().getOptionalBinding();
                    if (binding != null) {
                        value = binding.getValue(resultSet, columnName);
                    } else {
                        value = resultSet.getObject(columnName);
                    }

                    if (factory.columnTypeConversionHook != null) {
                        valueContext.setColumnName(columnName);
                        valueContext.setColumnNumber(colNum);
                        valueContext.setColumnValue(value);
                        valueContext.setResultSet(resultSet);
                        value = factory.columnTypeConversionHook.getColumnValue(valueContext);
                    }

                    row.put(columnName, value);
                    colNum++;
                }

                EventBean eventBeanRow = null;
                if (factory.outputRowConversionHook == null) {
                    eventBeanRow = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(row, factory.getEventType());
                } else {
                    rowContext.setValues(row);
                    rowContext.setRowNum(rowNum);
                    rowContext.setResultSet(resultSet);
                    Object rowData = factory.outputRowConversionHook.getOutputRow(rowContext);
                    if (rowData != null) {
                        eventBeanRow = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedBean(rowData, (BeanEventType) factory.getEventType());
                    }
                }

                if (eventBeanRow != null) {
                    rows.add(eventBeanRow);
                    rowNum++;
                }
            }
        } catch (SQLException ex) {
            throw new EPException("Error reading results for statement '" + factory.preparedStatementText + '\'', ex);
        }

        if (factory.enableJDBCLogging && JDBC_PERF_LOG.isInfoEnabled()) {
            JDBC_PERF_LOG.info("Statement '" + factory.preparedStatementText + "' " + rows.size() + " rows");
        }

        try {
            resultSet.close();
        } catch (SQLException ex) {
            throw new EPException("Error closing statement '" + factory.preparedStatementText + '\'', ex);
        }

        return rows;
    }

    private void setObject(PreparedStatement preparedStatement, int column, Object value) throws SQLException {
        // Allow java.util.Date conversion for JDBC drivers that don't provide this feature
        if (value instanceof Date) {
            value = new Timestamp(((Date) value).getTime());
        } else if (value instanceof Calendar) {
            value = new Timestamp(((Calendar) value).getTimeInMillis());
        }

        preparedStatement.setObject(column, value);
    }
}
