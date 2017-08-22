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
import com.espertech.esper.util.LikeUtil;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;

/**
 * Like-Node Form-1: constant pattern
 */
public class ExprLikeNodeForgeConst extends ExprLikeNodeForge {
    private final LikeUtil likeUtil;

    public ExprLikeNodeForgeConst(ExprLikeNode parent, boolean isNumericValue, LikeUtil likeUtil) {
        super(parent, isNumericValue);
        this.likeUtil = likeUtil;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprLikeNodeForgeConstEval(this, getForgeRenderable().getChildNodes()[0].getForge().getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = ExprLikeNodeForgeConstEval.codegen(this, getForgeRenderable().getChildNodes()[0], codegenMethodScope, exprSymbol, codegenClassScope);
        return localMethod(methodNode);
    }

    LikeUtil getLikeUtil() {
        return likeUtil;
    }
}
