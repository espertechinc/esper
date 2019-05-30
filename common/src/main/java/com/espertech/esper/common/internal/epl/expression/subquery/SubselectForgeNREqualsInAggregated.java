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
 * Represents a in-subselect evaluation strategy.
 */
public class SubselectForgeNREqualsInAggregated extends SubselectForgeNREqualsInBase {

    private final ExprForge havingEval;

    public SubselectForgeNREqualsInAggregated(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, boolean isNotIn, SimpleNumberCoercer coercer, ExprForge havingEval) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents, isNotIn, coercer);
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
            method.getBlock().ifCondition(exprDotMethod(left, "equals", ref("rhs"))).blockReturn(constant(!isNotIn));
        } else {
            method.getBlock().declareVar(Number.class, "left", coercer.coerceCodegen(left, symbols.getLeftResultType()))
                    .declareVar(Number.class, "right", coercer.coerceCodegen(ref("valueRight"), rightEvalType))
                    .declareVar(boolean.class, "eq", exprDotMethod(ref("left"), "equals", ref("right")))
                    .ifCondition(ref("eq")).blockReturn(constant(!isNotIn));
        }
        method.getBlock().methodReturn(constant(isNotIn));
        return localMethod(method);
    }
}
