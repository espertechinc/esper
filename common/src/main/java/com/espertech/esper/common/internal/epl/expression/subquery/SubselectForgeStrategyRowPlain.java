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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNodeEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.DECLARE_EVENTS_SHIFTED;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.REF_EVENTS_SHIFTED;

public class SubselectForgeStrategyRowPlain extends SubselectForgeStrategyRowBase {

    public SubselectForgeStrategyRowPlain(ExprSubselectRowNode subselect) {
        super(subselect);
    }

    public CodegenExpression evaluateCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        if (subselect.getEvaluationType() == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        CodegenMethod method = parent.makeChild((EPTypeClass) subselect.getEvaluationType(), this.getClass(), classScope);

        if (subselect.filterExpr == null) {
            method.getBlock()
                    .ifCondition(relational(exprDotMethod(symbols.getAddMatchingEvents(method), "size"), CodegenExpressionRelational.CodegenRelational.GT, constant(1)))
                    .blockReturn(constantNull());
            if (subselect.selectClause == null) {
                method.getBlock().methodReturn(cast((EPTypeClass) subselect.getEvaluationType(), staticMethod(EventBeanUtility.class, "getNonemptyFirstEventUnderlying", symbols.getAddMatchingEvents(method))));
                return localMethod(method);
            } else {
                method.getBlock().applyTri(DECLARE_EVENTS_SHIFTED, method, symbols)
                        .assignArrayElement(REF_EVENTS_SHIFTED, constant(0), staticMethod(EventBeanUtility.class, "getNonemptyFirstEvent", symbols.getAddMatchingEvents(method)));
            }
        } else {
            method.getBlock().applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);

            method.getBlock().declareVar(EventBean.EPTYPE, "filtered", constantNull());
            CodegenBlock foreach = method.getBlock().forEach(EventBean.EPTYPE, "event", symbols.getAddMatchingEvents(method));
            {
                foreach.assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("event"));
                CodegenMethod filter = CodegenLegoMethodExpression.codegenExpression(subselect.filterExpr, method, classScope);
                CodegenLegoBooleanExpression.codegenContinueIfNotNullAndNotPass(foreach, EPTypePremade.BOOLEANBOXED.getEPType(), localMethod(filter, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
                foreach.ifCondition(notEqualsNull(ref("filtered"))).blockReturn(constantNull())
                        .assignRef("filtered", ref("event"));
            }

            if (subselect.selectClause == null) {
                method.getBlock().ifRefNullReturnNull("filtered")
                        .methodReturn(cast((EPTypeClass) subselect.getEvaluationType(), exprDotUnderlying(ref("filtered"))));
                return localMethod(method);
            }

            method.getBlock().ifRefNullReturnNull("filtered")
                    .assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("filtered"));
        }

        CodegenExpression selectClause = getSelectClauseExpr(method, symbols, classScope);
        method.getBlock().methodReturn(selectClause);
        return localMethod(method);
    }

    public CodegenExpression evaluateGetCollEventsCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        if (subselect.filterExpr == null) {
            if (subselect.selectClause == null) {
                return symbols.getAddMatchingEvents(parent);
            } else {
                if (subselect.subselectMultirowType == null) {
                    ExprIdentNodeEvaluator eval = ((ExprIdentNode) subselect.selectClause[0]).getExprEvaluatorIdent();
                    CodegenMethod method = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), this.getClass(), classScope);
                    method.getBlock().declareVar(EPTypePremade.COLLECTION.getEPType(), "events", newInstance(EPTypePremade.ARRAYDEQUE.getEPType(), exprDotMethod(symbols.getAddMatchingEvents(method), "size")));
                    CodegenBlock foreach = method.getBlock().forEach(EventBean.EPTYPE, "event", symbols.getAddMatchingEvents(method));
                    {
                        foreach.declareVar(EPTypePremade.OBJECT.getEPType(), "fragment", eval.getGetter().eventBeanFragmentCodegen(ref("event"), method, classScope))
                                .ifRefNull("fragment").blockContinue()
                                .exprDotMethod(ref("events"), "add", ref("fragment"));
                    }
                    method.getBlock().methodReturn(ref("events"));
                    return localMethod(method);
                }

                // when selecting a combined output row that contains multiple fields
                CodegenMethod method = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), this.getClass(), classScope);
                CodegenExpressionField fieldEventType = classScope.addFieldUnshared(true, EventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(subselect.subselectMultirowType, EPStatementInitServices.REF));
                CodegenExpressionField eventBeanSvc = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);

                method.getBlock()
                        .declareVar(EPTypePremade.COLLECTION.getEPType(), "result", newInstance(EPTypePremade.ARRAYDEQUE.getEPType(), exprDotMethod(symbols.getAddMatchingEvents(method), "size")))
                        .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);
                CodegenBlock foreach = method.getBlock().forEach(EventBean.EPTYPE, "event", symbols.getAddMatchingEvents(method));
                {
                    foreach.assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("event"))
                            .declareVar(EPTypePremade.MAP.getEPType(), "row", localMethod(subselect.evaluateRowCodegen(method, classScope), REF_EVENTS_SHIFTED, constantTrue(), symbols.getAddExprEvalCtx(method)))
                            .declareVar(EventBean.EPTYPE, "rowEvent", exprDotMethod(eventBeanSvc, "adapterForTypedMap", ref("row"), fieldEventType))
                            .exprDotMethod(ref("result"), "add", ref("rowEvent"));
                }
                method.getBlock().methodReturn(ref("result"));
                return localMethod(method);
            }
        }

        if (subselect.selectClause != null) {
            return constantNull();
        }

        // handle filtered
        CodegenMethod method = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), this.getClass(), classScope);

        method.getBlock().applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);

        method.getBlock().declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "filtered", constantNull());
        CodegenBlock foreach = method.getBlock().forEach(EventBean.EPTYPE, "event", symbols.getAddMatchingEvents(method));
        {
            foreach.assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("event"));
            CodegenMethod filter = CodegenLegoMethodExpression.codegenExpression(subselect.filterExpr, method, classScope);
            CodegenLegoBooleanExpression.codegenContinueIfNullOrNotPass(foreach, EPTypePremade.BOOLEANBOXED.getEPType(), localMethod(filter, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
            foreach.ifCondition(equalsNull(ref("filtered")))
                    .assignRef("filtered", newInstance(EPTypePremade.ARRAYDEQUE.getEPType()))
                    .blockEnd()
                    .exprDotMethod(ref("filtered"), "add", ref("event"));
        }

        method.getBlock().methodReturn(ref("filtered"));
        return localMethod(method);
    }

    public CodegenExpression evaluateGetCollScalarCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        if (subselect.filterExpr == null) {
            if (subselect.selectClause == null) {
                return constantNull();
            } else {
                CodegenMethod method = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), this.getClass(), classScope);
                method.getBlock()
                        .declareVar(EPTypePremade.LIST.getEPType(), "result", newInstance(EPTypePremade.ARRAYLIST.getEPType()))
                        .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);
                CodegenExpression selectClause = getSelectClauseExpr(method, symbols, classScope);
                CodegenBlock foreach = method.getBlock().forEach(EventBean.EPTYPE, "event", symbols.getAddMatchingEvents(method));
                {
                    foreach.assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("event"))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "value", selectClause)
                            .exprDotMethod(ref("result"), "add", ref("value"));
                }
                method.getBlock().methodReturn(ref("result"));
                return localMethod(method);
            }
        }

        if (subselect.selectClause == null) {
            return constantNull();
        }

        CodegenMethod method = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), this.getClass(), classScope);
        method.getBlock()
                .declareVar(EPTypePremade.LIST.getEPType(), "result", newInstance(EPTypePremade.ARRAYLIST.getEPType()))
                .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);
        CodegenExpression selectClause = getSelectClauseExpr(method, symbols, classScope);
        CodegenMethod filter = CodegenLegoMethodExpression.codegenExpression(subselect.filterExpr, method, classScope);
        CodegenBlock foreach = method.getBlock().forEach(EventBean.EPTYPE, "event", symbols.getAddMatchingEvents(method));
        {
            foreach.assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("event"));
            CodegenLegoBooleanExpression.codegenContinueIfNullOrNotPass(foreach, EPTypePremade.BOOLEANBOXED.getEPType(), localMethod(filter, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
            foreach.declareVar(EPTypePremade.OBJECT.getEPType(), "value", selectClause)
                    .exprDotMethod(ref("result"), "add", ref("value"));
        }
        method.getBlock().methodReturn(ref("result"));
        return localMethod(method);
    }

    public CodegenExpression evaluateGetBeanCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EventBean.EPTYPE, this.getClass(), classScope);

        if (subselect.selectClause == null) {
            if (subselect.filterExpr == null) {
                method.getBlock()
                        .ifCondition(relational(exprDotMethod(symbols.getAddMatchingEvents(method), "size"), CodegenExpressionRelational.CodegenRelational.GT, constant(1)))
                        .blockReturn(constantNull())
                        .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols)
                        .methodReturn(staticMethod(EventBeanUtility.class, "getNonemptyFirstEvent", symbols.getAddMatchingEvents(method)));
                return localMethod(method);
            }

            CodegenExpression filter = ExprNodeUtilityCodegen.codegenEvaluator(subselect.filterExpr, method, this.getClass(), classScope);
            method.getBlock()
                    .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols)
                    .declareVar(EventBean.EPTYPE, "subSelectResult", staticMethod(EventBeanUtility.class, "evaluateFilterExpectSingleMatch",
                            REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddMatchingEvents(method), symbols.getAddExprEvalCtx(method),
                            filter))
                    .methodReturn(ref("subSelectResult"));
            return localMethod(method);
        }

        CodegenExpressionField eventBeanSvc = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField typeMember = classScope.addFieldUnshared(true, EventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(subselect.subselectMultirowType, EPStatementInitServices.REF));

        if (subselect.filterExpr == null) {
            method.getBlock()
                    .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols)
                    .assignArrayElement(REF_EVENTS_SHIFTED, constant(0), staticMethod(EventBeanUtility.class, "getNonemptyFirstEvent", symbols.getAddMatchingEvents(method)))
                    .declareVar(EPTypePremade.MAP.getEPType(), "row", localMethod(subselect.evaluateRowCodegen(method, classScope), REF_EVENTS_SHIFTED, constantTrue(), symbols.getAddExprEvalCtx(method)))
                    .declareVar(EventBean.EPTYPE, "bean", exprDotMethod(eventBeanSvc, "adapterForTypedMap", ref("row"), typeMember))
                    .methodReturn(ref("bean"));
            return localMethod(method);
        }

        CodegenExpression filter = ExprNodeUtilityCodegen.codegenEvaluator(subselect.filterExpr, method, this.getClass(), classScope);
        method.getBlock()
                .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols)
                .declareVar(EventBean.EPTYPE, "subSelectResult", staticMethod(EventBeanUtility.class, "evaluateFilterExpectSingleMatch",
                        REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddMatchingEvents(method), symbols.getAddExprEvalCtx(method),
                        filter))
                .ifRefNullReturnNull("subSelectResult")
                .declareVar(EPTypePremade.MAP.getEPType(), "row", localMethod(subselect.evaluateRowCodegen(method, classScope), REF_EVENTS_SHIFTED, constantTrue(), symbols.getAddExprEvalCtx(method)))
                .declareVar(EventBean.EPTYPE, "bean", exprDotMethod(eventBeanSvc, "adapterForTypedMap", ref("row"), typeMember))
                .methodReturn(ref("bean"));
        return localMethod(method);
    }

    private CodegenExpression getSelectClauseExpr(CodegenMethod method, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        if (subselect.selectClause.length == 1) {
            CodegenMethod eval = CodegenLegoMethodExpression.codegenExpression(subselect.selectClause[0].getForge(), method, classScope);
            return localMethod(eval, REF_EVENTS_SHIFTED, constantTrue(), symbols.getAddExprEvalCtx(method));
        }
        CodegenMethod methodSelect = ExprNodeUtilityCodegen.codegenMapSelect(subselect.selectClause, subselect.selectAsNames, this.getClass(), method, classScope);
        return localMethod(methodSelect, REF_EVENTS_SHIFTED, constantTrue(), symbols.getAddExprEvalCtx(method));
    }
}
