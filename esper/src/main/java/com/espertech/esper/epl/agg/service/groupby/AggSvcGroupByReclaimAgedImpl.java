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
package com.espertech.esper.epl.agg.service.groupby;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.codegen.core.CodegenTypedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP;
import static com.espertech.esper.epl.agg.service.groupby.AggSvcGroupByForge.REF_AGGREGATORSPERGROUP;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.REF_AGENTINSTANCECONTEXT;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByReclaimAgedImpl extends AggregationServiceBaseGrouped {
    private static final Logger log = LoggerFactory.getLogger(AggSvcGroupByReclaimAgedImpl.class);

    private final static CodegenExpressionRef REF_NEXTSWEEPTIME = ref("nextSweepTime");
    private final static CodegenExpressionRef REF_REMOVEDCALLBACK = ref("removedCallback");
    private final static CodegenExpressionRef REF_CURRENTMAXAGE = ref("currentMaxAge");
    private final static CodegenExpressionRef REF_CURRENTRECLAIMFREQUENCY = ref("currentReclaimFrequency");
    private final static CodegenExpressionRef REF_EVALUATORFUNCTIONMAXAGE = ref("evaluationFunctionMaxAge");
    private final static CodegenExpressionRef REF_EVALUATIONFUNCTIONFREQUENCY = ref("evaluationFunctionFrequency");

    public static final long DEFAULT_MAX_AGE_MSEC = 60000L;

    private final AggregationAccessorSlotPair[] accessors;
    protected final AggregationStateFactory[] accessAggregations;
    protected final boolean isJoin;
    private final TimeAbacus timeAbacus;

    private final AggSvcGroupByReclaimAgedEvalFunc evaluationFunctionMaxAge;
    private final AggSvcGroupByReclaimAgedEvalFunc evaluationFunctionFrequency;

    // maintain for each group a row of aggregator states that the expression node canb pull the data from via index
    protected Map<Object, AggregationMethodRowAged> aggregatorsPerGroup;

    // maintain a current row for random access into the aggregator state table
    // (row=groups, columns=expression nodes that have aggregation functions)
    private AggregationMethod[] currentAggregatorMethods;
    private AggregationState[] currentAggregatorStates;
    private Object currentGroupKey;

    private List<Object> removedKeys;
    private Long nextSweepTime = null;
    private AggregationRowRemovedCallback removedCallback;
    private volatile long currentMaxAge = DEFAULT_MAX_AGE_MSEC;
    private volatile long currentReclaimFrequency = DEFAULT_MAX_AGE_MSEC;

    public AggSvcGroupByReclaimAgedImpl(ExprEvaluator[] evaluators, AggregationMethodFactory[] aggregators, AggregationAccessorSlotPair[] accessors, AggregationStateFactory[] accessAggregations, boolean join, AggSvcGroupByReclaimAgedEvalFunc evaluationFunctionMaxAge, AggSvcGroupByReclaimAgedEvalFunc evaluationFunctionFrequency, TimeAbacus timeAbacus) {
        super(evaluators, aggregators);
        this.accessors = accessors;
        this.accessAggregations = accessAggregations;
        isJoin = join;
        this.evaluationFunctionMaxAge = evaluationFunctionMaxAge;
        this.evaluationFunctionFrequency = evaluationFunctionFrequency;
        this.aggregatorsPerGroup = new HashMap<>();
        this.timeAbacus = timeAbacus;
        removedKeys = new ArrayList<>();
    }

    public static void rowCtorCodegen(CodegenNamedMethods namedMethods, CodegenClassScope classScope, List<CodegenTypedParam> rowMembers) {
        rowMembers.add(new CodegenTypedParam(long.class, "lastUpdateTime"));
        namedMethods.addMethod(void.class, "setLastUpdateTime", CodegenNamedParam.from(long.class, "time"), AggSvcGroupByReclaimAgedImpl.class, classScope, method -> method.getBlock().assignRef("lastUpdateTime", ref("time")));
        namedMethods.addMethod(long.class, "getLastUpdateTime", Collections.emptyList(), AggSvcGroupByReclaimAgedImpl.class, classScope, method -> method.getBlock().methodReturn(ref("lastUpdateTime")));
    }

    public static void ctorCodegenReclaim(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope, AggSvcGroupByReclaimAgedEvalFuncFactory maxAgeFactory, AggSvcGroupByReclaimAgedEvalFuncFactory frequencyFactory) {
        CodegenMember memberMaxAgeFactory = classScope.makeAddMember(AggSvcGroupByReclaimAgedEvalFuncFactory.class, maxAgeFactory);
        CodegenMember memberFrequencyFactory = classScope.makeAddMember(AggSvcGroupByReclaimAgedEvalFuncFactory.class, frequencyFactory);
        explicitMembers.add(new CodegenTypedParam(Long.class, REF_NEXTSWEEPTIME.getRef()));
        explicitMembers.add(new CodegenTypedParam(AggregationRowRemovedCallback.class, REF_REMOVEDCALLBACK.getRef()));
        explicitMembers.add(new CodegenTypedParam(long.class, REF_CURRENTMAXAGE.getRef()));
        explicitMembers.add(new CodegenTypedParam(long.class, REF_CURRENTRECLAIMFREQUENCY.getRef()));
        explicitMembers.add(new CodegenTypedParam(AggSvcGroupByReclaimAgedEvalFunc.class, REF_EVALUATORFUNCTIONMAXAGE.getRef()));
        explicitMembers.add(new CodegenTypedParam(AggSvcGroupByReclaimAgedEvalFunc.class, REF_EVALUATIONFUNCTIONFREQUENCY.getRef()));
        ctor.getBlock().assignRef(REF_CURRENTMAXAGE, constant(DEFAULT_MAX_AGE_MSEC))
                .assignRef(REF_CURRENTRECLAIMFREQUENCY, constant(DEFAULT_MAX_AGE_MSEC))
                .assignRef(REF_EVALUATORFUNCTIONMAXAGE, exprDotMethod(member(memberMaxAgeFactory.getMemberId()), "make", REF_AGENTINSTANCECONTEXT))
                .assignRef(REF_EVALUATIONFUNCTIONFREQUENCY, exprDotMethod(member(memberFrequencyFactory.getMemberId()), "make", REF_AGENTINSTANCECONTEXT));
    }

    public void applyEnter(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, aggregators.length, accessAggregations.length, groupByKey);
        }
        long currentTime = exprEvaluatorContext.getTimeProvider().getTime();
        if ((nextSweepTime == null) || (nextSweepTime <= currentTime)) {
            currentMaxAge = computeTimeReclaimAgeFreq(currentMaxAge, evaluationFunctionMaxAge, timeAbacus);
            currentReclaimFrequency = computeTimeReclaimAgeFreq(currentReclaimFrequency, evaluationFunctionFrequency, timeAbacus);
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Reclaiming groups older then " + currentMaxAge + " msec and every " + currentReclaimFrequency + "msec in frequency");
            }
            nextSweepTime = currentTime + currentReclaimFrequency;
            sweep(currentTime, currentMaxAge);
        }

        handleRemovedKeys(); // we collect removed keys lazily on the next enter to reduce the chance of empty-group queries creating empty methodFactories temporarily

        AggregationMethodRowAged row = aggregatorsPerGroup.get(groupByKey);

        // The methodFactories for this group do not exist, need to create them from the prototypes
        AggregationMethod[] groupAggregators;
        AggregationState[] groupStates;
        if (row == null) {
            groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
            groupStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupByKey, null);
            row = new AggregationMethodRowAged(1, currentTime, groupAggregators, groupStates);
            aggregatorsPerGroup.put(groupByKey, row);
        } else {
            groupAggregators = row.getMethods();
            groupStates = row.getStates();
            row.increaseRefcount();
            row.setLastUpdateTime(currentTime);
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorMethods = groupAggregators;
        currentAggregatorStates = groupStates;
        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(true, i, currentAggregatorMethods[i], aggregators[i].getAggregationExpression());
            }
            Object columnResult = evaluators[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
            groupAggregators[i].enter(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(true, i, currentAggregatorMethods[i]);
            }
        }

        for (int i = 0; i < currentAggregatorStates.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(true, i, currentAggregatorStates[i], accessAggregations[i].getAggregationExpression());
            }
            currentAggregatorStates[i].applyEnter(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(true, i, currentAggregatorStates[i]);
            }
        }

        internalHandleUpdated(groupByKey, row);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);
        }
    }

    public static void applyEnterCodegenSweep(CodegenMethodNode method, CodegenClassScope classScope, TimeAbacus timeAbacus) {
        CodegenMember timeAbacusMember = classScope.makeAddMember(TimeAbacus.class, timeAbacus);
        method.getBlock().declareVar(long.class, "currentTime", exprDotMethodChain(REF_EXPREVALCONTEXT).add("getTimeProvider").add("getTime"))
                .ifCondition(or(equalsNull(REF_NEXTSWEEPTIME), relational(REF_NEXTSWEEPTIME, LE, ref("currentTime"))))
                    .assignRef(REF_CURRENTMAXAGE, staticMethod(AggSvcGroupByReclaimAgedImpl.class, "computeTimeReclaimAgeFreq", REF_CURRENTMAXAGE, REF_EVALUATORFUNCTIONMAXAGE, member(timeAbacusMember.getMemberId())))
                    .assignRef(REF_CURRENTRECLAIMFREQUENCY, staticMethod(AggSvcGroupByReclaimAgedImpl.class, "computeTimeReclaimAgeFreq", REF_CURRENTRECLAIMFREQUENCY, REF_EVALUATIONFUNCTIONFREQUENCY, member(timeAbacusMember.getMemberId())))
                    .assignRef(REF_NEXTSWEEPTIME, op(ref("currentTime"), "+", REF_CURRENTRECLAIMFREQUENCY))
                    .localMethod(sweepCodegen(method, classScope), ref("currentTime"), REF_CURRENTMAXAGE);
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, aggregators.length, accessAggregations.length, groupByKey);
        }
        AggregationMethodRowAged row = aggregatorsPerGroup.get(groupByKey);
        long currentTime = exprEvaluatorContext.getTimeProvider().getTime();

        // The methodFactories for this group do not exist, need to create them from the prototypes
        AggregationMethod[] groupAggregators;
        AggregationState[] groupStates;
        if (row != null) {
            groupAggregators = row.getMethods();
            groupStates = row.getStates();
        } else {
            groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
            groupStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupByKey, null);
            row = new AggregationMethodRowAged(1, currentTime, groupAggregators, groupStates);
            aggregatorsPerGroup.put(groupByKey, row);
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorMethods = groupAggregators;
        currentAggregatorStates = groupStates;
        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(false, i, currentAggregatorMethods[i], aggregators[i].getAggregationExpression());
            }
            Object columnResult = evaluators[i].evaluate(eventsPerStream, false, exprEvaluatorContext);
            groupAggregators[i].leave(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(false, i, currentAggregatorMethods[i]);
            }
        }

        for (int i = 0; i < currentAggregatorStates.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(false, i, currentAggregatorStates[i], accessAggregations[i].getAggregationExpression());
            }
            currentAggregatorStates[i].applyLeave(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(false, i, currentAggregatorStates[i]);
            }
        }

        row.decreaseRefcount();
        row.setLastUpdateTime(currentTime);
        if (row.getRefcount() <= 0) {
            removedKeys.add(groupByKey);
        }
        internalHandleUpdated(groupByKey, row);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false);
        }
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        aggregatorsPerGroup.clear();
    }

    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        AggregationMethodRowAged row = aggregatorsPerGroup.get(groupByKey);

        if (row != null) {
            currentAggregatorMethods = row.getMethods();
            currentAggregatorStates = row.getStates();
        } else {
            currentAggregatorMethods = null;
        }

        if (currentAggregatorMethods == null) {
            currentAggregatorMethods = AggSvcGroupByUtil.newAggregators(aggregators);
            currentAggregatorStates = AggSvcGroupByUtil.newAccesses(agentInstanceId, isJoin, accessAggregations, groupByKey, null);
        }

        this.currentGroupKey = groupByKey;
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (column < aggregators.length) {
            return currentAggregatorMethods[column].getValue();
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getValue(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvents(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableScalar(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvent(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        this.removedCallback = callback;
    }

    public void internalHandleUpdated(Object groupByKey, AggregationMethodRowAged row) {
        // no action required
    }

    public void internalHandleRemoved(Object key) {
        // no action required
    }

    public void accept(AggregationServiceVisitor visitor) {
        visitor.visitAggregations(aggregatorsPerGroup.size(), aggregatorsPerGroup);
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        visitor.visitGrouped(aggregatorsPerGroup.size());
        for (Map.Entry<Object, AggregationMethodRowAged> entry : aggregatorsPerGroup.entrySet()) {
            visitor.visitGroup(entry.getKey(), entry.getValue());
        }
    }

    public boolean isGrouped() {
        return true;
    }

    protected void handleRemovedKeys() {
        if (!removedKeys.isEmpty()) {
            // we collect removed keys lazily on the next enter to reduce the chance of empty-group queries creating empty methodFactories temporarily
            for (Object removedKey : removedKeys) {
                aggregatorsPerGroup.remove(removedKey);
                internalHandleRemoved(removedKey);
            }
            removedKeys.clear();
        }
    }

    public Object getGroupKey(int agentInstanceId) {
        return currentGroupKey;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return aggregatorsPerGroup.keySet();
    }

    public static long computeTimeReclaimAgeFreq(long current, AggSvcGroupByReclaimAgedEvalFunc func, TimeAbacus timeAbacus) {
        Double maxAge = func.getLongValue();
        if ((maxAge == null) || (maxAge <= 0)) {
            return current;
        }
        return timeAbacus.deltaForSecondsDouble(maxAge);
    }

    private void sweep(long currentTime, long currentMaxAge) {
        ArrayDeque<Object> removed = new ArrayDeque<Object>();
        for (Map.Entry<Object, AggregationMethodRowAged> entry : aggregatorsPerGroup.entrySet()) {
            long age = currentTime - entry.getValue().getLastUpdateTime();
            if (age > currentMaxAge) {
                removed.add(entry.getKey());
            }
        }

        for (Object key : removed) {
            aggregatorsPerGroup.remove(key);
            internalHandleRemoved(key);
            removedCallback.removedAggregationGroupKey(key);
        }
    }

    private static CodegenMethodNode sweepCodegen(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethodNode method = parent.makeChild(void.class, AggSvcGroupByReclaimAgedImpl.class, classScope).addParam(long.class, "currentTime").addParam(long.class, REF_CURRENTMAXAGE.getRef());
        method.getBlock().declareVar(ArrayDeque.class, "removed", newInstance(ArrayDeque.class))
                .forEach(Map.Entry.class, "entry", exprDotMethod(REF_AGGREGATORSPERGROUP, "entrySet"))
                    .declareVar(long.class, "age", op(ref("currentTime"), "-", exprDotMethod(cast(CLASSNAME_AGGREGATIONROW_TOP, exprDotMethod(ref("entry"), "getValue")), "getLastUpdateTime")))
                    .ifCondition(relational(ref("age"), GT, REF_CURRENTMAXAGE))
                            .exprDotMethod(ref("removed"), "add", exprDotMethod(ref("entry"), "getKey"))
                    .blockEnd()
                .blockEnd()
                .forEach(Object.class, "key", ref("removed"))
                    .exprDotMethod(REF_AGGREGATORSPERGROUP, "remove", ref("key"))
                    .exprDotMethod(REF_REMOVEDCALLBACK, "removedAggregationGroupKey", ref("key"));
        return method;
    }
}
