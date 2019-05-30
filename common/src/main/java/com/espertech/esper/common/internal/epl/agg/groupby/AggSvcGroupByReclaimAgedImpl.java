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
package com.espertech.esper.common.internal.epl.agg.groupby;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.core.AggSvcGroupByReclaimAgedEvalFunc;
import com.espertech.esper.common.internal.epl.agg.core.AggregationClassNames;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRowRemovedCallback;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacusField;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.common.internal.epl.agg.groupby.AggregationServiceGroupByForge.MEMBER_AGGREGATORSPERGROUP;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGENTINSTANCECONTEXT;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByReclaimAgedImpl {

    private final static CodegenExpressionRef REF_NEXTSWEEPTIME = ref("nextSweepTime");
    private final static CodegenExpressionRef REF_REMOVEDCALLBACK = ref("removedCallback");
    private final static CodegenExpressionRef REF_CURRENTMAXAGE = ref("currentMaxAge");
    private final static CodegenExpressionRef REF_CURRENTRECLAIMFREQUENCY = ref("currentReclaimFrequency");
    private final static CodegenExpressionRef REF_EVALUATORFUNCTIONMAXAGE = ref("evaluationFunctionMaxAge");
    private final static CodegenExpressionRef REF_EVALUATIONFUNCTIONFREQUENCY = ref("evaluationFunctionFrequency");

    public static final long DEFAULT_MAX_AGE_MSEC = 60000L;

    public static void rowCtorCodegen(CodegenNamedMethods namedMethods, CodegenClassScope classScope, List<CodegenTypedParam> rowMembers) {
        rowMembers.add(new CodegenTypedParam(long.class, "lastUpdateTime"));
        namedMethods.addMethod(void.class, "setLastUpdateTime", CodegenNamedParam.from(long.class, "time"), AggSvcGroupByReclaimAgedImpl.class, classScope, method -> method.getBlock().assignRef("lastUpdateTime", ref("time")));
        namedMethods.addMethod(long.class, "getLastUpdateTime", Collections.emptyList(), AggSvcGroupByReclaimAgedImpl.class, classScope, method -> method.getBlock().methodReturn(ref("lastUpdateTime")));
    }

    public static void ctorCodegenReclaim(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope, CodegenExpression maxAgeFactory, CodegenExpression frequencyFactory) {
        explicitMembers.add(new CodegenTypedParam(Long.class, REF_NEXTSWEEPTIME.getRef()));
        explicitMembers.add(new CodegenTypedParam(AggregationRowRemovedCallback.class, REF_REMOVEDCALLBACK.getRef()));
        explicitMembers.add(new CodegenTypedParam(long.class, REF_CURRENTMAXAGE.getRef()));
        explicitMembers.add(new CodegenTypedParam(long.class, REF_CURRENTRECLAIMFREQUENCY.getRef()));
        explicitMembers.add(new CodegenTypedParam(AggSvcGroupByReclaimAgedEvalFunc.class, REF_EVALUATORFUNCTIONMAXAGE.getRef()));
        explicitMembers.add(new CodegenTypedParam(AggSvcGroupByReclaimAgedEvalFunc.class, REF_EVALUATIONFUNCTIONFREQUENCY.getRef()));
        ctor.getBlock().assignRef(REF_CURRENTMAXAGE, constant(DEFAULT_MAX_AGE_MSEC))
                .assignRef(REF_CURRENTRECLAIMFREQUENCY, constant(DEFAULT_MAX_AGE_MSEC))
                .assignRef(REF_EVALUATORFUNCTIONMAXAGE, exprDotMethod(maxAgeFactory, "make", MEMBER_AGENTINSTANCECONTEXT))
                .assignRef(REF_EVALUATIONFUNCTIONFREQUENCY, exprDotMethod(frequencyFactory, "make", MEMBER_AGENTINSTANCECONTEXT));
    }

    public static void applyEnterCodegenSweep(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        CodegenExpressionField timeAbacus = classScope.addOrGetFieldSharable(TimeAbacusField.INSTANCE);
        method.getBlock().declareVar(long.class, "currentTime", exprDotMethodChain(REF_EXPREVALCONTEXT).add("getTimeProvider").add("getTime"))
                .ifCondition(or(equalsNull(REF_NEXTSWEEPTIME), relational(REF_NEXTSWEEPTIME, LE, ref("currentTime"))))
                .assignRef(REF_CURRENTMAXAGE, staticMethod(AggSvcGroupByReclaimAgedImpl.class, "computeTimeReclaimAgeFreq", REF_CURRENTMAXAGE, REF_EVALUATORFUNCTIONMAXAGE, timeAbacus))
                .assignRef(REF_CURRENTRECLAIMFREQUENCY, staticMethod(AggSvcGroupByReclaimAgedImpl.class, "computeTimeReclaimAgeFreq", REF_CURRENTRECLAIMFREQUENCY, REF_EVALUATIONFUNCTIONFREQUENCY, timeAbacus))
                .assignRef(REF_NEXTSWEEPTIME, op(ref("currentTime"), "+", REF_CURRENTRECLAIMFREQUENCY))
                .localMethod(sweepCodegen(method, classScope, classNames), ref("currentTime"), REF_CURRENTMAXAGE);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param current    current
     * @param func       func
     * @param timeAbacus abacus
     * @return delta
     */
    public static long computeTimeReclaimAgeFreq(long current, AggSvcGroupByReclaimAgedEvalFunc func, TimeAbacus timeAbacus) {
        Double maxAge = func.getLongValue();
        if ((maxAge == null) || (maxAge <= 0)) {
            return current;
        }
        return timeAbacus.deltaForSecondsDouble(maxAge);
    }

    private static CodegenMethod sweepCodegen(CodegenMethodScope parent, CodegenClassScope classScope, AggregationClassNames classNames) {
        CodegenMethod method = parent.makeChild(void.class, AggSvcGroupByReclaimAgedImpl.class, classScope).addParam(long.class, "currentTime").addParam(long.class, REF_CURRENTMAXAGE.getRef());
        method.getBlock().declareVar(ArrayDeque.class, "removed", newInstance(ArrayDeque.class))
                .forEach(Map.Entry.class, "entry", exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "entrySet"))
                .declareVar(long.class, "age", op(ref("currentTime"), "-", exprDotMethod(cast(classNames.getRowTop(), exprDotMethod(ref("entry"), "getValue")), "getLastUpdateTime")))
                .ifCondition(relational(ref("age"), GT, REF_CURRENTMAXAGE))
                .exprDotMethod(ref("removed"), "add", exprDotMethod(ref("entry"), "getKey"))
                .blockEnd()
                .blockEnd()
                .forEach(Object.class, "key", ref("removed"))
                .exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "remove", ref("key"))
                .exprDotMethod(REF_REMOVEDCALLBACK, "removedAggregationGroupKey", ref("key"));
        return method;
    }
}
