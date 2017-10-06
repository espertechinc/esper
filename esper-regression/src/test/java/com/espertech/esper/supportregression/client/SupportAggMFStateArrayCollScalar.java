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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.core.CodegenMethod;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.access.AggregationAccessorForgeGetCodegenContext;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenApplyContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenClearContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SupportAggMFStateArrayCollScalar implements AggregationState {

    private final SupportAggMFStateArrayCollScalarFactory factory;
    private final List values = new ArrayList();

    public SupportAggMFStateArrayCollScalar(SupportAggMFStateArrayCollScalarFactory factory) {
        this.factory = factory;
    }

    public static void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {
        context.getMembersColumnized().addMember(context.getColumn(), List.class, "values");
        context.getCtor().getBlock().assignRef(refCol("values", context.getColumn()), newInstance(ArrayList.class));
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        Object value = factory.getEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
        values.add(value);
    }

    public static void applyEnterCodegen(ExprForge forge, PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        context.getMethod().getBlock().declareVar(Object.class, "value", forge.evaluateCodegen(Object.class, context.getMethod(), context.getSymbols(), context.getClassScope()))
                .exprDotMethod(refCol("values", context.getColumn()), "add", ref("value"));
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // ever semantics
    }

    public static void applyLeaveCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        // no code
    }

    public void clear() {
        values.clear();
    }

    public static void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {
        context.getMethod().getBlock().exprDotMethod(refCol("values", context.getColumn()), "clear");
    }

    public int size() {
        return values.size();
    }

    public Object getValueAsArray() {
        Object array = Array.newInstance(factory.getForge().getEvaluationType(), values.size());
        Iterator<Object> it = values.iterator();
        int count = 0;
        for (; it.hasNext(); ) {
            Object value = it.next();
            Array.set(array, count++, value);
        }
        return array;
    }

    public static CodegenExpression getValueAsArrayCodegen(Class resultType, int column, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethodNode method = parent.makeChild(resultType, SupportAggMFStateArrayCollScalar.class, classScope);
        method.getBlock().declareVar(resultType, "array", newArrayByLength(resultType.getComponentType(), exprDotMethod(refCol("values", column), "size")))
                .declareVar(Iterator.class, "it", exprDotMethod(refCol("values", column), "iterator"))
                .declareVar(int.class, "count", constant(0))
                .whileLoop(exprDotMethod(ref("it"), "hasNext"))
                    .assignArrayElement(ref("array"), ref("count"), cast(resultType.getComponentType(), exprDotMethod(ref("it"), "next")))
                    .increment(ref("count"))
                .blockEnd()
                .methodReturn(ref("array"));
        return localMethod(method);
    }

    public Object getValueAsCollection() {
        return values;
    }

    public static void getValueAsCollectionCodegen(AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(refCol("values", context.getColumn()));
    }
}
