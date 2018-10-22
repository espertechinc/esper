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
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.type.MathContextCodegenField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.avg.AggregatorAvgBig.getValueBigDecimalDivide;

public class EnumAverageBigDecimalEventsForgeEval implements EnumEval {
    private static final Logger log = LoggerFactory.getLogger(EnumAverageBigDecimalEventsForgeEval.class);

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

    public static CodegenExpression codegen(EnumAverageBigDecimalEventsForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        Class innerType = forge.innerExpression.getEvaluationType();

        CodegenExpression math = codegenClassScope.addOrGetFieldSharable(new MathContextCodegenField(forge.optionalMathContext));

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(BigDecimal.class, EnumAverageBigDecimalEventsForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        CodegenBlock block = methodNode.getBlock();
        block.declareVar(AggregatorAvgBigDecimal.class, "agg", newInstance(AggregatorAvgBigDecimal.class, math));
        CodegenBlock forEach = block.forEach(EventBean.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda), ref("next"))
                .declareVar(innerType, "num", forge.innerExpression.evaluateCodegen(innerType, methodNode, scope, codegenClassScope));
        if (!innerType.isPrimitive()) {
            forEach.ifRefNull("num").blockContinue();
        }
        forEach.expression(exprDotMethod(ref("agg"), "enter", ref("num")))
                .blockEnd();
        block.methodReturn(exprDotMethod(ref("agg"), "getValue"));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }

    public static class AggregatorAvgBigDecimal {
        private BigDecimal sum;
        private long cnt;
        private MathContext optionalMathContext;

        public AggregatorAvgBigDecimal(MathContext optionalMathContext) {
            sum = new BigDecimal(0.0);
            this.optionalMathContext = optionalMathContext;
        }

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            cnt++;
            if (object instanceof BigInteger) {
                sum = sum.add(new BigDecimal((BigInteger) object));
                return;
            }
            sum = sum.add((BigDecimal) object);
        }

        public BigDecimal getValue() {
            return getValueBigDecimalDivide(cnt, optionalMathContext, sum);
        }
    }
}
