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
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.Viewable;

import java.util.*;

/**
 * Result set processor for the case: aggregation functions used in the select clause, and no group-by,
 * and not all of the properties in the select clause are under an aggregation function.
 * <p>
 * This processor does not perform grouping, every event entering and leaving is in the same group.
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 * Aggregation state is simply one row holding all the state.
 */
public class ResultSetProcessorAggregateAll implements ResultSetProcessor
{
    private final ResultSetProcessorAggregateAllFactory prototype;
    private final SelectExprProcessor selectExprProcessor;
    private final OrderByProcessor orderByProcessor;
    private final AggregationService aggregationService; 
    private ExprEvaluatorContext exprEvaluatorContext;
    private ResultSetProcessorAggregateAllOutputLastHelper outputLastUnordHelper;
    private ResultSetProcessorAggregateAllOutputAllHelper outputAllUnordHelper;

    public ResultSetProcessorAggregateAll(ResultSetProcessorAggregateAllFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AggregationService aggregationService, ExprEvaluatorContext exprEvaluatorContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.outputLastUnordHelper = prototype.isOutputLast() ? new ResultSetProcessorAggregateAllOutputLastHelper(this) : null;
        this.outputAllUnordHelper = prototype.isOutputAll() ? new ResultSetProcessorAggregateAllOutputAllHelper(this) : null;
    }

    public void setAgentInstanceContext(AgentInstanceContext context) {
        this.exprEvaluatorContext = context;
    }

    public EventType getResultEventType()
    {
        return prototype.getResultEventType();
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        EventBean[] eventsPerStream = new EventBean[1];
        ResultSetProcessorUtil.applyAggViewResult(aggregationService, exprEvaluatorContext, newData, oldData, eventsPerStream);
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        ResultSetProcessorUtil.applyAggJoinResult(aggregationService, exprEvaluatorContext, newEvents, oldEvents);
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qResultSetProcessUngroupedNonfullyAgg();}
        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;

        if (prototype.isUnidirectional())
        {
            this.clear();
        }

        ResultSetProcessorUtil.applyAggJoinResult(aggregationService, exprEvaluatorContext, newEvents, oldEvents);

        if (prototype.getOptionalHavingNode() == null)
        {
            if (prototype.isSelectRStream())
            {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHaving(selectExprProcessor, oldEvents, false, isSynthesize, exprEvaluatorContext);
                }
                else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldEvents, false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHaving(selectExprProcessor, newEvents, true, isSynthesize, exprEvaluatorContext);
            }
            else {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, newEvents, true, isSynthesize, exprEvaluatorContext);
            }
        }
        else
        {
            if (prototype.isSelectRStream())
            {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, oldEvents, prototype.getOptionalHavingNode(), false, isSynthesize, exprEvaluatorContext);
                }
                else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldEvents, prototype.getOptionalHavingNode(), false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, newEvents, prototype.getOptionalHavingNode(), true, isSynthesize, exprEvaluatorContext);
            }
            else {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, newEvents, prototype.getOptionalHavingNode(), true, isSynthesize, exprEvaluatorContext);
            }
        }

        if ((selectNewEvents == null) && (selectOldEvents == null))
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessUngroupedNonfullyAgg(null, null);}
            return null;
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessUngroupedNonfullyAgg(selectNewEvents, selectOldEvents);}
        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qResultSetProcessUngroupedNonfullyAgg();}
        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;

        EventBean[] eventsPerStream = new EventBean[1];
        ResultSetProcessorUtil.applyAggViewResult(aggregationService, exprEvaluatorContext, newData, oldData, eventsPerStream);

        // generate new events using select expressions
        if (prototype.getOptionalHavingNode() == null)
        {
            if (prototype.isSelectRStream())
            {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, oldData, false, isSynthesize, exprEvaluatorContext);
                }
                else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldData, false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, newData, true, isSynthesize, exprEvaluatorContext);
            }
            else {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, newData, true, isSynthesize, exprEvaluatorContext);
            }
        }
        else
        {
            if (prototype.isSelectRStream())
            {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, oldData, prototype.getOptionalHavingNode(), false, isSynthesize, exprEvaluatorContext);
                }
                else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldData, prototype.getOptionalHavingNode(), false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingNode(), true, isSynthesize, exprEvaluatorContext);
            }
            else {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, newData, prototype.getOptionalHavingNode(), true, isSynthesize, exprEvaluatorContext);
            }
        }

        if ((selectNewEvents == null) && (selectOldEvents == null))
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessUngroupedNonfullyAgg(null, null);}
            return null;
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessUngroupedNonfullyAgg(selectNewEvents, selectOldEvents);}
        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    public Iterator<EventBean> getIterator(Viewable parent)
    {
        if (!prototype.isHistoricalOnly()) {
            return obtainIterator(parent);
        }

        ResultSetProcessorUtil.clearAndAggregateUngrouped(exprEvaluatorContext, aggregationService, parent);
        ArrayDeque<EventBean> deque = ResultSetProcessorUtil.iteratorToDeque(obtainIterator(parent));
        aggregationService.clearResults(exprEvaluatorContext);
        return deque.iterator();
    }

    public Iterator<EventBean> obtainIterator(Viewable parent) {

        if (orderByProcessor == null)
        {
            return new ResultSetAggregateAllIterator(parent.iterator(), this, exprEvaluatorContext);
        }

        // Pull all parent events, generate order keys
        EventBean[] eventsPerStream = new EventBean[1];
        List<EventBean> outgoingEvents = new ArrayList<EventBean>();
        List<Object> orderKeys = new ArrayList<Object>();

        for (EventBean candidate : parent)
        {
            eventsPerStream[0] = candidate;

            Boolean pass = true;
            if (prototype.getOptionalHavingNode() != null)
            {
                pass = (Boolean) prototype.getOptionalHavingNode().evaluate(eventsPerStream, true, exprEvaluatorContext);
            }
            if ((pass == null) || (!pass))
            {
                continue;
            }

            outgoingEvents.add(selectExprProcessor.process(eventsPerStream, true, true, exprEvaluatorContext));

            Object orderKey = orderByProcessor.getSortKey(eventsPerStream, true, exprEvaluatorContext);
            orderKeys.add(orderKey);
        }

        // sort
        EventBean[] outgoingEventsArr = outgoingEvents.toArray(new EventBean[outgoingEvents.size()]);
        Object[] orderKeysArr = orderKeys.toArray(new Object[orderKeys.size()]);
        EventBean[] orderedEvents = orderByProcessor.sort(outgoingEventsArr, orderKeysArr, exprEvaluatorContext);

        return new ArrayEventIterator(orderedEvents);
    }

    /**
     * Returns the select expression processor
     * @return select processor.
     */
    public SelectExprProcessor getSelectExprProcessor()
    {
        return selectExprProcessor;
    }

    /**
     * Returns the optional having expression.
     * @return having expression node
     */
    public ExprEvaluator getOptionalHavingNode()
    {
        return prototype.getOptionalHavingNode();
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet)
    {
        EventBean[] result;
        if (prototype.getOptionalHavingNode() == null)
        {
            if (orderByProcessor == null) {
                result = ResultSetProcessorUtil.getSelectJoinEventsNoHaving(selectExprProcessor, joinSet, true, true, exprEvaluatorContext);
            }
            else {
                result = ResultSetProcessorUtil.getSelectJoinEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, joinSet, true, true, exprEvaluatorContext);
            }
        }
        else
        {
            if (orderByProcessor == null) {
                result = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, joinSet, prototype.getOptionalHavingNode(), true, true, exprEvaluatorContext);
            }
            else {
                result = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, joinSet, prototype.getOptionalHavingNode(), true, true, exprEvaluatorContext);
            }
        }
        return new ArrayEventIterator(result);
    }

    public void clear()
    {
        aggregationService.clearResults(exprEvaluatorContext);
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType)
    {
        if (outputLimitLimitType == OutputLimitLimitType.LAST)
        {
            EventBean lastOldEvent = null;
            EventBean lastNewEvent = null;

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
                    for (MultiKey<EventBean> eventsPerStream : newData)
                    {
                        aggregationService.applyEnter(eventsPerStream.getArray(), null, exprEvaluatorContext);
                    }
                }
                if (oldData != null)
                {
                    // apply old data to aggregates
                    for (MultiKey<EventBean> eventsPerStream : oldData)
                    {
                        aggregationService.applyLeave(eventsPerStream.getArray(), null, exprEvaluatorContext);
                    }
                }

                EventBean[] selectOldEvents;
                if (prototype.isSelectRStream())
                {
                    if (prototype.getOptionalHavingNode() == null)
                    {
                        selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHaving(selectExprProcessor, oldData, false, generateSynthetic, exprEvaluatorContext);
                    }
                    else
                    {
                        selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, oldData, prototype.getOptionalHavingNode(), false, generateSynthetic, exprEvaluatorContext);
                    }
                    if ((selectOldEvents != null) && (selectOldEvents.length > 0))
                    {
                        lastOldEvent = selectOldEvents[selectOldEvents.length - 1];
                    }
                }

                // generate new events using select expressions
                EventBean[] selectNewEvents;
                if (prototype.getOptionalHavingNode() == null)
                {
                    selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHaving(selectExprProcessor, newData, true, generateSynthetic, exprEvaluatorContext);
                }
                else
                {
                    selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingNode(), true, generateSynthetic, exprEvaluatorContext);
                }
                if ((selectNewEvents != null) && (selectNewEvents.length > 0))
                {
                    lastNewEvent = selectNewEvents[selectNewEvents.length - 1];
                }
            }

            EventBean[] lastNew = (lastNewEvent != null) ? new EventBean[] {lastNewEvent} : null;
            EventBean[] lastOld = (lastOldEvent != null) ? new EventBean[] {lastOldEvent} : null;

            if ((lastNew == null) && (lastOld == null))
            {
                return null;
            }
            return new UniformPair<EventBean[]>(lastNew, lastOld);
        }
        else
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
                    for (MultiKey<EventBean> row : newData)
                    {
                        aggregationService.applyEnter(row.getArray(), null, exprEvaluatorContext);
                    }
                }
                if (oldData != null)
                {
                    // apply old data to aggregates
                    for (MultiKey<EventBean> row : oldData)
                    {
                        aggregationService.applyLeave(row.getArray(), null, exprEvaluatorContext);
                    }
                }

                // generate old events using select expressions
                if (prototype.isSelectRStream())
                {
                    if (prototype.getOptionalHavingNode() == null)
                    {
                        if (orderByProcessor == null) {
                            ResultSetProcessorUtil.populateSelectJoinEventsNoHaving(selectExprProcessor, oldData, false, generateSynthetic, oldEvents, exprEvaluatorContext);
                        }
                        else {
                            ResultSetProcessorUtil.populateSelectJoinEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldData, false, generateSynthetic, oldEvents, oldEventsSortKey, exprEvaluatorContext);
                        }
                    }
                    // generate old events using having then select
                    else
                    {
                        if (orderByProcessor == null) {
                            ResultSetProcessorUtil.populateSelectJoinEventsHaving(selectExprProcessor, oldData, prototype.getOptionalHavingNode(), false, generateSynthetic, oldEvents, exprEvaluatorContext);
                        }
                        else {
                            ResultSetProcessorUtil.populateSelectJoinEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldData, prototype.getOptionalHavingNode(), false, generateSynthetic, oldEvents, oldEventsSortKey, exprEvaluatorContext);
                        }
                    }
                }

                // generate new events using select expressions
                if (prototype.getOptionalHavingNode() == null)
                {
                    if (orderByProcessor == null) {
                        ResultSetProcessorUtil.populateSelectJoinEventsNoHaving(selectExprProcessor, newData, true, generateSynthetic, newEvents, exprEvaluatorContext);
                    }
                    else {
                        ResultSetProcessorUtil.populateSelectJoinEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, newData, true, generateSynthetic, newEvents, newEventsSortKey, exprEvaluatorContext);
                    }
                }
                else
                {
                    if (orderByProcessor == null) {
                        ResultSetProcessorUtil.populateSelectJoinEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingNode(), true, generateSynthetic, newEvents, exprEvaluatorContext);
                    }
                    else {
                        ResultSetProcessorUtil.populateSelectJoinEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, newData, prototype.getOptionalHavingNode(), true, generateSynthetic, newEvents, newEventsSortKey, exprEvaluatorContext);
                    }
                }
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
                newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, exprEvaluatorContext);
                if (prototype.isSelectRStream())
                {
                    Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                    oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, exprEvaluatorContext);
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
        if (outputLimitLimitType == OutputLimitLimitType.LAST)
        {
            EventBean lastOldEvent = null;
            EventBean lastNewEvent = null;
            EventBean[] eventsPerStream = new EventBean[1];

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
                        aggregationService.applyEnter(eventsPerStream, null, exprEvaluatorContext);
                    }
                }
                if (oldData != null)
                {
                    // apply old data to aggregates
                    for (EventBean anOldData : oldData)
                    {
                        eventsPerStream[0] = anOldData;
                        aggregationService.applyLeave(eventsPerStream, null,exprEvaluatorContext);
                    }
                }

                EventBean[] selectOldEvents;
                if (prototype.isSelectRStream())
                {
                    if (prototype.getOptionalHavingNode() == null)
                    {
                        selectOldEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, oldData, false, generateSynthetic, exprEvaluatorContext);
                    }
                    else
                    {
                        selectOldEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, oldData, prototype.getOptionalHavingNode(), false, generateSynthetic, exprEvaluatorContext);
                    }
                    if ((selectOldEvents != null) && (selectOldEvents.length > 0))
                    {
                        lastOldEvent = selectOldEvents[selectOldEvents.length - 1];
                    }
                }

                // generate new events using select expressions
                EventBean[] selectNewEvents;
                if (prototype.getOptionalHavingNode() == null)
                {
                    selectNewEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, newData, true, generateSynthetic, exprEvaluatorContext);
                }
                else
                {
                    selectNewEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingNode(), true, generateSynthetic, exprEvaluatorContext);
                }
                if ((selectNewEvents != null) && (selectNewEvents.length > 0))
                {
                    lastNewEvent = selectNewEvents[selectNewEvents.length - 1];
                }
            }

            EventBean[] lastNew = (lastNewEvent != null) ? new EventBean[] {lastNewEvent} : null;
            EventBean[] lastOld = (lastOldEvent != null) ? new EventBean[] {lastOldEvent} : null;

            if ((lastNew == null) && (lastOld == null))
            {
                return null;
            }
            return new UniformPair<EventBean[]>(lastNew, lastOld);
        }
        else
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

            for (UniformPair<EventBean[]> pair : viewEventsList)
            {
                EventBean[] newData = pair.getFirst();
                EventBean[] oldData = pair.getSecond();

                EventBean[] eventsPerStream = new EventBean[1];
                if (newData != null)
                {
                    // apply new data to aggregates
                    for (EventBean aNewData : newData)
                    {
                        eventsPerStream[0] = aNewData;
                        aggregationService.applyEnter(eventsPerStream, null, exprEvaluatorContext);
                    }
                }
                if (oldData != null)
                {
                    // apply old data to aggregates
                    for (EventBean anOldData : oldData)
                    {
                        eventsPerStream[0] = anOldData;
                        aggregationService.applyLeave(eventsPerStream, null, exprEvaluatorContext);
                    }
                }

                // generate old events using select expressions
                if (prototype.isSelectRStream())
                {
                    if (prototype.getOptionalHavingNode() == null)
                    {
                        if (orderByProcessor == null) {
                            ResultSetProcessorUtil.populateSelectEventsNoHaving(selectExprProcessor, oldData, false, generateSynthetic, oldEvents, exprEvaluatorContext);
                        }
                        else {
                            ResultSetProcessorUtil.populateSelectEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldData, false, generateSynthetic, oldEvents, oldEventsSortKey, exprEvaluatorContext);
                        }
                    }
                    // generate old events using having then select
                    else
                    {
                        if (orderByProcessor == null) {
                            ResultSetProcessorUtil.populateSelectEventsHaving(selectExprProcessor, oldData, prototype.getOptionalHavingNode(), false, generateSynthetic, oldEvents, exprEvaluatorContext);
                        }
                        else {
                            ResultSetProcessorUtil.populateSelectEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldData, prototype.getOptionalHavingNode(), false, generateSynthetic, oldEvents, oldEventsSortKey, exprEvaluatorContext);
                        }
                    }
                }

                // generate new events using select expressions
                if (prototype.getOptionalHavingNode() == null)
                {
                    if (orderByProcessor == null) {
                        ResultSetProcessorUtil.populateSelectEventsNoHaving(selectExprProcessor, newData, true, generateSynthetic, newEvents, exprEvaluatorContext);
                    }
                    else {
                        ResultSetProcessorUtil.populateSelectEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, newData, true, generateSynthetic, newEvents, newEventsSortKey, exprEvaluatorContext);
                    }
                }
                else
                {
                    if (orderByProcessor == null) {
                        ResultSetProcessorUtil.populateSelectEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingNode(), true, generateSynthetic, newEvents, exprEvaluatorContext);
                    }
                    else {
                        ResultSetProcessorUtil.populateSelectEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, newData, prototype.getOptionalHavingNode(), true, generateSynthetic, newEvents, newEventsSortKey, exprEvaluatorContext);
                    }
                }
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
                newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, exprEvaluatorContext);

                if (prototype.isSelectRStream())
                {
                    Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                    oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, exprEvaluatorContext);
                }
            }

            if ((newEventsArr == null) && (oldEventsArr == null))
            {
                return null;
            }
            return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
        }
    }

    public boolean hasAggregation() {
        return true;
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllUnordHelper.processView(newData, oldData, isGenerateSynthetic);
        }
        else {
            outputLastUnordHelper.processView(newData, oldData, isGenerateSynthetic);
        }
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllUnordHelper.processJoin(newEvents, oldEvents, isGenerateSynthetic);
        }
        else {
            outputLastUnordHelper.processJoin(newEvents, oldEvents, isGenerateSynthetic);
        }
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize, boolean isAll) {
        if (isAll) {
            return outputAllUnordHelper.output();
        }
        return outputLastUnordHelper.output();
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedJoin(boolean isSynthesize, boolean isAll) {
        if (isAll) {
            return outputAllUnordHelper.output();
        }
        return outputLastUnordHelper.output();
    }

    public void stop() {
        // no action required
    }
}
