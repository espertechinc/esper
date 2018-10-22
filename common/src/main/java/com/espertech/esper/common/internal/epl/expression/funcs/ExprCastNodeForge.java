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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeInstrumentable;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

/**
 * Represents the CAST(expression, type) function is an expression tree.
 */
public class ExprCastNodeForge implements ExprForgeInstrumentable {
    private final ExprCastNode parent;
    private final ExprCastNode.CasterParserComputerForge casterParserComputerForge;
    private final Class targetType;
    private final boolean isConstant;
    private final Object constant;

    ExprCastNodeForge(ExprCastNode parent, ExprCastNode.CasterParserComputerForge casterParserComputerForge, Class targetType, boolean isConstant, Object constant) {
        this.parent = parent;
        this.casterParserComputerForge = casterParserComputerForge;
        this.targetType = targetType;
        this.isConstant = isConstant;
        this.constant = constant;
    }

    public ExprForgeConstantType getForgeConstantType() {
        if (isConstant) {
            return ExprForgeConstantType.DEPLOYCONST;
        }
        return ExprForgeConstantType.NONCONST;
    }

    public ExprEvaluator getExprEvaluator() {
        if (isConstant) {
            return new ExprCastNodeForgeConstEval(this, constant);
        } else {
            return new ExprCastNodeForgeNonConstEval(this, parent.getChildNodes()[0].getForge().getExprEvaluator(), casterParserComputerForge.getEvaluatorComputer());
        }
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (isConstant) {
            if (constant == null) {
                return constantNull();
            }
            return ExprCastNodeForgeConstEval.codegen(this, codegenClassScope);
        }
        return ExprCastNodeForgeNonConstEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprCast", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public Class getEvaluationType() {
        return targetType;
    }

    public ExprCastNode getForgeRenderable() {
        return parent;
    }

    public ExprCastNode.CasterParserComputerForge getCasterParserComputerForge() {
        return casterParserComputerForge;
    }

    public boolean isConstant() {
        return isConstant;
    }

    public Object getConstant() {
        return constant;
    }
}
