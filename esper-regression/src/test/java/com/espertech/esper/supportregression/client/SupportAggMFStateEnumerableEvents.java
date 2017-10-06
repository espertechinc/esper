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
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenApplyContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenClearContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext;
import com.espertech.esper.supportregression.bean.SupportBean;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SupportAggMFStateEnumerableEvents implements AggregationState {

    private List<EventBean> events = new ArrayList<EventBean>();

    public static void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {
        context.getMembersColumnized().addMember(context.getColumn(), List.class, "events");
        context.getCtor().getBlock().assignRef(refCol("events", context.getColumn()), newInstance(ArrayList.class));
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        events.add(eventsPerStream[0]);
    }

    public static void applyEnterCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        context.getMethod().getBlock().exprDotMethod(refCol("events", context.getColumn()), "add", arrayAtIndex(context.getSymbols().getAddEPS(context.getMethod()), constant(0)));
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // ever semantics
    }

    public static void applyLeaveCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        // ever semantics
    }

    public void clear() {
        events.clear();
    }

    public static void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {
        context.getMethod().getBlock().exprDotMethod(refCol("events", context.getColumn()), "clear");
    }

    public int size() {
        return events.size();
    }

    public List<EventBean> getEvents() {
        return events;
    }

    public static CodegenExpression getEventsCodegen(int column) {
        return refCol("events", column);
    }

    public Object getEventsAsUnderlyingArray() {
        SupportBean[] array = new SupportBean[events.size()];

        Iterator<EventBean> it = events.iterator();
        int count = 0;
        for (; it.hasNext(); ) {
            EventBean bean = it.next();
            Array.set(array, count++, bean.getUnderlying());
        }
        return array;
    }

    public static CodegenExpression getEventsAsUnderlyingArrayCodegen(int column, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethodNode method = parent.makeChild(SupportBean[].class, SupportAggMFStateEnumerableEvents.class, classScope);
        method.getBlock().declareVar(SupportBean[].class, "array", newArrayByLength(SupportBean.class, exprDotMethod(refCol("events", column), "size")))
                .declareVar(Iterator.class, "it", exprDotMethod(refCol("events", column), "iterator"))
                .declareVar(int.class, "count", constant(0))
                .whileLoop(exprDotMethod(ref("it"), "hasNext"))
                    .declareVar(EventBean.class, "bean", cast(EventBean.class, exprDotMethod(ref("it"), "next")))
                    .assignArrayElement("array", ref("count"), cast(SupportBean.class, exprDotUnderlying(ref("bean"))))
                    .increment(ref("count"))
                .blockEnd()
                .methodReturn(ref("array"));
        return localMethod(method);
    }
}
