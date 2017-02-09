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
package com.espertech.esper.view.ext;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConstFactory;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.*;
import com.espertech.esper.view.window.RandomAccessByIndexGetter;

import java.util.List;

/**
 * Factory for views for time-ordering events.
 */
public class TimeOrderViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    private List<ExprNode> viewParameters;

    /**
     * The timestamp expression.
     */
    protected ExprNode timestampExpression;

    protected ExprEvaluator timestampExpressionEvaluator;

    /**
     * The interval to wait for newer events to arrive.
     */
    protected ExprTimePeriodEvalDeltaConstFactory timeDeltaComputationFactory;

    private EventType eventType;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        viewParameters = expressionParameters;
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        ExprNode[] validated = ViewFactorySupport.validate(getViewName(), parentEventType, statementContext, viewParameters, true);

        if (viewParameters.size() != 2) {
            throw new ViewParameterException(getViewParamMessage());
        }

        if (!JavaClassHelper.isNumeric(validated[0].getExprEvaluator().getType())) {
            throw new ViewParameterException(getViewParamMessage());
        }
        timestampExpression = validated[0];
        timeDeltaComputationFactory = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDeltaFactory(getViewName(), statementContext, viewParameters.get(1), getViewParamMessage(), 1);
        timestampExpressionEvaluator = timestampExpression.getExprEvaluator();
        eventType = parentEventType;
    }

    public Object makePreviousGetter() {
        return new RandomAccessByIndexGetter();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        ExprTimePeriodEvalDeltaConst timeDeltaComputation = timeDeltaComputationFactory.make(getViewName(), "view", agentInstanceViewFactoryContext.getAgentInstanceContext());
        IStreamSortRankRandomAccess sortedRandomAccess = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprSortedRankedAccess(agentInstanceViewFactoryContext);
        return new TimeOrderView(agentInstanceViewFactoryContext, this, timestampExpression, timestampExpression.getExprEvaluator(), timeDeltaComputation, sortedRandomAccess);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof TimeOrderView)) {
            return false;
        }

        TimeOrderView other = (TimeOrderView) view;
        ExprTimePeriodEvalDeltaConst timeDeltaComputation = timeDeltaComputationFactory.make(getViewName(), "view", agentInstanceContext);
        if ((!timeDeltaComputation.equalsTimePeriod(other.getTimeDeltaComputation())) ||
                (!ExprNodeUtility.deepEquals(other.getTimestampExpression(), timestampExpression))) {
            return false;
        }

        return other.isEmpty();
    }

    public String getViewName() {
        return "Time-Order";
    }

    public ExprEvaluator getTimestampExpressionEvaluator() {
        return timestampExpressionEvaluator;
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires the expression supplying timestamp values, and a numeric or time period parameter for interval size";
    }
}
