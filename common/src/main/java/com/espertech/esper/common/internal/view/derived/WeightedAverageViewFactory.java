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
 * Factory for {@link WeightedAverageView} instances.
 */
public class WeightedAverageViewFactory implements ViewFactory {
    protected ExprEvaluator fieldNameXEvaluator;
    protected ExprEvaluator fieldNameWeightEvaluator;

    protected StatViewAdditionalPropsEval additionalProps;

    protected EventType eventType;

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        if (eventType == null) {
            throw new IllegalStateException("Event type not provided");
        }
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new WeightedAverageView(this, agentInstanceViewFactoryContext);
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setFieldNameXEvaluator(ExprEvaluator fieldNameXEvaluator) {
        this.fieldNameXEvaluator = fieldNameXEvaluator;
    }

    public void setFieldNameWeightEvaluator(ExprEvaluator fieldNameWeightEvaluator) {
        this.fieldNameWeightEvaluator = fieldNameWeightEvaluator;
    }

    public void setAdditionalProps(StatViewAdditionalPropsEval additionalProps) {
        this.additionalProps = additionalProps;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public ExprEvaluator getFieldNameXEvaluator() {
        return fieldNameXEvaluator;
    }

    public ExprEvaluator getFieldNameWeightEvaluator() {
        return fieldNameWeightEvaluator;
    }

    public StatViewAdditionalPropsEval getAdditionalProps() {
        return additionalProps;
    }

    public String getViewName() {
        return ViewEnum.WEIGHTED_AVERAGE.getName();
    }
}
