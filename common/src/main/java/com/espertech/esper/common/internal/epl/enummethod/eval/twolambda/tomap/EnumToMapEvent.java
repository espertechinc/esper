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
package com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.tomap;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeBasePlain;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumToMapEvent extends EnumForgeBasePlain {

    protected ExprForge secondExpression;

    public EnumToMapEvent(ExprForge innerExpression, int streamCountIncoming, ExprForge secondExpression) {
        super(innerExpression, streamCountIncoming);
        this.secondExpression = secondExpression;
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator first = innerExpression.getExprEvaluator();
        ExprEvaluator second = secondExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                if (enumcoll.isEmpty()) {
                    return Collections.emptyMap();
                }

                Map map = new HashMap();

                Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
                for (EventBean next : beans) {
                    eventsLambda[getStreamNumLambda()] = next;

                    Object key = first.evaluate(eventsLambda, isNewData, context);
                    Object value = second.evaluate(eventsLambda, isNewData, context);
                    map.put(key, value);
                }

                return map;
            }
        };
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(Map.class, EnumToMapEvent.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        CodegenBlock block = methodNode.getBlock()
            .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
            .blockReturn(staticMethod(Collections.class, "emptyMap"));
        block.declareVar(Map.class, "map", newInstance(HashMap.class));
        block.forEach(EventBean.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), ref("next"))
            .declareVar(Object.class, "key", innerExpression.evaluateCodegen(Object.class, methodNode, scope, codegenClassScope))
            .declareVar(Object.class, "value", secondExpression.evaluateCodegen(Object.class, methodNode, scope, codegenClassScope))
            .expression(exprDotMethod(ref("map"), "put", ref("key"), ref("value")));
        block.methodReturn(ref("map"));
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }
}
