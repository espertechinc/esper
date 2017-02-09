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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.rollup.GroupByRollupKey;
import com.espertech.esper.epl.agg.service.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.service.AggregationRowRemovedCallback;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.view.OutputConditionPolled;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.Viewable;

import java.util.*;

public class ResultSetProcessorRowPerGroupRollup implements ResultSetProcessor, AggregationRowRemovedCallback {

    protected final ResultSetProcessorRowPerGroupRollupFactory prototype;
    protected final OrderByProcessor orderByProcessor;
    protected final AggregationService aggregationService;
    protected AgentInstanceContext agentInstanceContext;

    private final Map<Object, EventBean[]>[] groupRepsPerLevelBuf;
    private final Map<Object, EventBean>[] eventPerGroupBuf;
    private final Map<Object, EventBean[]>[] eventPerGroupJoinBuf;
    private final EventArrayAndSortKeyArray rstreamEventSortArrayBuf;

    private final ResultSetProcessorRowPerGroupRollupOutputLastHelper outputLastHelper;
    private final ResultSetProcessorRowPerGroupRollupOutputAllHelper outputAllHelper;
    private final ResultSetProcessorGroupedOutputFirstHelper[] outputFirstHelpers;

    public ResultSetProcessorRowPerGroupRollup(ResultSetProcessorRowPerGroupRollupFactory prototype, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.agentInstanceContext = agentInstanceContext;
        aggregationService.setRemovedCallback(this);

        int levelCount = prototype.getGroupByRollupDesc().getLevels().length;

        if (prototype.isJoin()) {
            eventPerGroupJoinBuf = (LinkedHashMap<Object, EventBean[]>[]) new LinkedHashMap[levelCount];
            for (int i = 0; i < levelCount; i++) {
                eventPerGroupJoinBuf[i] = new LinkedHashMap<Object, EventBean[]>();
            }
            eventPerGroupBuf = null;
        } else {
            eventPerGroupBuf = (LinkedHashMap<Object, EventBean>[]) new LinkedHashMap[levelCount];
            for (int i = 0; i < levelCount; i++) {
                eventPerGroupBuf[i] = new LinkedHashMap<Object, EventBean>();
            }
            eventPerGroupJoinBuf = null;
        }

        if (prototype.getOutputLimitSpec() != null) {
            groupRepsPerLevelBuf = (LinkedHashMap<Object, EventBean[]>[]) new LinkedHashMap[levelCount];
            for (int i = 0; i < levelCount; i++) {
                groupRepsPerLevelBuf[i] = new LinkedHashMap<Object, EventBean[]>();
            }

            if (prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
                outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupRollupLast(agentInstanceContext, this, prototype);
                outputAllHelper = null;
            } else if (prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
                outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupRollupAll(agentInstanceContext, this, prototype);
                outputLastHelper = null;
            } else {
                outputLastHelper = null;
                outputAllHelper = null;
            }
        } else {
            groupRepsPerLevelBuf = null;
            outputLastHelper = null;
            outputAllHelper = null;
        }

        // Allocate output state for output-first
        if (prototype.getOutputLimitSpec() != null && prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.FIRST) {
            outputFirstHelpers = new ResultSetProcessorGroupedOutputFirstHelper[levelCount];
            for (int i = 0; i < levelCount; i++) {
                outputFirstHelpers[i] = prototype.getResultSetProcessorHelperFactory().makeRSGroupedOutputFirst(agentInstanceContext, prototype.getGroupKeyNodes(), prototype.getOptionalOutputFirstConditionFactory(), prototype.getGroupByRollupDesc(), i);
            }
        } else {
            outputFirstHelpers = null;
        }

        if (prototype.getOutputLimitSpec() != null && (prototype.isSelectRStream() || prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.FIRST)) {
            List<EventBean>[] eventsPerLevel = (List<EventBean>[]) new List[prototype.getGroupByRollupDesc().getLevels().length];
            List<Object>[] sortKeyPerLevel = null;
            if (orderByProcessor != null) {
                sortKeyPerLevel = (List<Object>[]) new List[prototype.getGroupByRollupDesc().getLevels().length];
            }
            for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                eventsPerLevel[level.getLevelNumber()] = new ArrayList<EventBean>();
                if (orderByProcessor != null) {
                    sortKeyPerLevel[level.getLevelNumber()] = new ArrayList<Object>();
                }
            }
            rstreamEventSortArrayBuf = new EventArrayAndSortKeyArray(eventsPerLevel, sortKeyPerLevel);
        } else {
            rstreamEventSortArrayBuf = null;
        }
    }

    public void setAgentInstanceContext(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public EventType getResultEventType() {
        return prototype.getResultEventType();
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerGroup();
        }

        if (prototype.isUnidirectional()) {
            this.clear();
        }

        resetEventPerGroupJoinBuf();
        Object[][] newDataMultiKey = generateGroupKeysJoin(newEvents, eventPerGroupJoinBuf, true);
        Object[][] oldDataMultiKey = generateGroupKeysJoin(oldEvents, eventPerGroupJoinBuf, false);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsJoin(eventPerGroupJoinBuf, false, isSynthesize);
        }

        // update aggregates
        if (newEvents != null) {
            int count = 0;
            for (MultiKey<EventBean> mk : newEvents) {
                aggregationService.applyEnter(mk.getArray(), newDataMultiKey[count++], agentInstanceContext);
            }
        }
        if (oldEvents != null) {
            int count = 0;
            for (MultiKey<EventBean> mk : oldEvents) {
                aggregationService.applyLeave(mk.getArray(), oldDataMultiKey[count++], agentInstanceContext);
            }
        }

        // generate new events using select expressions
        EventBean[] selectNewEvents = generateOutputEventsJoin(eventPerGroupJoinBuf, true, isSynthesize);

        if ((selectNewEvents != null) || (selectOldEvents != null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(selectNewEvents, selectOldEvents);
            }
            return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(null, null);
        }
        return null;
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerGroup();
        }

        resetEventPerGroupBuf();
        Object[][] newDataMultiKey = generateGroupKeysView(newData, eventPerGroupBuf, true);
        Object[][] oldDataMultiKey = generateGroupKeysView(oldData, eventPerGroupBuf, false);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsView(eventPerGroupBuf, false, isSynthesize);
        }

        // update aggregates
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                aggregationService.applyEnter(eventsPerStream, newDataMultiKey[i], agentInstanceContext);
            }
        }
        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                eventsPerStream[0] = oldData[i];
                aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[i], agentInstanceContext);
            }
        }

        // generate new events using select expressions
        EventBean[] selectNewEvents = generateOutputEventsView(eventPerGroupBuf, true, isSynthesize);

        if ((selectNewEvents != null) || (selectOldEvents != null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(selectNewEvents, selectOldEvents);
            }
            return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(null, null);
        }
        return null;
    }

    protected EventBean[] generateOutputEventsView(Map<Object, EventBean>[] keysAndEvents, boolean isNewData, boolean isSynthesize) {
        EventBean[] eventsPerStream = new EventBean[1];
        ArrayList<EventBean> events = new ArrayList<EventBean>(1);
        List<GroupByRollupKey> currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new ArrayList<GroupByRollupKey>(4);
        }

        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        SelectExprProcessor[] selectExprProcessors = prototype.getPerLevelExpression().getSelectExprProcessor();
        ExprEvaluator[] optionalHavingClauses = prototype.getPerLevelExpression().getOptionalHavingNodes();
        for (AggregationGroupByRollupLevel level : levels) {
            for (Map.Entry<Object, EventBean> entry : keysAndEvents[level.getLevelNumber()].entrySet()) {
                Object groupKey = entry.getKey();

                // Set the current row of aggregation states
                aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                eventsPerStream[0] = entry.getValue();

                // Filter the having clause
                if (optionalHavingClauses != null) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().qHavingClauseNonJoin(entry.getValue());
                    }
                    Boolean result = (Boolean) optionalHavingClauses[level.getLevelNumber()].evaluate(eventsPerStream, isNewData, agentInstanceContext);
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aHavingClauseNonJoin(result);
                    }
                    if ((result == null) || (!result)) {
                        continue;
                    }
                }
                events.add(selectExprProcessors[level.getLevelNumber()].process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));

                if (prototype.isSorting()) {
                    EventBean[] currentEventsPerStream = new EventBean[]{entry.getValue()};
                    currentGenerators.add(new GroupByRollupKey(currentEventsPerStream, level, groupKey));
                }
            }
        }

        if (events.isEmpty()) {
            return null;
        }
        EventBean[] outgoing = events.toArray(new EventBean[events.size()]);
        if (outgoing.length > 1 && prototype.isSorting()) {
            return orderByProcessor.sort(outgoing, currentGenerators, isNewData, agentInstanceContext, prototype.getPerLevelExpression().getOptionalOrderByElements());
        }
        return outgoing;
    }

    private EventBean[] generateOutputEventsJoin(Map<Object, EventBean[]>[] eventPairs, boolean isNewData, boolean synthesize) {
        ArrayList<EventBean> events = new ArrayList<EventBean>(1);
        List<GroupByRollupKey> currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new ArrayList<GroupByRollupKey>(4);
        }

        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        SelectExprProcessor[] selectExprProcessors = prototype.getPerLevelExpression().getSelectExprProcessor();
        ExprEvaluator[] optionalHavingClauses = prototype.getPerLevelExpression().getOptionalHavingNodes();
        for (AggregationGroupByRollupLevel level : levels) {
            for (Map.Entry<Object, EventBean[]> entry : eventPairs[level.getLevelNumber()].entrySet()) {
                Object groupKey = entry.getKey();

                // Set the current row of aggregation states
                aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);

                // Filter the having clause
                if (optionalHavingClauses != null) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().qHavingClauseJoin(entry.getValue());
                    }
                    Boolean result = (Boolean) optionalHavingClauses[level.getLevelNumber()].evaluate(entry.getValue(), isNewData, agentInstanceContext);
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aHavingClauseJoin(result);
                    }
                    if ((result == null) || (!result)) {
                        continue;
                    }
                }
                events.add(selectExprProcessors[level.getLevelNumber()].process(entry.getValue(), isNewData, synthesize, agentInstanceContext));

                if (prototype.isSorting()) {
                    currentGenerators.add(new GroupByRollupKey(entry.getValue(), level, groupKey));
                }
            }
        }

        if (events.isEmpty()) {
            return null;
        }
        EventBean[] outgoing = events.toArray(new EventBean[events.size()]);
        if (outgoing.length > 1 && prototype.isSorting()) {
            return orderByProcessor.sort(outgoing, currentGenerators, isNewData, agentInstanceContext, prototype.getPerLevelExpression().getOptionalOrderByElements());
        }
        return outgoing;
    }

    public Iterator<EventBean> getIterator(Viewable parent) {
        if (!prototype.isHistoricalOnly()) {
            return obtainIterator(parent);
        }

        aggregationService.clearResults(agentInstanceContext);
        Iterator<EventBean> it = parent.iterator();
        EventBean[] eventsPerStream = new EventBean[1];
        Object[] groupKeys = new Object[prototype.getGroupByRollupDesc().getLevels().length];
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        for (; it.hasNext(); ) {
            eventsPerStream[0] = it.next();
            Object groupKeyComplete = generateGroupKey(eventsPerStream, true);
            for (int j = 0; j < levels.length; j++) {
                Object subkey = levels[j].computeSubkey(groupKeyComplete);
                groupKeys[j] = subkey;
            }
            aggregationService.applyEnter(eventsPerStream, groupKeys, agentInstanceContext);
        }

        ArrayDeque<EventBean> deque = ResultSetProcessorUtil.iteratorToDeque(obtainIterator(parent));
        aggregationService.clearResults(agentInstanceContext);
        return deque.iterator();
    }

    private Iterator<EventBean> obtainIterator(Viewable parent) {
        resetEventPerGroupBuf();
        EventBean[] events = EPAssertionUtil.iteratorToArray(parent.iterator());
        generateGroupKeysView(events, eventPerGroupBuf, true);
        EventBean[] output = generateOutputEventsView(eventPerGroupBuf, true, true);
        return new ArrayEventIterator(output);
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        resetEventPerGroupJoinBuf();
        generateGroupKeysJoin(joinSet, eventPerGroupJoinBuf, true);
        EventBean[] output = generateOutputEventsJoin(eventPerGroupJoinBuf, true, true);
        return new ArrayEventIterator(output);
    }

    public void clear() {
        aggregationService.clearResults(agentInstanceContext);
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType) {
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return handleOutputLimitDefaultJoin(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return handleOutputLimitAllJoin(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return handleOutputLimitFirstJoin(joinEventsSet, generateSynthetic);
        }
        // (outputLimitLimitType == OutputLimitLimitType.LAST) {
        return handleOutputLimitLastJoin(joinEventsSet, generateSynthetic);
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType) {
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return handleOutputLimitDefaultView(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return handleOutputLimitAllView(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return handleOutputLimitFirstView(viewEventsList, generateSynthetic);
        }
        // (outputLimitLimitType == OutputLimitLimitType.LAST) {
        return handleOutputLimitLastView(viewEventsList, generateSynthetic);
    }

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
        if (outputLastHelper != null) {
            visitor.visit(outputLastHelper);
        }
        if (outputAllHelper != null) {
            visitor.visit(outputAllHelper);
        }
        if (outputFirstHelpers != null) {
            for (ResultSetProcessorGroupedOutputFirstHelper helper : outputFirstHelpers) {
                visitor.visit(helper);
            }
        }
    }

    private UniformPair<EventBean[]> handleOutputLimitFirstView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {

        for (Map<Object, EventBean[]> aGroupRepsView : groupRepsPerLevelBuf) {
            aGroupRepsView.clear();
        }

        rstreamEventSortArrayBuf.reset();

        int oldEventCount;
        if (prototype.getPerLevelExpression().getOptionalHavingNodes() == null) {
            oldEventCount = handleOutputLimitFirstViewNoHaving(viewEventsList, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
        } else {
            oldEventCount = handleOutputLimitFirstViewHaving(viewEventsList, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, oldEventCount);
    }

    private UniformPair<EventBean[]> handleOutputLimitFirstJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {

        for (Map<Object, EventBean[]> aGroupRepsView : groupRepsPerLevelBuf) {
            aGroupRepsView.clear();
        }

        rstreamEventSortArrayBuf.reset();

        int oldEventCount;
        if (prototype.getPerLevelExpression().getOptionalHavingNodes() == null) {
            oldEventCount = handleOutputLimitFirstJoinNoHaving(joinEventsSet, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
        } else {
            oldEventCount = handleOutputLimitFirstJoinHaving(joinEventsSet, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, oldEventCount);
    }

    private int handleOutputLimitFirstViewHaving(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, List<EventBean>[] oldEventsPerLevel, List<Object>[] oldEventsSortKeyPerLevel) {
        int oldEventCount = 0;

        ExprEvaluator[] havingPerLevel = prototype.getPerLevelExpression().getOptionalHavingNodes();

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            EventBean[] eventsPerStream;
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }

            if (newData != null) {
                for (EventBean aNewData : newData) {
                    eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);

                        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().qHavingClauseNonJoin(aNewData);
                        }
                        Boolean result = (Boolean) havingPerLevel[level.getLevelNumber()].evaluate(eventsPerStream, true, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aHavingClauseNonJoin(result);
                        }
                        if ((result == null) || (!result)) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatched(false, groupKey, level, eventsPerStream, true, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);

                        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().qHavingClauseNonJoin(anOldData);
                        }
                        Boolean result = (Boolean) havingPerLevel[level.getLevelNumber()].evaluate(eventsPerStream, false, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aHavingClauseNonJoin(result);
                        }
                        if ((result == null) || (!result)) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatched(false, groupKey, level, eventsPerStream, false, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return oldEventCount;
    }

    private int handleOutputLimitFirstJoinNoHaving(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventSet, boolean generateSynthetic, List<EventBean>[] oldEventsPerLevel, List<Object>[] oldEventsSortKeyPerLevel) {

        int oldEventCount = 0;

        // outer loop is the events
        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    Object groupKeyComplete = generateGroupKey(aNewData.getArray(), true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, aNewData.getArray()) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatched(false, groupKey, level, aNewData.getArray(), true, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                    aggregationService.applyEnter(aNewData.getArray(), groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    Object groupKeyComplete = generateGroupKey(anOldData.getArray(), false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, anOldData.getArray()) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatched(false, groupKey, level, anOldData.getArray(), false, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                    aggregationService.applyLeave(anOldData.getArray(), groupKeysPerLevel, agentInstanceContext);
                }
            }
        }
        return oldEventCount;
    }

    private int handleOutputLimitFirstJoinHaving(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventSet, boolean generateSynthetic, List<EventBean>[] oldEventsPerLevel, List<Object>[] oldEventsSortKeyPerLevel) {
        int oldEventCount = 0;

        ExprEvaluator[] havingPerLevel = prototype.getPerLevelExpression().getOptionalHavingNodes();

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    Object groupKeyComplete = generateGroupKey(aNewData.getArray(), true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    }
                    aggregationService.applyEnter(aNewData.getArray(), groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    Object groupKeyComplete = generateGroupKey(anOldData.getArray(), false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    }
                    aggregationService.applyLeave(anOldData.getArray(), groupKeysPerLevel, agentInstanceContext);
                }
            }

            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    Object groupKeyComplete = generateGroupKey(aNewData.getArray(), true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);

                        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().qHavingClauseJoin(aNewData.getArray());
                        }
                        Boolean result = (Boolean) havingPerLevel[level.getLevelNumber()].evaluate(aNewData.getArray(), true, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aHavingClauseJoin(result);
                        }
                        if ((result == null) || (!result)) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, aNewData.getArray()) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatched(false, groupKey, level, aNewData.getArray(), true, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    Object groupKeyComplete = generateGroupKey(anOldData.getArray(), false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);

                        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().qHavingClauseJoin(anOldData.getArray());
                        }
                        Boolean result = (Boolean) havingPerLevel[level.getLevelNumber()].evaluate(anOldData.getArray(), false, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aHavingClauseJoin(result);
                        }
                        if ((result == null) || (!result)) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, anOldData.getArray()) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatched(false, groupKey, level, anOldData.getArray(), false, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return oldEventCount;
    }

    private int handleOutputLimitFirstViewNoHaving(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, List<EventBean>[] oldEventsPerLevel, List<Object>[] oldEventsSortKeyPerLevel) {

        int oldEventCount = 0;

        // outer loop is the events
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            EventBean[] eventsPerStream;
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatched(false, groupKey, level, eventsPerStream, true, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatched(false, groupKey, level, eventsPerStream, false, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
        }
        return oldEventCount;
    }

    private UniformPair<EventBean[]> handleOutputLimitDefaultView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {

        List<EventBean> newEvents = new ArrayList<EventBean>();
        List<Object> newEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new ArrayList<Object>();
        }

        List<EventBean> oldEvents = null;
        List<Object> oldEventsSortKey = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new ArrayList<EventBean>();
            if (orderByProcessor != null) {
                oldEventsSortKey = new ArrayList<Object>();
            }
        }

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            resetEventPerGroupBuf();
            Object[][] newDataMultiKey = generateGroupKeysView(newData, eventPerGroupBuf, true);
            Object[][] oldDataMultiKey = generateGroupKeysView(oldData, eventPerGroupBuf, false);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedCollectNonJoin(eventPerGroupBuf, false, generateSynthetic, oldEvents, oldEventsSortKey);
            }

            // update aggregates
            EventBean[] eventsPerStream = new EventBean[1];
            if (newData != null) {
                for (int i = 0; i < newData.length; i++) {
                    eventsPerStream[0] = newData[i];
                    aggregationService.applyEnter(eventsPerStream, newDataMultiKey[i], agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (int i = 0; i < oldData.length; i++) {
                    eventsPerStream[0] = oldData[i];
                    aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[i], agentInstanceContext);
                }
            }

            generateOutputBatchedCollectNonJoin(eventPerGroupBuf, true, generateSynthetic, newEvents, newEventsSortKey);
        }

        return convertToArrayMaySort(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey);
    }

    private UniformPair<EventBean[]> handleOutputLimitDefaultJoin(List<UniformPair<Set<MultiKey<EventBean>>>> viewEventsList, boolean generateSynthetic) {

        List<EventBean> newEvents = new ArrayList<EventBean>();
        List<Object> newEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new ArrayList<Object>();
        }

        List<EventBean> oldEvents = null;
        List<Object> oldEventsSortKey = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new ArrayList<EventBean>();
            if (orderByProcessor != null) {
                oldEventsSortKey = new ArrayList<Object>();
            }
        }

        for (UniformPair<Set<MultiKey<EventBean>>> pair : viewEventsList) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            resetEventPerGroupJoinBuf();
            Object[][] newDataMultiKey = generateGroupKeysJoin(newData, eventPerGroupJoinBuf, true);
            Object[][] oldDataMultiKey = generateGroupKeysJoin(oldData, eventPerGroupJoinBuf, false);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedCollectJoin(eventPerGroupJoinBuf, false, generateSynthetic, oldEvents, oldEventsSortKey);
            }

            // update aggregates
            if (newData != null) {
                int count = 0;
                for (MultiKey<EventBean> newEvent : newData) {
                    aggregationService.applyEnter(newEvent.getArray(), newDataMultiKey[count++], agentInstanceContext);
                }
            }
            if (oldData != null) {
                int count = 0;
                for (MultiKey<EventBean> oldEvent : oldData) {
                    aggregationService.applyLeave(oldEvent.getArray(), oldDataMultiKey[count++], agentInstanceContext);
                }
            }

            generateOutputBatchedCollectJoin(eventPerGroupJoinBuf, true, generateSynthetic, newEvents, newEventsSortKey);
        }

        return convertToArrayMaySort(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey);
    }

    public boolean hasAggregation() {
        return true;
    }

    public void removed(Object key) {
        throw new UnsupportedOperationException();
    }

    public Object generateGroupKey(EventBean[] eventsPerStream, boolean isNewData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessComputeGroupKeys(isNewData, prototype.getGroupKeyNodeExpressions(), eventsPerStream);
            Object keyObject;
            if (prototype.getGroupKeyNode() != null) {
                keyObject = prototype.getGroupKeyNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
            } else {
                ExprEvaluator[] evals = prototype.getGroupKeyNodes();
                Object[] keys = new Object[evals.length];
                for (int i = 0; i < evals.length; i++) {
                    keys[i] = evals[i].evaluate(eventsPerStream, isNewData, agentInstanceContext);
                }
                keyObject = new MultiKeyUntyped(keys);
            }

            InstrumentationHelper.get().aResultSetProcessComputeGroupKeys(isNewData, keyObject);
            return keyObject;
        }

        if (prototype.getGroupKeyNode() != null) {
            return prototype.getGroupKeyNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
        } else {
            ExprEvaluator[] evals = prototype.getGroupKeyNodes();
            Object[] keys = new Object[evals.length];
            for (int i = 0; i < evals.length; i++) {
                keys[i] = evals[i].evaluate(eventsPerStream, isNewData, agentInstanceContext);
            }
            return new MultiKeyUntyped(keys);
        }
    }

    private void generateOutputBatched(boolean join, Object mk, AggregationGroupByRollupLevel level, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, List<EventBean>[] resultEvents, List<Object>[] optSortKeys) {
        List<EventBean> resultList = resultEvents[level.getLevelNumber()];
        List<Object> sortKeys = optSortKeys == null ? null : optSortKeys[level.getLevelNumber()];
        generateOutputBatched(join, mk, level, eventsPerStream, isNewData, isSynthesize, resultList, sortKeys);
    }

    public void generateOutputBatched(boolean join, Object mk, AggregationGroupByRollupLevel level, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys) {
        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), level);

        if (prototype.getPerLevelExpression().getOptionalHavingNodes() != null) {
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().qHavingClauseNonJoin(eventsPerStream[0]);
                else InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);
            }
            Boolean result = (Boolean) prototype.getPerLevelExpression().getOptionalHavingNodes()[level.getLevelNumber()].evaluate(eventsPerStream, isNewData, agentInstanceContext);
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().aHavingClauseNonJoin(result);
                else InstrumentationHelper.get().aHavingClauseJoin(result);
            }
            if ((result == null) || (!result)) {
                return;
            }
        }

        resultEvents.add(prototype.getPerLevelExpression().getSelectExprProcessor()[level.getLevelNumber()].process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));

        if (prototype.isSorting()) {
            optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext, prototype.getPerLevelExpression().getOptionalOrderByElements()[level.getLevelNumber()]));
        }
    }

    public void generateOutputBatchedMapUnsorted(boolean join, Object mk, AggregationGroupByRollupLevel level, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, Map<Object, EventBean> resultEvents) {
        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), level);

        if (prototype.getPerLevelExpression().getOptionalHavingNodes() != null) {
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().qHavingClauseNonJoin(eventsPerStream[0]);
                else InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);
            }
            Boolean result = (Boolean) prototype.getPerLevelExpression().getOptionalHavingNodes()[level.getLevelNumber()].evaluate(eventsPerStream, isNewData, agentInstanceContext);
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().aHavingClauseNonJoin(result);
                else InstrumentationHelper.get().aHavingClauseJoin(result);
            }
            if ((result == null) || (!result)) {
                return;
            }
        }

        resultEvents.put(mk, prototype.getPerLevelExpression().getSelectExprProcessor()[level.getLevelNumber()].process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));
    }

    private UniformPair<EventBean[]> handleOutputLimitLastView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        int oldEventCount = 0;
        if (prototype.isSelectRStream()) {
            rstreamEventSortArrayBuf.reset();
        }

        for (Map<Object, EventBean[]> aGroupRepsView : groupRepsPerLevelBuf) {
            aGroupRepsView.clear();
        }

        // outer loop is the events
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            EventBean[] eventsPerStream;
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                            if (prototype.isSelectRStream()) {
                                generateOutputBatched(false, groupKey, level, eventsPerStream, true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                                oldEventCount++;
                            }
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                            if (prototype.isSelectRStream()) {
                                generateOutputBatched(true, groupKey, level, eventsPerStream, true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                                oldEventCount++;
                            }
                        }
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, oldEventCount);
    }

    private UniformPair<EventBean[]> handleOutputLimitLastJoin(List<UniformPair<Set<MultiKey<EventBean>>>> viewEventsList, boolean generateSynthetic) {
        int oldEventCount = 0;
        if (prototype.isSelectRStream()) {
            rstreamEventSortArrayBuf.reset();
        }

        for (Map<Object, EventBean[]> aGroupRepsView : groupRepsPerLevelBuf) {
            aGroupRepsView.clear();
        }

        // outer loop is the events
        for (UniformPair<Set<MultiKey<EventBean>>> pair : viewEventsList) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    Object groupKeyComplete = generateGroupKey(aNewData.getArray(), true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, aNewData.getArray()) == null) {
                            if (prototype.isSelectRStream()) {
                                generateOutputBatched(false, groupKey, level, aNewData.getArray(), true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                                oldEventCount++;
                            }
                        }
                    }
                    aggregationService.applyEnter(aNewData.getArray(), groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    Object groupKeyComplete = generateGroupKey(anOldData.getArray(), false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, anOldData.getArray()) == null) {
                            if (prototype.isSelectRStream()) {
                                generateOutputBatched(true, groupKey, level, anOldData.getArray(), true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                                oldEventCount++;
                            }
                        }
                    }
                    aggregationService.applyLeave(anOldData.getArray(), groupKeysPerLevel, agentInstanceContext);
                }
            }
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, oldEventCount);
    }

    private UniformPair<EventBean[]> handleOutputLimitAllView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {

        int oldEventCount = 0;
        if (prototype.isSelectRStream()) {
            rstreamEventSortArrayBuf.reset();

            for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                Map<Object, EventBean[]> groupGenerators = groupRepsPerLevelBuf[level.getLevelNumber()];
                for (Map.Entry<Object, EventBean[]> entry : groupGenerators.entrySet()) {
                    generateOutputBatched(false, entry.getKey(), level, entry.getValue(), false, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                    oldEventCount++;
                }
            }
        }

        // outer loop is the events
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    EventBean[] eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        Object existing = groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream);

                        if (existing == null && prototype.isSelectRStream()) {
                            generateOutputBatched(false, groupKey, level, eventsPerStream, true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                            oldEventCount++;
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    EventBean[] eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKey(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        Object existing = groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream);

                        if (existing == null && prototype.isSelectRStream()) {
                            generateOutputBatched(false, groupKey, level, eventsPerStream, false, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                            oldEventCount++;
                        }
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, oldEventCount);
    }

    private UniformPair<EventBean[]> handleOutputLimitAllJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {

        int oldEventCount = 0;
        if (prototype.isSelectRStream()) {
            rstreamEventSortArrayBuf.reset();

            for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                Map<Object, EventBean[]> groupGenerators = groupRepsPerLevelBuf[level.getLevelNumber()];
                for (Map.Entry<Object, EventBean[]> entry : groupGenerators.entrySet()) {
                    generateOutputBatched(false, entry.getKey(), level, entry.getValue(), false, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                    oldEventCount++;
                }
            }
        }

        // outer loop is the events
        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    Object groupKeyComplete = generateGroupKey(aNewData.getArray(), true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        Object existing = groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, aNewData.getArray());

                        if (existing == null && prototype.isSelectRStream()) {
                            generateOutputBatched(false, groupKey, level, aNewData.getArray(), true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                            oldEventCount++;
                        }
                    }
                    aggregationService.applyEnter(aNewData.getArray(), groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    Object groupKeyComplete = generateGroupKey(anOldData.getArray(), false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        Object existing = groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, anOldData.getArray());

                        if (existing == null && prototype.isSelectRStream()) {
                            generateOutputBatched(false, groupKey, level, anOldData.getArray(), false, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                            oldEventCount++;
                        }
                    }
                    aggregationService.applyLeave(anOldData.getArray(), groupKeysPerLevel, agentInstanceContext);
                }
            }
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, oldEventCount);
    }

    private void generateOutputBatchedCollectNonJoin(Map<Object, EventBean>[] eventPairs, boolean isNewData, boolean generateSynthetic, List<EventBean> events, List<Object> sortKey) {
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        EventBean[] eventsPerStream = new EventBean[1];

        for (AggregationGroupByRollupLevel level : levels) {
            Map<Object, EventBean> eventsForLevel = eventPairs[level.getLevelNumber()];
            for (Map.Entry<Object, EventBean> pair : eventsForLevel.entrySet()) {
                eventsPerStream[0] = pair.getValue();
                generateOutputBatched(false, pair.getKey(), level, eventsPerStream, isNewData, generateSynthetic, events, sortKey);
            }
        }
    }

    private void generateOutputBatchedCollectJoin(Map<Object, EventBean[]>[] eventPairs, boolean isNewData, boolean generateSynthetic, List<EventBean> events, List<Object> sortKey) {
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();

        for (AggregationGroupByRollupLevel level : levels) {
            Map<Object, EventBean[]> eventsForLevel = eventPairs[level.getLevelNumber()];
            for (Map.Entry<Object, EventBean[]> pair : eventsForLevel.entrySet()) {
                generateOutputBatched(false, pair.getKey(), level, pair.getValue(), isNewData, generateSynthetic, events, sortKey);
            }
        }
    }

    private void resetEventPerGroupBuf() {
        for (Map<Object, EventBean> anEventPerGroupBuf : eventPerGroupBuf) {
            anEventPerGroupBuf.clear();
        }
    }

    private void resetEventPerGroupJoinBuf() {
        for (Map<Object, EventBean[]> anEventPerGroupBuf : eventPerGroupJoinBuf) {
            anEventPerGroupBuf.clear();
        }
    }

    private EventsAndSortKeysPair getOldEventsSortKeys(int oldEventCount, List<EventBean>[] oldEventsPerLevel, List<Object>[] oldEventsSortKeyPerLevel) {
        EventBean[] oldEventsArr = new EventBean[oldEventCount];
        Object[] oldEventsSortKeys = null;
        if (orderByProcessor != null) {
            oldEventsSortKeys = new Object[oldEventCount];
        }
        int countEvents = 0;
        int countSortKeys = 0;
        for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
            List<EventBean> events = oldEventsPerLevel[level.getLevelNumber()];
            for (EventBean event : events) {
                oldEventsArr[countEvents++] = event;
            }
            if (orderByProcessor != null) {
                List<Object> sortKeys = oldEventsSortKeyPerLevel[level.getLevelNumber()];
                for (Object sortKey : sortKeys) {
                    oldEventsSortKeys[countSortKeys++] = sortKey;
                }
            }
        }
        return new EventsAndSortKeysPair(oldEventsArr, oldEventsSortKeys);
    }

    protected Object[][] generateGroupKeysView(EventBean[] events, Map<Object, EventBean>[] eventPerKey, boolean isNewData) {
        if (events == null) {
            return null;
        }

        Object[][] result = new Object[events.length][];
        EventBean[] eventsPerStream = new EventBean[1];

        for (int i = 0; i < events.length; i++) {
            eventsPerStream[0] = events[i];
            Object groupKeyComplete = generateGroupKey(eventsPerStream, isNewData);
            AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
            result[i] = new Object[levels.length];
            for (int j = 0; j < levels.length; j++) {
                Object subkey = levels[j].computeSubkey(groupKeyComplete);
                result[i][j] = subkey;
                eventPerKey[levels[j].getLevelNumber()].put(subkey, events[i]);
            }
        }

        return result;
    }

    private Object[][] generateGroupKeysJoin(Set<MultiKey<EventBean>> events, Map<Object, EventBean[]>[] eventPerKey, boolean isNewData) {
        if (events == null || events.isEmpty()) {
            return null;
        }

        Object[][] result = new Object[events.size()][];

        int count = -1;
        for (MultiKey<EventBean> eventrow : events) {
            count++;
            Object groupKeyComplete = generateGroupKey(eventrow.getArray(), isNewData);
            AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
            result[count] = new Object[levels.length];
            for (int j = 0; j < levels.length; j++) {
                Object subkey = levels[j].computeSubkey(groupKeyComplete);
                result[count][j] = subkey;
                eventPerKey[levels[j].getLevelNumber()].put(subkey, eventrow.getArray());
            }
        }

        return result;
    }

    private UniformPair<EventBean[]> generateAndSort(Map<Object, EventBean[]>[] outputLimitGroupRepsPerLevel, boolean generateSynthetic, int oldEventCount) {
        // generate old events: ordered by level by default
        EventBean[] oldEventsArr = null;
        Object[] oldEventSortKeys = null;
        if (prototype.isSelectRStream() && oldEventCount > 0) {
            EventsAndSortKeysPair pair = getOldEventsSortKeys(oldEventCount, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
            oldEventsArr = pair.getEvents();
            oldEventSortKeys = pair.getSortKeys();
        }

        List<EventBean> newEvents = new ArrayList<EventBean>();
        List<Object> newEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new ArrayList<Object>();
        }

        for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
            Map<Object, EventBean[]> groupGenerators = outputLimitGroupRepsPerLevel[level.getLevelNumber()];
            for (Map.Entry<Object, EventBean[]> entry : groupGenerators.entrySet()) {
                generateOutputBatched(false, entry.getKey(), level, entry.getValue(), true, generateSynthetic, newEvents, newEventsSortKey);
            }
        }

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        if (orderByProcessor != null) {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream()) {
                oldEventsArr = orderByProcessor.sort(oldEventsArr, oldEventSortKeys, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            for (EventBean aNewData : newData) {
                eventsPerStream[0] = aNewData;
                Object[] keys = generateGroupKeysNonJoin(eventsPerStream, true);
                aggregationService.applyEnter(eventsPerStream, keys, agentInstanceContext);
            }
        }
        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                eventsPerStream[0] = anOldData;
                Object[] keys = generateGroupKeysNonJoin(eventsPerStream, false);
                aggregationService.applyLeave(eventsPerStream, keys, agentInstanceContext);
            }
        }
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        if (newEvents != null) {
            for (MultiKey<EventBean> mk : newEvents) {
                Object[] keys = generateGroupKeysNonJoin(mk.getArray(), true);
                aggregationService.applyEnter(mk.getArray(), keys, agentInstanceContext);
            }
        }
        if (oldEvents != null) {
            for (MultiKey<EventBean> mk : oldEvents) {
                Object[] keys = generateGroupKeysNonJoin(mk.getArray(), false);
                aggregationService.applyLeave(mk.getArray(), keys, agentInstanceContext);
            }
        }
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllHelper.processView(newData, oldData, isGenerateSynthetic);
        } else {
            outputLastHelper.processView(newData, oldData, isGenerateSynthetic);
        }
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllHelper.processJoin(newEvents, oldEvents, isGenerateSynthetic);
        } else {
            outputLastHelper.processJoin(newEvents, oldEvents, isGenerateSynthetic);
        }
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize, boolean isAll) {
        if (isAll) {
            return outputAllHelper.outputView(isSynthesize);
        }
        return outputLastHelper.outputView(isSynthesize);
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedJoin(boolean isSynthesize, boolean isAll) {
        if (isAll) {
            return outputAllHelper.outputJoin(isSynthesize);
        }
        return outputLastHelper.outputJoin(isSynthesize);
    }

    public void stop() {
        if (outputLastHelper != null) {
            outputLastHelper.destroy();
        }
        if (outputFirstHelpers != null) {
            for (ResultSetProcessorGroupedOutputFirstHelper helper : outputFirstHelpers) {
                helper.destroy();
            }
        }
        if (outputAllHelper != null) {
            outputAllHelper.destroy();
        }
    }

    private UniformPair<EventBean[]> convertToArrayMaySort(List<EventBean> newEvents, List<Object> newEventsSortKey, List<EventBean> oldEvents, List<Object> oldEventsSortKey) {
        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream()) {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null) {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream()) {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private Object[] generateGroupKeysNonJoin(EventBean[] eventsPerStream, boolean isNewData) {
        Object groupKeyComplete = generateGroupKey(eventsPerStream, true);
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        Object[] result = new Object[levels.length];
        for (int j = 0; j < levels.length; j++) {
            Object subkey = levels[j].computeSubkey(groupKeyComplete);
            result[j] = subkey;
        }
        return result;
    }

    private static class EventArrayAndSortKeyArray {
        private final List<EventBean>[] eventsPerLevel;
        private final List<Object>[] sortKeyPerLevel;

        private EventArrayAndSortKeyArray(List<EventBean>[] eventsPerLevel, List<Object>[] sortKeyPerLevel) {
            this.eventsPerLevel = eventsPerLevel;
            this.sortKeyPerLevel = sortKeyPerLevel;
        }

        public List<EventBean>[] getEventsPerLevel() {
            return eventsPerLevel;
        }

        public List<Object>[] getSortKeyPerLevel() {
            return sortKeyPerLevel;
        }

        public void reset() {
            for (List<EventBean> anEventsPerLevel : eventsPerLevel) {
                anEventsPerLevel.clear();
            }
            if (sortKeyPerLevel != null) {
                for (List<Object> anSortKeyPerLevel : sortKeyPerLevel) {
                    anSortKeyPerLevel.clear();
                }
            }
        }
    }

    private static class EventsAndSortKeysPair {
        private final EventBean[] events;
        private final Object[] sortKeys;

        private EventsAndSortKeysPair(EventBean[] events, Object[] sortKeys) {
            this.events = events;
            this.sortKeys = sortKeys;
        }

        public EventBean[] getEvents() {
            return events;
        }

        public Object[] getSortKeys() {
            return sortKeys;
        }
    }
}
