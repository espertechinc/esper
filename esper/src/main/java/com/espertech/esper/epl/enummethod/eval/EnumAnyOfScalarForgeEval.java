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
import com.espertech.esper.codegen.model.blocks.CodegenLegoBooleanExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;

public class EnumAnyOfScalarForgeEval implements EnumEval {

    private final EnumAnyOfScalarForge forge;
    private final ExprEvaluator innerExpression;

    public EnumAnyOfScalarForgeEval(EnumAnyOfScalarForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return false;
        }

        ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[1], forge.type);
        eventsLambda[forge.streamNumLambda] = evalEvent;
        Object[] props = evalEvent.getProperties();

        for (Object next : enumcoll) {

            props[0] = next;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass != null && ((Boolean) pass)) {
                return true;
            }
        }

        return false;
    }

    public static CodegenExpression codegen(EnumAnyOfScalarForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenMember typeMember = context.makeAddMember(ObjectArrayEventType.class, forge.type);
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(boolean.class, EnumAllOfScalarForgeEval.class).add(premade).begin()
                .ifConditionReturnConst(exprDotMethod(premade.enumcoll(), "isEmpty"), false)
                .declareVar(ObjectArrayEventBean.class, "evalEvent", newInstance(ObjectArrayEventBean.class, newArray(Object.class, constant(1)), ref(typeMember.getMemberName())))
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("evalEvent"))
                .declareVar(Object[].class, "props", exprDotMethod(ref("evalEvent"), "getProperties"));

        CodegenBlock forEach = block.forEach(Object.class, "next", premade.enumcoll())
                .assignArrayElement("props", constant(0), ref("next"));
        CodegenLegoBooleanExpression.codegenReturnBoolIfNullOrBool(forEach, forge.innerExpression, context, false, null, true, true);
        String method = block.methodReturn(constantFalse());
        return localMethodBuild(method).passAll(args).call();
    }
}
