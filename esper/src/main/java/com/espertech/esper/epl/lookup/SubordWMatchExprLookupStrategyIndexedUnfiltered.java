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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class SubordWMatchExprLookupStrategyIndexedUnfiltered implements SubordWMatchExprLookupStrategy {
    private final EventBean[] eventsPerStream;
    private final SubordTableLookupStrategy tableLookupStrategy;

    /**
     * Ctor.
     *
     * @param tableLookupStrategy the strategy for looking up in an index the matching events using correlation
     */
    public SubordWMatchExprLookupStrategyIndexedUnfiltered(SubordTableLookupStrategy tableLookupStrategy) {
        this.eventsPerStream = new EventBean[2];
        this.tableLookupStrategy = tableLookupStrategy;
    }

    public EventBean[] lookup(EventBean[] newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraTriggeredLookup(SubordWMatchExprLookupStrategyType.INDEXED_UNFILTERED);
        }

        Set<EventBean> removeEvents = null;

        // For every new event (usually 1)
        for (EventBean newEvent : newData) {
            eventsPerStream[1] = newEvent;

            // use index to find match
            Collection<EventBean> matches = tableLookupStrategy.lookup(eventsPerStream, exprEvaluatorContext);
            if ((matches == null) || (matches.isEmpty())) {
                continue;
            }

            if (removeEvents == null) {
                removeEvents = new LinkedHashSet<EventBean>();
            }
            removeEvents.addAll(matches);
        }

        if (removeEvents == null) {
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
        return this.getClass().getSimpleName() + " " + " strategy " + tableLookupStrategy.toQueryPlan();
    }
}
