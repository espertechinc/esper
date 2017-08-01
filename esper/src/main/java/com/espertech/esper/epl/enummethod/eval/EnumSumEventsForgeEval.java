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

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class EnumSumEventsForgeEval implements EnumEval {

    private final EnumSumEventsForge forge;
    private final ExprEvaluator innerExpression;

    public EnumSumEventsForgeEval(EnumSumEventsForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {

        ExprDotEvalSumMethod method = forge.sumMethodFactory.getSumAggregator();

        Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
        for (EventBean next : beans) {
            eventsLambda[forge.streamNumLambda] = next;

            Object value = innerExpression.evaluate(eventsLambda, isNewData, context);
            method.enter(value);
        }

        return method.getValue();
    }

    public static CodegenExpression codegen(EnumSumEventsForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(forge.sumMethodFactory.getValueType(), EnumSumEventsForgeEval.class).add(premade).begin();
        Class innerType = forge.innerExpression.getEvaluationType();

        forge.sumMethodFactory.codegenDeclare(block);

        CodegenBlock forEach = block.forEach(EventBean.class, "next", premade.enumcoll())
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("next"))
                .declareVar(innerType, "value", forge.innerExpression.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context));
        if (!innerType.isPrimitive()) {
            forEach.ifRefNull("value").blockContinue();
        }
        forge.sumMethodFactory.codegenEnterNumberTypedNonNull(forEach, ref("value"));

        String method = forge.sumMethodFactory.codegenReturn(block);
        return localMethodBuild(method).passAll(args).call();
    }
}
