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
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

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

        Map<Comparable, EventBean> distinct = new LinkedHashMap<Comparable, EventBean>();
        for (EventBean next : beans) {
            eventsLambda[forge.streamNumLambda] = next;

            Comparable comparable = (Comparable) innerExpression.evaluate(eventsLambda, isNewData, context);
            if (!distinct.containsKey(comparable)) {
                distinct.put(comparable, next);
            }
        }

        return distinct.values();
    }

    public static CodegenExpression codegen(EnumDistinctEventsForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        Class innerType = JavaClassHelper.getBoxedType(forge.innerExpression.getEvaluationType());
        CodegenBlock block = context.addMethod(Collection.class, EnumDistinctEventsForgeEval.class).add(premade).begin()
                .ifCondition(relational(exprDotMethod(premade.enumcoll(), "size"), LE, constant(1)))
                .blockReturn(premade.enumcoll())
                .declareVar(Map.class, "distinct", newInstance(LinkedHashMap.class));
        CodegenBlock forEach = block.forEach(EventBean.class, "next", premade.enumcoll())
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("next"))
                .declareVar(innerType, "comparable", forge.innerExpression.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context))
                .ifCondition(not(exprDotMethod(ref("distinct"), "containsKey", ref("comparable"))))
                .expression(exprDotMethod(ref("distinct"), "put", ref("comparable"), ref("next")))
                .blockEnd();
        CodegenMethodId method = block.methodReturn(exprDotMethod(ref("distinct"), "values"));
        return localMethodBuild(method).passAll(args).call();
    }
}
