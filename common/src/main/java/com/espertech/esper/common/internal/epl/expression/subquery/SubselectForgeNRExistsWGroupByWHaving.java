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

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.DECLARE_EVENTS_SHIFTED;

public class SubselectForgeNRExistsWGroupByWHaving implements SubselectForgeNR {

    private final ExprSubselectNode subselect;
    private final ExprForge havingEval;

    public SubselectForgeNRExistsWGroupByWHaving(ExprSubselectNode subselect, ExprForge havingEval) {
        this.subselect = subselect;
        this.havingEval = havingEval;
    }

    public CodegenExpression evaluateMatchesCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression aggService = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameSubqueryAgg(subselect.getSubselectNumber()), AggregationResultFuture.class);

        CodegenMethod method = parent.makeChild(boolean.class, this.getClass(), classScope);
        CodegenExpressionRef evalCtx = symbols.getAddExprEvalCtx(method);

        method.getBlock()
                .applyTri(new SubselectForgeCodegenUtil.ReturnIfNoMatch(constantFalse(), constantFalse()), method, symbols)
                .declareVar(int.class, "cpid", exprDotMethod(evalCtx, "getAgentInstanceId"))
                .declareVar(AggregationService.class, "aggregationService", exprDotMethod(aggService, "getContextPartitionAggregationService", ref("cpid")))
                .declareVar(Collection.class, "groupKeys", exprDotMethod(ref("aggregationService"), "getGroupKeys", evalCtx));
        method.getBlock().applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);

        CodegenBlock forEach = method.getBlock().forEach(Object.class, "groupKey", ref("groupKeys"));
        {
            forEach.exprDotMethod(ref("aggregationService"), "setCurrentAccess", ref("groupKey"), ref("cpid"), constantNull());
            CodegenLegoBooleanExpression.codegenContinueIfNullOrNotPass(forEach, havingEval.getEvaluationType(), havingEval.evaluateCodegen(havingEval.getEvaluationType(), method, symbols, classScope));
            forEach.blockReturn(constantTrue());
        }
        method.getBlock().methodReturn(constantFalse());

        return localMethod(method);
    }
}
