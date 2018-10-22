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
package com.espertech.esper.common.internal.epl.rowrecog.nfa;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

/**
 * NFA state for a single match that applies a filter.
 */
public class RowRecogNFAStateFilterEval extends RowRecogNFAStateBase implements RowRecogNFAState {
    private ExprEvaluator expression;
    private String expressionTextForAudit;

    public boolean matches(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext) {
        agentInstanceContext.getInstrumentationProvider().qRegFilter(expressionTextForAudit, eventsPerStream);
        Boolean result = (Boolean) expression.evaluate(eventsPerStream, true, agentInstanceContext);
        if (result != null) {
            agentInstanceContext.getInstrumentationProvider().aRegFilter(result);
            return result;
        }
        agentInstanceContext.getInstrumentationProvider().aRegFilter(false);
        return false;
    }

    public String toString() {
        return "FilterEvent";
    }

    public void setExpression(ExprEvaluator expression) {
        this.expression = expression;
    }

    public void setExpressionTextForAudit(String expressionTextForAudit) {
        this.expressionTextForAudit = expressionTextForAudit;
    }
}
