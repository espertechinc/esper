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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.supportregression.bean.SupportBean;

import java.util.ArrayList;
import java.util.List;

public class SupportAggMFEventsAsListState implements AggregationState {
    private final List<SupportBean> events = new ArrayList<>();

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        events.add((SupportBean) eventsPerStream[0].getUnderlying());
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        events.remove(eventsPerStream[0].getUnderlying());
    }

    public void clear() {
        events.clear();
    }

    public List<SupportBean> getEvents() {
        return events;
    }
}
