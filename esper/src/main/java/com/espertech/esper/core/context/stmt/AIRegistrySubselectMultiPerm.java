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
import com.espertech.esper.epl.expression.subquery.ExprSubselectStrategy;

import java.util.Collection;

public class AIRegistrySubselectMultiPerm implements AIRegistrySubselect, ExprSubselectStrategy {

    private final ArrayWrap<ExprSubselectStrategy> strategies;
    private int count;

    public AIRegistrySubselectMultiPerm() {
        strategies = new ArrayWrap<ExprSubselectStrategy>(ExprSubselectStrategy.class, 10);
    }

    public void assignService(int num, ExprSubselectStrategy subselectStrategy) {
        AIRegistryUtil.checkExpand(num, strategies);
        strategies.getArray()[num] = subselectStrategy;
        count++;
    }

    public void deassignService(int num) {
        strategies.getArray()[num] = null;
        count--;
    }

    public Collection<EventBean> evaluateMatching(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        int agentInstanceId = exprEvaluatorContext.getAgentInstanceId();
        ExprSubselectStrategy strategy = strategies.getArray()[agentInstanceId];
        return strategy.evaluateMatching(eventsPerStream, exprEvaluatorContext);
    }

    public int getAgentInstanceCount() {
        return count;
    }
}
