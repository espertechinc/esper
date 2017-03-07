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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.script.AgentInstanceScriptContext;
import com.espertech.esper.epl.table.mgmt.TableExprEvaluatorContext;
import com.espertech.esper.schedule.TimeProvider;

/**
 * Represents a statement-level-only context for expression evaluation, not allowing for agents instances and result cache.
 */
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

    public AgentInstanceScriptContext getAllocateAgentInstanceScriptContext() {
        return statementContext.getAllocateAgentInstanceScriptContext();
    }

    public String getStatementName() {
        return statementContext.getStatementName();
    }

    public String getEngineURI() {
        return statementContext.getEngineURI();
    }

    public int getStatementId() {
        return statementContext.getStatementId();
    }

    public StatementType getStatementType() {
        return statementContext.getStatementType();
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        return statementContext.getDefaultAgentInstanceLock();
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        if (!allowTableAccess) {
            throw new EPException("Access to tables is not allowed");
        }
        return statementContext.getTableExprEvaluatorContext();
    }

    public Object getStatementUserObject() {
        return statementContext.getStatementUserObject();
    }

    public void setContextProperties(EventBean contextProperties) {
        this.contextProperties = contextProperties;
    }
}