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
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConstFactory;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link com.espertech.esper.view.window.ExternallyTimedBatchView}.
 */
public class ExternallyTimedBatchViewFactory implements DataWindowBatchingViewFactory, DataWindowViewFactory, DataWindowViewWithPrevious {
    private List<ExprNode> viewParameters;

    private EventType eventType;

    /**
     * The timestamp property name.
     */
    protected ExprNode timestampExpression;
    protected ExprEvaluator timestampExpressionEval;
    protected Long optionalReferencePoint;

    /**
     * The number of msec to expire.
     */
    protected ExprTimePeriodEvalDeltaConstFactory timeDeltaComputationFactory;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        this.viewParameters = expressionParameters;
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        final String windowName = getViewName();
        ExprNode[] validated = ViewFactorySupport.validate(windowName, parentEventType, statementContext, viewParameters, true);
        if (viewParameters.size() < 2 || viewParameters.size() > 3) {
            throw new ViewParameterException(getViewParamMessage());
        }

        // validate first parameter: timestamp expression
        if (!JavaClassHelper.isNumeric(validated[0].getForge().getEvaluationType())) {
            throw new ViewParameterException(getViewParamMessage());
        }
        timestampExpression = validated[0];
        timestampExpressionEval = ExprNodeCompiler.allocateEvaluator(timestampExpression.getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
        ViewFactorySupport.assertReturnsNonConstant(windowName, validated[0], 0);

        timeDeltaComputationFactory = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDeltaFactory(getViewName(), statementContext, viewParameters.get(1), getViewParamMessage(), 1);

        // validate optional parameters
        if (validated.length == 3) {
            Object constant = ViewFactorySupport.validateAndEvaluate(windowName, statementContext, validated[2]);
            if ((!(constant instanceof Number)) || (JavaClassHelper.isFloatingPointNumber((Number) constant))) {
                throw new ViewParameterException("Externally-timed batch view requires a Long-typed reference point in msec as a third parameter");
            }
            optionalReferencePoint = ((Number) constant).longValue();
        }

        this.eventType = parentEventType;
    }

    public Object makePreviousGetter() {
        return new RelativeAccessByEventNIndexGetterImpl();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        ExprTimePeriodEvalDeltaConst timeDeltaComputation = timeDeltaComputationFactory.make(getViewName(), "view", agentInstanceViewFactoryContext.getAgentInstanceContext(), agentInstanceViewFactoryContext.getTimeAbacus());
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        return new ExternallyTimedBatchView(this, timestampExpression, timestampExpressionEval, timeDeltaComputation, optionalReferencePoint, viewUpdatedCollection, agentInstanceViewFactoryContext);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof ExternallyTimedBatchView)) {
            return false;
        }

        ExternallyTimedBatchView myView = (ExternallyTimedBatchView) view;
        ExprTimePeriodEvalDeltaConst delta = timeDeltaComputationFactory.make(getViewName(), "view", agentInstanceContext, agentInstanceContext.getTimeAbacus());
        if ((!delta.equalsTimePeriod(myView.getTimeDeltaComputation())) ||
                (!ExprNodeUtilityCore.deepEquals(myView.getTimestampExpression(), timestampExpression, false))) {
            return false;
        }
        return myView.isEmpty();
    }

    public String getViewName() {
        return "Externally-timed-batch";
    }

    public ExprEvaluator getTimestampExpressionEval() {
        return timestampExpressionEval;
    }

    public Long getOptionalReferencePoint() {
        return optionalReferencePoint;
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires a timestamp expression and a numeric or time period parameter for window size and an optional long-typed reference point in msec, and an optional list of control keywords as a string parameter (please see the documentation)";
    }
}
