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
package com.espertech.esper.common.internal.epl.resultset.order;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorCodegenNames.CLASSNAME_ORDERBYPROCESSOR;

public class OrderByProcessorForgeImpl implements OrderByProcessorFactoryForge {

    private final OrderByElementForge[] orderBy;
    private final boolean needsGroupByKeys;
    private final OrderByElementForge[][] orderByRollup;
    private final CodegenFieldSharable comparator;

    public OrderByProcessorForgeImpl(OrderByElementForge[] orderBy, boolean needsGroupByKeys, OrderByElementForge[][] orderByRollup, CodegenFieldSharable comparator) {
        this.orderBy = orderBy;
        this.needsGroupByKeys = needsGroupByKeys;
        this.orderByRollup = orderByRollup;
        this.comparator = comparator;
    }

    public void instantiateCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(newInstance(CLASSNAME_ORDERBYPROCESSOR, ref("o")));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> members, CodegenClassScope classScope) {
    }

    public void sortPlainCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.sortPlainCodegen(this, method, classScope, namedMethods);
    }

    public void sortWGroupKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.sortWGroupKeysCodegen(this, method, classScope, namedMethods);
    }

    public void sortRollupCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (orderByRollup == null) {
            method.getBlock().methodThrowUnsupported();
            return;
        }
        OrderByProcessorImpl.sortRollupCodegen(this, method, classScope, namedMethods);
    }

    public void getSortKeyCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.getSortKeyCodegen(this, method, classScope, namedMethods);
    }

    public void getSortKeyRollupCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (orderByRollup == null) {
            method.getBlock().methodThrowUnsupported();
            return;
        }
        OrderByProcessorImpl.getSortKeyRollupCodegen(this, method, classScope, namedMethods);
    }

    public void sortWOrderKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.sortWOrderKeysCodegen(this, method, classScope);
    }

    public void sortTwoKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.sortTwoKeysCodegen(this, method, classScope, namedMethods);
    }

    public OrderByElementForge[] getOrderBy() {
        return orderBy;
    }

    public boolean isNeedsGroupByKeys() {
        return needsGroupByKeys;
    }

    public OrderByElementForge[][] getOrderByRollup() {
        return orderByRollup;
    }

    public CodegenFieldSharable getComparator() {
        return comparator;
    }

    public String[] getExpressionTexts() {
        String[] expressions = new String[orderBy.length];
        for (int i = 0; i < orderBy.length; i++) {
            expressions[i] = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(orderBy[i].getExprNode());
        }
        return expressions;
    }

    public boolean[] getDescendingFlags() {
        boolean[] descending = new boolean[orderBy.length];
        for (int i = 0; i < orderBy.length; i++) {
            descending[i] = orderBy[i].isDescending();
        }
        return descending;
    }
}
