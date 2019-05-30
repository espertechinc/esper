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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.ArrayDeque;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.DECLARE_EVENTS_SHIFTED;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.REF_EVENTS_SHIFTED;

public abstract class SubselectForgeStrategyRowBase implements SubselectForgeRow {

    protected final ExprSubselectRowNode subselect;

    public SubselectForgeStrategyRowBase(ExprSubselectRowNode subselect) {
        this.subselect = subselect;
    }

    public CodegenExpression evaluateTypableSinglerowCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        if (subselect.selectClause == null) {
            return constantNull(); // no select-clause
        }

        CodegenMethod method = parent.makeChild(Object[].class, this.getClass(), classScope);
        if (subselect.filterExpr == null) {
            method.getBlock()
                    .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols)
                    .assignArrayElement(REF_EVENTS_SHIFTED, constant(0), staticMethod(EventBeanUtility.class, "getNonemptyFirstEvent", symbols.getAddMatchingEvents(method)));
        } else {
            CodegenExpression filter = ExprNodeUtilityCodegen.codegenEvaluator(subselect.filterExpr, method, this.getClass(), classScope);
            method.getBlock()
                    .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols)
                    .declareVar(EventBean.class, "subselectResult", staticMethod(EventBeanUtility.class, "evaluateFilterExpectSingleMatch",
                            REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddMatchingEvents(method), symbols.getAddExprEvalCtx(method),
                            filter))
                    .ifRefNullReturnNull("subselectResult")
                    .assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("subselectResult"));
        }

        method.getBlock().declareVar(Object[].class, "results", newArrayByLength(Object.class, constant(subselect.selectClause.length)));
        for (int i = 0; i < subselect.selectClause.length; i++) {
            CodegenMethod eval = CodegenLegoMethodExpression.codegenExpression(subselect.selectClause[i].getForge(), method, classScope);
            method.getBlock().assignArrayElement("results", constant(i), localMethod(eval, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
        }
        method.getBlock().methodReturn(ref("results"));

        return localMethod(method);
    }

    public CodegenExpression evaluateTypableMultirowCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        if (subselect.selectClause == null) {
            return constantNull();
        }
        if (subselect.filterExpr == null) {
            CodegenMethod method = parent.makeChild(Object[][].class, this.getClass(), classScope);
            method.getBlock()
                    .declareVar(Object[][].class, "rows", newArrayByLength(Object[].class, exprDotMethod(symbols.getAddMatchingEvents(method), "size")))
                    .declareVar(int.class, "index", constant(-1))
                    .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);
            CodegenBlock foreachEvent = method.getBlock().forEach(EventBean.class, "event", symbols.getAddMatchingEvents(method));
            {
                foreachEvent
                        .incrementRef("index")
                        .assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("event"))
                        .declareVar(Object[].class, "results", newArrayByLength(Object.class, constant(subselect.selectClause.length)))
                        .assignArrayElement("rows", ref("index"), ref("results"));
                for (int i = 0; i < subselect.selectClause.length; i++) {
                    CodegenMethod eval = CodegenLegoMethodExpression.codegenExpression(subselect.selectClause[i].getForge(), method, classScope);
                    foreachEvent.assignArrayElement("results", constant(i), localMethod(eval, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
                }
            }
            method.getBlock().methodReturn(ref("rows"));
            return localMethod(method);
        } else {
            CodegenMethod method = parent.makeChild(Object[][].class, this.getClass(), classScope);
            method.getBlock()
                    .declareVar(ArrayDeque.class, Object[].class, "rows", newInstance(ArrayDeque.class))
                    .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);
            CodegenBlock foreachEvent = method.getBlock().forEach(EventBean.class, "event", symbols.getAddMatchingEvents(method));
            {
                foreachEvent.assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("event"));

                CodegenMethod filter = CodegenLegoMethodExpression.codegenExpression(subselect.filterExpr, method, classScope);
                CodegenLegoBooleanExpression.codegenContinueIfNullOrNotPass(foreachEvent, Boolean.class, localMethod(filter, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));

                foreachEvent
                        .declareVar(Object[].class, "results", newArrayByLength(Object.class, constant(subselect.selectClause.length)))
                        .exprDotMethod(ref("rows"), "add", ref("results"));
                for (int i = 0; i < subselect.selectClause.length; i++) {
                    CodegenMethod eval = CodegenLegoMethodExpression.codegenExpression(subselect.selectClause[i].getForge(), method, classScope);
                    foreachEvent.assignArrayElement("results", constant(i), localMethod(eval, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
                }
            }
            method.getBlock()
                    .ifCondition(exprDotMethod(ref("rows"), "isEmpty"))
                    .blockReturn(enumValue(CollectionUtil.class, "OBJECTARRAYARRAY_EMPTY"))
                    .methodReturn(cast(Object[][].class, exprDotMethod(ref("rows"), "toArray", newArrayByLength(Object[].class, constant(0)))));
            return localMethod(method);
        }
    }
}
