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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.factory.AggregationStateLinearForge;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the aggregation accessor that provides the result for the "first" and "last" aggregation function with index.
 */
public class AggregationAccessorFirstLastIndexWEval implements AggregationAccessor {
    private final int streamNum;
    private final ExprEvaluator childNode;
    private final ExprEvaluator indexNode;
    private final int constant;
    private final boolean isFirst;

    /**
     * Ctor.
     *
     * @param streamNum stream id
     * @param childNode expression
     * @param indexNode index expression
     * @param constant  constant index
     * @param isFirst   true if returning first, false for returning last
     */
    public AggregationAccessorFirstLastIndexWEval(int streamNum, ExprEvaluator childNode, ExprEvaluator indexNode, int constant, boolean isFirst) {
        this.streamNum = streamNum;
        this.childNode = childNode;
        this.indexNode = indexNode;
        this.constant = constant;
        this.isFirst = isFirst;
    }

    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean bean = getBeanFirstLastIndex(state);
        if (bean == null) {
            return null;
        }
        EventBean[] eventsPerStreamBuf = new EventBean[streamNum + 1];
        eventsPerStreamBuf[streamNum] = bean;
        return childNode.evaluate(eventsPerStreamBuf, true, null);
    }

    public static void getValueCodegen(AggregationAccessorFirstLastIndexWEvalForge forge, AggregationAccessorForgeGetCodegenContext context) {
        AggregationStateLinearForge stateForge = (AggregationStateLinearForge) context.getAccessStateForge();
        CodegenMethodNode getBeanFirstLastIndex = getBeanFirstLastIndexCodegen(forge, context.getColumn(), context.getClassScope(), stateForge, context.getMethod(), context.getNamedMethods());
        context.getMethod().getBlock().declareVar(EventBean.class, "bean", localMethod(getBeanFirstLastIndex))
                .ifRefNullReturnNull("bean")
                .declareVar(EventBean[].class, "eventsPerStreamBuf", newArrayByLength(EventBean.class, constant(forge.getStreamNum() + 1)))
                .assignArrayElement("eventsPerStreamBuf", constant(forge.getStreamNum()), ref("bean"))
                .methodReturn(localMethod(CodegenLegoMethodExpression.codegenExpression(forge.getChildNode(), context.getMethod(), context.getClassScope()), ref("eventsPerStreamBuf"), constantTrue(), constantNull()));
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean bean = getBeanFirstLastIndex(state);
        if (bean == null) {
            return null;
        }
        return Collections.singletonList(bean);
    }

    public static void getEnumerableEventsCodegen(AggregationAccessorFirstLastIndexWEvalForge forge, AggregationAccessorForgeGetCodegenContext context) {
        AggregationStateLinearForge stateForge = (AggregationStateLinearForge) context.getAccessStateForge();
        CodegenMethodNode getBeanFirstLastIndex = getBeanFirstLastIndexCodegen(forge, context.getColumn(), context.getClassScope(), stateForge, context.getMethod(), context.getNamedMethods());
        context.getMethod().getBlock().declareVar(EventBean.class, "bean", localMethod(getBeanFirstLastIndex))
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

    public static void getEnumerableScalarCodegen(AggregationAccessorFirstLastIndexWEvalForge forge, AggregationAccessorForgeGetCodegenContext context) {
        AggregationStateLinearForge stateForge = (AggregationStateLinearForge) context.getAccessStateForge();
        CodegenMethodNode getBeanFirstLastIndex = getBeanFirstLastIndexCodegen(forge, context.getColumn(), context.getClassScope(), stateForge, context.getMethod(), context.getNamedMethods());
        context.getMethod().getBlock().declareVar(EventBean.class, "bean", localMethod(getBeanFirstLastIndex))
                .ifRefNullReturnNull("bean")
                .declareVar(EventBean[].class, "eventsPerStreamBuf", newArrayByLength(EventBean.class, constant(forge.getStreamNum() + 1)))
                .assignArrayElement("eventsPerStreamBuf", constant(forge.getStreamNum()), ref("bean"))
                .declareVar(Object.class, "value", localMethod(CodegenLegoMethodExpression.codegenExpression(forge.getChildNode(), context.getMethod(), context.getClassScope()), ref("eventsPerStreamBuf"), constantTrue(), constantNull()))
                .ifRefNullReturnNull("value")
                .methodReturn(staticMethod(Collections.class, "singletonList", ref("value")));
    }

    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return getBeanFirstLastIndex(state);
    }

    public static void getEnumerableEventCodegen(AggregationAccessorFirstLastIndexWEvalForge forge, AggregationAccessorForgeGetCodegenContext context) {
        AggregationStateLinearForge stateForge = (AggregationStateLinearForge) context.getAccessStateForge();
        CodegenMethodNode getBeanFirstLastIndex = getBeanFirstLastIndexCodegen(forge, context.getColumn(), context.getClassScope(), stateForge, context.getMethod(), context.getNamedMethods());
        context.getMethod().getBlock().methodReturn(localMethod(getBeanFirstLastIndex));
    }

    private EventBean getBeanFirstLastIndex(AggregationState state) {
        int index = constant;
        if (index == -1) {
            Object result = indexNode.evaluate(null, true, null);
            if ((result == null) || (!(result instanceof Integer))) {
                return null;
            }
            index = (Integer) result;
        }
        if (isFirst) {
            return ((AggregationStateLinear) state).getFirstNthValue(index);
        } else {
            return ((AggregationStateLinear) state).getLastNthValue(index);
        }
    }

    private static CodegenMethodNode getBeanFirstLastIndexCodegen(AggregationAccessorFirstLastIndexWEvalForge forge, int column, CodegenClassScope classScope, AggregationStateLinearForge stateForge, CodegenMethodNode parent, CodegenNamedMethods namedMethods) {
        CodegenMethodNode method = parent.makeChild(EventBean.class, AggregationAccessorFirstLastIndexWEval.class, classScope);
        if (forge.getConstant() == -1) {
            Class evalType = forge.getIndexNode().getEvaluationType();
            method.getBlock().declareVar(evalType, "indexResult", localMethod(CodegenLegoMethodExpression.codegenExpression(forge.getIndexNode(), method, classScope), constantNull(), constantTrue(), constantNull()));
            if (!evalType.isPrimitive()) {
                method.getBlock().ifRefNullReturnNull("indexResult");
            }
            method.getBlock().declareVar(int.class, "index", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("indexResult"), evalType));
        } else {
            method.getBlock().declareVar(int.class, "index", constant(forge.getConstant()));
        }
        CodegenExpression value = forge.isFirst() ? stateForge.getFirstNthValueCodegen(ref("index"), column, method, classScope, namedMethods) : stateForge.getLastNthValueCodegen(ref("index"), column, method, classScope, namedMethods);
        method.getBlock().methodReturn(value);
        return method;
    }
}