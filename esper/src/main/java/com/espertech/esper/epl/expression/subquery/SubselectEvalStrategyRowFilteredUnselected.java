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
package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventBeanUtility;

import java.util.ArrayDeque;
import java.util.Collection;

public class SubselectEvalStrategyRowFilteredUnselected implements SubselectEvalStrategyRow {

    // Filter and no-select
    public Object evaluate(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprSubselectRowNode parent) {
        EventBean[] eventsZeroBased = EventBeanUtility.allocatePerStreamShift(eventsPerStream);
        EventBean subSelectResult = ExprSubselectRowNodeUtility.evaluateFilterExpectSingleMatch(eventsZeroBased, newData, matchingEvents, exprEvaluatorContext, parent);
        if (subSelectResult == null) {
            return null;
        }
        return subSelectResult.getUnderlying();
    }

    // Filter and no-select
    public Collection<EventBean> evaluateGetCollEvents(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context, ExprSubselectRowNode parent) {
        EventBean[] events = EventBeanUtility.allocatePerStreamShift(eventsPerStream);

        ArrayDeque<EventBean> filtered = null;
        for (EventBean subselectEvent : matchingEvents) {
            events[0] = subselectEvent;
            Boolean pass = (Boolean) parent.filterExpr.evaluate(events, true, context);
            if ((pass != null) && pass) {
                if (filtered == null) {
                    filtered = new ArrayDeque<EventBean>();
                }
                filtered.add(subselectEvent);
            }
        }
        return filtered;
    }

    // Filter and no-select
    public Collection evaluateGetCollScalar(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context, ExprSubselectRowNode parent) {
        return null;
    }

    // Filter and no-select
    public Object[] typableEvaluate(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprSubselectRowNode parent) {
        return null;
    }

    // Filer and no-select
    public Object[][] typableEvaluateMultirow(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprSubselectRowNode parent) {
        return null;
    }

    // Filter and no-select
    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context, ExprSubselectRowNode parent) {
        return null;
    }
}
