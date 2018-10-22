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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

public class CodegenLegoMayVoid {

    public static CodegenExpression expressionMayVoid(Class requiredType, ExprForge forge, CodegenMethod parentNode, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (forge.getEvaluationType() != void.class) {
            return forge.evaluateCodegen(requiredType, parentNode, exprSymbol, codegenClassScope);
        }
        CodegenMethod methodNode = parentNode.makeChild(Object.class, CodegenLegoMayVoid.class, codegenClassScope);
        methodNode.getBlock().expression(forge.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope))
                .methodReturn(constantNull());
        return localMethod(methodNode);
    }
}
