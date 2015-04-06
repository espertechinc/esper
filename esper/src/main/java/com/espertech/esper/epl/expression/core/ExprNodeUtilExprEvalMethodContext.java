/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;

public class ExprNodeUtilExprEvalMethodContext implements ExprEvaluator {

    private final String functionName;

    public ExprNodeUtilExprEvalMethodContext(String functionName) {
        this.functionName = functionName;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return new EPLMethodInvocationContext(context.getStatementName(),
                context.getAgentInstanceId(), context.getEngineURI(), functionName, context.getStatementUserObject());
    }

    public Class getType() {
        return EPLMethodInvocationContext.class;
    }

}
