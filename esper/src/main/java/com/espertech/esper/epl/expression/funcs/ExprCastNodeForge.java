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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;

/**
 * Represents the CAST(expression, type) function is an expression tree.
 */
public class ExprCastNodeForge implements ExprForge {
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

    public ExprEvaluator getExprEvaluator() {
        if (isConstant) {
            return new ExprCastNodeForgeConstEval(this, constant);
        } else {
            return new ExprCastNodeForgeNonConstEval(this, parent.getChildNodes()[0].getForge().getExprEvaluator(), casterParserComputerForge.getEvaluatorComputer());
        }
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (isConstant) {
            CodegenMember member = codegenClassScope.makeAddMember(targetType, constant);
            return CodegenExpressionBuilder.member(member.getMemberId());
        }
        return ExprCastNodeForgeNonConstEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return isConstant() ? ExprForgeComplexityEnum.NONE : ExprForgeComplexityEnum.INTER;
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
