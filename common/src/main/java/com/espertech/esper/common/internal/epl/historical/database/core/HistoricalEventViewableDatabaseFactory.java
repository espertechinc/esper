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
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.client.hook.type.SQLColumnTypeConversion;
import com.espertech.esper.common.client.hook.type.SQLOutputRowConversion;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewable;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewableFactoryBase;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigException;
import com.espertech.esper.common.internal.settings.ClasspathImportUtil;

import java.util.Map;

/**
 * Implements a poller viewable that uses a polling strategy, a cache and
 * some input parameters extracted from event streams to perform the polling.
 */
public class HistoricalEventViewableDatabaseFactory extends HistoricalEventViewableFactoryBase {

    protected String databaseName;
    protected String[] inputParameters;
    protected String preparedStatementText;
    protected Map<String, DBOutputTypeDesc> outputTypes;
    protected SQLColumnTypeConversion columnTypeConversionHook;
    protected SQLOutputRowConversion outputRowConversionHook;
    protected boolean enableJDBCLogging;

    public HistoricalEventViewable activate(AgentInstanceContext agentInstanceContext) {
        ConnectionCache connectionCache = null;
        try {
            connectionCache = agentInstanceContext.getDatabaseConfigService().getConnectionCache(databaseName, preparedStatementText);
        } catch (DatabaseConfigException e) {
            throw new EPException("Failed to obtain connection cache: " + e.getMessage(), e);
        }
        PollExecStrategyDBQuery pollExecStrategy = new PollExecStrategyDBQuery(this, agentInstanceContext, connectionCache);
        return new HistoricalEventViewableDatabase(this, pollExecStrategy, agentInstanceContext);
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setInputParameters(String[] inputParameters) {
        this.inputParameters = inputParameters;
    }

    public void setPreparedStatementText(String preparedStatementText) {
        this.preparedStatementText = preparedStatementText;
    }

    public void setOutputTypes(Map<String, DBOutputTypeDesc> outputTypes) {
        this.outputTypes = outputTypes;
    }

    public void setColumnTypeConversionHook(SQLColumnTypeConversion columnTypeConversionHook) {
        this.columnTypeConversionHook = columnTypeConversionHook;
    }

    public void setOutputRowConversionHook(SQLOutputRowConversion outputRowConversionHook) {
        this.outputRowConversionHook = outputRowConversionHook;
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        try {
            columnTypeConversionHook = (SQLColumnTypeConversion) ClasspathImportUtil.getAnnotationHook(statementContext.getAnnotations(), HookType.SQLCOL, SQLColumnTypeConversion.class, statementContext.getClasspathImportServiceRuntime());
            outputRowConversionHook = (SQLOutputRowConversion) ClasspathImportUtil.getAnnotationHook(statementContext.getAnnotations(), HookType.SQLROW, SQLOutputRowConversion.class, statementContext.getClasspathImportServiceRuntime());
        } catch (ExprValidationException e) {
            throw new EPException("Failed to obtain annotation-defined sql-related hook: " + e.getMessage(), e);
        }
    }
}
