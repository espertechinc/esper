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
import com.espertech.esper.codegen.model.blocks.CodegenLegoBooleanExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;

public class EnumAnyOfEventsForgeEval implements EnumEval {
    private final EnumAnyOfEventsForge forge;
    private final ExprEvaluator innerExpression;

    public EnumAnyOfEventsForgeEval(EnumAnyOfEventsForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return false;
        }

        Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
        for (EventBean next : beans) {
            eventsLambda[forge.streamNumLambda] = next;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass != null && ((Boolean) pass)) {
                return true;
            }
        }

        return false;
    }

    public static CodegenExpression codegen(EnumAnyOfEventsForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(boolean.class, EnumAnyOfEventsForgeEval.class).add(premade).begin()
                .ifConditionReturnConst(exprDotMethod(premade.enumcoll(), "isEmpty"), false);

        CodegenBlock forEach = block.forEach(EventBean.class, "next", premade.enumcoll())
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("next"));
        CodegenLegoBooleanExpression.codegenReturnBoolIfNullOrBool(forEach, forge.innerExpression, context, false, null, true, true);
        String method = block.methodReturn(constantFalse());
        return localMethodBuild(method).passAll(args).call();
    }
}
