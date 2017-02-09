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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class SubordWMatchExprLookupStrategyAllFiltered implements SubordWMatchExprLookupStrategy {
    private final ExprEvaluator joinExpr;
    private final EventBean[] eventsPerStream;
    private final Iterable<EventBean> iterableEvents;

    /**
     * Ctor.
     *
     * @param joinExpr is the where clause
     * @param iterable iterable
     */
    public SubordWMatchExprLookupStrategyAllFiltered(ExprEvaluator joinExpr, Iterable<EventBean> iterable) {
        this.joinExpr = joinExpr;
        this.eventsPerStream = new EventBean[2];
        this.iterableEvents = iterable;
    }

    public EventBean[] lookup(EventBean[] newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraTriggeredLookup(SubordWMatchExprLookupStrategyType.FULLTABLESCAN_FILTERED);
        }

        Set<EventBean> removeEvents = null;

        Iterator<EventBean> eventsIt = iterableEvents.iterator();
        for (; eventsIt.hasNext(); ) {
            eventsPerStream[0] = eventsIt.next();

            for (EventBean aNewData : newData) {
                eventsPerStream[1] = aNewData;    // Stream 1 events are the originating events (on-delete events)

                Boolean result = (Boolean) joinExpr.evaluate(eventsPerStream, true, exprEvaluatorContext);
                if (result != null) {
                    if (result) {
                        if (removeEvents == null) {
                            removeEvents = new LinkedHashSet<EventBean>();
                        }
                        removeEvents.add(eventsPerStream[0]);
                    }
                }
            }
        }

        if (removeEvents == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aInfraTriggeredLookup(null);
            }
            return null;
        }

        EventBean[] result = removeEvents.toArray(new EventBean[removeEvents.size()]);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraTriggeredLookup(result);
        }
        return result;
    }

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
