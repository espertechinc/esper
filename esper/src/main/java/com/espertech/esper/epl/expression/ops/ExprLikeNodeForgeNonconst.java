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

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;

/**
 * Like-Node Form-1: non-constant pattern
 */
public class ExprLikeNodeForgeNonconst extends ExprLikeNodeForge {

    public ExprLikeNodeForgeNonconst(ExprLikeNode parent, boolean isNumericValue) {
        super(parent, isNumericValue);
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprLikeNodeFormNonconstEval(this,
                getForgeRenderable().getChildNodes()[0].getForge().getExprEvaluator(),
                getForgeRenderable().getChildNodes()[1].getForge().getExprEvaluator(),
                getForgeRenderable().getChildNodes().length == 2 ? null : getForgeRenderable().getChildNodes()[2].getForge().getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = ExprLikeNodeFormNonconstEval.codegen(this, getForgeRenderable().getChildNodes()[0], getForgeRenderable().getChildNodes()[1],
                getForgeRenderable().getChildNodes().length == 2 ? null : getForgeRenderable().getChildNodes()[2], codegenMethodScope, exprSymbol, codegenClassScope);
        return localMethod(methodNode);
    }
}
