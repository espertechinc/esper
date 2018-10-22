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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

public class ExprEqualsNodeForgeCoercion extends ExprEqualsNodeForge {

    private final SimpleNumberCoercer numberCoercerLHS;
    private final SimpleNumberCoercer numberCoercerRHS;

    public ExprEqualsNodeForgeCoercion(ExprEqualsNodeImpl parent, SimpleNumberCoercer numberCoercerLHS, SimpleNumberCoercer numberCoercerRHS) {
        super(parent);
        this.numberCoercerLHS = numberCoercerLHS;
        this.numberCoercerRHS = numberCoercerRHS;
    }

    public ExprEvaluator getExprEvaluator() {
        ExprNode lhs = getForgeRenderable().getChildNodes()[0];
        ExprNode rhs = getForgeRenderable().getChildNodes()[1];
        return new ExprEqualsNodeForgeCoercionEval(getForgeRenderable(), lhs.getForge().getExprEvaluator(), rhs.getForge().getExprEvaluator(), numberCoercerLHS, numberCoercerRHS);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprNode lhs = getForgeRenderable().getChildNodes()[0];
        ExprNode rhs = getForgeRenderable().getChildNodes()[1];
        CodegenMethod method = ExprEqualsNodeForgeCoercionEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope, lhs, rhs);
        return localMethod(method);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, getForgeRenderable().isIs() ? "ExprIs" : "ExprEquals", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public SimpleNumberCoercer getNumberCoercerLHS() {
        return numberCoercerLHS;
    }

    public SimpleNumberCoercer getNumberCoercerRHS() {
        return numberCoercerRHS;
    }
}
