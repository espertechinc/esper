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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorCodegenNames.CLASSNAME_ORDERBYPROCESSOR;

public class OrderByProcessorOrderedLimitForge implements OrderByProcessorFactoryForge {
    final static CodegenExpressionRef REF_ROWLIMITPROCESSOR = ref("rowLimitProcessor");

    private final OrderByProcessorForgeImpl orderByProcessorForge;
    private final RowLimitProcessorFactoryForge rowLimitProcessorFactoryForge;

    public OrderByProcessorOrderedLimitForge(OrderByProcessorForgeImpl orderByProcessorForge, RowLimitProcessorFactoryForge rowLimitProcessorFactoryForge) {
        this.orderByProcessorForge = orderByProcessorForge;
        this.rowLimitProcessorFactoryForge = rowLimitProcessorFactoryForge;
    }

    public void instantiateCodegen(CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpressionField rowLimitFactory = classScope.addFieldUnshared(true, RowLimitProcessorFactory.class, rowLimitProcessorFactoryForge.make(classScope.getPackageScope().getInitMethod(), classScope));
        method.getBlock().declareVar(RowLimitProcessor.class, REF_ROWLIMITPROCESSOR.getRef(), exprDotMethod(rowLimitFactory, "instantiate", MEMBER_AGENTINSTANCECONTEXT))
                .methodReturn(CodegenExpressionBuilder.newInstance(CLASSNAME_ORDERBYPROCESSOR, ref("o"), REF_ROWLIMITPROCESSOR));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> members, CodegenClassScope classScope) {
        ctor.getCtorParams().add(new CodegenTypedParam(RowLimitProcessor.class, REF_ROWLIMITPROCESSOR.getRef()));
    }

    public void sortPlainCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorOrderedLimit.sortPlainCodegenCodegen(this, method, classScope, namedMethods);
    }

    public void sortWGroupKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorOrderedLimit.sortWGroupKeysCodegen(this, method, classScope, namedMethods);
    }

    public void sortRollupCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (orderByProcessorForge.getOrderByRollup() == null) {
            method.getBlock().methodThrowUnsupported();
            return;
        }
        OrderByProcessorOrderedLimit.sortRollupCodegen(this, method, classScope, namedMethods);
    }

    public void getSortKeyCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorImpl.getSortKeyCodegen(orderByProcessorForge, method, classScope, namedMethods);
    }

    public void getSortKeyRollupCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (orderByProcessorForge.getOrderByRollup() == null) {
            method.getBlock().methodThrowUnsupported();
            return;
        }
        OrderByProcessorImpl.getSortKeyRollupCodegen(orderByProcessorForge, method, classScope, namedMethods);
    }

    public void sortWOrderKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorOrderedLimit.sortWOrderKeysCodegen(this, method, classScope, namedMethods);
    }

    public void sortTwoKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorOrderedLimit.sortTwoKeysCodegen(this, method, classScope, namedMethods);
    }

    OrderByProcessorForgeImpl getOrderByProcessorForge() {
        return orderByProcessorForge;
    }
}
