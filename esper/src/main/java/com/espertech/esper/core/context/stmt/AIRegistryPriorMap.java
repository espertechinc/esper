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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;

import java.util.HashMap;
import java.util.Map;

public class AIRegistryPriorMap implements AIRegistryPrior, ExprPriorEvalStrategy {

    private final Map<Integer, ExprPriorEvalStrategy> strategies;

    public AIRegistryPriorMap() {
        strategies = new HashMap<Integer, ExprPriorEvalStrategy>();
    }

    public void assignService(int num, ExprPriorEvalStrategy value) {
        strategies.put(num, value);
    }

    public void deassignService(int num) {
        strategies.remove(num);
    }

    public int getAgentInstanceCount() {
        return strategies.size();
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, int streamNumber, ExprEvaluator evaluator, int constantIndexNumber) {
        int agentInstanceId = exprEvaluatorContext.getAgentInstanceId();
        ExprPriorEvalStrategy strategy = strategies.get(agentInstanceId);
        return strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext, streamNumber, evaluator, constantIndexNumber);
    }
}
