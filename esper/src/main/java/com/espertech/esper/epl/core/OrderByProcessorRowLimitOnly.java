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
 * An order-by processor that sorts events according to the expressions
 * in the order_by clause.
 */
public class OrderByProcessorRowLimitOnly implements OrderByProcessor {

    private final RowLimitProcessor rowLimitProcessor;

    public OrderByProcessorRowLimitOnly(RowLimitProcessor rowLimitProcessor) {
        this.rowLimitProcessor = rowLimitProcessor;
    }

    public EventBean[] sort(EventBean[] outgoingEvents, EventBean[][] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return rowLimitProcessor.determineLimitAndApply(outgoingEvents);
    }

    public EventBean[] sort(EventBean[] outgoingEvents, EventBean[][] generatingEvents, Object[] groupByKeys, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return rowLimitProcessor.determineLimitAndApply(outgoingEvents);
    }

    public EventBean[] sort(EventBean[] outgoingEvents, List<GroupByRollupKey> currentGenerators, boolean newData, AgentInstanceContext agentInstanceContext, OrderByElement[][] elementsPerLevel) {
        return rowLimitProcessor.determineLimitAndApply(outgoingEvents);
    }

    public Object getSortKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public Object getSortKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, OrderByElement[] elementsForLevel) {
        return null;
    }

    public Object[] getSortKeyPerRow(EventBean[] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public EventBean[] sort(EventBean[] outgoingEvents, Object[] orderKeys, ExprEvaluatorContext exprEvaluatorContext) {
        return rowLimitProcessor.determineLimitAndApply(outgoingEvents);
    }
}
