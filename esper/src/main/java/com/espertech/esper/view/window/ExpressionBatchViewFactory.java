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
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link ExpressionBatchView}.
 */
public class ExpressionBatchViewFactory extends ExpressionViewFactoryBase implements DataWindowBatchingViewFactory {
    protected boolean includeTriggeringEvent = true;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        if (expressionParameters.size() != 1 && expressionParameters.size() != 2) {
            String errorMessage = getViewName() + " view requires a single expression as a parameter, or an expression and boolean flag";
            throw new ViewParameterException(errorMessage);
        }
        expiryExpression = expressionParameters.get(0);

        if (expressionParameters.size() > 1) {
            Object result = ViewFactorySupport.evaluateAssertNoProperties(getViewName(), expressionParameters.get(1), 1, new ExprEvaluatorContextStatement(viewFactoryContext.getStatementContext(), false));
            includeTriggeringEvent = (Boolean) result;
        }
    }

    public View makeView(final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        ObjectArrayEventBean builtinBean = new ObjectArrayEventBean(ExpressionViewOAFieldEnum.getPrototypeOA(), builtinMapType);
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        return new ExpressionBatchView(this, viewUpdatedCollection, expiryExpressionEvaluator, aggregationServiceFactoryDesc, builtinBean, variableNames, agentInstanceViewFactoryContext);
    }

    public Object makePreviousGetter() {
        return new RelativeAccessByEventNIndexGetterImpl();
    }

    public boolean isIncludeTriggeringEvent() {
        return includeTriggeringEvent;
    }

    public String getViewName() {
        return "Expression-batch";
    }
}
