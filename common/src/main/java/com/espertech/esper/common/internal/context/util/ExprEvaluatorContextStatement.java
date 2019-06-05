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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.script.core.AgentInstanceScriptContext;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.audit.AuditProviderDefault;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommonDefault;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;

public class ExprEvaluatorContextStatement implements ExprEvaluatorContext {
    protected final StatementContext statementContext;
    private final boolean allowTableAccess;
    private EventBean contextProperties;

    public ExprEvaluatorContextStatement(StatementContext statementContext, boolean allowTableAccess) {
        this.statementContext = statementContext;
        this.allowTableAccess = allowTableAccess;
    }

    /**
     * Returns the time provider.
     *
     * @return time provider
     */
    public TimeProvider getTimeProvider() {
        return statementContext.getTimeProvider();
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return statementContext.getExpressionResultCacheServiceSharable();
    }

    public int getAgentInstanceId() {
        return -1;
    }

    public EventBean getContextProperties() {
        return contextProperties;
    }

    public String getStatementName() {
        return statementContext.getStatementName();
    }

    public int getStatementId() {
        return statementContext.getStatementId();
    }

    public String getDeploymentId() {
        return statementContext.getDeploymentId();
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        throw new UnsupportedOperationException("Agent-instance lock not available");
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        if (!allowTableAccess) {
            throw new EPException("Access to tables is not allowed");
        }
        return statementContext.getTableExprEvaluatorContext();
    }

    public void setContextProperties(EventBean contextProperties) {
        this.contextProperties = contextProperties;
    }

    public Object getUserObjectCompileTime() {
        return statementContext.getUserObjectCompileTime();
    }

    public String getRuntimeURI() {
        return statementContext.getRuntimeURI();
    }

    public EventBeanService getEventBeanService() {
        return statementContext.getEventBeanService();
    }

    public AgentInstanceScriptContext getAllocateAgentInstanceScriptContext() {
        return statementContext.getAllocateAgentInstanceScriptContext();
    }

    public AuditProvider getAuditProvider() {
        return AuditProviderDefault.INSTANCE;
    }

    public InstrumentationCommon getInstrumentationProvider() {
        return InstrumentationCommonDefault.INSTANCE;
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return statementContext.getExceptionHandlingService();
    }
}
