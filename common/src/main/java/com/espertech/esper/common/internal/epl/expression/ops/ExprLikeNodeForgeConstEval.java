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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.util.LikeUtil;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprLikeNodeForgeConstEval implements ExprEvaluator {
    private final ExprLikeNodeForgeConst forge;
    private final ExprEvaluator lhsEval;

    ExprLikeNodeForgeConstEval(ExprLikeNodeForgeConst forge, ExprEvaluator lhsEval) {
        this.forge = forge;
        this.lhsEval = lhsEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object value = lhsEval.evaluate(eventsPerStream, isNewData, context);

        if (value == null) {
            return null;
        }

        if (forge.isNumericValue()) {
            value = value.toString();
        }

        boolean result = forge.getForgeRenderable().isNot() ^ forge.getLikeUtil().compare((String) value);

        return result;
    }

    public static CodegenMethod codegen(ExprLikeNodeForgeConst forge, ExprNode lhs, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression mLikeUtil = codegenClassScope.addFieldUnshared(true, LikeUtil.class, forge.getLikeUtilInit());

        CodegenMethod methodNode = codegenMethodScope.makeChild(Boolean.class, ExprLikeNodeForgeConstEval.class, codegenClassScope);
        if (!forge.isNumericValue()) {
            methodNode.getBlock()
                    .declareVar(String.class, "value", lhs.getForge().evaluateCodegen(String.class, methodNode, exprSymbol, codegenClassScope))
                    .ifRefNullReturnNull("value")
                    .methodReturn(getLikeCode(forge, mLikeUtil, ref("value")));
        } else {
            methodNode.getBlock().declareVar(Object.class, "value", lhs.getForge().evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                    .ifRefNullReturnNull("value")
                    .methodReturn(getLikeCode(forge, mLikeUtil, exprDotMethod(ref("value"), "toString")));
        }
        return methodNode;
    }

    static CodegenExpression getLikeCode(ExprLikeNodeForge forge, CodegenExpression refLike, CodegenExpression stringExpr) {
        CodegenExpression eval = exprDotMethod(refLike, "compare", stringExpr);
        return !forge.getForgeRenderable().isNot() ? eval : not(eval);
    }
}
