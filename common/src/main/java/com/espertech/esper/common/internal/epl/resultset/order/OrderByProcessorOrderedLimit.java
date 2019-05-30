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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_ISNEWDATA;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGGREGATIONSVC;
import static com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorOrderedLimitForge.REF_ROWLIMITPROCESSOR;

/**
 * Sorter and row limiter in one: sorts using a sorter and row limits
 */
public class OrderByProcessorOrderedLimit {
    static void sortPlainCodegenCodegen(OrderByProcessorOrderedLimitForge forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpression limit1 = equalsIdentity(exprDotMethod(REF_ROWLIMITPROCESSOR, "getCurrentRowLimit"), constant(1));
        CodegenExpression offset0 = equalsIdentity(exprDotMethod(REF_ROWLIMITPROCESSOR, "getCurrentOffset"), constant(0));
        CodegenExpression haveOutgoing = and(notEqualsNull(REF_OUTGOINGEVENTS), relational(arrayLength(REF_OUTGOINGEVENTS), GT, constant(1)));
        CodegenMethod determineLocalMinMax = OrderByProcessorImpl.determineLocalMinMaxCodegen(forge.getOrderByProcessorForge(), classScope, namedMethods);

        CodegenMethod sortPlain = method.makeChild(EventBean[].class, OrderByProcessorOrderedLimit.class, classScope).addParam(SORTPLAIN_PARAMS);
        OrderByProcessorImpl.sortPlainCodegen(forge.getOrderByProcessorForge(), sortPlain, classScope, namedMethods);

        method.getBlock().exprDotMethod(REF_ROWLIMITPROCESSOR, "determineCurrentLimit")
                .ifCondition(and(limit1, offset0, haveOutgoing))
                .declareVar(EventBean.class, "minmax", localMethod(determineLocalMinMax, REF_OUTGOINGEVENTS, REF_GENERATINGEVENTS, REF_ISNEWDATA, REF_EXPREVALCONTEXT, MEMBER_AGGREGATIONSVC))
                .blockReturn(newArrayWithInit(EventBean.class, ref("minmax")))
                .declareVar(EventBean[].class, "sorted", localMethod(sortPlain, REF_OUTGOINGEVENTS, REF_GENERATINGEVENTS, REF_ISNEWDATA, REF_EXPREVALCONTEXT, MEMBER_AGGREGATIONSVC))
                .methodReturn(exprDotMethod(REF_ROWLIMITPROCESSOR, "applyLimit", ref("sorted")));
    }

    public static void sortRollupCodegen(OrderByProcessorOrderedLimitForge forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod sortRollup = method.makeChild(EventBean[].class, OrderByProcessorOrderedLimit.class, classScope).addParam(SORTROLLUP_PARAMS);
        OrderByProcessorImpl.sortRollupCodegen(forge.getOrderByProcessorForge(), sortRollup, classScope, namedMethods);
        method.getBlock().declareVar(EventBean[].class, "sorted", localMethod(sortRollup, REF_OUTGOINGEVENTS, REF_ORDERCURRENTGENERATORS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT, MEMBER_AGGREGATIONSVC))
                .methodReturn(exprDotMethod(REF_ROWLIMITPROCESSOR, "determineLimitAndApply", ref("sorted")));
    }

    static void sortWGroupKeysCodegen(OrderByProcessorOrderedLimitForge forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod sortWGroupKeys = method.makeChild(EventBean[].class, OrderByProcessorOrderedLimit.class, classScope).addParam(SORTWGROUPKEYS_PARAMS);
        OrderByProcessorImpl.sortWGroupKeysCodegen(forge.getOrderByProcessorForge(), sortWGroupKeys, classScope, namedMethods);

        method.getBlock().declareVar(EventBean[].class, "sorted", localMethod(sortWGroupKeys, REF_OUTGOINGEVENTS, REF_GENERATINGEVENTS, REF_ORDERGROUPBYKEYS, REF_ISNEWDATA, REF_EXPREVALCONTEXT, MEMBER_AGGREGATIONSVC))
                .methodReturn(exprDotMethod(REF_ROWLIMITPROCESSOR, "determineLimitAndApply", ref("sorted")));
    }

    static void sortTwoKeysCodegen(OrderByProcessorOrderedLimitForge forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod sortTwoKeys = method.makeChild(EventBean[].class, OrderByProcessorOrderedLimit.class, classScope).addParam(SORTTWOKEYS_PARAMS);
        OrderByProcessorImpl.sortTwoKeysCodegen(forge.getOrderByProcessorForge(), sortTwoKeys, classScope, namedMethods);

        method.getBlock().declareVar(EventBean[].class, "sorted", localMethod(sortTwoKeys, REF_ORDERFIRSTEVENT, REF_ORDERFIRSTSORTKEY, REF_ORDERSECONDEVENT, REF_ORDERSECONDSORTKEY))
                .methodReturn(exprDotMethod(REF_ROWLIMITPROCESSOR, "determineLimitAndApply", ref("sorted")));
    }

    static void sortWOrderKeysCodegen(OrderByProcessorOrderedLimitForge forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpression comparator = classScope.addOrGetFieldSharable(forge.getOrderByProcessorForge().getComparator());
        method.getBlock().methodReturn(staticMethod(OrderByProcessorUtil.class, "sortWOrderKeysWLimit", REF_OUTGOINGEVENTS, REF_ORDERKEYS, comparator, REF_ROWLIMITPROCESSOR));
    }
}
