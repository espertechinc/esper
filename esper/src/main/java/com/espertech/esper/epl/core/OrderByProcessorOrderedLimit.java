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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.rollup.GroupByRollupKey;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.List;

/**
 * Sorter and row limiter in one: sorts using a sorter and row limits
 */
public class OrderByProcessorOrderedLimit implements OrderByProcessor {
    private final OrderByProcessorImpl orderByProcessor;
    private final RowLimitProcessor rowLimitProcessor;

    /**
     * Ctor.
     *
     * @param orderByProcessor  the sorter
     * @param rowLimitProcessor the row limiter
     */
    public OrderByProcessorOrderedLimit(OrderByProcessorImpl orderByProcessor, RowLimitProcessor rowLimitProcessor) {
        this.orderByProcessor = orderByProcessor;
        this.rowLimitProcessor = rowLimitProcessor;
    }

    public EventBean[] sort(EventBean[] outgoingEvents, EventBean[][] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        rowLimitProcessor.determineCurrentLimit();

        if (rowLimitProcessor.getCurrentRowLimit() == 1 &&
                rowLimitProcessor.getCurrentOffset() == 0 &&
                outgoingEvents != null && outgoingEvents.length > 1) {
            EventBean minmax = orderByProcessor.determineLocalMinMax(outgoingEvents, generatingEvents, isNewData, exprEvaluatorContext);
            return new EventBean[]{minmax};
        }

        EventBean[] sorted = orderByProcessor.sort(outgoingEvents, generatingEvents, isNewData, exprEvaluatorContext);
        return rowLimitProcessor.applyLimit(sorted);
    }

    public EventBean[] sort(EventBean[] outgoingEvents, List<GroupByRollupKey> currentGenerators, boolean newData, AgentInstanceContext agentInstanceContext, OrderByElement[][] elementsPerLevel) {
        EventBean[] sorted = orderByProcessor.sort(outgoingEvents, currentGenerators, newData, agentInstanceContext, elementsPerLevel);
        return rowLimitProcessor.determineLimitAndApply(sorted);
    }

    public EventBean[] sort(EventBean[] outgoingEvents, EventBean[][] generatingEvents, Object[] groupByKeys, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean[] sorted = orderByProcessor.sort(outgoingEvents, generatingEvents, groupByKeys, isNewData, exprEvaluatorContext);
        return rowLimitProcessor.determineLimitAndApply(sorted);
    }

    public Object getSortKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return orderByProcessor.getSortKey(eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Object getSortKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, OrderByElement[] elementsForLevel) {
        return orderByProcessor.getSortKey(eventsPerStream, isNewData, exprEvaluatorContext, elementsForLevel);
    }

    public Object[] getSortKeyPerRow(EventBean[] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return orderByProcessor.getSortKeyPerRow(generatingEvents, isNewData, exprEvaluatorContext);
    }

    public EventBean[] sort(EventBean[] outgoingEvents, Object[] orderKeys, ExprEvaluatorContext exprEvaluatorContext) {
        rowLimitProcessor.determineCurrentLimit();

        if (rowLimitProcessor.getCurrentRowLimit() == 1 &&
                rowLimitProcessor.getCurrentOffset() == 0 &&
                outgoingEvents != null && outgoingEvents.length > 1) {
            EventBean minmax = orderByProcessor.determineLocalMinMax(outgoingEvents, orderKeys);
            return new EventBean[]{minmax};
        }

        EventBean[] sorted = orderByProcessor.sort(outgoingEvents, orderKeys, exprEvaluatorContext);
        return rowLimitProcessor.applyLimit(sorted);
    }
}
