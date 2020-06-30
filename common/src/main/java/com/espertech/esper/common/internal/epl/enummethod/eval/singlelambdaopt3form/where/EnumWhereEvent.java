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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.where;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormEventPlain;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayDeque;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumWhereEvent extends ThreeFormEventPlain {

    public EnumWhereEvent(ExprDotEvalParamLambda lambda) {
        super(lambda);
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                if (enumcoll.isEmpty()) {
                    return enumcoll;
                }

                ArrayDeque<Object> result = new ArrayDeque<>();

                Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
                for (EventBean next : beans) {
                    eventsLambda[getStreamNumLambda()] = next;

                    Object pass = inner.evaluate(eventsLambda, isNewData, context);
                    if (pass == null || (!(Boolean) pass)) {
                        continue;
                    }

                    result.add(next);
                }

                return result;
            }
        };
    }

    public EPTypeClass returnTypeOfMethod() {
        return EPTypePremade.COLLECTION.getEPType();
    }

    public CodegenExpression returnIfEmptyOptional() {
        return EnumForgeCodegenNames.REF_ENUMCOLL;
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "result", newInstance(EPTypePremade.ARRAYDEQUE.getEPType()));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        CodegenLegoBooleanExpression.codegenContinueIfNotNullAndNotPass(block, innerExpression.getEvaluationType(), innerExpression.evaluateCodegen(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), methodNode, scope, codegenClassScope));
        block.expression(exprDotMethod(ref("result"), "add", ref("next")));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("result"));
    }
}
