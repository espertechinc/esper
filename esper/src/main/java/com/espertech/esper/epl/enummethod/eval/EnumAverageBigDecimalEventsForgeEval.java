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
import com.espertech.esper.epl.agg.aggregator.AggregatorAvgBigDecimal;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class EnumAverageBigDecimalEventsForgeEval implements EnumEval {

    private final EnumAverageBigDecimalEventsForge forge;
    private final ExprEvaluator innerExpression;

    public EnumAverageBigDecimalEventsForgeEval(EnumAverageBigDecimalEventsForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {

        AggregatorAvgBigDecimal agg = new AggregatorAvgBigDecimal(forge.optionalMathContext);

        Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
        for (EventBean next : beans) {
            eventsLambda[forge.streamNumLambda] = next;

            Number num = (Number) innerExpression.evaluate(eventsLambda, isNewData, context);
            if (num == null) {
                continue;
            }
            agg.enter(num);
        }

        return agg.getValue();
    }

    public static CodegenExpression codegen(EnumAverageBigDecimalEventsForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        Class innerType = forge.innerExpression.getEvaluationType();
        CodegenMember mathCtxMember = context.makeAddMember(MathContext.class, forge.optionalMathContext);

        CodegenBlock block = context.addMethod(BigDecimal.class, EnumAverageBigDecimalEventsForgeEval.class).add(premade).begin()
                .declareVar(AggregatorAvgBigDecimal.class, "agg", newInstance(AggregatorAvgBigDecimal.class, ref(mathCtxMember.getMemberName())));
        CodegenBlock forEach = block.forEach(EventBean.class, "next", premade.enumcoll())
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("next"))
                .declareVar(innerType, "num", forge.getInnerExpression().evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context));
        if (!innerType.isPrimitive()) {
            forEach.ifRefNull("num").blockContinue();
        }
        forEach.expression(exprDotMethod(ref("agg"), "enter", ref("num")))
                .blockEnd();
        String method = block.methodReturn(exprDotMethod(ref("agg"), "getValue"));
        return localMethodBuild(method).passAll(args).call();
    }
}
