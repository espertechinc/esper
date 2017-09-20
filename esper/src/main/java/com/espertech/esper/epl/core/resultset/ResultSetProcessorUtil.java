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
package com.espertech.esper.epl.core.resultset;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.Viewable;

import java.util.*;

public class ResultSetProcessorUtil {
    public static void applyAggViewResult(AggregationService aggregationService, ExprEvaluatorContext exprEvaluatorContext, EventBean[] newData, EventBean[] oldData, EventBean[] eventsPerStream) {
        if (newData != null) {
            // apply new data to aggregates
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                aggregationService.applyEnter(eventsPerStream, null, exprEvaluatorContext);
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            for (int i = 0; i < oldData.length; i++) {
                eventsPerStream[0] = oldData[i];
                aggregationService.applyLeave(eventsPerStream, null, exprEvaluatorContext);
            }
        }
    }

    public static void applyAggJoinResult(AggregationService aggregationService, ExprEvaluatorContext exprEvaluatorContext, Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        if (newEvents != null && !newEvents.isEmpty()) {
            // apply new data to aggregates
            for (MultiKey<EventBean> events : newEvents) {
                aggregationService.applyEnter(events.getArray(), null, exprEvaluatorContext);
            }
        }
        if (oldEvents != null && !oldEvents.isEmpty()) {
            // apply old data to aggregates
            for (MultiKey<EventBean> events : oldEvents) {
                aggregationService.applyLeave(events.getArray(), null, exprEvaluatorContext);
            }
        }
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param events               - input events
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param exprEvaluatorContext context for expression evalauation
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectEventsNoHaving(SelectExprProcessor exprProcessor, EventBean[] events, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return null;
        }

        EventBean[] result = new EventBean[events.length];
        EventBean[] eventsPerStream = new EventBean[1];
        for (int i = 0; i < events.length; i++) {
            eventsPerStream[0] = events[i];
            result[i] = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
        }
        return result;
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param orderByProcessor     - orders the outgoing events according to the order-by clause
     * @param events               - input events
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param exprEvaluatorContext context for expression evalauation
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectEventsNoHavingWithOrderBy(SelectExprProcessor exprProcessor, OrderByProcessor orderByProcessor, EventBean[] events, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return null;
        }

        EventBean[] result = new EventBean[events.length];
        EventBean[][] eventGenerators = new EventBean[events.length][];

        EventBean[] eventsPerStream = new EventBean[1];
        for (int i = 0; i < events.length; i++) {
            eventsPerStream[0] = events[i];
            result[i] = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            eventGenerators[i] = new EventBean[]{events[i]};
        }

        return orderByProcessor.sort(result, eventGenerators, isNewData, exprEvaluatorContext);
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     * <p>
     * Also applies a having clause.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param orderByProcessor     - for sorting output events according to the order-by clause
     * @param events               - input events
     * @param havingNode           - supplies the having-clause expression
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param exprEvaluatorContext context for expression evalauation
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectEventsHavingWithOrderBy(SelectExprProcessor exprProcessor, OrderByProcessor orderByProcessor, EventBean[] events, ExprEvaluator havingNode, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return null;
        }

        ArrayDeque<EventBean> result = null;
        ArrayDeque<EventBean[]> eventGenerators = null;

        EventBean[] eventsPerStream = new EventBean[1];
        for (EventBean theEvent : events) {
            eventsPerStream[0] = theEvent;

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qHavingClauseNonJoin(theEvent);
            }
            Boolean passesHaving = (Boolean) havingNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aHavingClauseNonJoin(passesHaving);
            }
            if ((passesHaving == null) || (!passesHaving)) {
                continue;
            }

            EventBean generated = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (generated != null) {
                if (result == null) {
                    result = new ArrayDeque<EventBean>(events.length);
                    eventGenerators = new ArrayDeque<EventBean[]>(events.length);
                }
                result.add(generated);
                eventGenerators.add(new EventBean[]{theEvent});
            }
        }

        if (result != null) {
            return orderByProcessor.sort(result.toArray(new EventBean[result.size()]), eventGenerators.toArray(new EventBean[eventGenerators.size()][]), isNewData, exprEvaluatorContext);
        }
        return null;
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     * <p>
     * Also applies a having clause.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param events               - input events
     * @param havingNode           - supplies the having-clause expression
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param exprEvaluatorContext context for expression evalauation
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectEventsHaving(SelectExprProcessor exprProcessor,
                                                       EventBean[] events,
                                                       ExprEvaluator havingNode,
                                                       boolean isNewData,
                                                       boolean isSynthesize,
                                                       ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return null;
        }

        ArrayDeque<EventBean> result = null;
        EventBean[] eventsPerStream = new EventBean[1];

        for (EventBean theEvent : events) {
            eventsPerStream[0] = theEvent;

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qHavingClauseNonJoin(theEvent);
            }
            Boolean passesHaving = (Boolean) havingNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aHavingClauseNonJoin(passesHaving);
            }
            if ((passesHaving == null) || (!passesHaving)) {
                continue;
            }

            EventBean generated = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (generated != null) {
                if (result == null) {
                    result = new ArrayDeque<EventBean>(events.length);
                }
                result.add(generated);
            }
        }

        if (result != null) {
            return result.toArray(new EventBean[result.size()]);
        }
        return null;
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param orderByProcessor     - for sorting output events according to the order-by clause
     * @param events               - input events
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param exprEvaluatorContext context for expression evalauation
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectJoinEventsNoHavingWithOrderBy(SelectExprProcessor exprProcessor, OrderByProcessor orderByProcessor, Set<MultiKey<EventBean>> events, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        if ((events == null) || (events.isEmpty())) {
            return null;
        }

        EventBean[] result = new EventBean[events.size()];
        EventBean[][] eventGenerators = new EventBean[events.size()][];

        int count = 0;
        for (MultiKey<EventBean> key : events) {
            EventBean[] eventsPerStream = key.getArray();
            result[count] = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            eventGenerators[count] = eventsPerStream;
            count++;
        }

        return orderByProcessor.sort(result, eventGenerators, isNewData, exprEvaluatorContext);
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param events               - input events
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param exprEvaluatorContext context for expression evalauation
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectJoinEventsNoHaving(SelectExprProcessor exprProcessor, Set<MultiKey<EventBean>> events, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        if ((events == null) || (events.isEmpty())) {
            return null;
        }

        EventBean[] result = new EventBean[events.size()];
        int count = 0;

        for (MultiKey<EventBean> key : events) {
            EventBean[] eventsPerStream = key.getArray();
            result[count] = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            count++;
        }

        return result;
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     * <p>
     * Also applies a having clause.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param events               - input events
     * @param havingNode           - supplies the having-clause expression
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param exprEvaluatorContext context for expression evalauation
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectJoinEventsHaving(SelectExprProcessor exprProcessor, Set<MultiKey<EventBean>> events, ExprEvaluator havingNode, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        if ((events == null) || (events.isEmpty())) {
            return null;
        }

        ArrayDeque<EventBean> result = null;

        for (MultiKey<EventBean> key : events) {
            EventBean[] eventsPerStream = key.getArray();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);
            }
            Boolean passesHaving = (Boolean) havingNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aHavingClauseJoin(passesHaving);
            }
            if ((passesHaving == null) || (!passesHaving)) {
                continue;
            }

            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                if (result == null) {
                    result = new ArrayDeque<EventBean>(events.size());
                }
                result.add(resultEvent);
            }
        }

        if (result != null) {
            return result.toArray(new EventBean[result.size()]);
        }
        return null;
    }

    /**
     * Applies the select-clause to the given events returning the selected events. The number of events stays the
     * same, i.e. this method does not filter it just transforms the result set.
     * <p>
     * Also applies a having clause.
     *
     * @param exprProcessor        - processes each input event and returns output event
     * @param orderByProcessor     - for sorting output events according to the order-by clause
     * @param events               - input events
     * @param havingNode           - supplies the having-clause expression
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param isSynthesize         - set to true to indicate that synthetic events are required for an iterator result set
     * @param exprEvaluatorContext context for expression evalauation
     * @return output events, one for each input event
     */
    protected static EventBean[] getSelectJoinEventsHavingWithOrderBy(SelectExprProcessor exprProcessor, OrderByProcessor orderByProcessor, Set<MultiKey<EventBean>> events, ExprEvaluator havingNode, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        if ((events == null) || (events.isEmpty())) {
            return null;
        }

        ArrayDeque<EventBean> result = null;
        ArrayDeque<EventBean[]> eventGenerators = null;

        for (MultiKey<EventBean> key : events) {
            EventBean[] eventsPerStream = key.getArray();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);
            }
            Boolean passesHaving = (Boolean) havingNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aHavingClauseJoin(passesHaving);
            }
            if ((passesHaving == null) || (!passesHaving)) {
                continue;
            }

            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                if (result == null) {
                    result = new ArrayDeque<EventBean>(events.size());
                    eventGenerators = new ArrayDeque<EventBean[]>(events.size());
                }
                result.add(resultEvent);
                eventGenerators.add(eventsPerStream);
            }
        }

        if (result != null) {
            return orderByProcessor.sort(result.toArray(new EventBean[result.size()]), eventGenerators.toArray(new EventBean[eventGenerators.size()][]), isNewData, exprEvaluatorContext);
        }
        return null;
    }

    protected static void populateSelectEventsNoHaving(SelectExprProcessor exprProcessor, EventBean[] events, boolean isNewData, boolean isSynthesize, Collection<EventBean> result, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        for (EventBean theEvent : events) {
            eventsPerStream[0] = theEvent;

            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                result.add(resultEvent);
            }
        }
    }

    protected static void populateSelectEventsNoHavingWithOrderBy(SelectExprProcessor exprProcessor, OrderByProcessor orderByProcessor, EventBean[] events, boolean isNewData, boolean isSynthesize, Collection<EventBean> result, List<Object> sortKeys, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        for (EventBean theEvent : events) {
            eventsPerStream[0] = theEvent;

            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                result.add(resultEvent);
                sortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, exprEvaluatorContext));
            }
        }
    }

    protected static void populateSelectEventsHaving(SelectExprProcessor exprProcessor, EventBean[] events, ExprEvaluator havingNode, boolean isNewData, boolean isSynthesize, List<EventBean> result, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        for (EventBean theEvent : events) {
            eventsPerStream[0] = theEvent;

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qHavingClauseNonJoin(theEvent);
            }
            Boolean passesHaving = (Boolean) havingNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aHavingClauseNonJoin(passesHaving);
            }
            if ((passesHaving == null) || (!passesHaving)) {
                continue;
            }

            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                result.add(resultEvent);
            }
        }
    }

    protected static void populateSelectEventsHavingWithOrderBy(SelectExprProcessor exprProcessor, OrderByProcessor orderByProcessor, EventBean[] events, ExprEvaluator havingNode, boolean isNewData, boolean isSynthesize, List<EventBean> result, List<Object> optSortKeys, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        for (EventBean theEvent : events) {
            eventsPerStream[0] = theEvent;

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qHavingClauseNonJoin(theEvent);
            }
            Boolean passesHaving = (Boolean) havingNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aHavingClauseNonJoin(passesHaving);
            }
            if ((passesHaving == null) || (!passesHaving)) {
                continue;
            }

            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                result.add(resultEvent);
                optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, exprEvaluatorContext));
            }
        }
    }

    protected static void populateSelectJoinEventsHaving(SelectExprProcessor exprProcessor, Set<MultiKey<EventBean>> events, ExprEvaluator havingNode, boolean isNewData, boolean isSynthesize, List<EventBean> result, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return;
        }

        for (MultiKey<EventBean> key : events) {
            EventBean[] eventsPerStream = key.getArray();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);
            }
            Boolean passesHaving = (Boolean) havingNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aHavingClauseJoin(passesHaving);
            }
            if ((passesHaving == null) || (!passesHaving)) {
                continue;
            }

            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                result.add(resultEvent);
            }
        }
    }

    protected static void populateSelectJoinEventsHavingWithOrderBy(SelectExprProcessor exprProcessor, OrderByProcessor orderByProcessor, Set<MultiKey<EventBean>> events, ExprEvaluator havingNode, boolean isNewData, boolean isSynthesize, List<EventBean> result, List<Object> sortKeys, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return;
        }

        for (MultiKey<EventBean> key : events) {
            EventBean[] eventsPerStream = key.getArray();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qHavingClauseJoin(eventsPerStream);
            }
            Boolean passesHaving = (Boolean) havingNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aHavingClauseJoin(passesHaving);
            }
            if ((passesHaving == null) || (!passesHaving)) {
                continue;
            }

            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                result.add(resultEvent);
                sortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, exprEvaluatorContext));
            }
        }
    }

    protected static void populateSelectJoinEventsNoHaving(SelectExprProcessor exprProcessor, Set<MultiKey<EventBean>> events, boolean isNewData, boolean isSynthesize, List<EventBean> result, ExprEvaluatorContext exprEvaluatorContext) {
        int length = (events != null) ? events.size() : 0;
        if (length == 0) {
            return;
        }

        for (MultiKey<EventBean> key : events) {
            EventBean[] eventsPerStream = key.getArray();
            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                result.add(resultEvent);
            }
        }
    }

    protected static void populateSelectJoinEventsNoHavingWithOrderBy(SelectExprProcessor exprProcessor, OrderByProcessor orderByProcessor, Set<MultiKey<EventBean>> events, boolean isNewData, boolean isSynthesize, List<EventBean> result, List<Object> optSortKeys, ExprEvaluatorContext exprEvaluatorContext) {
        int length = (events != null) ? events.size() : 0;
        if (length == 0) {
            return;
        }

        for (MultiKey<EventBean> key : events) {
            EventBean[] eventsPerStream = key.getArray();
            EventBean resultEvent = exprProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            if (resultEvent != null) {
                result.add(resultEvent);
                optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, exprEvaluatorContext));
            }
        }
    }

    public static void clearAndAggregateUngrouped(ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService, Viewable parent) {
        aggregationService.clearResults(exprEvaluatorContext);
        Iterator<EventBean> it = parent.iterator();
        EventBean[] eventsPerStream = new EventBean[1];
        for (; it.hasNext(); ) {
            eventsPerStream[0] = it.next();
            aggregationService.applyEnter(eventsPerStream, null, exprEvaluatorContext);
        }
    }

    public static ArrayDeque<EventBean> iteratorToDeque(Iterator<EventBean> it) {
        ArrayDeque<EventBean> deque = new ArrayDeque<EventBean>();
        for (; it.hasNext(); ) {
            deque.add(it.next());
        }
        return deque;
    }
}
