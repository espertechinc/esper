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

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;

/**
 * Like-Node Form-1: string input, constant pattern and no or constant escape character
 */
public class ExprRegexpNodeForgeNonconst extends ExprRegexpNodeForge {

    public ExprRegexpNodeForgeNonconst(ExprRegexpNode parent, boolean isNumericValue) {
        super(parent, isNumericValue);
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprRegexpNodeForgeNonconstEval(this,
                getForgeRenderable().getChildNodes()[0].getForge().getExprEvaluator(),
                getForgeRenderable().getChildNodes()[1].getForge().getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = ExprRegexpNodeForgeNonconstEval.codegen(this, getForgeRenderable().getChildNodes()[0], getForgeRenderable().getChildNodes()[1], codegenMethodScope, exprSymbol, codegenClassScope);
        return localMethod(methodNode);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }
}
