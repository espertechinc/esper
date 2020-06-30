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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForgeGetCodegenContext;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;

import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the aggregation accessor that provides the result for the "first" aggregation function without index.
 */
public class AggregationAccessorFirstWEval {
    public static void getValueCodegen(AggregationAccessorFirstWEvalForge forge, AggregationStateLinearForge accessStateFactory, AggregationAccessorForgeGetCodegenContext context) {
        CodegenMethod childExpr = CodegenLegoMethodExpression.codegenExpression(forge.getChildNode(), context.getMethod(), context.getClassScope());
        context.getMethod().getBlock().declareVar(EventBean.EPTYPE, "bean", accessStateFactory.getAggregatorLinear().getFirstValueCodegen(context.getClassScope(), context.getMethod()))
                .ifRefNullReturnNull("bean")
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStreamBuf", newArrayByLength(EventBean.EPTYPE, constant(forge.getStreamNum() + 1)))
                .assignArrayElement("eventsPerStreamBuf", constant(forge.getStreamNum()), ref("bean"))
                .methodReturn(localMethod(childExpr, ref("eventsPerStreamBuf"), constant(true), constantNull()));
    }

    public static void getEnumerableEventsCodegen(AggregationAccessorFirstWEvalForge forge, AggregationStateLinearForge stateForge, AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().declareVar(EventBean.EPTYPE, "bean", stateForge.getAggregatorLinear().getFirstValueCodegen(context.getClassScope(), context.getMethod()))
                .ifRefNullReturnNull("bean")
                .methodReturn(staticMethod(Collections.class, "singletonList", ref("bean")));
    }

    public static void getEnumerableScalarCodegen(AggregationAccessorFirstWEvalForge forge, AggregationStateLinearForge accessStateFactory, AggregationAccessorForgeGetCodegenContext context) {
        CodegenMethod childExpr = CodegenLegoMethodExpression.codegenExpression(forge.getChildNode(), context.getMethod(), context.getClassScope());
        context.getMethod().getBlock().declareVar(EventBean.EPTYPE, "bean", accessStateFactory.getAggregatorLinear().getFirstValueCodegen(context.getClassScope(), context.getMethod()))
                .ifRefNullReturnNull("bean")
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStreamBuf", newArrayByLength(EventBean.EPTYPE, constant(forge.getStreamNum() + 1)))
                .assignArrayElement("eventsPerStreamBuf", constant(forge.getStreamNum()), ref("bean"))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "value", localMethod(childExpr, ref("eventsPerStreamBuf"), constant(true), constantNull()))
                .ifRefNullReturnNull("value")
                .methodReturn(staticMethod(Collections.class, "singletonList", ref("value")));
    }

    public static void getEnumerableEventCodegen(AggregationAccessorFirstWEvalForge forge, AggregationStateLinearForge stateForge, AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(stateForge.getAggregatorLinear().getFirstValueCodegen(context.getClassScope(), context.getMethod()));
    }
}