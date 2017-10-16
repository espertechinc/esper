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
package com.espertech.esper.epl.core.orderby;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.rollup.GroupByRollupKey;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.epl.core.orderby.OrderByProcessorCodegenNames.*;
import static com.espertech.esper.epl.core.orderby.OrderByProcessorOrderedLimitForge.REF_ROWLIMITPROCESSOR;

/**
 * An order-by processor that sorts events according to the expressions
 * in the order_by clause.
 */
public class OrderByProcessorRowLimitOnly implements OrderByProcessor {

    private final RowLimitProcessor rowLimitProcessor;

    public OrderByProcessorRowLimitOnly(RowLimitProcessor rowLimitProcessor) {
        this.rowLimitProcessor = rowLimitProcessor;
    }

    public EventBean[] sortPlain(EventBean[] outgoingEvents, EventBean[][] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService) {
        return rowLimitProcessor.determineLimitAndApply(outgoingEvents);
    }

    public static void sortPlainCodegen(CodegenMethodNode method) {
        determineLimitAndApplyCodegen(method);
    }

    public EventBean[] sortWGroupKeys(EventBean[] outgoingEvents, EventBean[][] generatingEvents, Object[] groupByKeys, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService) {
        return rowLimitProcessor.determineLimitAndApply(outgoingEvents);
    }

    static void sortWGroupKeysCodegen(CodegenMethodNode method) {
        determineLimitAndApplyCodegen(method);
    }

    public EventBean[] sortRollup(EventBean[] outgoingEvents, List<GroupByRollupKey> currentGenerators, boolean newData, AgentInstanceContext agentInstanceContext, AggregationService aggregationService) {
        return rowLimitProcessor.determineLimitAndApply(outgoingEvents);
    }

    static void sortRollupCodegen(CodegenMethodNode method) {
        determineLimitAndApplyCodegen(method);
    }

    public Object getSortKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public Object getSortKeyRollup(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, AggregationGroupByRollupLevel level) {
        return null;
    }

    public EventBean[] sortWOrderKeys(EventBean[] outgoingEvents, Object[] orderKeys, ExprEvaluatorContext exprEvaluatorContext) {
        return rowLimitProcessor.determineLimitAndApply(outgoingEvents);
    }

    public EventBean[] sortTwoKeys(EventBean first, Object sortKeyFirst, EventBean second, Object sortKeySecond) {
        return rowLimitProcessor.determineApplyLimit2Events(first, second);
    }

    static void sortTwoKeysCodegen(CodegenMethodNode method) {
        method.getBlock().methodReturn(exprDotMethod(REF_ROWLIMITPROCESSOR, "determineApplyLimit2Events", REF_ORDERFIRSTEVENT, REF_ORDERSECONDEVENT));
    }

    static void sortWOrderKeysCodegen(CodegenMethodNode method) {
        determineLimitAndApplyCodegen(method);
    }

    private static void determineLimitAndApplyCodegen(CodegenMethodNode method) {
        method.getBlock().methodReturn(exprDotMethod(REF_ROWLIMITPROCESSOR, "determineLimitAndApply", REF_OUTGOINGEVENTS));
    }
}
