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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.arrayOf;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormEventPlain;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumArrayOfEvent extends ThreeFormEventPlain {

    private final Class arrayComponentType;

    public EnumArrayOfEvent(ExprDotEvalParamLambda lambda, Class arrayComponentType) {
        super(lambda);
        this.arrayComponentType = arrayComponentType;
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                Object array = Array.newInstance(arrayComponentType, enumcoll.size());
                if (enumcoll.isEmpty()) {
                    return array;
                }

                Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
                int count = -1;

                for (EventBean next : beans) {
                    count++;
                    eventsLambda[getStreamNumLambda()] = next;
                    Object item = inner.evaluate(eventsLambda, isNewData, context);
                    Array.set(array, count, item);
                }
                return array;
            }
        };
    }

    public Class returnType() {
        return JavaClassHelper.getArrayType(arrayComponentType);
    }

    public CodegenExpression returnIfEmptyOptional() {
        return newArrayByLength(arrayComponentType, constant(0));
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        Class arrayType = JavaClassHelper.getArrayType(arrayComponentType);
        block.declareVar(arrayType, "result", newArrayByLength(arrayComponentType, exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size")))
            .declareVar(int.class, "count", constant(0));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(Object.class, "item", innerExpression.evaluateCodegen(Object.class, methodNode, scope, codegenClassScope))
            .assignArrayElement(ref("result"), ref("count"), cast(arrayComponentType, ref("item")))
            .incrementRef("count");
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("result"));
    }
}
