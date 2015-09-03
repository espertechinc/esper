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
import com.espertech.esper.collection.TransformEventIterator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.Viewable;

import java.util.*;

/**
 * Result set processor for the simplest case: no aggregation functions used in the select clause, and no group-by.
 * <p>
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 */
public class ResultSetProcessorSimple extends ResultSetProcessorBaseSimple
{
    private final ResultSetProcessorSimpleFactory prototype;
    private final SelectExprProcessor selectExprProcessor;
    private final OrderByProcessor orderByProcessor;
    private ExprEvaluatorContext exprEvaluatorContext;
    private EventBean outputLastIStreamBufView;
    private EventBean outputLastRStreamBufView;
    private MultiKey<EventBean> outputLastIStreamBufJoin;
    private MultiKey<EventBean> outputLastRStreamBufJoin;

    public ResultSetProcessorSimple(ResultSetProcessorSimpleFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, ExprEvaluatorContext exprEvaluatorContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.exprEvaluatorContext = exprEvaluatorContext;
    }

    public void setAgentInstanceContext(AgentInstanceContext context) {
        exprEvaluatorContext = context;
    }

    public EventType getResultEventType()
    {
        return prototype.getResultEventType();
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qResultSetProcessSimple();}

        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;

        if (prototype.getOptionalHavingExpr() == null)
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
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, oldEvents, prototype.getOptionalHavingExpr(), false, isSynthesize, exprEvaluatorContext);
                }
                else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldEvents, prototype.getOptionalHavingExpr(), false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, newEvents, prototype.getOptionalHavingExpr(), true, isSynthesize, exprEvaluatorContext);
            }
            else {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, newEvents, prototype.getOptionalHavingExpr(), true, isSynthesize, exprEvaluatorContext);
            }
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessSimple(selectNewEvents, selectOldEvents);}
        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qResultSetProcessSimple();}

        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;
        if (prototype.getOptionalHavingExpr() == null)
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
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, oldData, prototype.getOptionalHavingExpr(), false, isSynthesize, exprEvaluatorContext);
                }
                else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldData, prototype.getOptionalHavingExpr(), false, isSynthesize, exprEvaluatorContext);
                }
            }
            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingExpr(), true, isSynthesize, exprEvaluatorContext);
            }
            else {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, newData, prototype.getOptionalHavingExpr(), true, isSynthesize, exprEvaluatorContext);
            }
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aResultSetProcessSimple(selectNewEvents, selectOldEvents);}
        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    /**
     * Process view results for the iterator.
     * @param newData new events
     * @return pair of insert and remove stream
     */
    public UniformPair<EventBean[]> processViewResultIterator(EventBean[] newData)
    {
        EventBean[] selectNewEvents;
        if (prototype.getOptionalHavingExpr() == null)
        {
            // ignore orderByProcessor
            selectNewEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, newData, true, true, exprEvaluatorContext);
        }
        else
        {
            // ignore orderByProcessor
            selectNewEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingExpr(), true, true, exprEvaluatorContext);
        }

        return new UniformPair<EventBean[]>(selectNewEvents, null);
    }

    public Iterator<EventBean> getIterator(Viewable parent)
    {
        if (orderByProcessor != null)
        {
            // Pull all events, generate order keys
            EventBean[] eventsPerStream = new EventBean[1];
            List<EventBean> events = new ArrayList<EventBean>();
            List<Object> orderKeys = new ArrayList<Object>();
            Iterator parentIterator = parent.iterator();
            if (parentIterator == null) {
                return CollectionUtil.NULL_EVENT_ITERATOR;
            }
            for (EventBean aParent : parent)
            {
                eventsPerStream[0] = aParent;
                Object orderKey = orderByProcessor.getSortKey(eventsPerStream, true, exprEvaluatorContext);
                UniformPair<EventBean[]> pair = processViewResultIterator(eventsPerStream);
                EventBean[] result = pair.getFirst();
                if (result != null && result.length != 0)
                {
                    events.add(result[0]);
                }
                orderKeys.add(orderKey);
            }

            // sort
            EventBean[] outgoingEvents = events.toArray(new EventBean[events.size()]);
            Object[] orderKeysArr = orderKeys.toArray(new Object[orderKeys.size()]);
            EventBean[] orderedEvents = orderByProcessor.sort(outgoingEvents, orderKeysArr, exprEvaluatorContext);

            return new ArrayEventIterator(orderedEvents);
        }
        // Return an iterator that gives row-by-row a result
        return new TransformEventIterator(parent.iterator(), new ResultSetProcessorSimpleTransform(this));
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet)
    {
        // Process join results set as a regular join, includes sorting and having-clause filter
        UniformPair<EventBean[]> result = processJoinResult(joinSet, CollectionUtil.EMPTY_ROW_SET, true);
        return new ArrayEventIterator(result.getFirst());
    }

    public void clear()
    {
        // No need to clear state, there is no state held
    }

    public boolean hasAggregation() {
        return false;
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
    }

    public void processOutputLimitedLastNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        if (prototype.getOptionalHavingExpr() == null) {
            if (newData != null && newData.length > 0) {
                outputLastIStreamBufView = newData[newData.length - 1];
            }
            if (oldData != null && oldData.length > 0) {
                outputLastRStreamBufView = oldData[oldData.length - 1];
            }
        }
        else {
            EventBean[] eventsPerStream = new EventBean[1];
            if (newData != null && newData.length > 0) {
                for (EventBean theEvent : newData) {
                    eventsPerStream[0] = theEvent;

                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseNonJoin(theEvent);}
                    Boolean passesHaving = (Boolean) prototype.getOptionalHavingExpr().evaluate(eventsPerStream, true, exprEvaluatorContext);
                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseNonJoin(passesHaving);}
                    if ((passesHaving == null) || (!passesHaving)) {
                        continue;
                    }
                    outputLastIStreamBufView = theEvent;
                }
            }
            if (oldData != null && oldData.length > 0) {
                for (EventBean theEvent : oldData) {
                    eventsPerStream[0] = theEvent;

                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseNonJoin(theEvent);}
                    Boolean passesHaving = (Boolean) prototype.getOptionalHavingExpr().evaluate(eventsPerStream, false, exprEvaluatorContext);
                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseNonJoin(passesHaving);}
                    if ((passesHaving == null) || (!passesHaving)) {
                        continue;
                    }
                    outputLastRStreamBufView = theEvent;
                }
            }
        }
    }

    public void processOutputLimitedLastNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic) {
        if (prototype.getOptionalHavingExpr() == null) {
            if (newEvents != null && !newEvents.isEmpty()) {
                outputLastIStreamBufJoin = getLast(newEvents);
            }
            if (oldEvents != null && !oldEvents.isEmpty()) {
                outputLastRStreamBufJoin = getLast(oldEvents);
            }
        }
        else {
            if (newEvents != null && newEvents.size() > 0) {
                for (MultiKey<EventBean> theEvent : newEvents) {
                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseJoin(theEvent.getArray());}
                    Boolean passesHaving = (Boolean) prototype.getOptionalHavingExpr().evaluate(theEvent.getArray(), true, exprEvaluatorContext);
                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseJoin(passesHaving);}
                    if ((passesHaving == null) || (!passesHaving)) {
                        continue;
                    }
                    outputLastIStreamBufJoin = theEvent;
                }
            }
            if (oldEvents != null && oldEvents.size() > 0) {
                for (MultiKey<EventBean> theEvent : oldEvents) {

                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qHavingClauseJoin(theEvent.getArray());}
                    Boolean passesHaving = (Boolean) prototype.getOptionalHavingExpr().evaluate(theEvent.getArray(), false, exprEvaluatorContext);
                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aHavingClauseJoin(passesHaving);}
                    if ((passesHaving == null) || (!passesHaving)) {
                        continue;
                    }
                    outputLastRStreamBufJoin = theEvent;
                }
            }
        }
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastNonBufferedView(boolean isSynthesize) {
        if (outputLastIStreamBufView == null && outputLastRStreamBufView == null) {
            return null;
        }
        UniformPair<EventBean[]> pair = processViewResult(toArr(outputLastIStreamBufView), toArr(outputLastRStreamBufView), isSynthesize);
        outputLastIStreamBufView = null;
        outputLastRStreamBufView = null;
        return pair;
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastNonBufferedJoin(boolean isSynthesize) {
        if (outputLastIStreamBufJoin == null && outputLastRStreamBufJoin == null) {
            return null;
        }
        UniformPair<EventBean[]> pair = processJoinResult(toSet(outputLastIStreamBufJoin), toSet(outputLastRStreamBufJoin), isSynthesize);
        outputLastIStreamBufJoin = null;
        outputLastRStreamBufJoin = null;
        return pair;
    }

    private Set<MultiKey<EventBean>> toSet(MultiKey<EventBean> row) {
        if (row == null) {
            return null;
        }
        return Collections.singleton(row);
    }

    private MultiKey<EventBean> getLast(Set<MultiKey<EventBean>> events) {
        int count = 0;
        for (MultiKey<EventBean> row : events) {
            count++;
            if (count == events.size()) {
                return row;
            }
        }
        throw new IllegalStateException("Cannot get last on empty collection");
    }

    private EventBean[] toArr(EventBean optionalEvent) {
        if (optionalEvent == null) {
            return null;
        }
        return new EventBean[] {optionalEvent};
    }
}
