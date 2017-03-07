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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.ExpressionResultCacheService;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.script.AgentInstanceScriptContext;
import com.espertech.esper.epl.table.mgmt.TableExprEvaluatorContext;
import com.espertech.esper.schedule.TimeProvider;

/**
 * Returns the context for expression evaluation.
 */
public interface ExprEvaluatorContext {
    public String getStatementName();

    public Object getStatementUserObject();

    public String getEngineURI();

    public int getStatementId();

    public StatementType getStatementType();

    /**
     * Returns the time provider.
     *
     * @return time provider
     */
    public TimeProvider getTimeProvider();

    public ExpressionResultCacheService getExpressionResultCacheService();

    public int getAgentInstanceId();

    public EventBean getContextProperties();

    public StatementAgentInstanceLock getAgentInstanceLock();

    public TableExprEvaluatorContext getTableExprEvaluatorContext();

    AgentInstanceScriptContext getAllocateAgentInstanceScriptContext();
}