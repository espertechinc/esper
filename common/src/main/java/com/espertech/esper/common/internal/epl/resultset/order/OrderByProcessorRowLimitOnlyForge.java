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

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorCodegenNames.CLASSNAME_ORDERBYPROCESSOR;
import static com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorOrderedLimitForge.REF_ROWLIMITPROCESSOR;

/**
 * An order-by processor that sorts events according to the expressions
 * in the order_by clause.
 */
public class OrderByProcessorRowLimitOnlyForge implements OrderByProcessorFactoryForge {

    private final RowLimitProcessorFactoryForge rowLimitProcessorFactoryForge;

    public OrderByProcessorRowLimitOnlyForge(RowLimitProcessorFactoryForge rowLimitProcessorFactoryForge) {
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
        OrderByProcessorRowLimitOnly.sortPlainCodegen(method);
    }

    public void sortWGroupKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorRowLimitOnly.sortWGroupKeysCodegen(method);
    }

    public void sortRollupCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorRowLimitOnly.sortRollupCodegen(method);
    }

    public void getSortKeyCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(constantNull());
    }

    public void getSortKeyRollupCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(constantNull());
    }

    public void sortWOrderKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorRowLimitOnly.sortWOrderKeysCodegen(method);
    }

    public void sortTwoKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByProcessorRowLimitOnly.sortTwoKeysCodegen(method);
    }
}
