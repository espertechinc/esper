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
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.Viewable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Result set processor for the case: aggregation functions used in the select clause, and no group-by,
 * and all properties in the select clause are under an aggregation function.
 * <p>
 * This processor does not perform grouping, every event entering and leaving is in the same group.
 * Produces one old event and one new event row every time either at least one old or new event is received.
 * Aggregation state is simply one row holding all the state.
 */
public class ResultSetProcessorRowForAll implements ResultSetProcessor {
    protected final ResultSetProcessorRowForAllFactory prototype;
    private final SelectExprProcessor selectExprProcessor;
    private final OrderByProcessor orderByProcessor;
    protected final AggregationService aggregationService;
    protected ExprEvaluatorContext exprEvaluatorContext;
    private ResultSetProcessorRowForAllOutputLastHelper outputLastHelper;
    private ResultSetProcessorRowForAllOutputAllHelper outputAllHelper;

    public ResultSetProcessorRowForAll(ResultSetProcessorRowForAllFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.exprEvaluatorContext = agentInstanceContext;
        if (prototype.isOutputLast()) {
            outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowForAllOutputLast(this, prototype, agentInstanceContext);
        } else if (prototype.isOutputAll()) {
            outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowForAllOutputAll(this, prototype, agentInstanceContext);
        }
    }

    public void setAgentInstanceContext(AgentInstanceContext context) {
        this.exprEvaluatorContext = context;
    }

    public EventType getResultEventType() {
        return prototype.getResultEventType();
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessUngroupedFullyAgg();
        }
        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;

        if (prototype.isUnidirectional()) {
            this.clear();
        }

        if (prototype.isSelectRStream()) {
            selectOldEvents = getSelectListEvents(false, isSynthesize, true);
        }

        ResultSetProcessorUtil.applyAggJoinResult(aggregationService, exprEvaluatorContext, newEvents, oldEvents);

        selectNewEvents = getSelectListEvents(true, isSynthesize, true);

        if ((selectNewEvents == null) && (selectOldEvents == null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aResultSetProcessUngroupedFullyAgg(null, null);
            }
            return null;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessUngroupedFullyAgg(selectNewEvents, selectOldEvents);
        }
        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessUngroupedFullyAgg();
        }
        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;

        if (prototype.isSelectRStream()) {
            selectOldEvents = getSelectListEvents(false, isSynthesize, false);
        }

        EventBean[] eventsPerStream = new EventBean[1];
        ResultSetProcessorUtil.applyAggViewResult(aggregationService, exprEvaluatorContext, newData, oldData, eventsPerStream);

        // generate new events using select expressions
        selectNewEvents = getSelectListEvents(true, isSynthesize, false);

        if ((selectNewEvents == null) && (selectOldEvents == null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aResultSetProcessUngroupedFullyAgg(null, null);
            }
            return null;
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessUngroupedFullyAgg(selectNewEvents, selectOldEvents);
        }
        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    public EventBean[] getSelectListEvents(boolean isNewData, boolean isSynthesize, boolean join) {
        if (prototype.getOptionalHavingNode() != null) {
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().qHavingClauseNonJoin(null);
                else InstrumentationHelper.get().qHavingClauseJoin(null);
            }
            Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(null, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().aHavingClauseNonJoin(result);
                else InstrumentationHelper.get().aHavingClauseJoin(result);
            }
            if ((result == null) || (!result)) {
                return null;
            }
        }

        // Since we are dealing with strictly aggregation nodes, there are no events required for evaluating
        EventBean theEvent = selectExprProcessor.process(CollectionUtil.EVENTBEANARRAY_EMPTY, isNewData, isSynthesize, exprEvaluatorContext);

        // The result is always a single row
        return new EventBean[]{theEvent};
    }

    private EventBean getSelectListEvent(boolean isNewData, boolean isSynthesize, boolean join) {
        if (prototype.getOptionalHavingNode() != null) {
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().qHavingClauseNonJoin(null);
                else InstrumentationHelper.get().qHavingClauseJoin(null);
            }
            Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(null, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().aHavingClauseNonJoin(result);
                else InstrumentationHelper.get().aHavingClauseJoin(result);
            }

            if ((result == null) || (!result)) {
                return null;
            }
        }

        // Since we are dealing with strictly aggregation nodes, there are no events required for evaluating
        EventBean theEvent = selectExprProcessor.process(CollectionUtil.EVENTBEANARRAY_EMPTY, isNewData, isSynthesize, exprEvaluatorContext);

        // The result is always a single row
        return theEvent;
    }

    public Iterator<EventBean> getIterator(Viewable parent) {
        if (!prototype.isHistoricalOnly()) {
            return obtainIterator();
        }

        ResultSetProcessorUtil.clearAndAggregateUngrouped(exprEvaluatorContext, aggregationService, parent);

        Iterator<EventBean> iterator = obtainIterator();
        aggregationService.clearResults(exprEvaluatorContext);
        return iterator;
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        EventBean[] result = getSelectListEvents(true, true, true);
        return new ArrayEventIterator(result);
    }

    public void clear() {
        aggregationService.clearResults(exprEvaluatorContext);
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType) {
        if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            return processOutputLimitedJoinLast(joinEventsSet, generateSynthetic);
        } else {
            return processOutputLimitedJoinDefault(joinEventsSet, generateSynthetic);
        }
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType) {
        if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            return processOutputLimitedViewLast(viewEventsList, generateSynthetic);
        } else {
            return processOutputLimitedViewDefault(viewEventsList, generateSynthetic);
        }
    }

    public boolean hasAggregation() {
        return true;
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        EventBean[] events = new EventBean[1];
        ResultSetProcessorUtil.applyAggViewResult(aggregationService, exprEvaluatorContext, newData, oldData, events);
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        ResultSetProcessorUtil.applyAggJoinResult(aggregationService, exprEvaluatorContext, newEvents, oldEvents);
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public void stop() {
        if (outputLastHelper != null) {
            outputLastHelper.destroy();
        }
        if (outputAllHelper != null) {
            outputAllHelper.destroy();
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

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
        if (outputLastHelper != null) {
            visitor.visit(outputLastHelper);
        }
        if (outputAllHelper != null) {
            visitor.visit(outputAllHelper);
        }
    }

    private void getSelectListEvent(boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, boolean join) {
        if (prototype.getOptionalHavingNode() != null) {
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().qHavingClauseNonJoin(null);
                else InstrumentationHelper.get().qHavingClauseJoin(null);
            }
            Boolean result = (Boolean) prototype.getOptionalHavingNode().evaluate(null, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                if (!join) InstrumentationHelper.get().aHavingClauseNonJoin(result);
                else InstrumentationHelper.get().aHavingClauseJoin(result);
            }
            if ((result == null) || (!result)) {
                return;
            }
        }

        // Since we are dealing with strictly aggregation nodes, there are no events required for evaluating
        EventBean theEvent = selectExprProcessor.process(CollectionUtil.EVENTBEANARRAY_EMPTY, isNewData, isSynthesize, exprEvaluatorContext);

        resultEvents.add(theEvent);
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
            if (prototype.isUnidirectional()) {
                this.clear();
            }

            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            if (prototype.isSelectRStream()) {
                getSelectListEvent(false, generateSynthetic, oldEvents, true);
            }

            if (newData != null) {
                // apply new data to aggregates
                for (MultiKey<EventBean> row : newData) {
                    aggregationService.applyEnter(row.getArray(), null, exprEvaluatorContext);
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                for (MultiKey<EventBean> row : oldData) {
                    aggregationService.applyLeave(row.getArray(), null, exprEvaluatorContext);
                }
            }

            getSelectListEvent(false, generateSynthetic, newEvents, true);
        }

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream()) {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }

        if (orderByProcessor != null) {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, exprEvaluatorContext);
            if (prototype.isSelectRStream()) {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, exprEvaluatorContext);
            }
        }

        if (joinEventsSet.isEmpty()) {
            if (prototype.isSelectRStream()) {
                oldEventsArr = getSelectListEvents(false, generateSynthetic, true);
            }
            newEventsArr = getSelectListEvents(true, generateSynthetic, true);
        }

        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinLast(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        EventBean lastOldEvent = null;
        EventBean lastNewEvent = null;

        // if empty (nothing to post)
        if (joinEventsSet.isEmpty()) {
            if (prototype.isSelectRStream()) {
                lastOldEvent = getSelectListEvent(false, generateSynthetic, true);
                lastNewEvent = lastOldEvent;
            } else {
                lastNewEvent = getSelectListEvent(false, generateSynthetic, true);
            }
        }

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            if (prototype.isUnidirectional()) {
                this.clear();
            }

            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            if ((lastOldEvent == null) && (prototype.isSelectRStream())) {
                lastOldEvent = getSelectListEvent(false, generateSynthetic, true);
            }

            if (newData != null) {
                // apply new data to aggregates
                for (MultiKey<EventBean> eventsPerStream : newData) {
                    aggregationService.applyEnter(eventsPerStream.getArray(), null, exprEvaluatorContext);
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                for (MultiKey<EventBean> eventsPerStream : oldData) {
                    aggregationService.applyLeave(eventsPerStream.getArray(), null, exprEvaluatorContext);
                }
            }

            lastNewEvent = getSelectListEvent(true, generateSynthetic, true);
        }

        EventBean[] lastNew = (lastNewEvent != null) ? new EventBean[]{lastNewEvent} : null;
        EventBean[] lastOld = (lastOldEvent != null) ? new EventBean[]{lastOldEvent} : null;

        if ((lastNew == null) && (lastOld == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(lastNew, lastOld);
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

            if (prototype.isSelectRStream()) {
                getSelectListEvent(false, generateSynthetic, oldEvents, false);
            }

            EventBean[] eventsPerStream = new EventBean[1];
            if (newData != null) {
                // apply new data to aggregates
                for (EventBean aNewData : newData) {
                    eventsPerStream[0] = aNewData;
                    aggregationService.applyEnter(eventsPerStream, null, exprEvaluatorContext);
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                for (EventBean anOldData : oldData) {
                    eventsPerStream[0] = anOldData;
                    aggregationService.applyLeave(eventsPerStream, null, exprEvaluatorContext);
                }
            }

            getSelectListEvent(true, generateSynthetic, newEvents, false);
        }

        EventBean[] newEventsArr = (newEvents.isEmpty()) ? null : newEvents.toArray(new EventBean[newEvents.size()]);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream()) {
            oldEventsArr = (oldEvents.isEmpty()) ? null : oldEvents.toArray(new EventBean[oldEvents.size()]);
        }
        if (orderByProcessor != null) {
            Object[] sortKeysNew = (newEventsSortKey.isEmpty()) ? null : newEventsSortKey.toArray(new Object[newEventsSortKey.size()]);
            newEventsArr = orderByProcessor.sort(newEventsArr, sortKeysNew, exprEvaluatorContext);
            if (prototype.isSelectRStream()) {
                Object[] sortKeysOld = (oldEventsSortKey.isEmpty()) ? null : oldEventsSortKey.toArray(new Object[oldEventsSortKey.size()]);
                oldEventsArr = orderByProcessor.sort(oldEventsArr, sortKeysOld, exprEvaluatorContext);
            }
        }

        if (viewEventsList.isEmpty()) {
            if (prototype.isSelectRStream()) {
                oldEventsArr = getSelectListEvents(false, generateSynthetic, false);
            }
            newEventsArr = getSelectListEvents(true, generateSynthetic, false);
        }

        if ((newEventsArr == null) && (oldEventsArr == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(newEventsArr, oldEventsArr);
    }

    private UniformPair<EventBean[]> processOutputLimitedViewLast(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        // For last, if there are no events:
        //   As insert stream, return the current value, if matching the having clause
        //   As remove stream, return the current value, if matching the having clause
        // For last, if there are events in the batch:
        //   As insert stream, return the newest value that is matching the having clause
        //   As remove stream, return the oldest value that is matching the having clause

        EventBean lastOldEvent = null;
        EventBean lastNewEvent = null;
        EventBean[] eventsPerStream = new EventBean[1];

        // if empty (nothing to post)
        if (viewEventsList.isEmpty()) {
            if (prototype.isSelectRStream()) {
                lastOldEvent = getSelectListEvent(false, generateSynthetic, false);
                lastNewEvent = lastOldEvent;
            } else {
                lastNewEvent = getSelectListEvent(false, generateSynthetic, false);
            }
        }

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            if ((lastOldEvent == null) && (prototype.isSelectRStream())) {
                lastOldEvent = getSelectListEvent(false, generateSynthetic, false);
            }

            if (newData != null) {
                // apply new data to aggregates
                for (EventBean aNewData : newData) {
                    eventsPerStream[0] = aNewData;
                    aggregationService.applyEnter(eventsPerStream, null, exprEvaluatorContext);
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                for (EventBean anOldData : oldData) {
                    eventsPerStream[0] = anOldData;
                    aggregationService.applyLeave(eventsPerStream, null, exprEvaluatorContext);
                }
            }

            lastNewEvent = getSelectListEvent(false, generateSynthetic, false);
        }

        EventBean[] lastNew = (lastNewEvent != null) ? new EventBean[]{lastNewEvent} : null;
        EventBean[] lastOld = (lastOldEvent != null) ? new EventBean[]{lastOldEvent} : null;

        if ((lastNew == null) && (lastOld == null)) {
            return null;
        }
        return new UniformPair<EventBean[]>(lastNew, lastOld);
    }

    private Iterator<EventBean> obtainIterator() {
        EventBean[] selectNewEvents = getSelectListEvents(true, true, false);
        if (selectNewEvents == null) {
            return CollectionUtil.NULL_EVENT_ITERATOR;
        }
        return new SingleEventIterator(selectNewEvents[0]);
    }
}
