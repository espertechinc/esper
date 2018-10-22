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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

public class ExprEqualsNodeForgeNC extends ExprEqualsNodeForge {
    public ExprEqualsNodeForgeNC(ExprEqualsNodeImpl parent) {
        super(parent);
    }

    public ExprEvaluator getExprEvaluator() {
        ExprForge lhs = getForgeRenderable().getChildNodes()[0].getForge();
        ExprForge rhs = getForgeRenderable().getChildNodes()[1].getForge();
        if (!getForgeRenderable().isIs()) {
            return new ExprEqualsNodeForgeNCEvalEquals(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
        }
        return new ExprEqualsNodeForgeNCEvalIs(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, getForgeRenderable().isIs() ? "ExprIs" : "ExprEquals", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge lhs = getForgeRenderable().getChildNodes()[0].getForge();
        ExprForge rhs = getForgeRenderable().getChildNodes()[1].getForge();
        if (!getForgeRenderable().isIs()) {
            if (lhs.getEvaluationType() == null || rhs.getEvaluationType() == null) {
                return constantNull();
            }
            return localMethod(ExprEqualsNodeForgeNCEvalEquals.codegen(ExprEqualsNodeForgeNC.this, codegenMethodScope, exprSymbol, codegenClassScope, lhs, rhs));
        }
        return localMethod(ExprEqualsNodeForgeNCEvalIs.codegen(ExprEqualsNodeForgeNC.this, codegenMethodScope, exprSymbol, codegenClassScope, lhs, rhs));
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
