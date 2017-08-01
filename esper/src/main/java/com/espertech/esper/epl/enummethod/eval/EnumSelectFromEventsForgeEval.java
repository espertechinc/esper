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
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumSelectFromEventsForgeEval implements EnumEval {

    private final EnumSelectFromEventsForge forge;
    private final ExprEvaluator innerExpression;

    public EnumSelectFromEventsForgeEval(EnumSelectFromEventsForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {

        if (enumcoll.isEmpty()) {
            return enumcoll;
        }

        Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
        Deque result = new ArrayDeque(enumcoll.size());
        for (EventBean next : beans) {
            eventsLambda[forge.streamNumLambda] = next;

            Object item = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (item != null) {
                result.add(item);
            }
        }

        return result;
    }

    public static CodegenExpression codegen(EnumSelectFromEventsForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(Collection.class, EnumSelectFromEventsForgeEval.class).add(premade).begin()
                .ifCondition(exprDotMethod(premade.enumcoll(), "isEmpty"))
                .blockReturn(premade.enumcoll())
                .declareVar(ArrayDeque.class, "result", newInstance(ArrayDeque.class, exprDotMethod(premade.enumcoll(), "size")));
        CodegenBlock forEach = block.forEach(EventBean.class, "next", premade.enumcoll())
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("next"))
                .declareVar(Object.class, "item", forge.innerExpression.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context))
                .ifCondition(notEqualsNull(ref("item")))
                .expression(exprDotMethod(ref("result"), "add", ref("item")));
        String method = block.methodReturn(ref("result"));
        return localMethodBuild(method).passAll(args).call();
    }
}
