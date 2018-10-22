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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.type.MathContextCodegenField;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumAverageBigDecimalScalarForge extends EnumForgeBase implements EnumEval {

    private final MathContext optionalMathContext;

    public EnumAverageBigDecimalScalarForge(int streamCountIncoming, MathContext optionalMathContext) {
        super(streamCountIncoming);
        this.optionalMathContext = optionalMathContext;
    }

    public EnumEval getEnumEvaluator() {
        return this;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        EnumAverageBigDecimalEventsForgeEval.AggregatorAvgBigDecimal agg = new EnumAverageBigDecimalEventsForgeEval.AggregatorAvgBigDecimal(optionalMathContext);

        for (Object next : enumcoll) {

            Number num = (Number) next;
            if (num == null) {
                continue;
            }
            agg.enter(num);
        }

        return agg.getValue();
    }

    public CodegenExpression codegen(EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpression math = codegenClassScope.addOrGetFieldSharable(new MathContextCodegenField(optionalMathContext));
        CodegenMethod method = codegenMethodScope.makeChild(BigDecimal.class, EnumAverageScalarForge.class, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS).getBlock()
                .declareVar(EnumAverageBigDecimalEventsForgeEval.AggregatorAvgBigDecimal.class, "agg", newInstance(EnumAverageBigDecimalEventsForgeEval.AggregatorAvgBigDecimal.class, math))
                .forEach(Number.class, "num", EnumForgeCodegenNames.REF_ENUMCOLL)
                .ifRefNull("num").blockContinue()
                .expression(exprDotMethod(ref("agg"), "enter", ref("num")))
                .blockEnd()
                .methodReturn(exprDotMethod(ref("agg"), "getValue"));
        return localMethod(method, args.getExpressions());
    }
}
