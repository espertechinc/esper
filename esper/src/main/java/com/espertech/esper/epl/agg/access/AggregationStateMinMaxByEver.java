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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Implementation of access function for single-stream (not joins).
 */
public class AggregationStateMinMaxByEver implements AggregationState, AggregationStateSorted {
    protected final AggregationStateMinMaxByEverSpec spec;
    protected EventBean currentMinMaxBean;
    protected Object currentMinMax;

    public AggregationStateMinMaxByEver(AggregationStateMinMaxByEverSpec spec) {
        this.spec = spec;
    }

    public void clear() {
        currentMinMax = null;
        currentMinMaxBean = null;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[spec.getStreamId()];
        if (theEvent == null) {
            return;
        }
        Object comparable = AggregationStateSortedImpl.getComparable(spec.getCriteria(), eventsPerStream, true, exprEvaluatorContext);
        if (currentMinMax == null) {
            currentMinMax = comparable;
            currentMinMaxBean = theEvent;
        } else {
            int compareResult = spec.getComparator().compare(currentMinMax, comparable);
            if (spec.isMax()) {
                if (compareResult < 0) {
                    currentMinMax = comparable;
                    currentMinMaxBean = theEvent;
                }
            } else {
                if (compareResult > 0) {
                    currentMinMax = comparable;
                    currentMinMaxBean = theEvent;
                }
            }
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // this is an ever-type aggregation
    }

    public EventBean getFirstValue() {
        if (spec.isMax()) {
            throw new UnsupportedOperationException("Only accepts max-value queries");
        }
        return currentMinMaxBean;
    }

    public EventBean getLastValue() {
        if (!spec.isMax()) {
            throw new UnsupportedOperationException("Only accepts min-value queries");
        }
        return currentMinMaxBean;
    }

    public Iterator<EventBean> iterator() {
        throw new UnsupportedOperationException();
    }

    public Iterator<EventBean> getReverseIterator() {
        throw new UnsupportedOperationException();
    }

    public Collection<EventBean> collectionReadOnly() {
        if (currentMinMaxBean != null) {
            return Collections.singletonList(currentMinMaxBean);
        }
        return null;
    }

    public int size() {
        return currentMinMax == null ? 0 : 1;
    }
}
