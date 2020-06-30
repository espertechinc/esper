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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubselectForgeNREqualsInWGroupBy extends SubselectForgeNREqualsInBase {
    private final ExprForge havingEval;

    public SubselectForgeNREqualsInWGroupBy(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, boolean isNotIn, SimpleNumberCoercer coercer, ExprForge havingEval) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents, isNotIn, coercer);
        this.havingEval = havingEval;
    }

    protected CodegenExpression codegenEvaluateInternal(CodegenMethodScope parent, SubselectForgeNRSymbol symbols, CodegenClassScope classScope) {
        if (subselect.getEvaluationType() == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        CodegenExpression aggService = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameSubqueryAgg(subselect.getSubselectNumber()), AggregationResultFuture.EPTYPE);

        CodegenMethod method = parent.makeChild((EPTypeClass) subselect.getEvaluationType(), this.getClass(), classScope);
        CodegenExpressionRef evalCtx = symbols.getAddExprEvalCtx(method);
        CodegenExpressionRef left = symbols.getAddLeftResult(method);

        method.getBlock()
                .ifCondition(equalsNull(left)).blockReturn(constantNull())
                .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "cpid", exprDotMethod(evalCtx, "getAgentInstanceId"))
                .declareVar(AggregationService.EPTYPE, "aggregationService", exprDotMethod(aggService, "getContextPartitionAggregationService", ref("cpid")))
                .declareVar(EPTypePremade.COLLECTION.getEPType(), "groupKeys", exprDotMethod(ref("aggregationService"), "getGroupKeys", evalCtx))
                .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "hasNullRow", constantFalse());

        CodegenBlock forEach = method.getBlock().forEach(EPTypePremade.OBJECT.getEPType(), "groupKey", ref("groupKeys"));
        {
            forEach.exprDotMethod(ref("aggregationService"), "setCurrentAccess", ref("groupKey"), ref("cpid"), constantNull());

            if (havingEval != null) {
                CodegenLegoBooleanExpression.codegenContinueIfNullOrNotPass(forEach, havingEval.getEvaluationType(), havingEval.evaluateCodegen((EPTypeClass) havingEval.getEvaluationType(), method, symbols, classScope));
            }

            EPTypeClass valueRightType;
            if (selectEval != null) {
                valueRightType = JavaClassHelper.getBoxedType((EPTypeClass) selectEval.getEvaluationType());
                forEach.declareVar(valueRightType, "valueRight", selectEval.evaluateCodegen(valueRightType, method, symbols, classScope));
            } else {
                valueRightType = EPTypePremade.OBJECT.getEPType();
                forEach.declareVar(valueRightType, "valueRight", exprDotUnderlying(arrayAtIndex(symbols.getAddEPS(method), constant(0))));
            }

            CodegenBlock ifRightNotNull = forEach.ifCondition(equalsNull(ref("valueRight")))
                    .assignRef("hasNullRow", constantTrue())
                    .ifElse();
            {
                if (coercer == null) {
                    ifRightNotNull.declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "eq", exprDotMethod(left, "equals", ref("valueRight")));
                } else {
                    ifRightNotNull.declareVar(EPTypePremade.NUMBER.getEPType(), "left", coercer.coerceCodegen(left, symbols.getLeftResultType()))
                            .declareVar(EPTypePremade.NUMBER.getEPType(), "right", coercer.coerceCodegen(ref("valueRight"), valueRightType))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "eq", exprDotMethod(ref("left"), "equals", ref("right")));
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
