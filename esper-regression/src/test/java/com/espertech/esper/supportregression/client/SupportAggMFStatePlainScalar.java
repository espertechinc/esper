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
import com.espertech.esper.epl.agg.access.AggregationAccessorForgeGetCodegenContext;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenApplyContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenClearContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.refCol;

public class SupportAggMFStatePlainScalar implements AggregationState {

    private SupportAggMFStatePlainScalarFactory factory;

    private Object lastValue;

    public SupportAggMFStatePlainScalar(SupportAggMFStatePlainScalarFactory factory) {
        this.factory = factory;
    }

    public static void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {
        context.getMembersColumnized().addMember(context.getColumn(), Object.class, "lastValue");
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        lastValue = factory.getEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
    }

    public static void applyEnterCodegen(ExprForge forge, PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        context.getMethod().getBlock().assignRef(refCol("lastValue", context.getColumn()), forge.evaluateCodegen(Object.class, context.getMethod(), context.getSymbols(), context.getClassScope()));
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // ever semantics
    }

    public void clear() {
        lastValue = null;
    }

    public static void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {
        context.getMethod().getBlock().assignRef(refCol("lastValue", context.getColumn()), constantNull());
    }

    public int size() {
        return lastValue == null ? 0 : 1;
    }

    public Object getLastValue() {
        return lastValue;
    }

    public static CodegenExpression getLastValueCodegen(int column) {
        return refCol("lastValue", column);
    }
}
