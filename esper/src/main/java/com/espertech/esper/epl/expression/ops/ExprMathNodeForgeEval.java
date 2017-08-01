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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class ExprMathNodeForgeEval implements ExprEvaluator {
    private final ExprMathNodeForge forge;
    private final ExprEvaluator evaluatorLeft;
    private final ExprEvaluator evaluatorRight;

    public ExprMathNodeForgeEval(ExprMathNodeForge forge, ExprEvaluator evaluatorLeft, ExprEvaluator evaluatorRight) {
        this.forge = forge;
        this.evaluatorLeft = evaluatorLeft;
        this.evaluatorRight = evaluatorRight;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprMath(forge.getForgeRenderable(), forge.getForgeRenderable().getMathArithTypeEnum().getExpressionText());
        }

        Object left = evaluatorLeft.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (left == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprMath(null);
            }
            return null;
        }

        Object right = evaluatorRight.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (right == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprMath(null);
            }
            return null;
        }

        Object result = forge.getArithTypeEnumComputer().compute((Number) left, (Number) right);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprMath(result);
        }

        return result;
    }

    public static String codegen(ExprMathNodeForge forge, CodegenContext context, CodegenParamSetExprPremade params, ExprNode lhs, ExprNode rhs) {
        CodegenBlock block = context.addMethod(forge.getEvaluationType(), ExprMathNodeForgeEval.class).add(params).begin()
                .declareVar(lhs.getForge().getEvaluationType(), "left", lhs.getForge().evaluateCodegen(params, context));
        if (!lhs.getForge().getEvaluationType().isPrimitive()) {
            block.ifRefNullReturnNull("left");
        }
        block.declareVar(rhs.getForge().getEvaluationType(), "right", rhs.getForge().evaluateCodegen(params, context));
        if (!rhs.getForge().getEvaluationType().isPrimitive()) {
            block.ifRefNullReturnNull("right");
        }
        return block.methodReturn(forge.getArithTypeEnumComputer().codegen(context, ref("left"), ref("right"), lhs.getForge().getEvaluationType(), rhs.getForge().getEvaluationType()));
    }
}
