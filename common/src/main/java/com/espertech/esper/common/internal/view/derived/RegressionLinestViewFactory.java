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
package com.espertech.esper.common.internal.view.derived;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.view.core.*;

/**
 * Factory for {@link RegressionLinestView} instances.
 */
public class RegressionLinestViewFactory implements ViewFactory {

    protected ExprEvaluator expressionXEval;
    protected ExprEvaluator expressionYEval;
    protected StatViewAdditionalPropsEval additionalProps;
    protected EventType eventType;

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        if (eventType == null) {
            throw new IllegalStateException("Event type not provided");
        }
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new RegressionLinestView(this, agentInstanceViewFactoryContext.getAgentInstanceContext(), expressionXEval, expressionYEval, eventType, additionalProps);
    }

    public void setExpressionXEval(ExprEvaluator expressionXEval) {
        this.expressionXEval = expressionXEval;
    }

    public void setExpressionYEval(ExprEvaluator expressionYEval) {
        this.expressionYEval = expressionYEval;
    }

    public void setAdditionalProps(StatViewAdditionalPropsEval additionalProps) {
        this.additionalProps = additionalProps;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public StatViewAdditionalPropsEval getAdditionalProps() {
        return additionalProps;
    }

    public EventType getEventType() {
        return eventType;
    }

    public ExprEvaluator getExpressionXEval() {
        return expressionXEval;
    }

    public ExprEvaluator getExpressionYEval() {
        return expressionYEval;
    }

    public String getViewName() {
        return ViewEnum.REGRESSION_LINEST.getName();
    }
}
