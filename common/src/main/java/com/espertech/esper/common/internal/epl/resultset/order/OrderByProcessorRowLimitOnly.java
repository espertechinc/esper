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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorOrderedLimitForge.REF_ROWLIMITPROCESSOR;

/**
 * An order-by processor that sorts events according to the expressions
 * in the order_by clause.
 */
public class OrderByProcessorRowLimitOnly {

    public static void sortPlainCodegen(CodegenMethod method) {
        determineLimitAndApplyCodegen(method);
    }

    static void sortWGroupKeysCodegen(CodegenMethod method) {
        determineLimitAndApplyCodegen(method);
    }

    static void sortRollupCodegen(CodegenMethod method) {
        determineLimitAndApplyCodegen(method);
    }

    static void sortTwoKeysCodegen(CodegenMethod method) {
        method.getBlock().methodReturn(exprDotMethod(REF_ROWLIMITPROCESSOR, "determineApplyLimit2Events", REF_ORDERFIRSTEVENT, REF_ORDERSECONDEVENT));
    }

    static void sortWOrderKeysCodegen(CodegenMethod method) {
        determineLimitAndApplyCodegen(method);
    }

    private static void determineLimitAndApplyCodegen(CodegenMethod method) {
        method.getBlock().methodReturn(exprDotMethod(REF_ROWLIMITPROCESSOR, "determineLimitAndApply", REF_OUTGOINGEVENTS));
    }
}
