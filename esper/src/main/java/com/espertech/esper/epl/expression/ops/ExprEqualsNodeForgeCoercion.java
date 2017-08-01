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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.util.SimpleNumberCoercer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;

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

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        ExprNode lhs = getForgeRenderable().getChildNodes()[0];
        ExprNode rhs = getForgeRenderable().getChildNodes()[1];
        return localMethodBuild(ExprEqualsNodeForgeCoercionEval.codegen(this, context, params, lhs, rhs)).passAll(params).call();
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public SimpleNumberCoercer getNumberCoercerLHS() {
        return numberCoercerLHS;
    }

    public SimpleNumberCoercer getNumberCoercerRHS() {
        return numberCoercerRHS;
    }
}
