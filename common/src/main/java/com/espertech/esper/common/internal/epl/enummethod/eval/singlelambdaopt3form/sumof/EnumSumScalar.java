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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.sumof;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
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

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class EnumSumScalar extends ThreeFormScalar {

    protected final ExprDotEvalSumMethodFactory sumMethodFactory;

    public EnumSumScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters, ExprDotEvalSumMethodFactory sumMethodFactory) {
        super(lambda, fieldEventType, numParameters);
        this.sumMethodFactory = sumMethodFactory;
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                ExprDotEvalSumMethod method = sumMethodFactory.getSumAggregator();

                ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
                eventsLambda[getStreamNumLambda()] = resultEvent;
                Object[] props = resultEvent.getProperties();
                props[2] = enumcoll.size();

                int count = -1;
                Collection<Object> values = (Collection<Object>) enumcoll;
                for (Object next : values) {
                    count++;
                    props[0] = next;
                    props[1] = count;

                    Object value = inner.evaluate(eventsLambda, isNewData, context);
                    method.enter(value);
                }

                return method.getValue();
            }
        };
    }

    public EPTypeClass returnTypeOfMethod() {
        return sumMethodFactory.getValueType();
    }

    public CodegenExpression returnIfEmptyOptional() {
        return null;
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        sumMethodFactory.codegenDeclare(block);
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        EPTypeClass innerType = (EPTypeClass) innerExpression.getEvaluationType();
        block.declareVar(innerType, "value", innerExpression.evaluateCodegen(innerType, methodNode, scope, codegenClassScope));
        if (!innerType.getType().isPrimitive()) {
            block.ifRefNull("value").blockContinue();
        }
        sumMethodFactory.codegenEnterNumberTypedNonNull(block, ref("value"));
    }

    public void returnResult(CodegenBlock block) {
        sumMethodFactory.codegenReturn(block);
    }
}
