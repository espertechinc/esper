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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumMostLeastFrequentEventForgeEval implements EnumEval {

    private final EnumMostLeastFrequentEventForge forge;
    private final ExprEvaluator innerExpression;

    public EnumMostLeastFrequentEventForgeEval(EnumMostLeastFrequentEventForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return null;
        }

        Map<Object, Integer> items = new LinkedHashMap<Object, Integer>();
        Collection<EventBean> beans = (Collection<EventBean>) enumcoll;

        for (EventBean next : beans) {
            eventsLambda[forge.streamNumLambda] = next;

            Object item = innerExpression.evaluate(eventsLambda, isNewData, context);
            Integer existing = items.get(item);

            if (existing == null) {
                existing = 1;
            } else {
                existing++;
            }
            items.put(item, existing);
        }

        return getEnumMostLeastFrequentResult(items, forge.isMostFrequent);
    }

    public static CodegenExpression codegen(EnumMostLeastFrequentEventForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        Class returnType = JavaClassHelper.getBoxedType(forge.innerExpression.getEvaluationType());
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(returnType, EnumMostLeastFrequentEventForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        CodegenBlock block = methodNode.getBlock()
                .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                .blockReturn(constantNull())
                .declareVar(Map.class, "items", newInstance(LinkedHashMap.class));
        CodegenBlock forEach = block.forEach(EventBean.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda), ref("next"))
                .declareVar(Object.class, "item", forge.innerExpression.evaluateCodegen(Object.class, methodNode, scope, codegenClassScope))
                .declareVar(Integer.class, "existing", cast(Integer.class, exprDotMethod(ref("items"), "get", ref("item"))))
                .ifCondition(equalsNull(ref("existing")))
                .assignRef("existing", constant(1))
                .ifElse()
                .increment("existing")
                .blockEnd()
                .exprDotMethod(ref("items"), "put", ref("item"), ref("existing"));
        block.methodReturn(cast(returnType, staticMethod(EnumMostLeastFrequentEventForgeEval.class, "getEnumMostLeastFrequentResult", ref("items"), constant(forge.isMostFrequent))));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param items items
     * @param mostFrequent flag
     * @return value
     */
    public static Object getEnumMostLeastFrequentResult(Map<Object, Integer> items, boolean mostFrequent) {
        if (mostFrequent) {
            Object maxKey = null;
            int max = Integer.MIN_VALUE;
            for (Map.Entry<Object, Integer> entry : items.entrySet()) {
                if (entry.getValue() > max) {
                    maxKey = entry.getKey();
                    max = entry.getValue();
                }
            }
            return maxKey;
        }

        int min = Integer.MAX_VALUE;
        Object minKey = null;
        for (Map.Entry<Object, Integer> entry : items.entrySet()) {
            if (entry.getValue() < min) {
                minKey = entry.getKey();
                min = entry.getValue();
            }
        }
        return minKey;
    }
}
