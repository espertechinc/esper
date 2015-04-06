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
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.view.OutputConditionPolled;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.Viewable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private static final Log log = LogFactory.getLog(ResultSetProcessorRowPerGroup.class);

    protected final ResultSetProcessorRowPerGroupFactory prototype;
    protected final SelectExprProcessor selectExprProcessor;
    protected final OrderByProcessor orderByProcessor;
    protected final AggregationService aggregationService;
    protected AgentInstanceContext agentInstanceContext;

    // For output rate limiting, keep a representative event for each group for
    // representing each group in an output limit clause
    protected final Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<Object, EventBean[]>();

    private final Map<Object, OutputConditionPolled> outputState = new HashMap<Object, OutputConditionPolled>();

    public ResultSetProcessorRowPerGroup(ResultSetProcessorRowPerGroupFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.agentInstanceContext = agentInstanceContext;
        aggregationService.setRemovedCallback(this);
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

    private void generateOutputBatched(Map<Object, EventBean> keysAndEvents, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys, AgentInstanceContext agentInstanceContext)
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

    private void generateOutputBatchedArr(boolean join, Map<Object, EventBean[]> keysAndEvents, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys)
    {
        for (Map.Entry<Object, EventBean[]> entry : keysAndEvents.entrySet())
        {
            generateOutputBatched(join, entry.getKey(), entry.getValue(), isNewData, isSynthesize, resultEvents, optSortKeys);
        }
    }

    private void generateOutputBatched(boolean join, Object mk, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys)
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
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT)
        {
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
                    generateOutputBatchedArr(true, keysAndEvents, false, generateSynthetic, oldEvents, oldEventsSortKey);
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

                generateOutputBatchedArr(true, keysAndEvents, true, generateSynthetic, newEvents, newEventsSortKey);

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
        else if (outputLimitLimitType == OutputLimitLimitType.ALL)
        {
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
                generateOutputBatchedArr(true, groupRepsView, false, generateSynthetic, oldEvents, oldEventsSortKey);
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
                        if (groupRepsView.put(mk, aNewData.getArray()) == null)
                        {
                            if (prototype.isSelectRStream())
                            {
                                generateOutputBatched(true, mk, aNewData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
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
                                generateOutputBatched(true, mk, anOldData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                            }
                        }

                        aggregationService.applyLeave(anOldData.getArray(), mk, agentInstanceContext);
                    }
                }
            }

            generateOutputBatchedArr(true, groupRepsView, true, generateSynthetic, newEvents, newEventsSortKey);

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
        else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {

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

            groupRepsView.clear();
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

                            OutputConditionPolled outputStateGroup = outputState.get(mk);
                            if (outputStateGroup == null) {
                                try {
                                    outputStateGroup = OutputConditionPolledFactory.createCondition(prototype.getOutputLimitSpec(), agentInstanceContext);
                                }
                                catch (ExprValidationException e) {
                                    log.error("Error starting output limit for group for statement '" + agentInstanceContext.getStatementContext().getStatementName() + "'");
                                }
                                outputState.put(mk, outputStateGroup);
                            }
                            boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                            if (pass) {
                                // if this is a newly encountered group, generate the remove stream event
                                if (groupRepsView.put(mk, aNewData.getArray()) == null)
                                {
                                    if (prototype.isSelectRStream())
                                    {
                                        generateOutputBatched(true, mk, aNewData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
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

                            OutputConditionPolled outputStateGroup = outputState.get(mk);
                            if (outputStateGroup == null) {
                                try {
                                    outputStateGroup = OutputConditionPolledFactory.createCondition(prototype.getOutputLimitSpec(), agentInstanceContext);
                                }
                                catch (ExprValidationException e) {
                                    log.error("Error starting output limit for group for statement '" + agentInstanceContext.getStatementContext().getStatementName() + "'");
                                }
                                outputState.put(mk, outputStateGroup);
                            }
                            boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                            if (pass) {
                                if (groupRepsView.put(mk, anOldData.getArray()) == null)
                                {
                                    if (prototype.isSelectRStream())
                                    {
                                        generateOutputBatched(true, mk, anOldData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
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

                            OutputConditionPolled outputStateGroup = outputState.get(mk);
                            if (outputStateGroup == null) {
                                try {
                                    outputStateGroup = OutputConditionPolledFactory.createCondition(prototype.getOutputLimitSpec(), agentInstanceContext);
                                }
                                catch (ExprValidationException e) {
                                    log.error("Error starting output limit for group for statement '" + agentInstanceContext.getStatementContext().getStatementName() + "'");
                                }
                                outputState.put(mk, outputStateGroup);
                            }
                            boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                            if (pass) {
                                if (groupRepsView.put(mk, aNewData.getArray()) == null)
                                {
                                    if (prototype.isSelectRStream())
                                    {
                                        generateOutputBatched(true, mk, aNewData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
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

                            OutputConditionPolled outputStateGroup = outputState.get(mk);
                            if (outputStateGroup == null) {
                                try {
                                    outputStateGroup = OutputConditionPolledFactory.createCondition(prototype.getOutputLimitSpec(), agentInstanceContext);
                                }
                                catch (ExprValidationException e) {
                                    log.error("Error starting output limit for group for statement '" + agentInstanceContext.getStatementContext().getStatementName() + "'");
                                }
                                outputState.put(mk, outputStateGroup);
                            }
                            boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                            if (pass) {
                                if (groupRepsView.put(mk, anOldData.getArray()) == null)
                                {
                                    if (prototype.isSelectRStream())
                                    {
                                        generateOutputBatched(true, mk, anOldData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                                    }
                                }
                            }
                            count++;
                        }
                    }
                }
            }

            generateOutputBatchedArr(true, groupRepsView, true, generateSynthetic, newEvents, newEventsSortKey);

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
        else // (outputLimitLimitType == OutputLimitLimitType.LAST)
        {
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

            groupRepsView.clear();
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
                                generateOutputBatched(true, mk, aNewData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
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
                                generateOutputBatched(true, mk, anOldData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                            }
                        }

                        aggregationService.applyLeave(anOldData.getArray(), mk, agentInstanceContext);
                    }
                }
            }

            generateOutputBatchedArr(true, groupRepsView, true, generateSynthetic, newEvents, newEventsSortKey);

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
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType)
    {
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT)
        {
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
                    generateOutputBatched(keysAndEvents, false, generateSynthetic, oldEvents, oldEventsSortKey, agentInstanceContext);
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

                generateOutputBatched(keysAndEvents, true, generateSynthetic, newEvents, newEventsSortKey, agentInstanceContext);

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
        else if (outputLimitLimitType == OutputLimitLimitType.ALL)
        {
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
                generateOutputBatchedArr(false, groupRepsView, false, generateSynthetic, oldEvents, oldEventsSortKey);
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
                        if (groupRepsView.put(mk, new EventBean[] {aNewData}) == null)
                        {
                            if (prototype.isSelectRStream())
                            {
                                generateOutputBatched(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
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

                        if (groupRepsView.put(mk, new EventBean[] {anOldData}) == null)
                        {
                            if (prototype.isSelectRStream())
                            {
                                generateOutputBatched(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                            }
                        }

                        aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                    }
                }
            }

            generateOutputBatchedArr(false, groupRepsView, true, generateSynthetic, newEvents, newEventsSortKey);

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
        else if (outputLimitLimitType == OutputLimitLimitType.FIRST)
        {
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

            if (prototype.getOptionalHavingNode() == null) {

                groupRepsView.clear();
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

                            OutputConditionPolled outputStateGroup = outputState.get(mk);
                            if (outputStateGroup == null) {
                                try {
                                    outputStateGroup = OutputConditionPolledFactory.createCondition(prototype.getOutputLimitSpec(), agentInstanceContext);
                                }
                                catch (ExprValidationException e) {
                                    log.error("Error starting output limit for group for statement '" + agentInstanceContext.getStatementContext().getStatementName() + "'");
                                }
                                outputState.put(mk, outputStateGroup);
                            }
                            boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                            if (pass) {
                                // if this is a newly encountered group, generate the remove stream event
                                if (groupRepsView.put(mk, eventsPerStream) == null)
                                {
                                    if (prototype.isSelectRStream())
                                    {
                                        generateOutputBatched(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
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

                            OutputConditionPolled outputStateGroup = outputState.get(mk);
                            if (outputStateGroup == null) {
                                try {
                                    outputStateGroup = OutputConditionPolledFactory.createCondition(prototype.getOutputLimitSpec(), agentInstanceContext);
                                }
                                catch (ExprValidationException e) {
                                    log.error("Error starting output limit for group for statement '" + agentInstanceContext.getStatementContext().getStatementName() + "'");
                                }
                                outputState.put(mk, outputStateGroup);
                            }
                            boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                            if (pass) {
                                if (groupRepsView.put(mk, eventsPerStream) == null)
                                {
                                    if (prototype.isSelectRStream())
                                    {
                                        generateOutputBatched(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
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
                groupRepsView.clear();
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

                            OutputConditionPolled outputStateGroup = outputState.get(mk);
                            if (outputStateGroup == null) {
                                try {
                                    outputStateGroup = OutputConditionPolledFactory.createCondition(prototype.getOutputLimitSpec(), agentInstanceContext);
                                }
                                catch (ExprValidationException e) {
                                    log.error("Error starting output limit for group for statement '" + agentInstanceContext.getStatementContext().getStatementName() + "'");
                                }
                                outputState.put(mk, outputStateGroup);
                            }
                            boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                            if (pass) {
                                EventBean[] eventsPerStream = new EventBean[] {newData[i]};
                                if (groupRepsView.put(mk, eventsPerStream) == null)
                                {
                                    if (prototype.isSelectRStream())
                                    {
                                        generateOutputBatched(false, mk, eventsPerStream, true, generateSynthetic, oldEvents, oldEventsSortKey);
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

                            OutputConditionPolled outputStateGroup = outputState.get(mk);
                            if (outputStateGroup == null) {
                                try {
                                    outputStateGroup = OutputConditionPolledFactory.createCondition(prototype.getOutputLimitSpec(), agentInstanceContext);
                                }
                                catch (ExprValidationException e) {
                                    log.error("Error starting output limit for group for statement '" + agentInstanceContext.getStatementContext().getStatementName() + "'");
                                }
                                outputState.put(mk, outputStateGroup);
                            }
                            boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                            if (pass) {
                                EventBean[] eventsPerStream = new EventBean[] {oldData[i]};
                                if (groupRepsView.put(mk, eventsPerStream) == null)
                                {
                                    if (prototype.isSelectRStream())
                                    {
                                        generateOutputBatched(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            generateOutputBatchedArr(false, groupRepsView, true, generateSynthetic, newEvents, newEventsSortKey);

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
        else // (outputLimitLimitType == OutputLimitLimitType.LAST)
        {
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

            groupRepsView.clear();
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
                                generateOutputBatched(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
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
                                generateOutputBatched(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                            }
                        }

                        aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                    }
                }
            }

            generateOutputBatchedArr(false, groupRepsView, true, generateSynthetic, newEvents, newEventsSortKey);

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

    public boolean hasAggregation() {
        return true;
    }

    public void removed(Object key) {
        groupRepsView.remove(key);
        outputState.remove(key);
    }

    protected Object generateGroupKey(EventBean[] eventsPerStream, boolean isNewData) {
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
}
