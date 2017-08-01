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
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationRowRemovedCallback;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.view.OutputConditionPolled;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.Viewable;

import java.util.*;

/**
 * Result-set processor for the aggregate-grouped case:
 * there is a group-by and one or more non-aggregation event properties in the select clause are not listed in the group by,
 * and there are aggregation functions.
 * <p>
 * This processor does perform grouping by computing MultiKey group-by keys for each row.
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 * <p>
 * Aggregation state is a table of rows held by {@link AggregationService} where the row key is the group-by MultiKey.
 */
public class ResultSetProcessorAggregateGrouped implements ResultSetProcessor, AggregationRowRemovedCallback {

    protected final ResultSetProcessorAggregateGroupedFactory prototype;
    private final SelectExprProcessor selectExprProcessor;
    private final OrderByProcessor orderByProcessor;
    protected final AggregationService aggregationService;
    protected AgentInstanceContext agentInstanceContext;

    protected final EventBean[] eventsPerStreamOneStream = new EventBean[1];

    // For output limiting, keep a representative of each group-by group
    private ResultSetProcessorGroupedOutputAllGroupReps outputAllGroupReps;
    private final Map<Object, EventBean[]> workCollection = new LinkedHashMap<Object, EventBean[]>();
    private final Map<Object, EventBean[]> workCollectionTwo = new LinkedHashMap<Object, EventBean[]>();

    private ResultSetProcessorAggregateGroupedOutputLastHelper outputLastHelper;
    private ResultSetProcessorAggregateGroupedOutputAllHelper outputAllHelper;
    private ResultSetProcessorGroupedOutputFirstHelper outputFirstHelper;

    public ResultSetProcessorAggregateGrouped(ResultSetProcessorAggregateGroupedFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.agentInstanceContext = agentInstanceContext;

        aggregationService.setRemovedCallback(this);

        if (prototype.isOutputLast() && prototype.isEnableOutputLimitOpt()) {
            outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSAggregateGroupedOutputLastOpt(agentInstanceContext, this, prototype);
        } else if (prototype.isOutputAll()) {
            if (!prototype.isEnableOutputLimitOpt()) {
                outputAllGroupReps = prototype.getResultSetProcessorHelperFactory().makeRSGroupedOutputAllNoOpt(agentInstanceContext, prototype.getGroupKeyNodeExpressions(), prototype.getNumStreams());
            } else {
                outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSAggregateGroupedOutputAll(agentInstanceContext, this, prototype);
            }
        } else if (prototype.isOutputFirst()) {
            outputFirstHelper = prototype.getResultSetProcessorHelperFactory().makeRSGroupedOutputFirst(agentInstanceContext, prototype.getGroupKeyNodes(), prototype.getGroupKeyNodeExpressions(), prototype.getOptionalOutputFirstConditionFactory(), null, -1);
        }
    }

    public void setAgentInstanceContext(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    public EventType getResultEventType() {
        return prototype.getResultEventType();
    }

    public EventBean[] getEventsPerStreamOneStream() {
        return eventsPerStreamOneStream;
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            // apply new data to aggregates
            for (EventBean aNewData : newData) {
                eventsPerStream[0] = aNewData;
                Object mk = generateGroupKey(eventsPerStream, true);
                aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            for (EventBean anOldData : oldData) {
                eventsPerStream[0] = anOldData;
                Object mk = generateGroupKey(eventsPerStream, false);
                aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
            }
        }
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        if (!newEvents.isEmpty()) {
            // apply old data to aggregates
            for (MultiKey<EventBean> eventsPerStream : newEvents) {
                Object mk = generateGroupKey(eventsPerStream.getArray(), true);
                aggregationService.applyEnter(eventsPerStream.getArray(), mk, agentInstanceContext);
            }
        }
        if (oldEvents != null && !oldEvents.isEmpty()) {
            // apply old data to aggregates
            for (MultiKey<EventBean> eventsPerStream : oldEvents) {
                Object mk = generateGroupKey(eventsPerStream.getArray(), false);
                aggregationService.applyLeave(eventsPerStream.getArray(), mk, agentInstanceContext);
            }
        }
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerEvent();
        }
        // Generate group-by keys for all events
        Object[] newDataGroupByKeys = generateGroupKeys(newEvents, true);
        Object[] oldDataGroupByKeys = generateGroupKeys(oldEvents, false);

        // generate old events
        if (prototype.isUnidirectional()) {
            this.clear();
        }

        // update aggregates
        if (!newEvents.isEmpty()) {
            // apply old data to aggregates
            int count = 0;
            for (MultiKey<EventBean> eventsPerStream : newEvents) {
                aggregationService.applyEnter(eventsPerStream.getArray(), newDataGroupByKeys[count], agentInstanceContext);
                count++;
            }
        }
        if (!oldEvents.isEmpty()) {
            // apply old data to aggregates
            int count = 0;
            for (MultiKey<EventBean> eventsPerStream : oldEvents) {
                aggregationService.applyLeave(eventsPerStream.getArray(), oldDataGroupByKeys[count], agentInstanceContext);
                count++;
            }
        }

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsJoin(oldEvents, oldDataGroupByKeys, false, isSynthesize);
        }
        EventBean[] selectNewEvents = generateOutputEventsJoin(newEvents, newDataGroupByKeys, true, isSynthesize);

        if ((selectNewEvents != null) || (selectOldEvents != null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aResultSetProcessGroupedRowPerEvent(selectNewEvents, selectOldEvents);
            }
            return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerEvent(null, null);
        }
        return null;
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerEvent();
        }

        // Generate group-by keys for all events
        Object[] newDataGroupByKeys = generateGroupKeys(newData, true);
        Object[] oldDataGroupByKeys = generateGroupKeys(oldData, false);

        // update aggregates
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            // apply new data to aggregates
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                aggregationService.applyEnter(eventsPerStream, newDataGroupByKeys[i], agentInstanceContext);
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            for (int i = 0; i < oldData.length; i++) {
                eventsPerStream[0] = oldData[i];
                aggregationService.applyLeave(eventsPerStream, oldDataGroupByKeys[i], agentInstanceContext);
            }
        }

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsView(oldData, oldDataGroupByKeys, false, isSynthesize);
        }
        EventBean[] selectNewEvents = generateOutputEventsView(newData, newDataGroupByKeys, true, isSynthesize);

        if ((selectNewEvents != null) || (selectOldEvents != null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aResultSetProcessGroupedRowPerEvent(selectNewEvents, selectOldEvents);
            }
            return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerEvent(null, null);
        }
        return null;
    }

    private EventBean[] generateOutputEventsView(EventBean[] outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize) {
        if (outputEvents == null) {
            return null;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        EventBean[] events = new EventBean[outputEvents.length];
        Object[] keys = new Object[outputEvents.length];
        EventBean[][] currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new EventBean[outputEvents.length][];
        }

        int countOutputRows = 0;
        for (int countInputRows = 0; countInputRows < outputEvents.length; countInputRows++) {
            aggregationService.setCurrentAccess(groupByKeys[countInputRows], agentInstanceContext.getAgentInstanceId(), null);
            eventsPerStream[0] = outputEvents[countInputRows];

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseNonJoin(outputEvents[countInputRows]);
                }
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseNonJoin(result);
                }
                if ((result == null) || (!result)) {
                    continue;
                }
            }

            events[countOutputRows] = selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
            keys[countOutputRows] = groupByKeys[countInputRows];
            if (prototype.isSorting()) {
                EventBean[] currentEventsPerStream = new EventBean[]{outputEvents[countInputRows]};
                currentGenerators[countOutputRows] = currentEventsPerStream;
            }

            countOutputRows++;
        }

        // Resize if some rows were filtered out
        if (countOutputRows != events.length) {
            if (countOutputRows == 0) {
                return null;
            }
            EventBean[] outEvents = new EventBean[countOutputRows];
            System.arraycopy(events, 0, outEvents, 0, countOutputRows);
            events = outEvents;

            if (prototype.isSorting()) {
                Object[] outKeys = new Object[countOutputRows];
                System.arraycopy(keys, 0, outKeys, 0, countOutputRows);
                keys = outKeys;

                EventBean[][] outGens = new EventBean[countOutputRows][];
                System.arraycopy(currentGenerators, 0, outGens, 0, countOutputRows);
                currentGenerators = outGens;
            }
        }

        if (prototype.isSorting()) {
            events = orderByProcessor.sort(events, currentGenerators, keys, isNewData, agentInstanceContext);
        }

        return events;
    }

    public Object[] generateGroupKeys(Set<MultiKey<EventBean>> resultSet, boolean isNewData) {
        if (resultSet.isEmpty()) {
            return null;
        }

        Object[] keys = new Object[resultSet.size()];

        int count = 0;
        for (MultiKey<EventBean> eventsPerStream : resultSet) {
            keys[count] = generateGroupKey(eventsPerStream.getArray(), isNewData);
            count++;
        }

        return keys;
    }

    public Object[] generateGroupKeys(EventBean[] events, boolean isNewData) {
        if (events == null) {
            return null;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        Object[] keys = new Object[events.length];

        for (int i = 0; i < events.length; i++) {
            eventsPerStream[0] = events[i];
            keys[i] = generateGroupKey(eventsPerStream, isNewData);
        }

        return keys;
    }

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
        if (outputAllGroupReps != null) {
            visitor.visit(outputAllGroupReps);
        }
        if (outputLastHelper != null) {
            visitor.visit(outputLastHelper);
        }
        if (outputAllHelper != null) {
            visitor.visit(outputAllHelper);
        }
        if (outputFirstHelper != null) {
            visitor.visit(outputFirstHelper);
        }
    }

    /**
     * Generates the group-by key for the row
     *
     * @param eventsPerStream is the row of events
     * @param isNewData       is true for new data
     * @return grouping keys
     */
    protected Object generateGroupKey(EventBean[] eventsPerStream, boolean isNewData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessComputeGroupKeys(isNewData, prototype.getGroupKeyNodeExpressions(), eventsPerStream);

            Object keyObject;
            if (prototype.getGroupKeyNode() != null) {
                keyObject = prototype.getGroupKeyNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
            } else {
                Object[] keys = new Object[prototype.getGroupKeyNodes().length];
                int count = 0;
                for (ExprEvaluator exprNode : prototype.getGroupKeyNodes()) {
                    keys[count] = exprNode.evaluate(eventsPerStream, isNewData, agentInstanceContext);
                    count++;
                }
                keyObject = new MultiKeyUntyped(keys);
            }
            InstrumentationHelper.get().aResultSetProcessComputeGroupKeys(isNewData, keyObject);
            return keyObject;
        }

        if (prototype.getGroupKeyNode() != null) {
            return prototype.getGroupKeyNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
        }

        Object[] keys = new Object[prototype.getGroupKeyNodes().length];
        int count = 0;
        for (ExprEvaluator exprNode : prototype.getGroupKeyNodes()) {
            keys[count] = exprNode.evaluate(eventsPerStream, isNewData, agentInstanceContext);
            count++;
        }
        return new MultiKeyUntyped(keys);
    }

    private EventBean[] generateOutputEventsJoin(Set<MultiKey<EventBean>> resultSet, Object[] groupByKeys, boolean isNewData, boolean isSynthesize) {
        if (resultSet.isEmpty()) {
            return null;
        }

        EventBean[] events = new EventBean[resultSet.size()];
        Object[] keys = new Object[resultSet.size()];
        EventBean[][] currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new EventBean[resultSet.size()][];
        }

        int countOutputRows = 0;
        int countInputRows = -1;
        for (MultiKey<EventBean> row : resultSet) {
            countInputRows++;
            EventBean[] eventsPerStream = row.getArray();

            aggregationService.setCurrentAccess(groupByKeys[countInputRows], agentInstanceContext.getAgentInstanceId(), null);

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);
                }
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseJoin(result);
                }
                if ((result == null) || (!result)) {
                    continue;
                }
            }

            events[countOutputRows] = selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
            keys[countOutputRows] = groupByKeys[countInputRows];
            if (prototype.isSorting()) {
                currentGenerators[countOutputRows] = eventsPerStream;
            }

            countOutputRows++;
        }

        // Resize if some rows were filtered out
        if (countOutputRows != events.length) {
            if (countOutputRows == 0) {
                return null;
            }
            EventBean[] outEvents = new EventBean[countOutputRows];
            System.arraycopy(events, 0, outEvents, 0, countOutputRows);
            events = outEvents;

            if (prototype.isSorting()) {
                Object[] outKeys = new Object[countOutputRows];
                System.arraycopy(keys, 0, outKeys, 0, countOutputRows);
                keys = outKeys;

                EventBean[][] outGens = new EventBean[countOutputRows][];
                System.arraycopy(currentGenerators, 0, outGens, 0, countOutputRows);
                currentGenerators = outGens;
            }
        }

        if (prototype.isSorting()) {
            events = orderByProcessor.sort(events, currentGenerators, keys, isNewData, agentInstanceContext);
        }
        return events;
    }

    public Iterator<EventBean> getIterator(Viewable parent) {
        if (!prototype.isHistoricalOnly()) {
            return obtainIterator(parent);
        }

        aggregationService.clearResults(agentInstanceContext);
        Iterator<EventBean> it = parent.iterator();
        EventBean[] eventsPerStream = new EventBean[1];
        for (; it.hasNext(); ) {
            eventsPerStream[0] = it.next();
            Object groupKey = generateGroupKey(eventsPerStream, true);
            aggregationService.applyEnter(eventsPerStream, groupKey, agentInstanceContext);
        }

        ArrayDeque<EventBean> deque = ResultSetProcessorUtil.iteratorToDeque(obtainIterator(parent));
        aggregationService.clearResults(agentInstanceContext);
        return deque.iterator();
    }

    private Iterator<EventBean> obtainIterator(Viewable parent) {
        if (orderByProcessor == null) {
            return new ResultSetAggregateGroupedIterator(parent.iterator(), this, aggregationService, agentInstanceContext);
        }

        // Pull all parent events, generate order keys
        EventBean[] eventsPerStream = new EventBean[1];
        List<EventBean> outgoingEvents = new ArrayList<EventBean>();
        List<Object> orderKeys = new ArrayList<Object>();

        for (EventBean candidate : parent) {
            eventsPerStream[0] = candidate;

            Object groupKey = generateGroupKey(eventsPerStream, true);
            aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);

            if (prototype.getOptionalHavingNode() != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseNonJoin(candidate);
                }
                Boolean pass = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, true, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseNonJoin(pass);
                }
                if ((pass == null) || (!pass)) {
                    continue;
                }
            }

            outgoingEvents.add(selectExprProcessor.process(eventsPerStream, true, true, agentInstanceContext));

            Object orderKey = orderByProcessor.getSortKey(eventsPerStream, true, agentInstanceContext);
            orderKeys.add(orderKey);
        }

        // sort
        EventBean[] outgoingEventsArr = outgoingEvents.toArray(new EventBean[outgoingEvents.size()]);
        Object[] orderKeysArr = orderKeys.toArray(new Object[orderKeys.size()]);
        EventBean[] orderedEvents = orderByProcessor.sort(outgoingEventsArr, orderKeysArr, agentInstanceContext);

        return new ArrayEventIterator(orderedEvents);
    }

    /**
     * Returns the select expression processor
     *
     * @return select processor.
     */
    public SelectExprProcessor getSelectExprProcessor() {
        return selectExprProcessor;
    }

    /**
     * Returns the having node.
     *
     * @return having expression
     */
    public ExprEvaluator getOptionalHavingNode() {
        return prototype.getOptionalHavingNode();
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        // Generate group-by keys for all events
        Object[] groupByKeys = generateGroupKeys(joinSet, true);
        EventBean[] result = generateOutputEventsJoin(joinSet, groupByKeys, true, true);
        return new ArrayEventIterator(result);
    }

    public void clear() {
        aggregationService.clearResults(agentInstanceContext);
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType) {
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return processOutputLimitedJoinDefault(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return processOutputLimitedJoinAll(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return processOutputLimitedJoinFirst(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            return processOutputLimitedJoinLast(joinEventsSet, generateSynthetic);
        } else {
            throw new IllegalStateException("Unrecognized output limit " + outputLimitLimitType);
        }
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType) {
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return processOutputLimitedViewDefault(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return processOutputLimitedViewAll(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return processOutputLimitedViewFirst(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            return processOutputLimitedViewLast(viewEventsList, generateSynthetic);
        } else {
            throw new IllegalStateException("Unrecognized output limited type " + outputLimitLimitType);
        }
    }

    public void stop() {
        if (outputAllGroupReps != null) {
            outputAllGroupReps.destroy();
        }
        if (outputAllHelper != null) {
            outputAllHelper.destroy();
        }
        if (outputLastHelper != null) {
            outputLastHelper.destroy();
        }
        if (outputFirstHelper != null) {
            outputFirstHelper.destroy();
        }
    }

    public void generateOutputBatchedJoinUnkeyed(Set<MultiKey<EventBean>> outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Collection<EventBean> resultEvents, List<Object> optSortKeys) {
        if (outputEvents == null) {
            return;
        }

        EventBean[] eventsPerStream;

        int count = 0;
        for (MultiKey<EventBean> row : outputEvents) {
            aggregationService.setCurrentAccess(groupByKeys[count], agentInstanceContext.getAgentInstanceId(), null);
            eventsPerStream = row.getArray();

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);
                }
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseJoin(result);
                }
                if ((result == null) || (!result)) {
                    continue;
                }
            }

            resultEvents.add(selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));
            if (prototype.isSorting()) {
                optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
            }

            count++;
        }
    }

    public EventBean generateOutputBatchedSingle(Object groupByKey, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize) {
        aggregationService.setCurrentAccess(groupByKey, agentInstanceContext.getAgentInstanceId(), null);

        // Filter the having clause
        if (prototype.getOptionalHavingNode() != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);
            }
            Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aHavingClauseJoin(result);
            }
            if ((result == null) || (!result)) {
                return null;
            }
        }

        return selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
    }

    public void generateOutputBatchedViewPerKey(EventBean[] outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Map<Object, EventBean> resultEvents, Map<Object, Object> optSortKeys) {
        if (outputEvents == null) {
            return;
        }

        EventBean[] eventsPerStream = new EventBean[1];

        int count = 0;
        for (int i = 0; i < outputEvents.length; i++) {
            Object groupKey = groupByKeys[count];
            aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);
            eventsPerStream[0] = outputEvents[count];

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseNonJoin(outputEvents[count]);
                }
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseNonJoin(result);
                }
                if ((result == null) || (!result)) {
                    continue;
                }
            }

            resultEvents.put(groupKey, selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));
            if (prototype.isSorting()) {
                optSortKeys.put(groupKey, orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
            }

            count++;
        }
    }

    public void generateOutputBatchedJoinPerKey(Set<MultiKey<EventBean>> outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Map<Object, EventBean> resultEvents, Map<Object, Object> optSortKeys) {
        if (outputEvents == null) {
            return;
        }

        int count = 0;
        for (MultiKey<EventBean> row : outputEvents) {
            Object groupKey = groupByKeys[count];
            aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseJoin(row.getArray());
                }
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(row.getArray(), isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseJoin(result);
                }
                if ((result == null) || (!result)) {
                    continue;
                }
            }

            resultEvents.put(groupKey, selectExprProcessor.process(row.getArray(), isNewData, isSynthesize, agentInstanceContext));
            if (prototype.isSorting()) {
                optSortKeys.put(groupKey, orderByProcessor.getSortKey(row.getArray(), isNewData, agentInstanceContext));
            }

            count++;
        }
    }

    public boolean hasAggregation() {
        return true;
    }

    public void removed(Object key) {
        if (outputAllGroupReps != null) {
            outputAllGroupReps.remove(key);
        }
        if (outputAllHelper != null) {
            outputAllHelper.remove(key);
        }
        if (outputLastHelper != null) {
            outputLastHelper.remove(key);
        }
        if (outputFirstHelper != null) {
            outputFirstHelper.remove(key);
        }
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllHelper.processView(newData, oldData, isGenerateSynthetic);
        } else {
            outputLastHelper.processView(newData, oldData, isGenerateSynthetic);
        }
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newData, Set<MultiKey<EventBean>> oldData, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllHelper.processJoin(newData, oldData, isGenerateSynthetic);
        } else {
            outputLastHelper.processJoin(newData, oldData, isGenerateSynthetic);
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

    private UniformPair<EventBean[]> processOutputLimitedJoinLast(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        Map<Object, EventBean> lastPerGroupNew = new LinkedHashMap<Object, EventBean>();
        Map<Object, EventBean> lastPerGroupOld = null;
        if (prototype.isSelectRStream()) {
            lastPerGroupOld = new LinkedHashMap<Object, EventBean>();
        }

        Map<Object, Object> newEventsSortKey = null; // group key to sort key
        Map<Object, Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedHashMap<Object, Object>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedHashMap<Object, Object>();
            }
        }

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeys(newData, true);
            Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

            if (prototype.isUnidirectional()) {
                this.clear();
            }

            if (newData != null) {
                // apply new data to aggregates
                int count = 0;
                for (MultiKey<EventBean> aNewData : newData) {
                    Object mk = newDataMultiKey[count];
                    aggregationService.applyEnter(aNewData.getArray(), mk, agentInstanceContext);
                    count++;
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                int count = 0;
                for (MultiKey<EventBean> anOldData : oldData) {
                    aggregationService.applyLeave(anOldData.getArray(), oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            if (prototype.isSelectRStream()) {
                generateOutputBatchedJoinPerKey(oldData, oldDataMultiKey, false, generateSynthetic, lastPerGroupOld, oldEventsSortKey);
            }
            generateOutputBatchedJoinPerKey(newData, newDataMultiKey, false, generateSynthetic, lastPerGroupNew, newEventsSortKey);
        }

        EventBean[] newEventsArr = (lastPerGroupNew.isEmpty()) ? null : lastPerGroupNew.values().toArray(new EventBean[lastPerGroupNew.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream()) {
            oldEventsArr = (lastPerGroupOld.isEmpty()) ? null : lastPerGroupOld.values().toArray(new EventBean[lastPerGroupOld.size()]);
        }

        if (orderByProcessor != null) {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.values().toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream()) {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.values().toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinFirst(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        List<EventBean> resultNewEvents = new ArrayList<EventBean>();
        List<Object> resultNewSortKeys = null;
        List<EventBean> resultOldEvents = null;
        List<Object> resultOldSortKeys = null;

        if (orderByProcessor != null) {
            resultNewSortKeys = new ArrayList<Object>();
        }
        if (prototype.isSelectRStream()) {
            resultOldEvents = new ArrayList<EventBean>();
            resultOldSortKeys = new ArrayList<Object>();
        }

        workCollection.clear();

        if (prototype.getOptionalHavingNode() == null) {
            for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
                Set<MultiKey<EventBean>> newData = pair.getFirst();
                Set<MultiKey<EventBean>> oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeys(newData, true);
                Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

                if (newData != null) {
                    // apply new data to aggregates
                    int count = 0;
                    for (MultiKey<EventBean> aNewData : newData) {
                        Object mk = newDataMultiKey[count];
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            workCollection.put(mk, aNewData.getArray());
                        }
                        aggregationService.applyEnter(aNewData.getArray(), mk, agentInstanceContext);
                        count++;
                    }
                }

                if (oldData != null) {
                    // apply new data to aggregates
                    int count = 0;
                    for (MultiKey<EventBean> aOldData : oldData) {
                        Object mk = oldDataMultiKey[count];
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            workCollection.put(mk, aOldData.getArray());
                        }
                        aggregationService.applyLeave(aOldData.getArray(), mk, agentInstanceContext);
                        count++;
                    }
                }

                // there is no remove stream currently for output first
                generateOutputBatchedArr(workCollection, false, generateSynthetic, resultNewEvents, resultNewSortKeys);
            }
        } else {
            // there is a having-clause, apply after aggregations
            for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
                Set<MultiKey<EventBean>> newData = pair.getFirst();
                Set<MultiKey<EventBean>> oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeys(newData, true);
                Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

                if (newData != null) {
                    // apply new data to aggregates
                    int count = 0;
                    for (MultiKey<EventBean> aNewData : newData) {
                        Object mk = newDataMultiKey[count];
                        aggregationService.applyEnter(aNewData.getArray(), mk, agentInstanceContext);
                        count++;
                    }
                }

                if (oldData != null) {
                    int count = 0;
                    for (MultiKey<EventBean> aOldData : oldData) {
                        Object mk = oldDataMultiKey[count];
                        aggregationService.applyLeave(aOldData.getArray(), mk, agentInstanceContext);
                        count++;
                    }
                }

                if (newData != null) {
                    // check having clause and first-condition
                    int count = 0;
                    for (MultiKey<EventBean> aNewData : newData) {
                        Object mk = newDataMultiKey[count];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().qHavingClauseJoin(aNewData.getArray());
                        }
                        Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(aNewData.getArray(), true, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aHavingClauseJoin(result);
                        }
                        if ((result == null) || (!result)) {
                            count++;
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            workCollection.put(mk, aNewData.getArray());
                        }
                        count++;
                    }
                }

                if (oldData != null) {
                    // apply new data to aggregates
                    int count = 0;
                    for (MultiKey<EventBean> aOldData : oldData) {
                        Object mk = oldDataMultiKey[count];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().qHavingClauseJoin(aOldData.getArray());
                        }
                        Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(aOldData.getArray(), true, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aHavingClauseJoin(result);
                        }
                        if ((result == null) || (!result)) {
                            count++;
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            workCollection.put(mk, aOldData.getArray());
                        }
                    }
                }

                // there is no remove stream currently for output first
                generateOutputBatchedArr(workCollection, false, generateSynthetic, resultNewEvents, resultNewSortKeys);
            }
        }

        EventBean[] newEventsArr = null;
        EventBean[] oldEventsArr = null;
        if (!resultNewEvents.isEmpty()) {
            newEventsArr = resultNewEvents.toArray(new EventBean[resultNewEvents.size()]);
        }
        if ((resultOldEvents != null) && (!resultOldEvents.isEmpty())) {
            oldEventsArr = resultOldEvents.toArray(new EventBean[resultOldEvents.size()]);
        }

        if (orderByProcessor != null) {
            Object[] sortKeysNew = (resultNewSortKeys.isEmpty()) ? null : resultNewSortKeys.toArray(new Object[resultNewSortKeys.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream()) {
                Object[] sortKeysOld = (resultOldSortKeys.isEmpty()) ? null : resultOldSortKeys.toArray(new Object[resultOldSortKeys.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinAll(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        workCollection.clear();

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeys(newData, true);
            Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

            if (prototype.isUnidirectional()) {
                this.clear();
            }

            if (newData != null) {
                // apply new data to aggregates
                int count = 0;
                for (MultiKey<EventBean> aNewData : newData) {
                    Object mk = newDataMultiKey[count];
                    aggregationService.applyEnter(aNewData.getArray(), mk, agentInstanceContext);
                    count++;

                    // keep the new event as a representative for the group
                    workCollection.put(mk, aNewData.getArray());
                    outputAllGroupReps.put(mk, aNewData.getArray());
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                int count = 0;
                for (MultiKey<EventBean> anOldData : oldData) {
                    aggregationService.applyLeave(anOldData.getArray(), oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            if (prototype.isSelectRStream()) {
                generateOutputBatchedJoinUnkeyed(oldData, oldDataMultiKey, false, generateSynthetic, oldEvents, oldEventsSortKey);
            }
            generateOutputBatchedJoinUnkeyed(newData, newDataMultiKey, true, generateSynthetic, newEvents, newEventsSortKey);
        }

        // For any group representatives not in the work collection, generate a row
        Iterator<Map.Entry<Object, EventBean[]>> entryIterator = outputAllGroupReps.entryIterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Object, EventBean[]> entry = entryIterator.next();
            if (!workCollection.containsKey(entry.getKey())) {
                workCollectionTwo.put(entry.getKey(), entry.getValue());
                generateOutputBatchedArr(workCollectionTwo, true, generateSynthetic, newEvents, newEventsSortKey);
                workCollectionTwo.clear();
            }
        }

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

    private UniformPair<EventBean[]> processOutputLimitedJoinDefault(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeys(newData, true);
            Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

            if (prototype.isUnidirectional()) {
                this.clear();
            }

            if (newData != null) {
                // apply new data to aggregates
                int count = 0;
                for (MultiKey<EventBean> aNewData : newData) {
                    aggregationService.applyEnter(aNewData.getArray(), newDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                int count = 0;
                for (MultiKey<EventBean> anOldData : oldData) {
                    aggregationService.applyLeave(anOldData.getArray(), oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            if (prototype.isSelectRStream()) {
                generateOutputBatchedJoinUnkeyed(oldData, oldDataMultiKey, false, generateSynthetic, oldEvents, oldEventsSortKey);
            }
            generateOutputBatchedJoinUnkeyed(newData, newDataMultiKey, true, generateSynthetic, newEvents, newEventsSortKey);
        }

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

    private UniformPair<EventBean[]> processOutputLimitedViewLast(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        Map<Object, EventBean> lastPerGroupNew = new LinkedHashMap<Object, EventBean>();
        Map<Object, EventBean> lastPerGroupOld = null;
        if (prototype.isSelectRStream()) {
            lastPerGroupOld = new LinkedHashMap<Object, EventBean>();
        }

        Map<Object, Object> newEventsSortKey = null; // group key to sort key
        Map<Object, Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedHashMap<Object, Object>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedHashMap<Object, Object>();
            }
        }

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeys(newData, true);
            Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

            if (newData != null) {
                // apply new data to aggregates
                int count = 0;
                for (EventBean aNewData : newData) {
                    Object mk = newDataMultiKey[count];
                    eventsPerStreamOneStream[0] = aNewData;
                    aggregationService.applyEnter(eventsPerStreamOneStream, mk, agentInstanceContext);
                    count++;
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                int count = 0;
                for (EventBean anOldData : oldData) {
                    eventsPerStreamOneStream[0] = anOldData;
                    aggregationService.applyLeave(eventsPerStreamOneStream, oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            if (prototype.isSelectRStream()) {
                generateOutputBatchedViewPerKey(oldData, oldDataMultiKey, false, generateSynthetic, lastPerGroupOld, oldEventsSortKey);
            }
            generateOutputBatchedViewPerKey(newData, newDataMultiKey, false, generateSynthetic, lastPerGroupNew, newEventsSortKey);
        }

        EventBean[] newEventsArr = (lastPerGroupNew.isEmpty()) ? null : lastPerGroupNew.values().toArray(new EventBean[lastPerGroupNew.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream()) {
            oldEventsArr = (lastPerGroupOld.isEmpty()) ? null : lastPerGroupOld.values().toArray(new EventBean[lastPerGroupOld.size()]);
        }

        if (orderByProcessor != null) {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.values().toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream()) {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.values().toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedViewFirst(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        List<EventBean> resultNewEvents = new ArrayList<EventBean>();
        List<Object> resultNewSortKeys = null;
        List<EventBean> resultOldEvents = null;
        List<Object> resultOldSortKeys = null;

        if (orderByProcessor != null) {
            resultNewSortKeys = new ArrayList<Object>();
        }
        if (prototype.isSelectRStream()) {
            resultOldEvents = new ArrayList<EventBean>();
            resultOldSortKeys = new ArrayList<Object>();
        }

        workCollection.clear();
        if (prototype.getOptionalHavingNode() == null) {
            for (UniformPair<EventBean[]> pair : viewEventsList) {
                EventBean[] newData = pair.getFirst();
                EventBean[] oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeys(newData, true);
                Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

                if (newData != null) {
                    // apply new data to aggregates
                    for (int i = 0; i < newData.length; i++) {
                        eventsPerStreamOneStream[0] = newData[i];
                        Object mk = newDataMultiKey[i];
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            workCollection.put(mk, new EventBean[]{newData[i]});
                        }
                        aggregationService.applyEnter(eventsPerStreamOneStream, mk, agentInstanceContext);
                    }
                }

                if (oldData != null) {
                    // apply new data to aggregates
                    for (int i = 0; i < oldData.length; i++) {
                        eventsPerStreamOneStream[0] = oldData[i];
                        Object mk = oldDataMultiKey[i];
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            workCollection.put(mk, new EventBean[]{oldData[i]});
                        }
                        aggregationService.applyLeave(eventsPerStreamOneStream, mk, agentInstanceContext);
                    }
                }

                // there is no remove stream currently for output first
                generateOutputBatchedArr(workCollection, false, generateSynthetic, resultNewEvents, resultNewSortKeys);
            }
        } else {  // has a having-clause
            for (UniformPair<EventBean[]> pair : viewEventsList) {
                EventBean[] newData = pair.getFirst();
                EventBean[] oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeys(newData, true);
                Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

                if (newData != null) {
                    // apply new data to aggregates
                    for (int i = 0; i < newData.length; i++) {
                        eventsPerStreamOneStream[0] = newData[i];
                        Object mk = newDataMultiKey[i];
                        aggregationService.applyEnter(eventsPerStreamOneStream, mk, agentInstanceContext);
                    }
                }

                if (oldData != null) {
                    for (int i = 0; i < oldData.length; i++) {
                        eventsPerStreamOneStream[0] = oldData[i];
                        Object mk = oldDataMultiKey[i];
                        aggregationService.applyLeave(eventsPerStreamOneStream, mk, agentInstanceContext);
                    }
                }

                if (newData != null) {
                    // check having clause and first-condition
                    for (int i = 0; i < newData.length; i++) {
                        eventsPerStreamOneStream[0] = newData[i];
                        Object mk = newDataMultiKey[i];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().qHavingClauseNonJoin(newData[i]);
                        }
                        Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStreamOneStream, true, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aHavingClauseNonJoin(result);
                        }
                        if ((result == null) || (!result)) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            workCollection.put(mk, new EventBean[]{newData[i]});
                        }
                    }
                }

                if (oldData != null) {
                    // apply new data to aggregates
                    for (int i = 0; i < oldData.length; i++) {
                        eventsPerStreamOneStream[0] = oldData[i];
                        Object mk = oldDataMultiKey[i];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().qHavingClauseNonJoin(oldData[i]);
                        }
                        Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStreamOneStream, true, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aHavingClauseNonJoin(result);
                        }
                        if ((result == null) || (!result)) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            workCollection.put(mk, new EventBean[]{oldData[i]});
                        }
                    }
                }

                // there is no remove stream currently for output first
                generateOutputBatchedArr(workCollection, false, generateSynthetic, resultNewEvents, resultNewSortKeys);
            }
        }

        EventBean[] newEventsArr = null;
        EventBean[] oldEventsArr = null;
        if (!resultNewEvents.isEmpty()) {
            newEventsArr = resultNewEvents.toArray(new EventBean[resultNewEvents.size()]);
        }
        if ((resultOldEvents != null) && (!resultOldEvents.isEmpty())) {
            oldEventsArr = resultOldEvents.toArray(new EventBean[resultOldEvents.size()]);
        }

        if (orderByProcessor != null) {
            Object[] sortKeysNew = (resultNewSortKeys.isEmpty()) ? null : resultNewSortKeys.toArray(new Object[resultNewSortKeys.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream()) {
                Object[] sortKeysOld = (resultOldSortKeys.isEmpty()) ? null : resultOldSortKeys.toArray(new Object[resultOldSortKeys.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedViewAll(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        workCollection.clear();

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeys(newData, true);
            Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

            EventBean[] eventsPerStream = new EventBean[1];
            if (newData != null) {
                // apply new data to aggregates
                int count = 0;
                for (EventBean aNewData : newData) {
                    Object mk = newDataMultiKey[count];
                    eventsPerStream[0] = aNewData;
                    aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                    count++;

                    // keep the new event as a representative for the group
                    workCollection.put(mk, eventsPerStream);
                    outputAllGroupReps.put(mk, new EventBean[]{aNewData});
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                int count = 0;
                for (EventBean anOldData : oldData) {
                    eventsPerStream[0] = anOldData;
                    aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            if (prototype.isSelectRStream()) {
                generateOutputBatchedViewUnkeyed(oldData, oldDataMultiKey, false, generateSynthetic, oldEvents, oldEventsSortKey);
            }
            generateOutputBatchedViewUnkeyed(newData, newDataMultiKey, true, generateSynthetic, newEvents, newEventsSortKey);
        }

        // For any group representatives not in the work collection, generate a row
        Iterator<Map.Entry<Object, EventBean[]>> entryIterator = outputAllGroupReps.entryIterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Object, EventBean[]> entry = entryIterator.next();
            if (!workCollection.containsKey(entry.getKey())) {
                workCollectionTwo.put(entry.getKey(), entry.getValue());
                generateOutputBatchedArr(workCollectionTwo, true, generateSynthetic, newEvents, newEventsSortKey);
                workCollectionTwo.clear();
            }
        }

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

    private UniformPair<EventBean[]> processOutputLimitedViewDefault(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeys(newData, true);
            Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

            if (newData != null) {
                // apply new data to aggregates
                int count = 0;
                for (EventBean aNewData : newData) {
                    eventsPerStreamOneStream[0] = aNewData;
                    aggregationService.applyEnter(eventsPerStreamOneStream, newDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                int count = 0;
                for (EventBean anOldData : oldData) {
                    eventsPerStreamOneStream[0] = anOldData;
                    aggregationService.applyLeave(eventsPerStreamOneStream, oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            if (prototype.isSelectRStream()) {
                generateOutputBatchedViewUnkeyed(oldData, oldDataMultiKey, false, generateSynthetic, oldEvents, oldEventsSortKey);
            }
            generateOutputBatchedViewUnkeyed(newData, newDataMultiKey, true, generateSynthetic, newEvents, newEventsSortKey);
        }

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

    private void generateOutputBatchedArr(Map<Object, EventBean[]> keysAndEvents, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys) {
        for (Map.Entry<Object, EventBean[]> entry : keysAndEvents.entrySet()) {
            EventBean[] eventsPerStream = entry.getValue();

            // Set the current row of aggregation states
            aggregationService.setCurrentAccess(entry.getKey(), agentInstanceContext.getAgentInstanceId(), null);

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseJoin(entry.getValue());
                }
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseJoin(result);
                }
                if ((result == null) || (!result)) {
                    continue;
                }
            }

            resultEvents.add(selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));

            if (prototype.isSorting()) {
                optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
            }
        }
    }

    public void generateOutputBatchedViewUnkeyed(EventBean[] outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Collection<EventBean> resultEvents, List<Object> optSortKeys) {
        if (outputEvents == null) {
            return;
        }

        EventBean[] eventsPerStream = new EventBean[1];

        int count = 0;
        for (int i = 0; i < outputEvents.length; i++) {
            aggregationService.setCurrentAccess(groupByKeys[count], agentInstanceContext.getAgentInstanceId(), null);
            eventsPerStream[0] = outputEvents[count];

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseNonJoin(outputEvents[count]);
                }
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseNonJoin(result);
                }
                if ((result == null) || (!result)) {
                    continue;
                }
            }

            resultEvents.add(selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));
            if (prototype.isSorting()) {
                optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
            }

            count++;
        }
    }
}
