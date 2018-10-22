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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery.getEvaluatorsNoCompile;

class ExprNodeVarargOnlyArrayForge implements ExprForge, ExprNodeRenderable {
    private final ExprForge[] forges;
    protected final Class varargClass;
    protected final SimpleNumberCoercer[] optionalCoercers;

    public ExprNodeVarargOnlyArrayForge(ExprForge[] forges, Class varargClass, SimpleNumberCoercer[] optionalCoercers) {
        this.forges = forges;
        this.varargClass = varargClass;
        this.optionalCoercers = optionalCoercers;
    }

    public ExprEvaluator getExprEvaluator() {
        if (optionalCoercers == null) {
            return new ExprNodeVarargOnlyArrayEvalNoCoerce(this, getEvaluatorsNoCompile(forges));
        }
        return new ExprNodeVarargOnlyArrayForgeWithCoerce(this, getEvaluatorsNoCompile(forges));
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class arrayType = JavaClassHelper.getArrayType(varargClass);
        CodegenMethod methodNode = codegenMethodScope.makeChild(arrayType, ExprNodeVarargOnlyArrayForge.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(arrayType, "array", newArrayByLength(varargClass, constant(forges.length)));
        for (int i = 0; i < forges.length; i++) {
            CodegenExpression expression = forges[i].evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope);
            CodegenExpression assignment;
            if (optionalCoercers == null || optionalCoercers[i] == null) {
                assignment = expression;
            } else {
                Class evalType = forges[i].getEvaluationType();
                if (evalType.isPrimitive()) {
                    assignment = optionalCoercers[i].coerceCodegen(expression, evalType);
                } else {
                    assignment = optionalCoercers[i].coerceCodegenMayNullBoxed(expression, evalType, methodNode, codegenClassScope);
                }
            }
            block.assignArrayElement("array", constant(i), assignment);
        }
        block.methodReturn(ref("array"));
        return localMethod(methodNode);
    }

    public Class getEvaluationType() {
        return JavaClassHelper.getArrayType(varargClass);
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
