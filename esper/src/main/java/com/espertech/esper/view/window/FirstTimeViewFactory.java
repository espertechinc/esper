/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link FirstTimeView}.
 */
public class FirstTimeViewFactory implements AsymetricDataWindowViewFactory, DataWindowBatchingViewFactory
{
    private EventType eventType;

    /**
     * Number of msec before expiry.
     */
    protected ExprTimePeriodEvalDeltaConst timeDeltaComputation;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException
    {
        if (expressionParameters.size() != 1) {
            throw new ViewParameterException(getViewParamMessage());
        }
        timeDeltaComputation = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDelta(getViewName(), viewFactoryContext.getStatementContext(), expressionParameters.get(0), getViewParamMessage(), 0);
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException
    {
        this.eventType = parentEventType;
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext)
    {
        return new FirstTimeView(this, agentInstanceViewFactoryContext, timeDeltaComputation);
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public boolean canReuse(View view)
    {
        if (!(view instanceof FirstTimeView))
        {
            return false;
        }

        FirstTimeView myView = (FirstTimeView) view;
        if (!timeDeltaComputation.equalsTimePeriod(myView.getTimeDeltaComputation()))
        {
            return false;
        }

        return myView.isEmpty();
    }

    public String getViewName() {
        return "First-Time";
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires a single numeric or time period parameter";
    }
}
