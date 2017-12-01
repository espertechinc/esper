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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.epl.agg.factory.AggregationStateLinearForge;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the aggregation accessor that provides the result for the "last" aggregation function without index.
 */
public class AggregationAccessorLastWEval implements AggregationAccessor {
    private final int streamNum;
    private final ExprEvaluator childNode;

    /**
     * Ctor.
     *
     * @param streamNum stream id
     * @param childNode expression
     */
    public AggregationAccessorLastWEval(int streamNum, ExprEvaluator childNode) {
        this.streamNum = streamNum;
        this.childNode = childNode;
    }

    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean bean = ((AggregationStateLinear) state).getLastValue();
        if (bean == null) {
            return null;
        }
        EventBean[] eventsPerStreamBuf = new EventBean[streamNum + 1];
        eventsPerStreamBuf[streamNum] = bean;
        return childNode.evaluate(eventsPerStreamBuf, true, null);
    }

    public static void getValueCodegen(AggregationAccessorLastWEvalForge forge, AggregationStateLinearForge factoryLinear, AggregationAccessorForgeGetCodegenContext context) {
        CodegenMethodNode childExpr = CodegenLegoMethodExpression.codegenExpression(forge.getChildNode(), context.getMethod(), context.getClassScope());
        context.getMethod().getBlock().declareVar(EventBean.class, "bean", factoryLinear.getLastValueCodegen(context.getColumn(), context.getClassScope(), context.getMethod(), context.getNamedMethods()))
                .ifRefNullReturnNull("bean")
                .declareVar(EventBean[].class, "eventsPerStreamBuf", newArrayByLength(EventBean.class, constant(forge.getStreamNum() + 1)))
                .assignArrayElement("eventsPerStreamBuf", constant(forge.getStreamNum()), ref("bean"))
                .methodReturn(localMethod(childExpr, ref("eventsPerStreamBuf"), constant(true), constantNull()));
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean bean = ((AggregationStateLinear) state).getLastValue();
        if (bean == null) {
            return null;
        }
        return Collections.singletonList(bean);
    }

    public static void getEnumerableEventsCodegen(AggregationAccessorLastWEvalForge forge, AggregationStateLinearForge factoryLinear, AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().declareVar(EventBean.class, "bean", factoryLinear.getLastValueCodegen(context.getColumn(), context.getClassScope(), context.getMethod(), context.getNamedMethods()))
                .ifRefNullReturnNull("bean")
                .methodReturn(staticMethod(Collections.class, "singletonList", ref("bean")));
    }

    public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object value = getValue(state, eventsPerStream, isNewData, exprEvaluatorContext);
        if (value == null) {
            return null;
        }
        return Collections.singletonList(value);
    }

    public static void getEnumerableScalarCodegen(AggregationAccessorLastWEvalForge forge, AggregationStateLinearForge stateForge, AggregationAccessorForgeGetCodegenContext context) {
        CodegenMethodNode childExpr = CodegenLegoMethodExpression.codegenExpression(forge.getChildNode(), context.getMethod(), context.getClassScope());
        context.getMethod().getBlock().declareVar(EventBean.class, "bean", stateForge.getLastValueCodegen(context.getColumn(), context.getClassScope(), context.getMethod(), context.getNamedMethods()))
                .ifRefNullReturnNull("bean")
                .declareVar(EventBean[].class, "eventsPerStreamBuf", newArrayByLength(EventBean.class, constant(forge.getStreamNum() + 1)))
                .assignArrayElement("eventsPerStreamBuf", constant(forge.getStreamNum()), ref("bean"))
                .declareVar(Object.class, "value", localMethod(childExpr, ref("eventsPerStreamBuf"), constant(true), constantNull()))
                .ifRefNullReturnNull("value")
                .methodReturn(staticMethod(Collections.class, "singletonList", ref("value")));
    }

    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return ((AggregationStateLinear) state).getLastValue();
    }

    public static void getEnumerableEventCodegen(AggregationAccessorLastWEvalForge forge, AggregationStateLinearForge stateForge, AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(stateForge.getLastValueCodegen(context.getColumn(), context.getClassScope(), context.getMethod(), context.getNamedMethods()));
    }
}