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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameSubqueryAgg;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationResultFuture;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.DECLARE_EVENTS_SHIFTED;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.REF_EVENTS_SHIFTED;

/**
 * Represents a subselect in an expression tree.
 */
public class SubselectForgeRowUnfilteredSelectedGroupedNoHaving extends SubselectForgeStrategyRowPlain {

    public SubselectForgeRowUnfilteredSelectedGroupedNoHaving(ExprSubselectRowNode subselect) {
        super(subselect);
    }

    public CodegenExpression evaluateCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {

        CodegenExpression aggService = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameSubqueryAgg(subselect.getSubselectNumber()), AggregationResultFuture.class);

        CodegenMethod method = parent.makeChild(subselect.getEvaluationType(), this.getClass(), classScope);
        CodegenExpressionRef evalCtx = symbols.getAddExprEvalCtx(method);

        method.getBlock()
                .declareVar(int.class, "cpid", exprDotMethod(evalCtx, "getAgentInstanceId"))
                .declareVar(Collection.class, "groupKeys", exprDotMethod(aggService, "getGroupKeys", evalCtx))
                .ifCondition(not(equalsIdentity(exprDotMethod(ref("groupKeys"), "size"), constant(1))))
                .blockReturn(constantNull())
                .exprDotMethod(aggService, "setCurrentAccess", exprDotMethodChain(ref("groupKeys")).add("iterator").add("next"), ref("cpid"), constantNull())
                .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols)
                .assignArrayElement(REF_EVENTS_SHIFTED, constant(0), staticMethod(EventBeanUtility.class, "getNonemptyFirstEvent", symbols.getAddMatchingEvents(method)));

        if (subselect.selectClause.length == 1) {
            CodegenMethod eval = CodegenLegoMethodExpression.codegenExpression(subselect.selectClause[0].getForge(), method, classScope);
            method.getBlock().methodReturn(localMethod(eval, REF_EVENTS_SHIFTED, constantTrue(), symbols.getAddExprEvalCtx(method)));
        } else {
            CodegenMethod methodSelect = ExprNodeUtilityCodegen.codegenMapSelect(subselect.selectClause, subselect.selectAsNames, this.getClass(), method, classScope);
            CodegenExpression select = localMethod(methodSelect, REF_EVENTS_SHIFTED, constantTrue(), symbols.getAddExprEvalCtx(method));
            method.getBlock().methodReturn(select);
        }
        return localMethod(method);
    }

    public CodegenExpression evaluateGetCollEventsCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression aggService = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameSubqueryAgg(subselect.getSubselectNumber()), AggregationResultFuture.class);

        CodegenMethod method = parent.makeChild(Collection.class, this.getClass(), classScope);
        CodegenExpressionRef evalCtx = symbols.getAddExprEvalCtx(method);
        CodegenExpressionField eventBeanSvc = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField typeMember = classScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(subselect.subselectMultirowType, EPStatementInitServices.REF));

        method.getBlock()
                .declareVar(int.class, "cpid", exprDotMethod(evalCtx, "getAgentInstanceId"))
                .declareVar(AggregationService.class, "aggregationService", exprDotMethod(aggService, "getContextPartitionAggregationService", ref("cpid")))
                .declareVar(Collection.class, "groupKeys", exprDotMethod(aggService, "getGroupKeys", evalCtx))
                .ifCondition(exprDotMethod(ref("groupKeys"), "isEmpty"))
                .blockReturn(constantNull())
                .declareVar(Collection.class, "events", newInstance(ArrayDeque.class, exprDotMethod(ref("groupKeys"), "size")))
                .forEach(Object.class, "groupKey", ref("groupKeys"))
                .exprDotMethod(aggService, "setCurrentAccess", ref("groupKey"), ref("cpid"), constantNull())
                .declareVar(Map.class, "row", localMethod(subselect.evaluateRowCodegen(method, classScope), constantNull(), constantTrue(), symbols.getAddExprEvalCtx(method)))
                .declareVar(EventBean.class, "event", exprDotMethod(eventBeanSvc, "adapterForTypedMap", ref("row"), typeMember))
                .exprDotMethod(ref("events"), "add", ref("event"))
                .blockEnd()
                .methodReturn(ref("events"));
        return localMethod(method);
    }

    public CodegenExpression evaluateGetCollScalarCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbol, CodegenClassScope classScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetBeanCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }
}
