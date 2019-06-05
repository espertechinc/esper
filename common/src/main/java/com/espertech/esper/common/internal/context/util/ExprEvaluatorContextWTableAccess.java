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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.script.core.AgentInstanceScriptContext;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;

public class ExprEvaluatorContextWTableAccess implements ExprEvaluatorContext {
    private final ExprEvaluatorContext context;
    private final TableExprEvaluatorContext tableExprEvaluatorContext;

    public ExprEvaluatorContextWTableAccess(ExprEvaluatorContext context, TableExprEvaluatorContext tableExprEvaluatorContext) {
        this.context = context;
        this.tableExprEvaluatorContext = tableExprEvaluatorContext;
    }

    public String getStatementName() {
        return context.getStatementName();
    }

    public String getRuntimeURI() {
        return context.getRuntimeURI();
    }

    public int getStatementId() {
        return context.getStatementId();
    }

    public String getDeploymentId() {
        return context.getDeploymentId();
    }

    public TimeProvider getTimeProvider() {
        return context.getTimeProvider();
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return context.getExpressionResultCacheService();
    }

    public int getAgentInstanceId() {
        return context.getAgentInstanceId();
    }

    public EventBean getContextProperties() {
        return context.getContextProperties();
    }

    public AgentInstanceScriptContext getAllocateAgentInstanceScriptContext() {
        return context.getAllocateAgentInstanceScriptContext();
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        return context.getAgentInstanceLock();
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return tableExprEvaluatorContext;
    }

    public Object getUserObjectCompileTime() {
        return context.getUserObjectCompileTime();
    }

    public EventBeanService getEventBeanService() {
        return context.getEventBeanService();
    }

    public AuditProvider getAuditProvider() {
        return context.getAuditProvider();
    }

    public InstrumentationCommon getInstrumentationProvider() {
        return context.getInstrumentationProvider();
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return context.getExceptionHandlingService();
    }
}