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
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByReclaimAgedImpl extends AggregationServiceBaseGrouped
{
    private static final Logger log = LoggerFactory.getLogger(AggSvcGroupByReclaimAgedImpl.class);

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

    public AggSvcGroupByReclaimAgedImpl(ExprEvaluator evaluators[], AggregationMethodFactory aggregators[], AggregationAccessorSlotPair[] accessors, AggregationStateFactory[] accessAggregations, boolean join, AggSvcGroupByReclaimAgedEvalFunc evaluationFunctionMaxAge, AggSvcGroupByReclaimAgedEvalFunc evaluationFunctionFrequency, TimeAbacus timeAbacus) {
        super(evaluators, aggregators);
        this.accessors = accessors;
        this.accessAggregations = accessAggregations;
        isJoin = join;
        this.evaluationFunctionMaxAge = evaluationFunctionMaxAge;
        this.evaluationFunctionFrequency = evaluationFunctionFrequency;
        this.aggregatorsPerGroup = new HashMap<Object, AggregationMethodRowAged>();
        this.timeAbacus = timeAbacus;
        removedKeys = new ArrayList<Object>();
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext)
    {
        aggregatorsPerGroup.clear();
    }

    public void applyEnter(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, aggregators.length, accessAggregations.length, groupByKey);}
        long currentTime = exprEvaluatorContext.getTimeProvider().getTime();
        if ((nextSweepTime == null) || (nextSweepTime <= currentTime))
        {
            currentMaxAge = getMaxAge(currentMaxAge);
            currentReclaimFrequency = getReclaimFrequency(currentReclaimFrequency);
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()))
            {
                log.debug("Reclaiming groups older then " + currentMaxAge + " msec and every " + currentReclaimFrequency + "msec in frequency");
            }
            nextSweepTime = currentTime + currentReclaimFrequency;
            sweep(currentTime, currentMaxAge);
        }

        handleRemovedKeys(); // we collect removed keys lazily on the next enter to reduce the chance of empty-group queries creating empty aggregators temporarily

        AggregationMethodRowAged row = aggregatorsPerGroup.get(groupByKey);

        // The aggregators for this group do not exist, need to create them from the prototypes
        AggregationMethod[] groupAggregators;
        AggregationState[] groupStates;
        if (row == null)
        {
            groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
            groupStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupByKey, null);
            row = new AggregationMethodRowAged(1, currentTime, groupAggregators, groupStates);
            aggregatorsPerGroup.put(groupByKey, row);
        }
        else
        {
            groupAggregators = row.getMethods();
            groupStates = row.getStates();
            row.increaseRefcount();
            row.setLastUpdateTime(currentTime);
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorMethods = groupAggregators;
        currentAggregatorStates = groupStates;
        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggNoAccessEnterLeave(true, i, currentAggregatorMethods[i], aggregators[i].getAggregationExpression());}
            Object columnResult = evaluators[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
            groupAggregators[i].enter(columnResult);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggNoAccessEnterLeave(true, i, currentAggregatorMethods[i]);}
        }

        for (int i = 0; i < currentAggregatorStates.length; i++) {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggAccessEnterLeave(true, i, currentAggregatorStates[i], accessAggregations[i].getAggregationExpression());}
            currentAggregatorStates[i].applyEnter(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggAccessEnterLeave(true, i, currentAggregatorStates[i]);}
        }

        internalHandleUpdated(groupByKey, row);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);}
    }

    private void sweep(long currentTime, long currentMaxAge)
    {
        ArrayDeque<Object> removed = new ArrayDeque<Object>();
        for (Map.Entry<Object, AggregationMethodRowAged> entry : aggregatorsPerGroup.entrySet())
        {
            long age = currentTime - entry.getValue().getLastUpdateTime();
            if (age > currentMaxAge)
            {
                removed.add(entry.getKey());
            }
        }

        for (Object key : removed)
        {
            aggregatorsPerGroup.remove(key);
            internalHandleRemoved(key);
            removedCallback.removed(key);
        }
    }

    private long getMaxAge(long currentMaxAge)
    {
        Double maxAge = evaluationFunctionMaxAge.getLongValue();
        if ((maxAge == null) || (maxAge <= 0))
        {
            return currentMaxAge;
        }
        return timeAbacus.deltaForSecondsDouble(maxAge);
    }

    private long getReclaimFrequency(long currentReclaimFrequency)
    {
        Double frequency = evaluationFunctionFrequency.getLongValue();
        if ((frequency == null) || (frequency <= 0))
        {
            return currentReclaimFrequency;
        }
        return timeAbacus.deltaForSecondsDouble(frequency);
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, aggregators.length, accessAggregations.length, groupByKey);}
        AggregationMethodRowAged row = aggregatorsPerGroup.get(groupByKey);
        long currentTime = exprEvaluatorContext.getTimeProvider().getTime();

        // The aggregators for this group do not exist, need to create them from the prototypes
        AggregationMethod[] groupAggregators;
        AggregationState[] groupStates;
        if (row != null)
        {
            groupAggregators = row.getMethods();
            groupStates = row.getStates();
        }
        else
        {
            groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
            groupStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupByKey, null);
            row = new AggregationMethodRowAged(1, currentTime, groupAggregators, groupStates);
            aggregatorsPerGroup.put(groupByKey, row);
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorMethods = groupAggregators;
        currentAggregatorStates = groupStates;
        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggNoAccessEnterLeave(false, i, currentAggregatorMethods[i], aggregators[i].getAggregationExpression());}
            Object columnResult = evaluators[i].evaluate(eventsPerStream, false, exprEvaluatorContext);
            groupAggregators[i].leave(columnResult);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggNoAccessEnterLeave(false, i, currentAggregatorMethods[i]);}
        }

        for (int i = 0; i < currentAggregatorStates.length; i++) {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggAccessEnterLeave(false, i, currentAggregatorStates[i], accessAggregations[i].getAggregationExpression());}
            currentAggregatorStates[i].applyLeave(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggAccessEnterLeave(false, i, currentAggregatorStates[i]);}
        }

        row.decreaseRefcount();
        row.setLastUpdateTime(currentTime);
        if (row.getRefcount() <= 0) {
            removedKeys.add(groupByKey);
        }
        internalHandleUpdated(groupByKey, row);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false); }
    }

    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel)
    {
        AggregationMethodRowAged row = aggregatorsPerGroup.get(groupByKey);

        if (row != null) {
            currentAggregatorMethods = row.getMethods();
            currentAggregatorStates = row.getStates();
        }
        else {
            currentAggregatorMethods = null;
        }

        if (currentAggregatorMethods == null) {
            currentAggregatorMethods = AggSvcGroupByUtil.newAggregators(aggregators);
            currentAggregatorStates = AggSvcGroupByUtil.newAccesses(agentInstanceId, isJoin, accessAggregations, groupByKey, null);
        }

        this.currentGroupKey = groupByKey;
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext)
    {
        if (column < aggregators.length) {
            return currentAggregatorMethods[column].getValue();
        }
        else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getValue(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        }
        else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvents(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        }
        else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableScalar(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        }
        else {
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
        if (!removedKeys.isEmpty())     // we collect removed keys lazily on the next enter to reduce the chance of empty-group queries creating empty aggregators temporarily
        {
            for (Object removedKey : removedKeys)
            {
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
}
