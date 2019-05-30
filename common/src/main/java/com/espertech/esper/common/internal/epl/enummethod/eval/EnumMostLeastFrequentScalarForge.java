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
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumMostLeastFrequentScalarForge extends EnumForgeBase implements EnumEval {

    private final boolean isMostFrequent;
    private final Class returnType;

    public EnumMostLeastFrequentScalarForge(int streamCountIncoming, boolean isMostFrequent, Class returnType) {
        super(streamCountIncoming);
        this.isMostFrequent = isMostFrequent;
        this.returnType = returnType;
    }

    public EnumEval getEnumEvaluator() {
        return this;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return null;
        }

        Map<Object, Integer> items = new LinkedHashMap<Object, Integer>();

        for (Object next : enumcoll) {
            Integer existing = items.get(next);

            if (existing == null) {
                existing = 1;
            } else {
                existing++;
            }
            items.put(next, existing);
        }

        return EnumMostLeastFrequentEventForgeEval.getEnumMostLeastFrequentResult(items, isMostFrequent);
    }

    public CodegenExpression codegen(EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(JavaClassHelper.getBoxedType(returnType), EnumMostLeastFrequentScalarForge.class, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS).getBlock()
                .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                .blockReturn(constantNull())
                .declareVar(Map.class, "items", newInstance(LinkedHashMap.class));
        CodegenBlock forEach = block.forEach(Object.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
                .declareVar(Integer.class, "existing", cast(Integer.class, exprDotMethod(ref("items"), "get", ref("next"))))
                .ifCondition(equalsNull(ref("existing")))
                .assignRef("existing", constant(1))
                .ifElse()
                .incrementRef("existing")
                .blockEnd()
                .exprDotMethod(ref("items"), "put", ref("next"), ref("existing"));
        CodegenMethod method = block.methodReturn(cast(returnType, staticMethod(EnumMostLeastFrequentEventForgeEval.class, "getEnumMostLeastFrequentResult", ref("items"), constant(isMostFrequent))));
        return localMethod(method, args.getExpressions());
    }
}
