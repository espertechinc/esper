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
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
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

    public static CodegenExpression codegen(ExprRelationalOpNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
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

        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, ExprRelationalOpNodeForgeEval.class, codegenClassScope);


        CodegenBlock block = methodNode.getBlock()
                .declareVar(lhsType, "left", lhs.evaluateCodegen(lhsType, methodNode, exprSymbol, codegenClassScope));
        if (!lhsType.isPrimitive()) {
            block.ifRefNullReturnNull("left");
        }

        block.declareVar(rhsType, "right", rhs.evaluateCodegen(rhsType, methodNode, exprSymbol, codegenClassScope));
        if (!rhsType.isPrimitive()) {
            block.ifRefNullReturnNull("right");
        }

        block.methodReturn(forge.getComputer().codegen(ref("left"), lhsType, ref("right"), rhsType));
        return localMethod(methodNode);
    }
}
