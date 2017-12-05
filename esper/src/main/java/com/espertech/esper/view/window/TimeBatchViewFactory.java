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

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link TimeBatchView}.
 */
public class TimeBatchViewFactory extends TimeBatchViewFactoryParams implements DataWindowViewFactory, DataWindowViewWithPrevious, DataWindowBatchingViewFactory {
    /**
     * The reference point, or null if none supplied.
     */
    protected Long optionalReferencePoint;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        if ((expressionParameters.size() < 1) || (expressionParameters.size() > 3)) {
            throw new ViewParameterException(getViewParamMessage());
        }
        Object[] viewParamValues = new Object[expressionParameters.size()];
        for (int i = 1; i < viewParamValues.length; i++) {
            viewParamValues[i] = ViewFactorySupport.validateAndEvaluate(getViewName(), viewFactoryContext.getStatementContext(), expressionParameters.get(i));
        }

        timeDeltaComputationFactory = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDeltaFactory(getViewName(), viewFactoryContext.getStatementContext(), expressionParameters.get(0), getViewParamMessage(), 0);

        if ((viewParamValues.length == 2) && (viewParamValues[1] instanceof String)) {
            processKeywords(viewParamValues[1], getViewParamMessage());
        } else {
            if (viewParamValues.length >= 2) {
                Object paramRef = viewParamValues[1];
                if ((!(paramRef instanceof Number)) || (JavaClassHelper.isFloatingPointNumber((Number) paramRef))) {
                    throw new ViewParameterException(getViewName() + " view requires a Long-typed reference point in msec as a second parameter");
                }
                optionalReferencePoint = ((Number) paramRef).longValue();
            }
            if (viewParamValues.length == 3) {
                processKeywords(viewParamValues[2], getViewParamMessage());
            }
        }
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        this.eventType = parentEventType;
    }

    public Object makePreviousGetter() {
        return new RelativeAccessByEventNIndexGetterImpl();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        ExprTimePeriodEvalDeltaConst timeDeltaComputation = timeDeltaComputationFactory.make(getViewName(), "view", agentInstanceViewFactoryContext.getAgentInstanceContext(), agentInstanceViewFactoryContext.getTimeAbacus());
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        if (agentInstanceViewFactoryContext.isRemoveStream()) {
            return new TimeBatchViewRStream(this, agentInstanceViewFactoryContext, timeDeltaComputation, optionalReferencePoint, isForceUpdate, isStartEager);
        } else {
            return new TimeBatchView(this, agentInstanceViewFactoryContext, timeDeltaComputation, optionalReferencePoint, isForceUpdate, isStartEager, viewUpdatedCollection);
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof TimeBatchView)) {
            return false;
        }

        TimeBatchView myView = (TimeBatchView) view;
        ExprTimePeriodEvalDeltaConst timeDeltaComputation = timeDeltaComputationFactory.make(getViewName(), "view", agentInstanceContext, agentInstanceContext.getTimeAbacus());
        if (!timeDeltaComputation.equalsTimePeriod(myView.getTimeDeltaComputation())) {
            return false;
        }

        if ((myView.getInitialReferencePoint() != null) && (optionalReferencePoint != null)) {
            if (!myView.getInitialReferencePoint().equals(optionalReferencePoint.longValue())) {
                return false;
            }
        }
        if (((myView.getInitialReferencePoint() == null) && (optionalReferencePoint != null)) ||
                ((myView.getInitialReferencePoint() != null) && (optionalReferencePoint == null))) {
            return false;
        }

        if (myView.isForceOutput() != isForceUpdate) {
            return false;
        }

        if (myView.isStartEager()) {
            // since it's already started
            return false;
        }

        return myView.isEmpty();
    }

    public String getViewName() {
        return "Time-Batch";
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires a single numeric or time period parameter, and an optional long-typed reference point in msec, and an optional list of control keywords as a string parameter (please see the documentation)";
    }
}
