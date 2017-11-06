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
package com.espertech.esper.epl.core.orderby;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.codegen.core.CodegenTypedParam;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

import java.util.Comparator;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.newInstanceInnerClass;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.epl.core.orderby.OrderByProcessorCodegenNames.CLASSNAME_ORDERBYPROCESSOR;

public class OrderByProcessorForgeImpl implements OrderByProcessorFactoryForge {

    private final OrderByElementForge[] orderBy;
    private final boolean needsGroupByKeys;
    private final Comparator<Object> comparator;
    private final OrderByElementForge[][] orderByRollup;

    public OrderByProcessorForgeImpl(OrderByElementForge[] orderBy, boolean needsGroupByKeys, Comparator<Object> comparator, OrderByElementForge[][] orderByRollup) {
        this.orderBy = orderBy;
        this.needsGroupByKeys = needsGroupByKeys;
        this.comparator = comparator;
        this.orderByRollup = orderByRollup;
    }

    public OrderByProcessorFactoryImpl make(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        OrderByElementEval[] elements = makeEvalArray(orderBy, engineImportService, isFireAndForget, statementName);
        OrderByElementEval[][] rollupEvals = orderByRollup != null ? makeEvalRollup(orderByRollup, engineImportService, isFireAndForget, statementName) : null;
        return new OrderByProcessorFactoryImpl(elements, needsGroupByKeys, comparator, rollupEvals);
    }

    public void instantiateCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(newInstanceInnerClass(CLASSNAME_ORDERBYPROCESSOR, ref("o")));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> members, CodegenClassScope classScope) {
    }

    public void sortPlainCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.sortPlainCodegen(this, method, classScope, namedMethods);
    }

    public void sortWGroupKeysCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.sortWGroupKeysCodegen(this, method, classScope, namedMethods);
    }

    public void sortRollupCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (orderByRollup == null) {
            method.getBlock().methodThrowUnsupported();
            return;
        }
        OrderByProcessorImpl.sortRollupCodegen(this, method, classScope, namedMethods);
    }

    public void getSortKeyCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.getSortKeyCodegen(this, method, classScope, namedMethods);
    }

    public void getSortKeyRollupCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (orderByRollup == null) {
            method.getBlock().methodThrowUnsupported();
            return;
        }
        OrderByProcessorImpl.getSortKeyRollupCodegen(this, method, classScope, namedMethods);
    }

    public void sortWOrderKeysCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.sortWOrderKeysCodegen(this, method, classScope);
    }

    public void sortTwoKeysCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.sortTwoKeysCodegen(this, method, classScope, namedMethods);
    }

    public OrderByElementForge[] getOrderBy() {
        return orderBy;
    }

    public boolean isNeedsGroupByKeys() {
        return needsGroupByKeys;
    }

    public Comparator<Object> getComparator() {
        return comparator;
    }

    public OrderByElementForge[][] getOrderByRollup() {
        return orderByRollup;
    }

    private OrderByElementEval[] makeEvalArray(OrderByElementForge[] orderBy, EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        OrderByElementEval[] elements = new OrderByElementEval[orderBy.length];
        for (int i = 0; i < orderBy.length; i++) {
            ExprEvaluator eval = ExprNodeCompiler.allocateEvaluator(orderBy[i].getExprNode().getForge(), engineImportService, OrderByProcessorForgeImpl.class, isFireAndForget, statementName);
            elements[i] = new OrderByElementEval(orderBy[i].getExprNode(), eval, orderBy[i].isDescending());
        }
        return elements;
    }

    private OrderByElementEval[][] makeEvalRollup(OrderByElementForge[][] orderByRollup, EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        OrderByElementEval[][] evals = new OrderByElementEval[orderByRollup.length][];
        for (int i = 0; i < orderByRollup.length; i++) {
            evals[i] = makeEvalArray(orderByRollup[i], engineImportService, isFireAndForget, statementName);
        }
        return evals;
    }
}
