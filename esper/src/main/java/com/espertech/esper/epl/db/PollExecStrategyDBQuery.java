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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.DatabaseTypeBinding;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Viewable providing historical data from a database.
 */
public class PollExecStrategyDBQuery implements PollExecStrategy {
    private static final Logger JDBC_PERF_LOG = LoggerFactory.getLogger(AuditPath.JDBC_LOG);

    private static final Logger log = LoggerFactory.getLogger(PollExecStrategyDBQuery.class);
    private final EventAdapterService eventAdapterService;
    private final String preparedStatementText;
    private final Map<String, DBOutputTypeDesc> outputTypes;
    private final ConnectionCache connectionCache;
    private final EventType eventType;
    private final SQLColumnTypeConversion columnTypeConversionHook;
    private final SQLOutputRowConversion outputRowConversionHook;
    private final boolean enableJDBCLogging;

    private Pair<Connection, PreparedStatement> resources;

    /**
     * Ctor.
     *
     * @param eventAdapterService      for generating event beans
     * @param eventType                is the event type that this poll generates
     * @param connectionCache          caches Connection and PreparedStatement
     * @param preparedStatementText    is the SQL to use for polling
     * @param outputTypes              describe columns selected by the SQL
     * @param outputRowConversionHook  hook to convert rows, if any hook is registered
     * @param columnTypeConversionHook hook to convert columns, if any hook is registered
     * @param enableJDBCLogging        jdbc logging flag
     */
    public PollExecStrategyDBQuery(EventAdapterService eventAdapterService,
                                   EventType eventType,
                                   ConnectionCache connectionCache,
                                   String preparedStatementText,
                                   Map<String, DBOutputTypeDesc> outputTypes,
                                   SQLColumnTypeConversion columnTypeConversionHook,
                                   SQLOutputRowConversion outputRowConversionHook,
                                   boolean enableJDBCLogging) {
        this.eventAdapterService = eventAdapterService;
        this.eventType = eventType;
        this.connectionCache = connectionCache;
        this.preparedStatementText = preparedStatementText;
        this.outputTypes = outputTypes;
        this.columnTypeConversionHook = columnTypeConversionHook;
        this.outputRowConversionHook = outputRowConversionHook;
        this.enableJDBCLogging = enableJDBCLogging;
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

    public List<EventBean> poll(Object[] lookupValues, ExprEvaluatorContext exprEvaluatorContext) {
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
                                                 Object[] lookupValuePerStream) {
        if (ExecutionPathDebugLog.isDebugEnabled && log.isInfoEnabled()) {
            log.info(".execute Executing prepared statement '" + preparedStatementText + "'");
        }

        boolean hasJDBCLogging = enableJDBCLogging && JDBC_PERF_LOG.isInfoEnabled();

        // set parameters
        SQLInputParameterContext inputParameterContext = null;
        if (columnTypeConversionHook != null) {
            inputParameterContext = new SQLInputParameterContext();
        }

        int count = 1;
        Object[] parameters = null;
        if (hasJDBCLogging) {
            parameters = new Object[lookupValuePerStream.length];
        }
        for (int i = 0; i < lookupValuePerStream.length; i++) {
            try {
                Object parameter = lookupValuePerStream[i];
                if (ExecutionPathDebugLog.isDebugEnabled && log.isInfoEnabled()) {
                    log.info(".execute Setting parameter " + count + " to " + parameter + " typed " + ((parameter == null) ? "null" : parameter.getClass()));
                }

                if (columnTypeConversionHook != null) {
                    inputParameterContext.setParameterNumber(i + 1);
                    inputParameterContext.setParameterValue(parameter);
                    parameter = columnTypeConversionHook.getParameterValue(inputParameterContext);
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
                throw new EPException("Error executing statement '" + preparedStatementText + '\'', ex);
            }
            long endTimeNS = System.nanoTime();
            long endTimeMS = System.currentTimeMillis();
            JDBC_PERF_LOG.info("Statement '" + preparedStatementText + "' delta nanosec " + (endTimeNS - startTimeNS) +
                    " delta msec " + (endTimeMS - startTimeMS) +
                    " parameters " + Arrays.toString(parameters));
        } else {
            try {
                resultSet = preparedStatement.executeQuery();
            } catch (SQLException ex) {
                throw new EPException("Error executing statement '" + preparedStatementText + '\'', ex);
            }
        }

        // generate events for result set
        List<EventBean> rows = new LinkedList<EventBean>();
        try {
            SQLColumnValueContext valueContext = null;
            if (columnTypeConversionHook != null) {
                valueContext = new SQLColumnValueContext();
            }

            SQLOutputRowValueContext rowContext = null;
            if (outputRowConversionHook != null) {
                rowContext = new SQLOutputRowValueContext();
            }

            int rowNum = 0;
            while (resultSet.next()) {
                int colNum = 1;
                Map<String, Object> row = new HashMap<String, Object>();
                for (Map.Entry<String, DBOutputTypeDesc> entry : outputTypes.entrySet()) {
                    String columnName = entry.getKey();

                    Object value;
                    DatabaseTypeBinding binding = entry.getValue().getOptionalBinding();
                    if (binding != null) {
                        value = binding.getValue(resultSet, columnName);
                    } else {
                        value = resultSet.getObject(columnName);
                    }

                    if (columnTypeConversionHook != null) {
                        valueContext.setColumnName(columnName);
                        valueContext.setColumnNumber(colNum);
                        valueContext.setColumnValue(value);
                        valueContext.setResultSet(resultSet);
                        value = columnTypeConversionHook.getColumnValue(valueContext);
                    }

                    row.put(columnName, value);
                    colNum++;
                }

                EventBean eventBeanRow = null;
                if (this.outputRowConversionHook == null) {
                    eventBeanRow = eventAdapterService.adapterForTypedMap(row, eventType);
                } else {
                    rowContext.setValues(row);
                    rowContext.setRowNum(rowNum);
                    rowContext.setResultSet(resultSet);
                    Object rowData = outputRowConversionHook.getOutputRow(rowContext);
                    if (rowData != null) {
                        eventBeanRow = eventAdapterService.adapterForTypedBean(rowData, (BeanEventType) eventType);
                    }
                }

                if (eventBeanRow != null) {
                    rows.add(eventBeanRow);
                    rowNum++;
                }
            }
        } catch (SQLException ex) {
            throw new EPException("Error reading results for statement '" + preparedStatementText + '\'', ex);
        }

        if (enableJDBCLogging && JDBC_PERF_LOG.isInfoEnabled()) {
            JDBC_PERF_LOG.info("Statement '" + preparedStatementText + "' " + rows.size() + " rows");
        }

        try {
            resultSet.close();
        } catch (SQLException ex) {
            throw new EPException("Error closing statement '" + preparedStatementText + '\'', ex);
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
