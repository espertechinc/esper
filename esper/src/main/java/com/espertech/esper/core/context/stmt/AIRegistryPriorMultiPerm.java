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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;

public class AIRegistryPriorMultiPerm implements AIRegistryPrior, ExprPriorEvalStrategy {

    private final ArrayWrap<ExprPriorEvalStrategy> strategies;
    private int count;

    public AIRegistryPriorMultiPerm() {
        strategies = new ArrayWrap<ExprPriorEvalStrategy>(ExprPriorEvalStrategy.class, 10);
    }

    public void assignService(int num, ExprPriorEvalStrategy value) {
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

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, int streamNumber, ExprEvaluator evaluator, int constantIndexNumber) {
        int agentInstanceId = exprEvaluatorContext.getAgentInstanceId();
        ExprPriorEvalStrategy strategy = strategies.getArray()[agentInstanceId];
        return strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext, streamNumber, evaluator, constantIndexNumber);
    }
}
