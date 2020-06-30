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
import com.espertech.esper.common.internal.type.RelationalOpEnum;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubselectForgeNRRelOpAnyWGroupBy extends SubselectForgeNRRelOpBase {

    private final ExprForge havingEval;

    public SubselectForgeNRRelOpAnyWGroupBy(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, RelationalOpEnum.Computer computer, ExprForge havingEval) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents, computer);
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
                .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "cpid", exprDotMethod(evalCtx, "getAgentInstanceId"))
                .declareVar(AggregationService.EPTYPE, "aggregationService", exprDotMethod(aggService, "getContextPartitionAggregationService", ref("cpid")))
                .declareVar(EPTypePremade.COLLECTION.getEPType(), "groupKeys", exprDotMethod(ref("aggregationService"), "getGroupKeys", evalCtx))
                .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "hasRows", constantFalse())
                .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "hasNonNullRow", constantFalse());

        CodegenBlock forEach = method.getBlock().forEach(EPTypePremade.OBJECT.getEPType(), "groupKey", ref("groupKeys"));
        {
            forEach.exprDotMethod(ref("aggregationService"), "setCurrentAccess", ref("groupKey"), ref("cpid"), constantNull());

            if (havingEval != null) {
                CodegenLegoBooleanExpression.codegenContinueIfNullOrNotPass(forEach, havingEval.getEvaluationType(), havingEval.evaluateCodegen((EPTypeClass) havingEval.getEvaluationType(), method, symbols, classScope));
            }

            forEach.assignRef("hasRows", constantTrue());

            EPTypeClass valueRightType;
            if (selectEval != null) {
                valueRightType = JavaClassHelper.getBoxedType((EPTypeClass) selectEval.getEvaluationType());
                forEach.declareVar(valueRightType, "valueRight", selectEval.evaluateCodegen(valueRightType, method, symbols, classScope));
            } else {
                valueRightType = EPTypePremade.OBJECT.getEPType();
                forEach.declareVar(valueRightType, "valueRight", exprDotUnderlying(arrayAtIndex(symbols.getAddEPS(method), constant(0))));
            }

            forEach.ifCondition(notEqualsNull(ref("valueRight")))
                    .assignRef("hasNonNullRow", constantTrue())
                    .blockEnd()
                    .ifCondition(and(notEqualsNull(left), notEqualsNull(ref("valueRight"))))
                    .ifCondition(computer.codegen(left, symbols.getLeftResultType(), ref("valueRight"), valueRightType))
                    .blockReturn(constantTrue());
        }

        method.getBlock()
                .ifCondition(not(ref("hasRows"))).blockReturn(constantFalse())
                .ifCondition(not(ref("hasNonNullRow"))).blockReturn(constantNull())
                .ifCondition(equalsNull(left)).blockReturn(constantNull())
                .methodReturn(constantFalse());

        return localMethod(method);
    }
}
