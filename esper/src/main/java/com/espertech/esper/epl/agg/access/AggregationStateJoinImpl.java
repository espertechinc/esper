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
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.*;

/**
 * Implementation of access function for joins.
 */
public class AggregationStateJoinImpl implements AggregationStateWithSize, AggregationStateLinear {
    protected int streamId;
    protected LinkedHashMap<EventBean, Integer> refSet = new LinkedHashMap<EventBean, Integer>();
    private EventBean[] array;

    /**
     * Ctor.
     *
     * @param streamId stream id
     */
    public AggregationStateJoinImpl(int streamId) {
        this.streamId = streamId;
    }

    public void clear() {
        refSet.clear();
        array = null;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[streamId];
        if (theEvent == null) {
            return;
        }
        array = null;
        Integer value = refSet.get(theEvent);
        if (value == null) {
            refSet.put(theEvent, 1);
            return;
        }

        value++;
        refSet.put(theEvent, value);
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[streamId];
        if (theEvent == null) {
            return;
        }
        array = null;

        Integer value = refSet.get(theEvent);
        if (value == null) {
            return;
        }

        if (value == 1) {
            refSet.remove(theEvent);
            return;
        }

        value--;
        refSet.put(theEvent, value);
    }

    public EventBean getFirstNthValue(int index) {
        if (index < 0) {
            return null;
        }
        if (refSet.isEmpty()) {
            return null;
        }
        if (index >= refSet.size()) {
            return null;
        }
        if (array == null) {
            initArray();
        }
        return array[index];
    }

    public EventBean getLastNthValue(int index) {
        if (index < 0) {
            return null;
        }
        if (refSet.isEmpty()) {
            return null;
        }
        if (index >= refSet.size()) {
            return null;
        }
        if (array == null) {
            initArray();
        }
        return array[array.length - index - 1];
    }

    public EventBean getFirstValue() {
        if (refSet.isEmpty()) {
            return null;
        }
        return refSet.entrySet().iterator().next().getKey();
    }

    public EventBean getLastValue() {
        if (refSet.isEmpty()) {
            return null;
        }
        if (array == null) {
            initArray();
        }
        return array[array.length - 1];
    }

    public Iterator<EventBean> iterator() {
        if (array == null) {
            initArray();
        }
        return new ArrayEventIterator(array);
    }

    public Collection<EventBean> collectionReadOnly() {
        if (array == null) {
            initArray();
        }
        return Arrays.asList(array);
    }

    public int size() {
        return refSet.size();
    }

    private void initArray() {
        Set<EventBean> events = refSet.keySet();
        array = events.toArray(new EventBean[events.size()]);
    }
}