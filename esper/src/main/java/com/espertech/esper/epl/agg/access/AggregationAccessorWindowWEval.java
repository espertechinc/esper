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
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.factory.AggregationStateLinearForge;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the aggregation accessor that provides the result for the "window" aggregation function.
 */
public class AggregationAccessorWindowWEval implements AggregationAccessor {
    private final int streamNum;
    private final ExprEvaluator childNode;
    private final Class componentType;

    /**
     * Ctor.
     *
     * @param streamNum     stream id
     * @param childNode     expression
     * @param componentType type
     */
    public AggregationAccessorWindowWEval(int streamNum, ExprEvaluator childNode, Class componentType) {
        this.streamNum = streamNum;
        this.childNode = childNode;
        this.componentType = componentType;
    }

    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateLinear linear = (AggregationStateLinear) state;
        if (linear.size() == 0) {
            return null;
        }
        Object array = Array.newInstance(componentType, linear.size());
        Iterator<EventBean> it = linear.iterator();
        int count = 0;
        EventBean[] eventsPerStreamBuf = new EventBean[streamNum + 1];
        while (it.hasNext()) {
            EventBean bean = it.next();
            eventsPerStreamBuf[streamNum] = bean;
            Object value = childNode.evaluate(eventsPerStreamBuf, true, null);
            Array.set(array, count++, value);
        }

        return array;
    }

    public static void getValueCodegen(AggregationAccessorWindowWEvalForge forge, AggregationStateLinearForge accessStateFactory, AggregationAccessorForgeGetCodegenContext context) {
        CodegenExpression size = accessStateFactory.sizeCodegen(context.getColumn());
        CodegenExpression iterator = accessStateFactory.iteratorCodegen(context.getColumn(), context.getClassScope(), context.getMethod(), context.getNamedMethods());
        CodegenMethodNode childExpr = CodegenLegoMethodExpression.codegenExpression(forge.getChildNode(), context.getMethod(), context.getClassScope());

        context.getMethod().getBlock().ifCondition(equalsIdentity(size, constant(0))).blockReturn(constantNull())
                .declareVar(JavaClassHelper.getArrayType(forge.getComponentType()), "array", newArrayByLength(forge.getComponentType(), size))
                .declareVar(int.class, "count", constant(0))
                .declareVar(Iterator.class, "it", iterator)
                .declareVar(EventBean[].class, "eventsPerStreamBuf", newArrayByLength(EventBean.class, constant(forge.getStreamNum() + 1)))
                .whileLoop(exprDotMethod(ref("it"), "hasNext"))
                .declareVar(EventBean.class, "bean", cast(EventBean.class, exprDotMethod(ref("it"), "next")))
                .assignArrayElement("eventsPerStreamBuf", constant(forge.getStreamNum()), ref("bean"))
                .assignArrayElement(ref("array"), ref("count"), localMethod(childExpr, ref("eventsPerStreamBuf"), constant(true), constantNull()))
                .increment("count")
                .blockEnd()
                .methodReturn(ref("array"));
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateLinear linear = (AggregationStateLinear) state;
        if (linear.size() == 0) {
            return null;
        }
        return linear.collectionReadOnly();
    }

    public static void getEnumerableEventsCodegen(AggregationAccessorWindowWEvalForge forge, AggregationStateLinearForge stateForge, AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().ifCondition(equalsIdentity(stateForge.sizeCodegen(context.getColumn()), constant(0)))
                .blockReturn(constantNull())
                .methodReturn(stateForge.collectionReadOnlyCodegen(context.getColumn(), context.getMethod(), context.getClassScope(), context.getNamedMethods()));
    }

    public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateLinear linear = (AggregationStateLinear) state;
        int size = linear.size();
        if (size == 0) {
            return null;
        }
        List<Object> values = new ArrayList<Object>(size);
        Iterator<EventBean> it = linear.iterator();
        EventBean[] eventsPerStreamBuf = new EventBean[streamNum + 1];
        for (; it.hasNext(); ) {
            EventBean bean = it.next();
            eventsPerStreamBuf[streamNum] = bean;
            Object value = childNode.evaluate(eventsPerStreamBuf, true, null);
            values.add(value);
        }

        return values;
    }

    public static void getEnumerableScalarCodegen(AggregationAccessorWindowWEvalForge forge, AggregationStateLinearForge stateForge, AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().declareVar(int.class, "size", stateForge.sizeCodegen(context.getColumn()))
                .ifCondition(equalsIdentity(ref("size"), constant(0))).blockReturn(constantNull())
                .declareVar(List.class, "values", newInstance(ArrayList.class, ref("size")))
                .declareVar(Iterator.class, "it", stateForge.iteratorCodegen(context.getColumn(), context.getClassScope(), context.getMethod(), context.getNamedMethods()))
                .declareVar(EventBean[].class, "eventsPerStreamBuf", newArrayByLength(EventBean.class, constant(forge.getStreamNum() + 1)))
                .whileLoop(exprDotMethod(ref("it"), "hasNext"))
                .declareVar(EventBean.class, "bean", cast(EventBean.class, exprDotMethod(ref("it"), "next")))
                .assignArrayElement("eventsPerStreamBuf", constant(forge.getStreamNum()), ref("bean"))
                .declareVar(JavaClassHelper.getBoxedType(forge.getChildNode().getEvaluationType()), "value", localMethod(CodegenLegoMethodExpression.codegenExpression(forge.getChildNode(), context.getMethod(), context.getClassScope()), ref("eventsPerStreamBuf"), constantTrue(), constantNull()))
                .exprDotMethod(ref("values"), "add", ref("value"))
                .blockEnd()
                .methodReturn(ref("values"));
    }

    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }
}