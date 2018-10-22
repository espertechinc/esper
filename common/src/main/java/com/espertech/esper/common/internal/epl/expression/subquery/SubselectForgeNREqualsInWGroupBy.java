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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameSubqueryAgg;
import com.espertech.esper.common.internal.epl.agg.core.AggregationResultFuture;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubselectForgeNREqualsInWGroupBy extends SubselectForgeNREqualsInBase {
    private final ExprForge havingEval;

    public SubselectForgeNREqualsInWGroupBy(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, boolean isNotIn, SimpleNumberCoercer coercer, ExprForge havingEval) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents, isNotIn, coercer);
        this.havingEval = havingEval;
    }

    protected CodegenExpression codegenEvaluateInternal(CodegenMethodScope parent, SubselectForgeNRSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression aggService = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameSubqueryAgg(subselect.getSubselectNumber()), AggregationResultFuture.class);

        CodegenMethod method = parent.makeChild(subselect.getEvaluationType(), this.getClass(), classScope);
        CodegenExpressionRef evalCtx = symbols.getAddExprEvalCtx(method);
        CodegenExpressionRef left = symbols.getAddLeftResult(method);

        method.getBlock()
                .ifCondition(equalsNull(left)).blockReturn(constantNull())
                .declareVar(int.class, "cpid", exprDotMethod(evalCtx, "getAgentInstanceId"))
                .declareVar(AggregationService.class, "aggregationService", exprDotMethod(aggService, "getContextPartitionAggregationService", ref("cpid")))
                .declareVar(Collection.class, "groupKeys", exprDotMethod(ref("aggregationService"), "getGroupKeys", evalCtx))
                .declareVar(boolean.class, "hasNullRow", constantFalse());

        CodegenBlock forEach = method.getBlock().forEach(Object.class, "groupKey", ref("groupKeys"));
        {
            forEach.exprDotMethod(ref("aggregationService"), "setCurrentAccess", ref("groupKey"), ref("cpid"), constantNull());

            if (havingEval != null) {
                CodegenLegoBooleanExpression.codegenContinueIfNullOrNotPass(forEach, havingEval.getEvaluationType(), havingEval.evaluateCodegen(havingEval.getEvaluationType(), method, symbols, classScope));
            }

            Class valueRightType;
            if (selectEval != null) {
                valueRightType = JavaClassHelper.getBoxedType(selectEval.getEvaluationType());
                forEach.declareVar(valueRightType, "valueRight", selectEval.evaluateCodegen(valueRightType, method, symbols, classScope));
            } else {
                valueRightType = Object.class;
                forEach.declareVar(valueRightType, "valueRight", exprDotUnderlying(arrayAtIndex(symbols.getAddEPS(method), constant(0))));
            }

            CodegenBlock ifRightNotNull = forEach.ifCondition(equalsNull(ref("valueRight")))
                    .assignRef("hasNullRow", constantTrue())
                    .ifElse();
            {
                if (coercer == null) {
                    ifRightNotNull.declareVar(boolean.class, "eq", exprDotMethod(left, "equals", ref("valueRight")));
                } else {
                    ifRightNotNull.declareVar(Number.class, "left", coercer.coerceCodegen(left, symbols.getLeftResultType()))
                            .declareVar(Number.class, "right", coercer.coerceCodegen(ref("valueRight"), valueRightType))
                            .declareVar(boolean.class, "eq", exprDotMethod(ref("left"), "equals", ref("right")));
                }
                ifRightNotNull.ifCondition(ref("eq")).blockReturn(constant(!isNotIn));
            }
        }

        method.getBlock()
                .ifCondition(ref("hasNullRow")).blockReturn(constantNull())
                .methodReturn(constant(isNotIn));

        return localMethod(method);
    }

}
