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
package com.espertech.esper.common.internal.context.airegistry;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.ArrayWrap;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;

import java.util.Collection;

public class AIRegistryTableAccessMultiPerm implements AIRegistryTableAccess {

    private final ArrayWrap<ExprTableEvalStrategy> strategies;
    private int count;

    AIRegistryTableAccessMultiPerm() {
        strategies = new ArrayWrap<>(ExprTableEvalStrategy.class, 8);
    }

    public void assignService(int num, ExprTableEvalStrategy subselectStrategy) {
        AIRegistryUtil.checkExpand(num, strategies);
        strategies.getArray()[num] = subselectStrategy;
        count++;
    }

    public void deassignService(int num) {
        strategies.getArray()[num] = null;
        count--;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return strategies.getArray()[exprEvaluatorContext.getAgentInstanceId()].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategies.getArray()[context.getAgentInstanceId()].evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategies.getArray()[context.getAgentInstanceId()].evaluateGetEventBean(eventsPerStream, isNewData, context);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategies.getArray()[context.getAgentInstanceId()].evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategies.getArray()[context.getAgentInstanceId()].evaluateTypableSingle(eventsPerStream, isNewData, context);
    }

    public AggregationRow getAggregationRow(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategies.getArray()[context.getAgentInstanceId()].getAggregationRow(eventsPerStream, isNewData, context);
    }

    public int getInstanceCount() {
        return count;
    }
}
