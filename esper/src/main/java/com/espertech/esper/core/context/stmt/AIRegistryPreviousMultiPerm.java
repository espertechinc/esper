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
package com.espertech.esper.core.context.stmt;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.ArrayWrap;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;

import java.util.Collection;

public class AIRegistryPreviousMultiPerm implements AIRegistryPrevious, ExprPreviousEvalStrategy {

    private final ArrayWrap<ExprPreviousEvalStrategy> strategies;
    private int count;

    public AIRegistryPreviousMultiPerm() {
        strategies = new ArrayWrap<ExprPreviousEvalStrategy>(ExprPreviousEvalStrategy.class, 10);
    }

    public void assignService(int num, ExprPreviousEvalStrategy value) {
        AIRegistryUtil.checkExpand(num, strategies);
        strategies.getArray()[num] = value;
        count++;
    }

    public void deassignService(int num) {
        strategies.getArray()[num] = null;
        count--;
    }

    public int getAgentInstanceCount() {
        return count;
    }

    public Object evaluate(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        int agentInstanceId = exprEvaluatorContext.getAgentInstanceId();
        ExprPreviousEvalStrategy strategy = strategies.getArray()[agentInstanceId];
        return strategy.evaluate(eventsPerStream, exprEvaluatorContext);
    }

    public Collection<EventBean> evaluateGetCollEvents(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        int agentInstanceId = context.getAgentInstanceId();
        ExprPreviousEvalStrategy strategy = strategies.getArray()[agentInstanceId];
        return strategy.evaluateGetCollEvents(eventsPerStream, context);
    }

    public Collection evaluateGetCollScalar(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        int agentInstanceId = context.getAgentInstanceId();
        ExprPreviousEvalStrategy strategy = strategies.getArray()[agentInstanceId];
        return strategy.evaluateGetCollScalar(eventsPerStream, context);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        int agentInstanceId = context.getAgentInstanceId();
        ExprPreviousEvalStrategy strategy = strategies.getArray()[agentInstanceId];
        return strategy.evaluateGetEventBean(eventsPerStream, context);
    }
}
