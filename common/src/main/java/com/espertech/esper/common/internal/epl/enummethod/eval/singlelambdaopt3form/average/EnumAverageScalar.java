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
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumAverageScalar extends ThreeFormScalar {

    public EnumAverageScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters) {
        super(lambda, fieldEventType, numParameters);
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                double sum = 0d;
                int count = 0;
                ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
                eventsLambda[getStreamNumLambda()] = resultEvent;
                Object[] props = resultEvent.getProperties();
                props[2] = enumcoll.size();
                Collection<Object> values = (Collection<Object>) enumcoll;

                int index = -1;
                for (Object next : values) {
                    index++;
                    props[1] = index;
                    props[0] = next;

                    Number num = (Number) inner.evaluate(eventsLambda, isNewData, context);
                    if (num == null) {
                        continue;
                    }
                    count++;
                    sum += num.doubleValue();
                }

                if (count == 0) {
                    return null;
                }
                return sum / count;
            }
        };
    }

    public Class returnType() {
        return Double.class;
    }

    public CodegenExpression returnIfEmptyOptional() {
        return null;
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(double.class, "sum", constant(0d))
            .declareVar(int.class, "rowcount", constant(0));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        Class innerType = innerExpression.getEvaluationType();
        block.declareVar(innerType, "num", innerExpression.evaluateCodegen(innerType, methodNode, scope, codegenClassScope));
        if (!innerType.isPrimitive()) {
            block.ifRefNull("num").blockContinue();
        }
        block.incrementRef("rowcount")
            .assignRef("sum", op(ref("sum"), "+", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(ref("num"), innerType)))
            .blockEnd();
    }

    public void returnResult(CodegenBlock block) {
        block.ifCondition(equalsIdentity(ref("rowcount"), constant(0))).blockReturn(constantNull())
            .methodReturn(op(ref("sum"), "/", ref("rowcount")));
    }
}
