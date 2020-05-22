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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.countof;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class EnumCountOfScalar extends ThreeFormScalar {

    public EnumCountOfScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType resultEventType, int numParameters) {
        super(lambda, resultEventType, numParameters);
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return (eventsLambda, enumcoll, isNewData, context) -> {
            if (enumcoll.isEmpty()) {
                return 0;
            }

            int rowcount = 0;
            ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
            eventsLambda[getStreamNumLambda()] = evalEvent;
            Object[] props = evalEvent.getProperties();
            props[2] = enumcoll.size();

            int count = -1;
            for (Object next : enumcoll) {
                count++;
                props[0] = next;
                props[1] = count;

                Object pass = inner.evaluate(eventsLambda, isNewData, context);
                if (pass == null || (!(Boolean) pass)) {
                    continue;
                }
                rowcount++;
            }

            return rowcount;
        };
    }

    public Class returnType() {
        return int.class;
    }

    public CodegenExpression returnIfEmptyOptional() {
        return constant(0);
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(int.class, "rowcount", constant(0));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        CodegenLegoBooleanExpression.codegenContinueIfNotNullAndNotPass(block, innerExpression.getEvaluationType(), innerExpression.evaluateCodegen(Boolean.class, methodNode, scope, codegenClassScope));
        block.incrementRef("rowcount");
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("rowcount"));
    }
}
