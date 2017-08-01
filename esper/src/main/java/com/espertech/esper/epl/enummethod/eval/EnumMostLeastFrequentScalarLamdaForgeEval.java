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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;

public class EnumMostLeastFrequentScalarLamdaForgeEval implements EnumEval {

    private final EnumMostLeastFrequentScalarLamdaForge forge;
    private final ExprEvaluator innerExpression;

    public EnumMostLeastFrequentScalarLamdaForgeEval(EnumMostLeastFrequentScalarLamdaForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return null;
        }

        Map<Object, Integer> items = new LinkedHashMap<Object, Integer>();
        Collection<Object> values = (Collection<Object>) enumcoll;

        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], forge.resultEventType);
        eventsLambda[forge.streamNumLambda] = resultEvent;
        Object[] props = resultEvent.getProperties();

        for (Object next : values) {

            props[0] = next;

            Object item = innerExpression.evaluate(eventsLambda, isNewData, context);
            Integer existing = items.get(item);

            if (existing == null) {
                existing = 1;
            } else {
                existing++;
            }
            items.put(item, existing);
        }

        return EnumMostLeastFrequentEventForgeEval.getEnumMostLeastFrequentResult(items, forge.isMostFrequent);
    }

    public static CodegenExpression codegen(EnumMostLeastFrequentScalarLamdaForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenMember resultTypeMember = context.makeAddMember(ObjectArrayEventType.class, forge.resultEventType);
        Class returnType = JavaClassHelper.getBoxedType(forge.innerExpression.getEvaluationType());
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(returnType, EnumMostLeastFrequentScalarLamdaForgeEval.class).add(premade).begin()
                .ifCondition(exprDotMethod(premade.enumcoll(), "isEmpty"))
                .blockReturn(constantNull())
                .declareVar(Map.class, "items", newInstance(LinkedHashMap.class))
                .declareVar(ObjectArrayEventBean.class, "resultEvent", newInstance(ObjectArrayEventBean.class, newArray(Object.class, constant(1)), ref(resultTypeMember.getMemberName())))
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("resultEvent"))
                .declareVar(Object[].class, "props", exprDotMethod(ref("resultEvent"), "getProperties"));

        CodegenBlock forEach = block.forEach(Object.class, "next", premade.enumcoll())
                .assignArrayElement("props", constant(0), ref("next"))
                .declareVar(Object.class, "item", forge.innerExpression.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context))
                .declareVar(Integer.class, "existing", cast(Integer.class, exprDotMethod(ref("items"), "get", ref("item"))))
                .ifCondition(equalsNull(ref("existing")))
                .assignRef("existing", constant(1))
                .ifElse()
                .expression(increment("existing"))
                .blockEnd()
                .exprDotMethod(ref("items"), "put", ref("item"), ref("existing"));
        String method = block.methodReturn(cast(returnType, staticMethod(EnumMostLeastFrequentEventForgeEval.class, "getEnumMostLeastFrequentResult", ref("items"), constant(forge.isMostFrequent))));
        return localMethodBuild(method).passAll(args).call();
    }
}
