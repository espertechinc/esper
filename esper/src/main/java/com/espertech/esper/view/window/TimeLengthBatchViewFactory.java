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
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link com.espertech.esper.view.window.TimeLengthBatchView}.
 */
public class TimeLengthBatchViewFactory extends TimeBatchViewFactoryParams implements DataWindowViewFactory, DataWindowViewWithPrevious, DataWindowBatchingViewFactory
{
    /**
     * Number of events to collect before batch fires.
     */
    protected long numberOfEvents;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException
    {
        Object[] viewParameters = new Object[expressionParameters.size()];
        for (int i = 1; i < expressionParameters.size(); i++) {
            viewParameters[i] = ViewFactorySupport.validateAndEvaluate(getViewName(), viewFactoryContext.getStatementContext(), expressionParameters.get(i));
        }
        String errorMessage = getViewName() + " view requires a numeric or time period parameter as a time interval size, and an integer parameter as a maximal number-of-events, and an optional list of control keywords as a string parameter (please see the documentation)";
        if ((viewParameters.length != 2) && (viewParameters.length != 3)) {
            throw new ViewParameterException(errorMessage);
        }

        timeDeltaComputation = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDelta(getViewName(), viewFactoryContext.getStatementContext(), expressionParameters.get(0), errorMessage, 0);

        // parameter 2
        Object parameter = viewParameters[1];
        if (!(parameter instanceof Number) || (JavaClassHelper.isFloatingPointNumber((Number) parameter))) {
            throw new ViewParameterException(errorMessage);
        }
        numberOfEvents = ((Number) parameter).longValue();

        if (viewParameters.length > 2) {
            processKeywords(viewParameters[2], errorMessage);
        }
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException
    {
        this.eventType = parentEventType;
    }

    public Object makePreviousGetter() {
        return new RelativeAccessByEventNIndexGetterImpl();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext)
    {
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        return new TimeLengthBatchView(this, agentInstanceViewFactoryContext, timeDeltaComputation, numberOfEvents, isForceUpdate, isStartEager, viewUpdatedCollection);
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public boolean canReuse(View view)
    {
        if (!(view instanceof TimeLengthBatchView))
        {
            return false;
        }

        TimeLengthBatchView myView = (TimeLengthBatchView) view;

        if (!timeDeltaComputation.equalsTimePeriod(myView.getTimeDeltaComputation()))
        {
            return false;
        }

        if (myView.getNumberOfEvents() != numberOfEvents)
        {
            return false;
        }

        if (myView.isForceOutput() != isForceUpdate)
        {
            return false;
        }

        if (myView.isStartEager())  // since it's already started
        {
            return false;
        }

        return myView.isEmpty();
    }

    public String getViewName() {
        return "Time-Length-Batch";
    }

    public long getNumberOfEvents() {
        return numberOfEvents;
    }
}
