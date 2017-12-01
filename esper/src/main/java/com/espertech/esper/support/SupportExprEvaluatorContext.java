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
package com.espertech.esper.support;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.ExpressionResultCacheService;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.script.AgentInstanceScriptContext;
import com.espertech.esper.epl.table.mgmt.TableExprEvaluatorContext;
import com.espertech.esper.schedule.TimeProvider;

public class SupportExprEvaluatorContext implements ExprEvaluatorContext {

    private final TimeProvider timeProvider;

    public SupportExprEvaluatorContext(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public TimeProvider getTimeProvider() {
        return timeProvider;
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return null;
    }

    public int getAgentInstanceId() {
        return -1;
    }

    public EventBean getContextProperties() {
        return null;
    }

    public AgentInstanceScriptContext getAllocateAgentInstanceScriptContext() {
        return null;
    }

    public String getStatementName() {
        return null;
    }

    public String getEngineURI() {
        return null;
    }

    public int getStatementId() {
        return 1;
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        return null;
    }

    public StatementType getStatementType() {
        return StatementType.SELECT;
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return null;
    }

    public Object getStatementUserObject() {
        return null;
    }
}
