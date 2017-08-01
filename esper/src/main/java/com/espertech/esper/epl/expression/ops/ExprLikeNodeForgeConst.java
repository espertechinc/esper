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
import com.espertech.esper.util.LikeUtil;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;

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

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return localMethodBuild(ExprLikeNodeForgeConstEval.codegen(this, getForgeRenderable().getChildNodes()[0], context, params)).passAll(params).call();
    }

    LikeUtil getLikeUtil() {
        return likeUtil;
    }
}
