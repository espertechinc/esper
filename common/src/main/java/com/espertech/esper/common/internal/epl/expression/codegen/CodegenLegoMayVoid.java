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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

public class CodegenLegoMayVoid {

    public static CodegenExpression expressionMayVoid(EPType requiredType, ExprForge forge, CodegenMethod parentNode, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        EPTypeClass requiredTypeClass = requiredType == null || requiredType == EPTypeNull.INSTANCE ? EPTypePremade.OBJECT.getEPType() : (EPTypeClass) requiredType;
        EPType evalType = forge.getEvaluationType();
        if (!JavaClassHelper.isTypeVoid(evalType)) {
            return forge.evaluateCodegen(requiredTypeClass, parentNode, exprSymbol, codegenClassScope);
        }
        CodegenMethod methodNode = parentNode.makeChild(EPTypePremade.OBJECT.getEPType(), CodegenLegoMayVoid.class, codegenClassScope);
        methodNode.getBlock().expression(forge.evaluateCodegen(requiredTypeClass, methodNode, exprSymbol, codegenClassScope))
            .methodReturn(constantNull());
        return localMethod(methodNode);
    }
}
