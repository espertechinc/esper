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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.average;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.type.MathContextCodegenField;

import java.math.MathContext;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumAverageBigDecimalScalar extends ThreeFormScalar {

    protected final MathContext optionalMathContext;

    public EnumAverageBigDecimalScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters, MathContext optionalMathContext) {
        super(lambda, fieldEventType, numParameters);
        this.optionalMathContext = optionalMathContext;
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                AggregatorAvgBigDecimal agg = new AggregatorAvgBigDecimal(optionalMathContext);
                ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
                eventsLambda[getStreamNumLambda()] = resultEvent;
                Object[] props = resultEvent.getProperties();
                Collection<Object> values = (Collection<Object>) enumcoll;
                props[2] = enumcoll.size();

                int index = -1;
                for (Object next : values) {
                    index++;
                    props[1] = index;
                    props[0] = next;

                    Number num = (Number) inner.evaluate(eventsLambda, isNewData, context);
                    if (num == null) {
                        continue;
                    }
                    agg.enter(num);
                }

                return agg.getValue();
            }
        };
    }

    public EPTypeClass returnTypeOfMethod() {
        return EPTypePremade.BIGDECIMAL.getEPType();
    }

    public CodegenExpression returnIfEmptyOptional() {
        return null;
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        CodegenExpression math = codegenClassScope.addOrGetFieldSharable(new MathContextCodegenField(optionalMathContext));
        block.declareVar(AggregatorAvgBigDecimal.EPTYPE, "agg", newInstance(AggregatorAvgBigDecimal.EPTYPE, math));

    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        EPTypeClass innerType = (EPTypeClass) innerExpression.getEvaluationType();
        block.declareVar(innerType, "num", innerExpression.evaluateCodegen(EPTypePremade.NUMBER.getEPType(), methodNode, scope, codegenClassScope));
        if (!innerType.getType().isPrimitive()) {
            block.ifRefNull("num").blockContinue();
        }
        block.expression(exprDotMethod(ref("agg"), "enter", ref("num")));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(exprDotMethod(ref("agg"), "getValue"));
    }
}
