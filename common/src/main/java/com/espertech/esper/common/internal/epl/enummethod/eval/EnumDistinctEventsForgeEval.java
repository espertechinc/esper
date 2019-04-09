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
package com.espertech.esper.common.internal.epl.enummethod.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

public class EnumDistinctEventsForgeEval implements EnumEval {

    private final EnumDistinctEventsForge forge;
    private final ExprEvaluator innerExpression;

    public EnumDistinctEventsForgeEval(EnumDistinctEventsForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
        if (beans.size() <= 1) {
            return beans;
        }

        Map<Object, EventBean> distinct = new LinkedHashMap<>();
        for (EventBean next : beans) {
            eventsLambda[forge.streamNumLambda] = next;

            Object comparable = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (!distinct.containsKey(comparable)) {
                distinct.put(comparable, next);
            }
        }

        return distinct.values();
    }

    public static CodegenExpression codegen(EnumDistinctEventsForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        Class innerType = JavaClassHelper.getBoxedType(forge.innerExpression.getEvaluationType());

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(Collection.class, EnumDistinctEventsForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        CodegenBlock block = methodNode.getBlock()
            .ifCondition(relational(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"), LE, constant(1)))
            .blockReturn(EnumForgeCodegenNames.REF_ENUMCOLL)
            .declareVar(Map.class, "distinct", newInstance(LinkedHashMap.class));

        CodegenExpression expr = forge.innerExpression.evaluateCodegen(innerType, methodNode, scope, codegenClassScope);
        CodegenBlock loop = block.forEach(EventBean.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL);
        {
            loop.assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda), ref("next"));
            if (!innerType.isArray()) {
                loop.declareVar(innerType, "comparable", expr);
            } else {
                Class arrayMK = MultiKeyPlanner.getMKClassForComponentType(innerType.getComponentType());
                loop.declareVar(arrayMK, "comparable", newInstance(arrayMK, expr));
            }
            loop.ifCondition(not(exprDotMethod(ref("distinct"), "containsKey", ref("comparable"))))
                .expression(exprDotMethod(ref("distinct"), "put", ref("comparable"), ref("next")))
                .blockEnd();
        }

        block.methodReturn(exprDotMethod(ref("distinct"), "values"));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }
}
