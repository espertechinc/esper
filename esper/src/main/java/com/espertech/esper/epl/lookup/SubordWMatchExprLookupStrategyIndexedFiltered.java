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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class SubordWMatchExprLookupStrategyIndexedFiltered implements SubordWMatchExprLookupStrategy {
    private final ExprEvaluator joinExpr;
    private final EventBean[] eventsPerStream;
    private final SubordTableLookupStrategy tableLookupStrategy;

    /**
     * Ctor.
     *
     * @param joinExpr            the validated where clause of the on-delete
     * @param tableLookupStrategy the strategy for looking up in an index the matching events using correlation
     */
    public SubordWMatchExprLookupStrategyIndexedFiltered(ExprEvaluator joinExpr, SubordTableLookupStrategy tableLookupStrategy) {
        this.joinExpr = joinExpr;
        this.eventsPerStream = new EventBean[2];
        this.tableLookupStrategy = tableLookupStrategy;
    }

    public SubordTableLookupStrategy getTableLookupStrategy() {
        return tableLookupStrategy;
    }

    public EventBean[] lookup(EventBean[] newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraTriggeredLookup(SubordWMatchExprLookupStrategyType.INDEXED_FILTERED);
        }
        Set<EventBean> foundEvents = null;

        // For every new event (usually 1)
        for (EventBean newEvent : newData) {
            eventsPerStream[1] = newEvent;

            // use index to find match
            Collection<EventBean> matches = tableLookupStrategy.lookup(eventsPerStream, exprEvaluatorContext);
            if ((matches == null) || (matches.isEmpty())) {
                continue;
            }

            // evaluate expression
            Iterator<EventBean> eventsIt = matches.iterator();
            for (; eventsIt.hasNext(); ) {
                eventsPerStream[0] = eventsIt.next();

                for (EventBean aNewData : newData) {
                    eventsPerStream[1] = aNewData;    // Stream 1 events are the originating events (on-delete events)

                    Boolean result = (Boolean) joinExpr.evaluate(eventsPerStream, true, exprEvaluatorContext);
                    if (result != null) {
                        if (result) {
                            if (foundEvents == null) {
                                foundEvents = new LinkedHashSet<EventBean>();
                            }
                            foundEvents.add(eventsPerStream[0]);
                        }
                    }
                }
            }
        }

        if (foundEvents == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aInfraTriggeredLookup(null);
            }
            return null;
        }

        EventBean[] events = foundEvents.toArray(new EventBean[foundEvents.size()]);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraTriggeredLookup(events);
        }

        return events;
    }

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " " + " strategy " + tableLookupStrategy.toQueryPlan();
    }
}
