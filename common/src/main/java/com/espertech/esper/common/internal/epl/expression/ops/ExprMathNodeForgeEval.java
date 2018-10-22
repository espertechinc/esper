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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

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
        Object left = evaluatorLeft.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (left == null) {
            return null;
        }

        Object right = evaluatorRight.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (right == null) {
            return null;
        }

        return forge.getArithTypeEnumComputer().compute((Number) left, (Number) right);
    }

    public static CodegenMethod codegen(ExprMathNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, ExprNode lhs, ExprNode rhs) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprMathNodeForgeEval.class, codegenClassScope);
        Class lhsType = lhs.getForge().getEvaluationType();
        Class rhsType = rhs.getForge().getEvaluationType();
        CodegenBlock block = methodNode.getBlock()
                .declareVar(lhsType, "left", lhs.getForge().evaluateCodegen(lhsType, methodNode, exprSymbol, codegenClassScope));
        if (!lhsType.isPrimitive()) {
            block.ifRefNullReturnNull("left");
        }
        block.declareVar(rhsType, "right", rhs.getForge().evaluateCodegen(rhsType, methodNode, exprSymbol, codegenClassScope));
        if (!rhsType.isPrimitive()) {
            block.ifRefNullReturnNull("right");
        }
        block.methodReturn(forge.getArithTypeEnumComputer().codegen(methodNode, codegenClassScope, ref("left"), ref("right"), lhsType, rhsType));
        return methodNode;
    }
}
