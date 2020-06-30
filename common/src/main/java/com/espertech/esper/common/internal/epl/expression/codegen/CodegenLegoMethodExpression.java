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
package com.espertech.esper.common.internal.epl.expression.codegen;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CodegenLegoMethodExpression {
    private final static String PASS_NAME = "pass";

    public static CodegenExpression codegenBooleanExpressionReturnTrueFalse(ExprForge forge, CodegenClassScope classScope, CodegenMethod parent, CodegenExpression eps, CodegenExpression isNewData, CodegenExpression exprEvalCtx) {
        checkEvaluationType(forge);
        CodegenMethod expressionMethod = codegenBooleanExpressionBoxedToPrimitive(forge, parent, classScope);
        return localMethod(expressionMethod, eps, isNewData, exprEvalCtx);
    }

    public static CodegenMethod codegenExpression(ExprForge forge, CodegenMethod parent, CodegenClassScope classScope) {
        EPType evaluationType = forge.getEvaluationType();

        EPTypeClass methodReturnType = EPTypePremade.OBJECT.getEPType();
        if (evaluationType instanceof EPTypeClass) {
            methodReturnType = (EPTypeClass) evaluationType;
        }

        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod exprMethod = parent.makeChildWithScope(methodReturnType, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        if (evaluationType == null || evaluationType == EPTypeNull.INSTANCE) {
            exprMethod.getBlock().methodReturn(constantNull());
        } else {
            CodegenExpression expression = forge.evaluateCodegen(methodReturnType, exprMethod, exprSymbol, classScope);
            exprSymbol.derivedSymbolsCodegen(parent, exprMethod.getBlock(), classScope);
            if (!JavaClassHelper.isTypeVoid(methodReturnType)) {
                exprMethod.getBlock().methodReturn(expression);
            } else {
                exprMethod.getBlock().expression(expression);
            }
        }
        return exprMethod;
    }

    private static CodegenMethod codegenBooleanExpressionBoxedToPrimitive(ExprForge forge, CodegenMethod parent, CodegenClassScope classScope) {
        EPType evaluationType = forge.getEvaluationType();
        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod exprMethod = parent.makeChildWithScope(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        CodegenExpression expression = forge.evaluateCodegen((EPTypeClass) evaluationType, exprMethod, exprSymbol, classScope);
        exprSymbol.derivedSymbolsCodegen(parent, exprMethod.getBlock(), classScope);

        if (evaluationType == EPTypeNull.INSTANCE || ((EPTypeClass) evaluationType).getType().isPrimitive()) {
            exprMethod.getBlock().methodReturn(expression);
        } else {
            exprMethod.getBlock()
                .declareVar((EPTypeClass) evaluationType, PASS_NAME, expression)
                .ifRefNull(PASS_NAME).blockReturn(constantFalse())
                .methodReturn(ref(PASS_NAME));
        }
        return exprMethod;
    }

    private static void checkEvaluationType(ExprForge forge) {
        EPType evaluationType = forge.getEvaluationType();
        if (!JavaClassHelper.isTypeBoolean(evaluationType)) {
            throw new IllegalStateException("Invalid non-boolean expression");
        }
    }
}
