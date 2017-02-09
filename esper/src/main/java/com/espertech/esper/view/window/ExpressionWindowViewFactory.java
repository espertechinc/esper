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
package com.espertech.esper.view.window;

import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewFactoryContext;
import com.espertech.esper.view.ViewParameterException;

import java.util.List;

/**
 * Factory for {@link com.espertech.esper.view.window.ExpressionWindowView}.
 */
public class ExpressionWindowViewFactory extends ExpressionViewFactoryBase {
    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        if (expressionParameters.size() != 1) {
            String errorMessage = getViewName() + " view requires a single expression as a parameter";
            throw new ViewParameterException(errorMessage);
        }
        expiryExpression = expressionParameters.get(0);
    }

    public View makeView(final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        ObjectArrayEventBean builtinBean = new ObjectArrayEventBean(ExpressionViewOAFieldEnum.getPrototypeOA(), builtinMapType);
        ViewUpdatedCollection randomAccess = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRandomAccess(agentInstanceViewFactoryContext);
        return new ExpressionWindowView(this, randomAccess, expiryExpressionEvaluator, aggregationServiceFactoryDesc, builtinBean, variableNames, agentInstanceViewFactoryContext);
    }

    public Object makePreviousGetter() {
        return new RandomAccessByIndexGetter();
    }

    public String getViewName() {
        return "Expression";
    }
}
