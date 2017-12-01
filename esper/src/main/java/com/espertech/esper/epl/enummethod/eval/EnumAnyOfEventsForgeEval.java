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
import com.espertech.esper.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

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

    public static CodegenExpression codegen(EnumAnyOfEventsForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(boolean.class, EnumAnyOfEventsForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        CodegenBlock block = methodNode.getBlock()
                .ifConditionReturnConst(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"), false);
        CodegenBlock forEach = block.forEach(EventBean.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda), ref("next"));
        CodegenLegoBooleanExpression.codegenReturnBoolIfNullOrBool(forEach, forge.innerExpression.getEvaluationType(), forge.innerExpression.evaluateCodegen(Boolean.class, methodNode, scope, codegenClassScope), false, null, true, true);
        block.methodReturn(constantFalse());
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }
}
