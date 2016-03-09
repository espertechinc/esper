/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
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
 * Result set processor for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 * <p>
 * Produces one row for each group that changed (and not one row per event). Computes MultiKey group-by keys for
 * each event and uses a set of the group-by keys to generate the result rows, using the first (old or new, anyone) event
 * for each distinct group-by key.
 */
public class ResultSetProcessorRowPerGroup implements ResultSetProcessor, AggregationRowRemovedCallback {
    protected final ResultSetProcessorRowPerGroupFactory prototype;
    protected final SelectExprProcessor selectExprProcessor;
    protected final OrderByProcessor orderByProcessor;
    protected final AggregationService aggregationService;
    protected AgentInstanceContext agentInstanceContext;

    // For output rate limiting, keep a representative event for each group for
    // representing each group in an output limit clause
    protected ResultSetProcessorGroupedOutputAllGroupReps outputAllGroupReps;

    private ResultSetProcessorGroupedOutputFirstHelper outputFirstHelper;
    private ResultSetProcessorRowPerGroupOutputLastHelper outputLastHelper;
    private ResultSetProcessorRowPerGroupOutputAllHelper outputAllHelper;

    public ResultSetProcessorRowPerGroup(ResultSetProcessorRowPerGroupFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.agentInstanceContext = agentInstanceContext;

        aggregationService.setRemovedCallback(this);

        if (prototype.isOutputLast()) {
            outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupOutputLastOpt(agentInstanceContext, this, prototype);
        }
        else if (prototype.isOutputAll()) {
            if (!prototype.isEnableOutputLimitOpt()) {
                outputAllGroupReps = prototype.getResultSetProcessorHelperFactory().makeRSGroupedOutputAllNoOpt(agentInstanceContext, prototype.getGroupKeyNodes(), prototype.getNumStreams());
            }
            else {
                outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupOutputAllOpt(agentInstanceContext, this, prototype);
            }
        }
        else if (prototype.isOutputFirst()) {
            outputFirstHelper = prototype.getResultSetProcessorHelperFactory().makeRSGroupedOutputFirst(agentInstanceContext, prototype.getGroupKeyNodes(), prototype.getOptionalOutputFirstConditionFactory(), null, -1);
        }
    }

    public void setAgentInstanceContext(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    public EventType getResultEventType()
    {
        return prototype.getResultEventType();
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

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qResultSetProcessGroupedRowPerGroup();}
        // Generate group-by keys for all events, collect all keys in a set for later event generation
        Map<Object, EventBean[]> keysAndEvents = new HashMap<Object, EventBean[]>();
        Object[] newDataMultiKey = generateGroupKeys(newEvents, keysAndEvents, true);
        Object[] oldDataMultiKey = generateGroupKeys(oldEvents, keysAndEvents, false);

        if (prototype.isUnidirectional())
        {
            this.clear();
        }

        // generate old events
        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream())
        {
            selectOldEvents = generateOutputEventsJoin(keysAndEvents, false, isSynthesize);
        }

        // update aggregates
        if (!newEvents.isEmpty())
        {
            // apply old data to aggregates
            int count = 0;
            for (MultiKey<EventBean> eventsPerStream : newEvents)
            {
                aggregationService.applyEnter(eventsPerStream.getArray(), newDataMultiKey[count], agentInstanceContext);
                count++;
            }
        }
        if (oldEvents != null && !oldEvents.isEmpty())
        {
            // apply old data to aggregates
            int count = 0;
            for (MultiKey<EventBean> eventsPerStream : oldEvents)
            {
                aggregationService.applyLeave(eventsPerStream.getArray(), oldDataMultiKey[count], agentInstanceContext);
                count++;
            }
        }

        // generate new events using select expressions
        EventBean[] selectNewEvents = generateOutputEventsJoin(keysAndEvents, true, isSynthesize);

        if ((selectNewEvents != null) || (selectOldEvents != null))
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(selectNewEvents, selectOldEvents);}
            return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(null, null);}
        return null;
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qResultSetProcessGroupedRowPerGroup();}
        // Generate group-by keys for all events, collect all keys in a set for later event generation
        Map<Object, EventBean> keysAndEvents = new HashMap<Object, EventBean>();

        Object[] newDataMultiKey = generateGroupKeys(newData, keysAndEvents, true);
        Object[] oldDataMultiKey = generateGroupKeys(oldData, keysAndEvents, false);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream())
        {
            selectOldEvents = generateOutputEventsView(keysAndEvents, false, isSynthesize);
        }

        // update aggregates
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null)
        {
            // apply new data to aggregates
            for (int i = 0; i < newData.length; i++)
            {
                eventsPerStream[0] = newData[i];
                aggregationService.applyEnter(eventsPerStream, newDataMultiKey[i], agentInstanceContext);
            }
        }
        if (oldData != null)
        {
            // apply old data to aggregates
            for (int i = 0; i < oldData.length; i++)
            {
                eventsPerStream[0] = oldData[i];
                aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[i], agentInstanceContext);
            }
        }

        // generate new events using select expressions
        EventBean[] selectNewEvents = generateOutputEventsView(keysAndEvents, true, isSynthesize);

        if ((selectNewEvents != null) || (selectOldEvents != null))
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(selectNewEvents, selectOldEvents);}
            return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(null, null);}
        return null;
    }

    protected EventBean[] generateOutputEventsView(Map<Object, EventBean> keysAndEvents, boolean isNewData, boolean isSynthesize)
    {
        EventBean[] eventsPerStream = new EventBean[1];
        EventBean[] events = new EventBean[keysAndEvents.size()];
        Object[] keys = new Object[keysAndEvents.size()];
        EventBean[][] currentGenerators = null;
        if(prototype.isSorting())
        {
            currentGenerators = new EventBean[keysAndEvents.size()][];
        }

        int count = 0;
        for (Map.Entry<Object, EventBean> entry : keysAndEvents.entrySet())
        {
            // Set the current row of aggregation states
            aggregationService.setCurrentAccess(entry.getKey(), agentInstanceContext.getAgentInstanceId(), null);

            eventsPerStream[0] = entry.getValue();

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null)
            {
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseNonJoin(entry.getValue());}
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseNonJoin(result);}
                if ((result == null) || (!result))
                {
                    continue;
                }
            }

            events[count] = selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
            keys[count] = entry.getKey();
            if(prototype.isSorting())
            {
                EventBean[] currentEventsPerStream = new EventBean[] { entry.getValue() };
                currentGenerators[count] = currentEventsPerStream;
            }

            count++;
        }

        // Resize if some rows were filtered out
        if (count != events.length)
        {
            if (count == 0)
            {
                return null;
            }
            EventBean[] outEvents = new EventBean[count];
            System.arraycopy(events, 0, outEvents, 0, count);
            events = outEvents;

            if(prototype.isSorting())
            {
                Object[] outKeys = new Object[count];
                System.arraycopy(keys, 0, outKeys, 0, count);
                keys = outKeys;

                EventBean[][] outGens = new EventBean[count][];
                System.arraycopy(currentGenerators, 0, outGens, 0, count);
                currentGenerators = outGens;
            }
        }

        if(prototype.isSorting())
        {
            events = orderByProcessor.sort(events, currentGenerators, keys, isNewData, agentInstanceContext);
        }

        return events;
    }

    private void generateOutputBatchedRow(Map<Object, EventBean> keysAndEvents, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys, AgentInstanceContext agentInstanceContext)
    {
        EventBean[] eventsPerStream = new EventBean[1];

        for (Map.Entry<Object, EventBean> entry : keysAndEvents.entrySet())
        {
            // Set the current row of aggregation states
            aggregationService.setCurrentAccess(entry.getKey(), agentInstanceContext.getAgentInstanceId(), null);

            eventsPerStream[0] = entry.getValue();

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null)
            {
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseNonJoin(entry.getValue());}
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseNonJoin(result);}
                if ((result == null) || (!result))
                {
                    continue;
                }
            }

            resultEvents.add(selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));

            if(prototype.isSorting())
            {
                optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
            }
        }
    }

    public void generateOutputBatchedArr(boolean join, Iterator<Map.Entry<Object, EventBean[]>> keysAndEvents, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys)
    {
        while (keysAndEvents.hasNext()) {
            Map.Entry<Object, EventBean[]> entry = keysAndEvents.next();
            generateOutputBatchedRow(join, entry.getKey(), entry.getValue(), isNewData, isSynthesize, resultEvents, optSortKeys);
        }
    }

    private void generateOutputBatchedRow(boolean join, Object mk, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys)
    {
        // Set the current row of aggregation states
        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

        // Filter the having clause
        if (prototype.getOptionalHavingNode() != null)
        {
            if (InstrumentationHelper.ENABLED) { if (!join) InstrumentationHelper.get().qHavingClauseNonJoin(eventsPerStream[0]); else InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);}
            Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
            if (InstrumentationHelper.ENABLED) { if (!join) InstrumentationHelper.get().aHavingClauseNonJoin(result); else InstrumentationHelper.get().aHavingClauseJoin(result);}
            if ((result == null) || (!result))
            {
                return;
            }
        }

        resultEvents.add(selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));

        if(prototype.isSorting())
        {
            optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
        }
    }

    public EventBean generateOutputBatchedNoSortWMap(boolean join, Object mk, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize)
    {
        // Set the current row of aggregation states
        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

        // Filter the having clause
        if (prototype.getOptionalHavingNode() != null)
        {
            if (InstrumentationHelper.ENABLED) { if (!join) InstrumentationHelper.get().qHavingClauseNonJoin(eventsPerStream[0]); else InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);}
            Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
            if (InstrumentationHelper.ENABLED) { if (!join) InstrumentationHelper.get().aHavingClauseNonJoin(result); else InstrumentationHelper.get().aHavingClauseJoin(result);}
            if ((result == null) || (!result)) {
                return null;
            }
        }

        return selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
    }

    private EventBean[] generateOutputEventsJoin(Map<Object, EventBean[]> keysAndEvents, boolean isNewData, boolean isSynthesize)
    {
        EventBean[] events = new EventBean[keysAndEvents.size()];
        Object[] keys = new Object[keysAndEvents.size()];
        EventBean[][] currentGenerators = null;
        if(prototype.isSorting())
        {
            currentGenerators = new EventBean[keysAndEvents.size()][];
        }

        int count = 0;
        for (Map.Entry<Object, EventBean[]> entry : keysAndEvents.entrySet())
        {
            aggregationService.setCurrentAccess(entry.getKey(), agentInstanceContext.getAgentInstanceId(), null);
            EventBean[] eventsPerStream = entry.getValue();

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null)
            {
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);}
                Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseJoin(result);}
                if ((result == null) || (!result))
                {
                    continue;
                }
            }

            events[count] = selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
            keys[count] = entry.getKey();
            if(prototype.isSorting())
            {
                currentGenerators[count] = eventsPerStream;
            }

            count++;
        }

        // Resize if some rows were filtered out
        if (count != events.length)
        {
            if (count == 0)
            {
                return null;
            }
            EventBean[] outEvents = new EventBean[count];
            System.arraycopy(events, 0, outEvents, 0, count);
            events = outEvents;

            if(prototype.isSorting())
            {
                Object[] outKeys = new Object[count];
                System.arraycopy(keys, 0, outKeys, 0, count);
                keys = outKeys;

                EventBean[][] outGens = new EventBean[count][];
                System.arraycopy(currentGenerators, 0, outGens, 0, count);
                currentGenerators = outGens;
            }
        }

        if(prototype.isSorting())
        {
            events =  orderByProcessor.sort(events, currentGenerators, keys, isNewData, agentInstanceContext);
        }

        return events;
    }

    private Object[] generateGroupKeys(EventBean[] events, boolean isNewData)
    {
        if (events == null)
        {
            return null;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        Object keys[] = new Object[events.length];

        for (int i = 0; i < events.length; i++)
        {
            eventsPerStream[0] = events[i];
            keys[i] = generateGroupKey(eventsPerStream, isNewData);
        }

        return keys;
    }

    protected Object[] generateGroupKeys(EventBean[] events, Map<Object, EventBean> eventPerKey, boolean isNewData)
    {
        if (events == null) {
            return null;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        Object keys[] = new Object[events.length];

        for (int i = 0; i < events.length; i++)
        {
            eventsPerStream[0] = events[i];
            keys[i] = generateGroupKey(eventsPerStream, isNewData);
            eventPerKey.put(keys[i], events[i]);
        }

        return keys;
    }

    private Object[] generateGroupKeys(Set<MultiKey<EventBean>> resultSet, Map<Object, EventBean[]> eventPerKey, boolean isNewData)
    {
        if (resultSet == null || resultSet.isEmpty())
        {
            return null;
        }

        Object keys[] = new Object[resultSet.size()];

        int count = 0;
        for (MultiKey<EventBean> eventsPerStream : resultSet)
        {
            keys[count] = generateGroupKey(eventsPerStream.getArray(), isNewData);
            eventPerKey.put(keys[count], eventsPerStream.getArray());

            count++;
        }

        return keys;
    }

    /**
     * Returns the optional having expression.
     * @return having expression node
     */
    public ExprEvaluator getOptionalHavingNode()
    {
        return prototype.getOptionalHavingNode();
    }

    /**
     * Returns the select expression processor
     * @return select processor.
     */
    public SelectExprProcessor getSelectExprProcessor()
    {
        return selectExprProcessor;
    }

    public Iterator<EventBean> getIterator(Viewable parent)
    {
        if (!prototype.isHistoricalOnly()) {
            return obtainIterator(parent);
        }

        aggregationService.clearResults(agentInstanceContext);
        Iterator<EventBean> it = parent.iterator();
        EventBean[] eventsPerStream = new EventBean[1];
        for (;it.hasNext();) {
            eventsPerStream[0] = it.next();
            Object groupKey = generateGroupKey(eventsPerStream, true);
            aggregationService.applyEnter(eventsPerStream, groupKey, agentInstanceContext);
        }

        ArrayDeque<EventBean> deque = ResultSetProcessorUtil.iteratorToDeque(obtainIterator(parent));
        aggregationService.clearResults(agentInstanceContext);
        return deque.iterator();
    }

    public Iterator<EventBean> obtainIterator(Viewable parent)
    {
        if (orderByProcessor == null)
        {
            return new ResultSetRowPerGroupIterator(parent.iterator(), this, aggregationService, agentInstanceContext);
        }
        return getIteratorSorted(parent.iterator());
    }

    protected Iterator<EventBean> getIteratorSorted(Iterator<EventBean> parentIter) {

        // Pull all parent events, generate order keys
        EventBean[] eventsPerStream = new EventBean[1];
        List<EventBean> outgoingEvents = new ArrayList<EventBean>();
        List<Object> orderKeys = new ArrayList<Object>();
        Set<Object> priorSeenGroups = new HashSet<Object>();

        for (;parentIter.hasNext();) {
            EventBean candidate = parentIter.next();
            eventsPerStream[0] = candidate;

            Object groupKey = generateGroupKey(eventsPerStream, true);
            aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);

            if (prototype.getOptionalHavingNode() != null)
            {
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseNonJoin(candidate);}
                Boolean pass = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, true, agentInstanceContext);
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseNonJoin(pass);}
                if ((pass == null) || (!pass)) {
                    continue;
                }
            }
            if (priorSeenGroups.contains(groupKey))
            {
                continue;
            }
            priorSeenGroups.add(groupKey);

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

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet)
    {
        Map<Object, EventBean[]> keysAndEvents = new HashMap<Object, EventBean[]>();
        generateGroupKeys(joinSet, keysAndEvents, true);
        EventBean[] selectNewEvents = generateOutputEventsJoin(keysAndEvents, true, true);
        return new ArrayEventIterator(selectNewEvents);
    }

    public void clear()
    {
        aggregationService.clearResults(agentInstanceContext);
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType)
    {
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return processOutputLimitedJoinDefault(joinEventsSet, generateSynthetic);
        }
        else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return processOutputLimitedJoinAll(joinEventsSet, generateSynthetic);
        }
        else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return processOutputLimitedJoinFirst(joinEventsSet, generateSynthetic);
        }
        else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            return processOutputLimitedJoinLast(joinEventsSet, generateSynthetic);
        }
        throw new IllegalStateException("Unrecognized output limit type " + outputLimitLimitType);
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType)
    {
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return processOutputLimitedViewDefault(viewEventsList, generateSynthetic);
        }
        else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return processOutputLimitedViewAll(viewEventsList, generateSynthetic);
        }
        else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return processOutputLimitedViewFirst(viewEventsList, generateSynthetic);
        }
        else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            return processOutputLimitedViewLast(viewEventsList, generateSynthetic);
        }
        throw new IllegalStateException("Unrecognized output limit type " + outputLimitLimitType);
    }

    public boolean hasAggregation() {
        return true;
    }

    public void removed(Object key) {
        if (outputAllGroupReps != null) {
            outputAllGroupReps.remove(key);
        }
        if (outputLastHelper != null) {
            outputLastHelper.remove(key);
        }
        if (outputAllGroupReps != null) {
            outputAllGroupReps.remove(key);
        }
        if (outputFirstHelper != null) {
            outputFirstHelper.remove(key);
        }
    }

    public Object generateGroupKey(EventBean[] eventsPerStream, boolean isNewData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessComputeGroupKeys(isNewData, prototype.getGroupKeyNodeExpressions(), eventsPerStream);
            Object keyObject;
            if (prototype.getGroupKeyNode() != null) {
                keyObject = prototype.getGroupKeyNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
            }
            else {
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
        }
        else {
            ExprEvaluator[] evals = prototype.getGroupKeyNodes();
            Object[] keys = new Object[evals.length];
            for (int i = 0; i < evals.length; i++) {
                keys[i] = evals[i].evaluate(eventsPerStream, isNewData, agentInstanceContext);
            }
            return new MultiKeyUntyped(keys);
        }
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllHelper.processView(newData, oldData, isGenerateSynthetic);
        }
        else {
            outputLastHelper.processView(newData, oldData, isGenerateSynthetic);
        }
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newData, Set<MultiKey<EventBean>> oldData, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllHelper.processJoin(newData, oldData, isGenerateSynthetic);
        }
        else {
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

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinLast(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream())
        {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null)
        {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream())
            {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<Object, EventBean[]>();
        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet)
        {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            if (prototype.isUnidirectional())
            {
                this.clear();
            }

            if (newData != null)
            {
                // apply new data to aggregates
                for (MultiKey<EventBean> aNewData : newData)
                {
                    Object mk = generateGroupKey(aNewData.getArray(), true);

                    // if this is a newly encountered group, generate the remove stream event
                    if (groupRepsView.put(mk, aNewData.getArray()) == null)
                    {
                        if (prototype.isSelectRStream())
                        {
                            generateOutputBatchedRow(true, mk, aNewData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }
                    aggregationService.applyEnter(aNewData.getArray(), mk, agentInstanceContext);
                }
            }
            if (oldData != null)
            {
                // apply old data to aggregates
                for (MultiKey<EventBean> anOldData : oldData)
                {
                    Object mk = generateGroupKey(anOldData.getArray(), true);

                    if (groupRepsView.put(mk, anOldData.getArray()) == null)
                    {
                        if (prototype.isSelectRStream())
                        {
                            generateOutputBatchedRow(true, mk, anOldData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }

                    aggregationService.applyLeave(anOldData.getArray(), mk, agentInstanceContext);
                }
            }
        }

        generateOutputBatchedArr(true, groupRepsView.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream())
        {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null)
        {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);

            if (prototype.isSelectRStream())
            {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null))
        {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinFirst(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream())
        {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null)
        {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream())
            {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<Object, EventBean[]>();
        if (prototype.getOptionalHavingNode() == null) {
            for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet)
            {
                Set<MultiKey<EventBean>> newData = pair.getFirst();
                Set<MultiKey<EventBean>> oldData = pair.getSecond();

                if (newData != null)
                {
                    // apply new data to aggregates
                    for (MultiKey<EventBean> aNewData : newData)
                    {
                        Object mk = generateGroupKey(aNewData.getArray(), true);
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            // if this is a newly encountered group, generate the remove stream event
                            if (groupRepsView.put(mk, aNewData.getArray()) == null)
                            {
                                if (prototype.isSelectRStream())
                                {
                                    generateOutputBatchedRow(true, mk, aNewData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                        aggregationService.applyEnter(aNewData.getArray(), mk, agentInstanceContext);
                    }
                }
                if (oldData != null)
                {
                    // apply old data to aggregates
                    for (MultiKey<EventBean> anOldData : oldData)
                    {
                        Object mk = generateGroupKey(anOldData.getArray(), true);
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            if (groupRepsView.put(mk, anOldData.getArray()) == null)
                            {
                                if (prototype.isSelectRStream())
                                {
                                    generateOutputBatchedRow(true, mk, anOldData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }

                        aggregationService.applyLeave(anOldData.getArray(), mk, agentInstanceContext);
                    }
                }
            }
        }
        else {
            groupRepsView.clear();
            for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet)
            {
                Set<MultiKey<EventBean>> newData = pair.getFirst();
                Set<MultiKey<EventBean>> oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeys(newData, true);
                Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

                if (newData != null)
                {
                    // apply new data to aggregates
                    int count = 0;
                    for (MultiKey<EventBean> aNewData : newData)
                    {
                        aggregationService.applyEnter(aNewData.getArray(), newDataMultiKey[count], agentInstanceContext);
                        count++;
                    }
                }
                if (oldData != null)
                {
                    // apply old data to aggregates
                    int count = 0;
                    for (MultiKey<EventBean> anOldData : oldData)
                    {
                        aggregationService.applyLeave(anOldData.getArray(), oldDataMultiKey[count], agentInstanceContext);
                        count++;
                    }
                }

                // evaluate having-clause
                if (newData != null)
                {
                    int count = 0;
                    for (MultiKey<EventBean> aNewData : newData)
                    {
                        Object mk = newDataMultiKey[count];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseJoin(aNewData.getArray());}
                        Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(aNewData.getArray(), true, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseJoin(result);}
                        if ((result == null) || (!result))
                        {
                            count++;
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsView.put(mk, aNewData.getArray()) == null)
                            {
                                if (prototype.isSelectRStream())
                                {
                                    generateOutputBatchedRow(true, mk, aNewData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                        count++;
                    }
                }

                // evaluate having-clause
                if (oldData != null)
                {
                    int count = 0;
                    for (MultiKey<EventBean> anOldData : oldData)
                    {
                        Object mk = oldDataMultiKey[count];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseJoin(anOldData.getArray());}
                        Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(anOldData.getArray(), false, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseJoin(result);}
                        if ((result == null) || (!result))
                        {
                            count++;
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            if (groupRepsView.put(mk, anOldData.getArray()) == null)
                            {
                                if (prototype.isSelectRStream())
                                {
                                    generateOutputBatchedRow(true, mk, anOldData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                        count++;
                    }
                }
            }
        }

        generateOutputBatchedArr(true, groupRepsView.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream())
        {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null)
        {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream())
            {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null))
        {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinAll(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream())
        {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null)
        {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream())
            {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        if (prototype.isSelectRStream())
        {
            generateOutputBatchedArr(true, outputAllGroupReps.entryIterator(), false, generateSynthetic, oldEvents, oldEventsSortKey);
        }

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet)
        {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            if (prototype.isUnidirectional())
            {
                this.clear();
            }

            if (newData != null)
            {
                // apply new data to aggregates
                for (MultiKey<EventBean> aNewData : newData)
                {
                    Object mk = generateGroupKey(aNewData.getArray(), true);

                    // if this is a newly encountered group, generate the remove stream event
                    if (outputAllGroupReps.put(mk, aNewData.getArray()) == null)
                    {
                        if (prototype.isSelectRStream())
                        {
                            generateOutputBatchedRow(true, mk, aNewData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }
                    aggregationService.applyEnter(aNewData.getArray(), mk, agentInstanceContext);
                }
            }
            if (oldData != null)
            {
                // apply old data to aggregates
                for (MultiKey<EventBean> anOldData : oldData)
                {
                    Object mk = generateGroupKey(anOldData.getArray(), true);

                    if (outputAllGroupReps.put(mk, anOldData.getArray()) == null)
                    {
                        if (prototype.isSelectRStream())
                        {
                            generateOutputBatchedRow(true, mk, anOldData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }

                    aggregationService.applyLeave(anOldData.getArray(), mk, agentInstanceContext);
                }
            }
        }

        generateOutputBatchedArr(true, outputAllGroupReps.entryIterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream())
        {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null)
        {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream())
            {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null))
        {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinDefault(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream())
        {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;

        if (orderByProcessor != null)
        {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream())
            {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        Map<Object, EventBean[]> keysAndEvents = new HashMap<Object, EventBean[]>();

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet)
        {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            if (prototype.isUnidirectional())
            {
                this.clear();
            }

            Object[] newDataMultiKey = generateGroupKeys(newData, keysAndEvents, true);
            Object[] oldDataMultiKey = generateGroupKeys(oldData, keysAndEvents, false);

            if (prototype.isSelectRStream())
            {
                generateOutputBatchedArr(true, keysAndEvents.entrySet().iterator(), false, generateSynthetic, oldEvents, oldEventsSortKey);
            }

            if (newData != null)
            {
                // apply new data to aggregates
                int count = 0;
                for (MultiKey<EventBean> aNewData : newData)
                {
                    aggregationService.applyEnter(aNewData.getArray(), newDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }
            if (oldData != null)
            {
                // apply old data to aggregates
                int count = 0;
                for (MultiKey<EventBean> anOldData : oldData)
                {
                    aggregationService.applyLeave(anOldData.getArray(), oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            generateOutputBatchedArr(true, keysAndEvents.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

            keysAndEvents.clear();
        }

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream())
        {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null)
        {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream())
            {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null))
        {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedViewLast(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream())
        {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null)
        {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream())
            {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<Object, EventBean[]>();
        for (UniformPair<EventBean[]> pair : viewEventsList)
        {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            if (newData != null)
            {
                // apply new data to aggregates
                for (EventBean aNewData : newData)
                {
                    EventBean[] eventsPerStream = new EventBean[] {aNewData};
                    Object mk = generateGroupKey(eventsPerStream, true);

                    // if this is a newly encountered group, generate the remove stream event
                    if (groupRepsView.put(mk, eventsPerStream) == null)
                    {
                        if (prototype.isSelectRStream())
                        {
                            generateOutputBatchedRow(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                }
            }
            if (oldData != null)
            {
                // apply old data to aggregates
                for (EventBean anOldData : oldData)
                {
                    EventBean[] eventsPerStream = new EventBean[] {anOldData};
                    Object mk = generateGroupKey(eventsPerStream, true);

                    if (groupRepsView.put(mk, eventsPerStream) == null)
                    {
                        if (prototype.isSelectRStream())
                        {
                            generateOutputBatchedRow(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }

                    aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                }
            }
        }

        generateOutputBatchedArr(false, groupRepsView.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream())
        {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null)
        {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream())
            {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null))
        {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedViewFirst(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream())
        {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null)
        {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream())
            {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<Object, EventBean[]>();
        if (prototype.getOptionalHavingNode() == null) {
            for (UniformPair<EventBean[]> pair : viewEventsList)
            {
                EventBean[] newData = pair.getFirst();
                EventBean[] oldData = pair.getSecond();

                if (newData != null)
                {
                    // apply new data to aggregates
                    for (EventBean aNewData : newData)
                    {
                        EventBean[] eventsPerStream = new EventBean[] {aNewData};
                        Object mk = generateGroupKey(eventsPerStream, true);
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            // if this is a newly encountered group, generate the remove stream event
                            if (groupRepsView.put(mk, eventsPerStream) == null)
                            {
                                if (prototype.isSelectRStream())
                                {
                                    generateOutputBatchedRow(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                        aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                    }
                }
                if (oldData != null)
                {
                    // apply old data to aggregates
                    for (EventBean anOldData : oldData)
                    {
                        EventBean[] eventsPerStream = new EventBean[] {anOldData};
                        Object mk = generateGroupKey(eventsPerStream, true);
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            if (groupRepsView.put(mk, eventsPerStream) == null)
                            {
                                if (prototype.isSelectRStream())
                                {
                                    generateOutputBatchedRow(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }

                        aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                    }
                }
            }
        }
        else { // having clause present, having clause evaluates at the level of individual posts
            EventBean[] eventsPerStreamOneStream = new EventBean[1];
            for (UniformPair<EventBean[]> pair : viewEventsList)
            {
                EventBean[] newData = pair.getFirst();
                EventBean[] oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeys(newData, true);
                Object[] oldDataMultiKey = generateGroupKeys(oldData, false);

                if (newData != null)
                {
                    // apply new data to aggregates
                    for (int i = 0; i < newData.length; i++)
                    {
                        eventsPerStreamOneStream[0] = newData[i];
                        aggregationService.applyEnter(eventsPerStreamOneStream, newDataMultiKey[i], agentInstanceContext);
                    }
                }
                if (oldData != null)
                {
                    // apply old data to aggregates
                    for (int i = 0; i < oldData.length; i++)
                    {
                        eventsPerStreamOneStream[0] = oldData[i];
                        aggregationService.applyLeave(eventsPerStreamOneStream, oldDataMultiKey[i], agentInstanceContext);
                    }
                }

                // evaluate having-clause
                if (newData != null)
                {
                    for (int i = 0; i < newData.length; i++)
                    {
                        Object mk = newDataMultiKey[i];
                        eventsPerStreamOneStream[0] = newData[i];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseNonJoin(newData[i]);}
                        Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStreamOneStream, true, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseNonJoin(result);}
                        if ((result == null) || (!result))
                        {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            EventBean[] eventsPerStream = new EventBean[] {newData[i]};
                            if (groupRepsView.put(mk, eventsPerStream) == null)
                            {
                                if (prototype.isSelectRStream())
                                {
                                    generateOutputBatchedRow(false, mk, eventsPerStream, true, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                    }
                }

                // evaluate having-clause
                if (oldData != null)
                {
                    for (int i = 0; i < oldData.length; i++)
                    {
                        Object mk = oldDataMultiKey[i];
                        eventsPerStreamOneStream[0] = oldData[i];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseNonJoin(oldData[i]);}
                        Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStreamOneStream, false, agentInstanceContext);
                        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseNonJoin(result);}
                        if ((result == null) || (!result))
                        {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            EventBean[] eventsPerStream = new EventBean[] {oldData[i]};
                            if (groupRepsView.put(mk, eventsPerStream) == null)
                            {
                                if (prototype.isSelectRStream())
                                {
                                    generateOutputBatchedRow(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                    }
                }
            }
        }

        generateOutputBatchedArr(false, groupRepsView.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream())
        {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null)
        {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream())
            {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null))
        {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedViewAll(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        EventBean[] eventsPerStream = new EventBean[1];

        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream())
        {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null)
        {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream())
            {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        if (prototype.isSelectRStream())
        {
            generateOutputBatchedArr(false, outputAllGroupReps.entryIterator(), false, generateSynthetic, oldEvents, oldEventsSortKey);
        }

        for (UniformPair<EventBean[]> pair : viewEventsList)
        {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            if (newData != null)
            {
                // apply new data to aggregates
                for (EventBean aNewData : newData)
                {
                    eventsPerStream[0] = aNewData;
                    Object mk = generateGroupKey(eventsPerStream, true);

                    // if this is a newly encountered group, generate the remove stream event
                    if (outputAllGroupReps.put(mk, new EventBean[] {aNewData}) == null)
                    {
                        if (prototype.isSelectRStream())
                        {
                            generateOutputBatchedRow(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                }
            }
            if (oldData != null)
            {
                // apply old data to aggregates
                for (EventBean anOldData : oldData)
                {
                    eventsPerStream[0] = anOldData;
                    Object mk = generateGroupKey(eventsPerStream, true);

                    if (outputAllGroupReps.put(mk, new EventBean[] {anOldData}) == null)
                    {
                        if (prototype.isSelectRStream())
                        {
                            generateOutputBatchedRow(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }

                    aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                }
            }
        }

        generateOutputBatchedArr(false, outputAllGroupReps.entryIterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream())
        {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null)
        {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream())
            {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null))
        {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedViewDefault(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        List<EventBean> newEvents = new LinkedList<EventBean>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream())
        {
            oldEvents = new LinkedList<EventBean>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null)
        {
            newEventsSortKey = new LinkedList<Object>();
            if (prototype.isSelectRStream())
            {
                oldEventsSortKey = new LinkedList<Object>();
            }
        }

        Map<Object, EventBean> keysAndEvents = new HashMap<Object, EventBean>();

        for (UniformPair<EventBean[]> pair : viewEventsList)
        {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeys(newData, keysAndEvents, true);
            Object[] oldDataMultiKey = generateGroupKeys(oldData, keysAndEvents, false);

            if (prototype.isSelectRStream())
            {
                generateOutputBatchedRow(keysAndEvents, false, generateSynthetic, oldEvents, oldEventsSortKey, agentInstanceContext);
            }

            EventBean[] eventsPerStream = new EventBean[1];
            if (newData != null)
            {
                // apply new data to aggregates
                int count = 0;
                for (EventBean aNewData : newData)
                {
                    eventsPerStream[0] = aNewData;
                    aggregationService.applyEnter(eventsPerStream, newDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }
            if (oldData != null)
            {
                // apply old data to aggregates
                int count = 0;
                for (EventBean anOldData : oldData)
                {
                    eventsPerStream[0] = anOldData;
                    aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            generateOutputBatchedRow(keysAndEvents, true, generateSynthetic, newEvents, newEventsSortKey, agentInstanceContext);

            keysAndEvents.clear();
        }

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream())
        {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null)
        {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream())
            {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        if ((newEventsArr == null) && (oldEventsArr == null))
        {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private Object[] generateGroupKeys(Set<MultiKey<EventBean>> resultSet, boolean isNewData)
    {
        if (resultSet.isEmpty())
        {
            return null;
        }

        Object keys[] = new Object[resultSet.size()];

        int count = 0;
        for (MultiKey<EventBean> eventsPerStream : resultSet)
        {
            keys[count] = generateGroupKey(eventsPerStream.getArray(), isNewData);
            count++;
        }

        return keys;
    }
}
