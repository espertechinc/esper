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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link com.espertech.esper.view.window.TimeLengthBatchView}.
 */
public class TimeLengthBatchViewFactory extends TimeBatchViewFactoryParams implements DataWindowViewFactory, DataWindowViewWithPrevious, DataWindowBatchingViewFactory {
    /**
     * Number of events to collect before batch fires.
     */
    protected ExprEvaluator sizeEvaluator;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        ExprNode[] validated = ViewFactorySupport.validate(getViewName(), viewFactoryContext.getStatementContext(), expressionParameters);
        String errorMessage = getViewName() + " view requires a numeric or time period parameter as a time interval size, and an integer parameter as a maximal number-of-events, and an optional list of control keywords as a string parameter (please see the documentation)";
        if ((validated.length != 2) && (validated.length != 3)) {
            throw new ViewParameterException(errorMessage);
        }

        timeDeltaComputationFactory = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDeltaFactory(getViewName(), viewFactoryContext.getStatementContext(), expressionParameters.get(0), errorMessage, 0);

        sizeEvaluator = ViewFactorySupport.validateSizeParam(getViewName(), viewFactoryContext.getStatementContext(), validated[1], 1);

        if (validated.length > 2) {
            Object keywords = ViewFactorySupport.evaluate(validated[2].getForge().getExprEvaluator(), 2, getViewName(), viewFactoryContext.getStatementContext());
            processKeywords(keywords, errorMessage);
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
        int size = ViewFactorySupport.evaluateSizeParam(getViewName(), sizeEvaluator, agentInstanceViewFactoryContext.getAgentInstanceContext());
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        return new TimeLengthBatchView(this, agentInstanceViewFactoryContext, timeDeltaComputation, size, isForceUpdate, isStartEager, viewUpdatedCollection);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof TimeLengthBatchView)) {
            return false;
        }

        TimeLengthBatchView myView = (TimeLengthBatchView) view;
        ExprTimePeriodEvalDeltaConst timeDeltaComputation = timeDeltaComputationFactory.make(getViewName(), "view", agentInstanceContext, agentInstanceContext.getTimeAbacus());
        if (!timeDeltaComputation.equalsTimePeriod(myView.getTimeDeltaComputation())) {
            return false;
        }

        int size = ViewFactorySupport.evaluateSizeParam(getViewName(), sizeEvaluator, agentInstanceContext);
        if (myView.getNumberOfEvents() != size) {
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
        return "Time-Length-Batch";
    }
}
