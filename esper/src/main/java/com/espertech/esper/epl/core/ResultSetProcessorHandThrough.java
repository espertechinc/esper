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
import com.espertech.esper.collection.TransformEventIterator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.Viewable;

import java.util.Iterator;
import java.util.Set;

/**
 * Result set processor for the hand-through case:
 * no aggregation functions used in the select clause, and no group-by, no having and ordering.
 */
public class ResultSetProcessorHandThrough extends ResultSetProcessorBaseSimple {
    private final ResultSetProcessorHandThroughFactory prototype;
    private final SelectExprProcessor selectExprProcessor;
    private AgentInstanceContext agentInstanceContext;

    public ResultSetProcessorHandThrough(ResultSetProcessorHandThroughFactory prototype, SelectExprProcessor selectExprProcessor, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.agentInstanceContext = agentInstanceContext;
    }

    public void setAgentInstanceContext(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    public EventType getResultEventType() {
        return prototype.getResultEventType();
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize) {
        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;

        if (prototype.isSelectRStream()) {
            selectOldEvents = getSelectEventsNoHaving(selectExprProcessor, oldEvents, false, isSynthesize, agentInstanceContext);
        }
        selectNewEvents = getSelectEventsNoHaving(selectExprProcessor, newEvents, true, isSynthesize, agentInstanceContext);

        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        EventBean[] selectOldEvents = null;

        if (prototype.isSelectRStream()) {
            selectOldEvents = getSelectEventsNoHaving(selectExprProcessor, oldData, false, isSynthesize, agentInstanceContext);
        }
        EventBean[] selectNewEvents = getSelectEventsNoHaving(selectExprProcessor, newData, true, isSynthesize, agentInstanceContext);

        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param events               - input events
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param agentInstanceContext context
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectEventsNoHaving(SelectExprProcessor exprProcessor, EventBean[] events, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext agentInstanceContext) {
        if (events == null) {
            return null;
        }

        EventBean[] result = new EventBean[events.length];

        EventBean[] eventsPerStream = new EventBean[1];
        for (int i = 0; i < events.length; i++) {
            eventsPerStream[0] = events[i];
            result[i] = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
        }

        return result;
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param events               - input events
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param agentInstanceContext context
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectEventsNoHaving(SelectExprProcessor exprProcessor, Set<MultiKey<EventBean>> events, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext agentInstanceContext) {
        int length = events.size();
        if (length == 0) {
            return null;
        }

        EventBean[] result = new EventBean[length];
        int count = 0;
        for (MultiKey<EventBean> key : events) {
            EventBean[] eventsPerStream = key.getArray();
            result[count] = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
            count++;
        }

        return result;
    }


    public void clear() {
        // No need to clear state, there is no state held
    }

    public Iterator<EventBean> getIterator(Viewable parent) {
        // Return an iterator that gives row-by-row a result
        return new TransformEventIterator(parent.iterator(), new ResultSetProcessorSimpleTransform(this));
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        // Process join results set as a regular join, includes sorting and having-clause filter
        UniformPair<EventBean[]> result = processJoinResult(joinSet, CollectionUtil.EMPTY_ROW_SET, true);
        return new ArrayEventIterator(result.getFirst());
    }

    public boolean hasAggregation() {
        return false;
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic, boolean isAll) {
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic, boolean isAll) {
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize, boolean isAll) {
        return null;
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedJoin(boolean isSynthesize, boolean isAll) {
        return null;
    }

    public void stop() {
        // no action required
    }

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
        // nothing to visit
    }
}
