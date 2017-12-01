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
package com.espertech.esper.epl.expression.codegen;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.core.ExprForge;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class CodegenLegoMethodExpression {
    private final static String PASS_NAME = "pass";

    public static CodegenExpression codegenBooleanExpressionReturnTrueFalse(ExprForge forge, CodegenClassScope classScope, CodegenMethodNode parent, CodegenExpression eps, CodegenExpression isNewData, CodegenExpression exprEvalCtx) {
        checkEvaluationType(forge);
        CodegenMethodNode expressionMethod = codegenBooleanExpressionBoxedToPrimitive(forge, parent, classScope);
        return localMethod(expressionMethod, eps, isNewData, exprEvalCtx);
    }

    public static void codegenBooleanExpressionReturnNullIfNullOrNotPass(ExprForge forge, CodegenClassScope classScope, CodegenMethodNode parent, CodegenExpression eps, CodegenExpression isNewData, CodegenExpression exprEvalCtx) {
        checkEvaluationType(forge);
        CodegenMethodNode expressionMethod = codegenBooleanExpressionBoxedToPrimitive(forge, parent, classScope);
        CodegenExpression evaluation = localMethod(expressionMethod, eps, isNewData, exprEvalCtx);
        parent.getBlock().ifCondition(not(evaluation)).blockReturn(constantNull());
    }

    public static void codegenBooleanExpressionReturnIfNullOrNotPass(ExprForge forge, CodegenClassScope classScope, CodegenMethodNode parent, CodegenExpression eps, CodegenExpression isNewData, CodegenExpression exprEvalCtx) {
        checkEvaluationType(forge);
        CodegenMethodNode expressionMethod = codegenBooleanExpressionBoxedToPrimitive(forge, parent, classScope);
        CodegenExpression evaluation = localMethod(expressionMethod, eps, isNewData, exprEvalCtx);
        parent.getBlock().ifCondition(not(evaluation)).blockReturnNoValue();
    }

    public static CodegenMethodNode codegenExpression(ExprForge forge, CodegenMethodNode parent, CodegenClassScope classScope) {
        Class evaluationType = forge.getEvaluationType();
        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethodNode exprMethod = parent.makeChildWithScope(evaluationType, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        CodegenExpression expression = forge.evaluateCodegen(evaluationType, exprMethod, exprSymbol, classScope);
        exprSymbol.derivedSymbolsCodegen(parent, exprMethod.getBlock(), classScope);
        exprMethod.getBlock().methodReturn(expression);
        return exprMethod;
    }

    private static CodegenMethodNode codegenBooleanExpressionBoxedToPrimitive(ExprForge forge, CodegenMethodNode parent, CodegenClassScope classScope) {
        Class evaluationType = forge.getEvaluationType();
        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethodNode exprMethod = parent.makeChildWithScope(boolean.class, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        CodegenExpression expression = forge.evaluateCodegen(evaluationType, exprMethod, exprSymbol, classScope);
        exprSymbol.derivedSymbolsCodegen(parent, exprMethod.getBlock(), classScope);

        if (evaluationType.isPrimitive()) {
            exprMethod.getBlock().methodReturn(expression);
        } else {
            exprMethod.getBlock()
                    .declareVar(evaluationType, PASS_NAME, expression)
                    .ifRefNull(PASS_NAME).blockReturn(constantFalse())
                    .methodReturn(ref(PASS_NAME));
        }
        return exprMethod;
    }

    private static void checkEvaluationType(ExprForge forge) {
        Class evaluationType = forge.getEvaluationType();
        if (evaluationType != boolean.class && evaluationType != Boolean.class) {
            throw new IllegalStateException("Invalid non-boolean expression");
        }
    }
}
