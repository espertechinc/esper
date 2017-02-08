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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AIRegistryPreviousMap implements AIRegistryPrevious, ExprPreviousEvalStrategy {

    private final Map<Integer, ExprPreviousEvalStrategy> strategies;

    public AIRegistryPreviousMap() {
        strategies = new HashMap<Integer, ExprPreviousEvalStrategy>();
    }

    public void assignService(int num, ExprPreviousEvalStrategy value) {
        strategies.put(num, value);
    }

    public void deassignService(int num) {
        strategies.remove(num);
    }

    public int getAgentInstanceCount() {
        return strategies.size();
    }

    public Object evaluate(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        int agentInstanceId = exprEvaluatorContext.getAgentInstanceId();
        ExprPreviousEvalStrategy strategy = strategies.get(agentInstanceId);
        return strategy.evaluate(eventsPerStream, exprEvaluatorContext);
    }

    public Collection<EventBean> evaluateGetCollEvents(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        int agentInstanceId = context.getAgentInstanceId();
        ExprPreviousEvalStrategy strategy = strategies.get(agentInstanceId);
        return strategy.evaluateGetCollEvents(eventsPerStream, context);
    }

    public Collection evaluateGetCollScalar(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        int agentInstanceId = context.getAgentInstanceId();
        ExprPreviousEvalStrategy strategy = strategies.get(agentInstanceId);
        return strategy.evaluateGetCollScalar(eventsPerStream, context);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        int agentInstanceId = context.getAgentInstanceId();
        ExprPreviousEvalStrategy strategy = strategies.get(agentInstanceId);
        return strategy.evaluateGetEventBean(eventsPerStream, context);
    }
}
