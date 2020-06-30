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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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
        if (subselect.getEvaluationType() == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        CodegenExpression aggService = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameSubqueryAgg(subselect.getSubselectNumber()), AggregationResultFuture.EPTYPE);
        CodegenMethod method = parent.makeChild((EPTypeClass) subselect.getEvaluationType(), this.getClass(), classScope);
        CodegenExpressionRef evalCtx = symbols.getAddExprEvalCtx(method);

        method.getBlock()
                .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "cpid", exprDotMethod(evalCtx, "getAgentInstanceId"))
                .declareVar(EPTypePremade.COLLECTION.getEPType(), "groupKeys", exprDotMethod(aggService, "getGroupKeys", evalCtx))
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
        CodegenExpression aggService = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameSubqueryAgg(subselect.getSubselectNumber()), AggregationResultFuture.EPTYPE);

        CodegenMethod method = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), this.getClass(), classScope);
        CodegenExpressionRef evalCtx = symbols.getAddExprEvalCtx(method);
        CodegenExpressionField eventBeanSvc = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField typeMember = classScope.addFieldUnshared(true, EventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(subselect.subselectMultirowType, EPStatementInitServices.REF));

        method.getBlock()
                .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "cpid", exprDotMethod(evalCtx, "getAgentInstanceId"))
                .declareVar(AggregationService.EPTYPE, "aggregationService", exprDotMethod(aggService, "getContextPartitionAggregationService", ref("cpid")))
                .declareVar(EPTypePremade.COLLECTION.getEPType(), "groupKeys", exprDotMethod(aggService, "getGroupKeys", evalCtx))
                .ifCondition(exprDotMethod(ref("groupKeys"), "isEmpty"))
                .blockReturn(constantNull())
                .declareVar(EPTypePremade.COLLECTION.getEPType(), "events", newInstance(EPTypePremade.ARRAYDEQUE.getEPType(), exprDotMethod(ref("groupKeys"), "size")))
                .forEach(EPTypePremade.OBJECT.getEPType(), "groupKey", ref("groupKeys"))
                .exprDotMethod(aggService, "setCurrentAccess", ref("groupKey"), ref("cpid"), constantNull())
                .declareVar(EPTypePremade.MAP.getEPType(), "row", localMethod(subselect.evaluateRowCodegen(method, classScope), constantNull(), constantTrue(), symbols.getAddExprEvalCtx(method)))
                .declareVar(EventBean.EPTYPE, "event", exprDotMethod(eventBeanSvc, "adapterForTypedMap", ref("row"), typeMember))
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
