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
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.LikeUtil;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

/**
 * Like-Node Form-1: constant pattern
 */
public class ExprLikeNodeForgeConst extends ExprLikeNodeForge {
    private final LikeUtil likeUtil;
    private final CodegenExpression likeUtilInit;

    public ExprLikeNodeForgeConst(ExprLikeNode parent, boolean isNumericValue, LikeUtil likeUtil, CodegenExpression likeUtilInit) {
        super(parent, isNumericValue);
        this.likeUtil = likeUtil;
        this.likeUtilInit = likeUtilInit;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprLikeNodeForgeConstEval(this, getForgeRenderable().getChildNodes()[0].getForge().getExprEvaluator());
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = ExprLikeNodeForgeConstEval.codegen(this, getForgeRenderable().getChildNodes()[0], codegenMethodScope, exprSymbol, codegenClassScope);
        return localMethod(methodNode);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprLike", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    LikeUtil getLikeUtil() {
        return likeUtil;
    }

    public CodegenExpression getLikeUtilInit() {
        return likeUtilInit;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
