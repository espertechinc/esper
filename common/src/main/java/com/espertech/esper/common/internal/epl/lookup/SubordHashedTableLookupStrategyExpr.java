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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;

import java.util.Collection;
import java.util.Set;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordHashedTableLookupStrategyExpr implements SubordTableLookupStrategy {
    private final SubordHashedTableLookupStrategyExprFactory factory;
    private final PropertyHashedEventTable index;
    private EventBean[] events;

    public SubordHashedTableLookupStrategyExpr(SubordHashedTableLookupStrategyExprFactory factory, PropertyHashedEventTable index) {
        this.factory = factory;
        events = new EventBean[factory.numStreamsOuter + 1];
        this.index = index;
    }

    /**
     * Returns index to look up in.
     *
     * @return index to use
     */
    public PropertyHashedEventTable getIndex() {
        return index;
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (context.getInstrumentationProvider().activated()) {
            context.getInstrumentationProvider().qIndexSubordLookup(this, index, null);
            Object key = getKey(eventsPerStream, context);
            Set<EventBean> result = index.lookup(key);
            context.getInstrumentationProvider().aIndexSubordLookup(result, key);
            return result;
        }

        Object key = getKey(eventsPerStream, context);
        return index.lookup(key);
    }

    /**
     * Get the index lookup keys.
     *
     * @param eventsPerStream is the events for each stream
     * @param context         context
     * @return key object
     */
    protected Object getKey(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);
        return factory.evaluator.evaluate(events, true, context);
    }

    public LookupStrategyDesc getStrategyDesc() {
        return factory.getLookupStrategyDesc();
    }

    public String toQueryPlan() {
        return factory.toQueryPlan();
    }
}
