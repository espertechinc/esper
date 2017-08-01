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
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;

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

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        ExprForge lhs = getForgeRenderable().getChildNodes()[0].getForge();
        ExprForge rhs = getForgeRenderable().getChildNodes()[1].getForge();
        if (!getForgeRenderable().isIs()) {
            if (lhs.getEvaluationType() == null || rhs.getEvaluationType() == null) {
                return constantNull();
            }
            return localMethodBuild(ExprEqualsNodeForgeNCEvalEquals.codegen(this, context, params, lhs, rhs)).passAll(params).call();
        }
        return localMethodBuild(ExprEqualsNodeForgeNCEvalIs.codegen(this, context, params, lhs, rhs)).passAll(params).call();
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }
}
