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
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprRelationalOpNodeForgeEval implements ExprEvaluator {
    private final ExprRelationalOpNodeForge forge;
    private final ExprEvaluator left;
    private final ExprEvaluator right;

    public ExprRelationalOpNodeForgeEval(ExprRelationalOpNodeForge forge, ExprEvaluator left, ExprEvaluator right) {
        this.forge = forge;
        this.left = left;
        this.right = right;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprRelOp(forge.getForgeRenderable(), forge.getForgeRenderable().getRelationalOpEnum().getExpressionText());
        }
        Object lvalue = left.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (lvalue == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprRelOp(null);
            }
            return null;
        }

        Object rvalue = right.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (rvalue == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprRelOp(null);
            }
            return null;
        }

        if (InstrumentationHelper.ENABLED) {
            Boolean result = forge.getComputer().compare(lvalue, rvalue);
            InstrumentationHelper.get().aExprRelOp(result);
            return result;
        }
        return forge.getComputer().compare(lvalue, rvalue);
    }

    public static CodegenExpression codegen(ExprRelationalOpNodeForge forge, CodegenContext context, CodegenParamSetExprPremade params) {
        ExprForge lhs = forge.getForgeRenderable().getChildNodes()[0].getForge();
        ExprForge rhs = forge.getForgeRenderable().getChildNodes()[1].getForge();
        Class lhsType = lhs.getEvaluationType();
        if (lhsType == null) {
            return constantNull();
        }
        Class rhsType = rhs.getEvaluationType();
        if (rhsType == null) {
            return constantNull();
        }

        CodegenBlock block = context.addMethod(Boolean.class, ExprRelationalOpNodeForgeEval.class).add(params).begin()
                .declareVar(lhsType, "left", lhs.evaluateCodegen(params, context));
        if (!lhsType.isPrimitive()) {
            block.ifRefNullReturnNull("left");
        }

        block.declareVar(rhsType, "right", rhs.evaluateCodegen(params, context));
        if (!rhsType.isPrimitive()) {
            block.ifRefNullReturnNull("right");
        }

        CodegenMethodId method = block.methodReturn(forge.getComputer().codegen(ref("left"), lhsType, ref("right"), rhsType));
        return localMethodBuild(method).passAll(params).call();
    }
}
