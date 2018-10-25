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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionState;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.support.SupportBean;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SupportAggMFMultiRTEnumerableEventsState implements AggregationMultiFunctionState {

    private List<EventBean> events = new ArrayList<EventBean>();

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        events.add(eventsPerStream[0]);
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // ever semantics
    }

    public void clear() {
        events.clear();
    }

    public int size() {
        return events.size();
    }

    public List<EventBean> getEvents() {
        return events;
    }

    public Object getEventsAsUnderlyingArray() {
        SupportBean[] array = new SupportBean[events.size()];

        Iterator<EventBean> it = events.iterator();
        int count = 0;
        for (; it.hasNext(); ) {
            EventBean bean = it.next();
            Array.set(array, count++, bean.getUnderlying());
        }
        return array;
    }
}
