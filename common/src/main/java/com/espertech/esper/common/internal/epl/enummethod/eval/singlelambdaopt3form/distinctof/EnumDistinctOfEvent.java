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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.distinctof;

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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

public class EnumDistinctOfEvent extends ThreeFormEventPlain {

    private final Class innerType;

    public EnumDistinctOfEvent(ExprDotEvalParamLambda lambda) {
        super(lambda);
        innerType = JavaClassHelper.getBoxedType(innerExpression.getEvaluationType());
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
                if (beans.size() <= 1) {
                    return beans;
                }

                Map<Object, EventBean> distinct = new LinkedHashMap<>();
                for (EventBean next : beans) {
                    eventsLambda[getStreamNumLambda()] = next;

                    Object comparable = inner.evaluate(eventsLambda, isNewData, context);
                    if (!distinct.containsKey(comparable)) {
                        distinct.put(comparable, next);
                    }
                }

                return distinct.values();
            }
        };
    }

    public Class returnType() {
        return Collection.class;
    }

    public CodegenExpression returnIfEmptyOptional() {
        return null;
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.ifCondition(relational(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"), LE, constant(1)))
            .blockReturn(EnumForgeCodegenNames.REF_ENUMCOLL)
            .declareVar(Map.class, "distinct", newInstance(LinkedHashMap.class));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        CodegenExpression eval = innerExpression.evaluateCodegen(innerType, methodNode, scope, codegenClassScope);
        EnumDistinctOfHelper.forEachBlock(block, eval, innerType);
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(exprDotMethod(ref("distinct"), "values"));
    }
}
