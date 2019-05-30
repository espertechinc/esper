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
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_ISNEWDATA;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGGREGATIONSVC;

public class OrderByProcessorCodegenNames {
    final static String CLASSNAME_ORDERBYPROCESSORFACTORY = "OrderProcFactory";
    final static String CLASSNAME_ORDERBYPROCESSOR = "OrderProc";

    final static CodegenExpressionRef REF_OUTGOINGEVENTS = ref("orderOutgoingEvents");
    final static CodegenExpressionRef REF_GENERATINGEVENTS = ref("orderGeneratingEvents");
    final static CodegenExpressionRef REF_ORDERGROUPBYKEYS = ref("orderGroupByKeys");
    final static CodegenExpressionRef REF_ORDERCURRENTGENERATORS = ref("orderCurrentGenerators");
    final static CodegenExpressionRef REF_ORDERROLLUPLEVEL = ref("orderlevel");
    final static CodegenExpressionRef REF_ORDERKEYS = ref("orderKeys");

    final static CodegenExpressionRef REF_ORDERFIRSTEVENT = ref("first");
    final static CodegenExpressionRef REF_ORDERFIRSTSORTKEY = ref("firstSortKey");
    final static CodegenExpressionRef REF_ORDERSECONDEVENT = ref("second");
    final static CodegenExpressionRef REF_ORDERSECONDSORTKEY = ref("secondSortKey");

    final static List<CodegenNamedParam> SORTPLAIN_PARAMS = CodegenNamedParam.from(EventBean[].class, REF_OUTGOINGEVENTS.getRef(),
            EventBean[][].class, REF_GENERATINGEVENTS.getRef(),
            boolean.class, REF_ISNEWDATA.getRef(),
            ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef(),
            AggregationService.class, MEMBER_AGGREGATIONSVC.getRef());

    final static List<CodegenNamedParam> SORTWGROUPKEYS_PARAMS = CodegenNamedParam.from(
            EventBean[].class, REF_OUTGOINGEVENTS.getRef(),
            EventBean[][].class, REF_GENERATINGEVENTS.getRef(),
            Object[].class, REF_ORDERGROUPBYKEYS.getRef(),
            boolean.class, REF_ISNEWDATA.getRef(),
            ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef(),
            AggregationService.class, MEMBER_AGGREGATIONSVC.getRef());

    final static List<CodegenNamedParam> SORTROLLUP_PARAMS = CodegenNamedParam.from(
            EventBean[].class, REF_OUTGOINGEVENTS.getRef(),
            List.class, REF_ORDERCURRENTGENERATORS.getRef(),
            boolean.class, REF_ISNEWDATA.getRef(),
            AgentInstanceContext.class, MEMBER_AGENTINSTANCECONTEXT.getRef(),
            AggregationService.class, MEMBER_AGGREGATIONSVC.getRef());

    final static List<CodegenNamedParam> SORTTWOKEYS_PARAMS = CodegenNamedParam.from(
            EventBean.class, REF_ORDERFIRSTEVENT.getRef(),
            Object.class, REF_ORDERFIRSTSORTKEY.getRef(),
            EventBean.class, REF_ORDERSECONDEVENT.getRef(),
            Object.class, REF_ORDERSECONDSORTKEY.getRef());
}
