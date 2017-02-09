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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Implementation of access function for single-stream (not joins).
 */
public class AggregationStateImpl implements AggregationStateWithSize, AggregationStateLinear {
    protected int streamId;
    protected ArrayList<EventBean> events = new ArrayList<EventBean>();

    /**
     * Ctor.
     *
     * @param streamId stream id
     */
    public AggregationStateImpl(int streamId) {
        this.streamId = streamId;
    }

    public void clear() {
        events.clear();
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[streamId];
        if (theEvent == null) {
            return;
        }
        events.remove(theEvent);
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[streamId];
        if (theEvent == null) {
            return;
        }
        events.add(theEvent);
    }

    public EventBean getFirstNthValue(int index) {
        if (index < 0) {
            return null;
        }
        if (index >= events.size()) {
            return null;
        }
        return events.get(index);
    }

    public EventBean getLastNthValue(int index) {
        if (index < 0) {
            return null;
        }
        if (index >= events.size()) {
            return null;
        }
        return events.get(events.size() - index - 1);
    }

    public EventBean getFirstValue() {
        if (events.isEmpty()) {
            return null;
        }
        return events.get(0);
    }

    public EventBean getLastValue() {
        if (events.isEmpty()) {
            return null;
        }
        return events.get(events.size() - 1);
    }

    public Iterator<EventBean> iterator() {
        return events.iterator();
    }

    public Collection<EventBean> collectionReadOnly() {
        return events;
    }

    public int size() {
        return events.size();
    }
}
