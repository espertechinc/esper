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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenApplyContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenClearContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SupportAggMFStateSingleEvent implements AggregationState {

    private EventBean event;

    public static void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {
        context.getMembersColumnized().addMember(context.getColumn(), EventBean.class, "event");
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        event = eventsPerStream[0];
    }

    public static void applyEnterCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        context.getMethod().getBlock().assignRef(refCol("event", context.getColumn()), arrayAtIndex(context.getSymbols().getAddEPS(context.getMethod()), constant(0)));
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // ever semantics
    }

    public static void applyLeaveCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        // no code
    }

    public void clear() {
        event = null;
    }

    public static void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {
        context.getMethod().getBlock().assignRef(refCol("event", context.getColumn()), constantNull());
    }

    public int size() {
        return event == null ? 0 : 1;
    }

    public EventBean getEvent() {
        return event;
    }

    public static CodegenExpression getEventCodegen(int column) {
        return refCol("event", column);
    }
}
