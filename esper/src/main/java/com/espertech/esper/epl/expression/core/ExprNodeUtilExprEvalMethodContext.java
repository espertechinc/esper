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
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.hook.EventBeanService;

public class ExprNodeUtilExprEvalMethodContext implements ExprEvaluator {

    private final EPLMethodInvocationContext defaultContextForFilters;

    public ExprNodeUtilExprEvalMethodContext(String engineURI, String functionName, EventBeanService eventBeanService) {
        this.defaultContextForFilters = new EPLMethodInvocationContext(null, -1, engineURI, functionName, null, eventBeanService);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (context == null) {
            return defaultContextForFilters;
        }
        return new EPLMethodInvocationContext(context.getStatementName(),
                context.getAgentInstanceId(), defaultContextForFilters.getEngineURI(), defaultContextForFilters.getFunctionName(), context.getStatementUserObject(), defaultContextForFilters.getEventBeanService());
    }

    public Class getType() {
        return EPLMethodInvocationContext.class;
    }

}
