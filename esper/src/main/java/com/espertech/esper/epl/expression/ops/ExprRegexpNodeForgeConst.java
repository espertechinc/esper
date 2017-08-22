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

import java.util.regex.Pattern;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;

/**
 * Regex-Node Form-1: constant pattern
 */
public class ExprRegexpNodeForgeConst extends ExprRegexpNodeForge {
    private final Pattern pattern;

    public ExprRegexpNodeForgeConst(ExprRegexpNode parent, boolean isNumericValue, Pattern pattern) {
        super(parent, isNumericValue);
        this.pattern = pattern;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprRegexpNodeForgeConstEval(this, getForgeRenderable().getChildNodes()[0].getForge().getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = ExprRegexpNodeForgeConstEval.codegen(this, getForgeRenderable().getChildNodes()[0], codegenMethodScope, exprSymbol, codegenClassScope);
        return localMethod(methodNode);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    Pattern getPattern() {
        return pattern;
    }
}
