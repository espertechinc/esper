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
package com.espertech.esper.common.internal.epl.expression.subquery;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Strategy for subselects with "=/!=/&gt;&lt; ALL".
 */
public class SubselectForgeNREqualsAllAnyAggregated extends SubselectForgeNREqualsBase {
    private final ExprForge havingEval;

    public SubselectForgeNREqualsAllAnyAggregated(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, boolean isNot, SimpleNumberCoercer coercer, ExprForge havingEval) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents, isNot, coercer);
        this.havingEval = havingEval;
    }

    protected CodegenExpression codegenEvaluateInternal(CodegenMethodScope parent, SubselectForgeNRSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Boolean.class, this.getClass(), classScope);
        CodegenExpressionRef eps = symbols.getAddEPS(method);
        CodegenExpressionRef evalCtx = symbols.getAddExprEvalCtx(method);
        CodegenExpressionRef left = symbols.getAddLeftResult(method);

        method.getBlock().ifNullReturnNull(symbols.getAddLeftResult(method));
        if (havingEval != null) {
            CodegenExpression having = localMethod(CodegenLegoMethodExpression.codegenExpression(havingEval, method, classScope), eps, constantTrue(), evalCtx);
            CodegenLegoBooleanExpression.codegenReturnValueIfNullOrNotPass(method.getBlock(), havingEval.getEvaluationType(), having, constantNull());
        }

        CodegenExpression select = localMethod(CodegenLegoMethodExpression.codegenExpression(selectEval, method, classScope), eps, constantTrue(), evalCtx);
        Class rightEvalType = JavaClassHelper.getBoxedType(selectEval.getEvaluationType());
        method.getBlock()
                .declareVar(rightEvalType, "rhs", select)
                .ifRefNullReturnNull("rhs");

        if (coercer == null) {
            method.getBlock().declareVar(boolean.class, "eq", exprDotMethod(left, "equals", ref("rhs")));
            if (isNot) {
                method.getBlock().ifCondition(ref("eq")).blockReturn(constantFalse());
            } else {
                method.getBlock().ifCondition(not(ref("eq"))).blockReturn(constantFalse());
            }
        } else {
            method.getBlock().declareVar(Number.class, "left", coercer.coerceCodegen(left, symbols.getLeftResultType()))
                    .declareVar(Number.class, "right", coercer.coerceCodegen(ref("rhs"), rightEvalType))
                    .declareVar(boolean.class, "eq", exprDotMethod(ref("left"), "equals", ref("right")));
            if (isNot) {
                method.getBlock().ifCondition(ref("eq")).blockReturn(constantFalse());
            } else {
                method.getBlock().ifCondition(not(ref("eq"))).blockReturn(constantFalse());
            }
        }
        method.getBlock().methodReturn(constantTrue());
        return localMethod(method);
    }
}
