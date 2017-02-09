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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.*;

/**
 * Implementation of access function for single-stream (not joins).
 */
public class AggregationStateSortedImpl implements AggregationStateWithSize, AggregationStateSorted {
    protected final AggregationStateSortedSpec spec;
    protected final TreeMap<Object, Object> sorted;
    protected int size;

    /**
     * Ctor.
     *
     * @param spec aggregation spec
     */
    public AggregationStateSortedImpl(AggregationStateSortedSpec spec) {
        this.spec = spec;
        sorted = new TreeMap<Object, Object>(spec.getComparator());
    }

    public void clear() {
        sorted.clear();
        size = 0;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[spec.getStreamId()];
        if (theEvent == null) {
            return;
        }
        if (referenceEvent(theEvent)) {
            Object comparable = getComparable(spec.getCriteria(), eventsPerStream, true, exprEvaluatorContext);
            Object existing = sorted.get(comparable);
            if (existing == null) {
                sorted.put(comparable, theEvent);
            } else if (existing instanceof EventBean) {
                ArrayDeque coll = new ArrayDeque(2);
                coll.add(existing);
                coll.add(theEvent);
                sorted.put(comparable, coll);
            } else {
                ArrayDeque q = (ArrayDeque) existing;
                q.add(theEvent);
            }
            size++;
        }
    }

    protected boolean referenceEvent(EventBean theEvent) {
        // no action
        return true;
    }

    protected boolean dereferenceEvent(EventBean theEvent) {
        // no action
        return true;
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[spec.getStreamId()];
        if (theEvent == null) {
            return;
        }
        if (dereferenceEvent(theEvent)) {
            Object comparable = getComparable(spec.getCriteria(), eventsPerStream, false, exprEvaluatorContext);
            Object existing = sorted.get(comparable);
            if (existing != null) {
                if (existing.equals(theEvent)) {
                    sorted.remove(comparable);
                    size--;
                } else if (existing instanceof ArrayDeque) {
                    ArrayDeque q = (ArrayDeque) existing;
                    q.remove(theEvent);
                    if (q.isEmpty()) {
                        sorted.remove(comparable);
                    }
                    size--;
                }
            }
        }
    }

    public EventBean getFirstValue() {
        if (sorted.isEmpty()) {
            return null;
        }
        Map.Entry<Object, Object> max = sorted.firstEntry();
        return checkedPayload(max.getValue());
    }

    public EventBean getLastValue() {
        if (sorted.isEmpty()) {
            return null;
        }
        Map.Entry<Object, Object> min = sorted.lastEntry();
        return checkedPayload(min.getValue());
    }

    public Iterator<EventBean> iterator() {
        return new AggregationStateSortedIterator(sorted, false);
    }

    public Iterator<EventBean> getReverseIterator() {
        return new AggregationStateSortedIterator(sorted, true);
    }

    public Collection<EventBean> collectionReadOnly() {
        return new AggregationStateSortedWrappingCollection(sorted, size);
    }

    public int size() {
        return size;
    }

    public static Object getComparable(ExprEvaluator[] criteria, EventBean[] eventsPerStream, boolean istream, ExprEvaluatorContext exprEvaluatorContext) {
        if (criteria.length == 1) {
            return criteria[0].evaluate(eventsPerStream, istream, exprEvaluatorContext);
        } else {
            Object[] result = new Object[criteria.length];
            int count = 0;
            for (ExprEvaluator expr : criteria) {
                result[count++] = expr.evaluate(eventsPerStream, true, exprEvaluatorContext);
            }
            return new MultiKeyUntyped(result);
        }
    }

    private EventBean checkedPayload(Object value) {
        if (value instanceof EventBean) {
            return (EventBean) value;
        }
        ArrayDeque<EventBean> q = (ArrayDeque<EventBean>) value;
        return q.getFirst();
    }
}