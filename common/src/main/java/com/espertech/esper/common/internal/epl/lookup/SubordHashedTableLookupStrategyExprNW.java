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
public class SubordHashedTableLookupStrategyExprNW implements SubordTableLookupStrategy {
    private final SubordHashedTableLookupStrategyExprFactory factory;
    private final PropertyHashedEventTable index;

    public SubordHashedTableLookupStrategyExprNW(SubordHashedTableLookupStrategyExprFactory factory, PropertyHashedEventTable index) {
        this.factory = factory;
        this.index = index;
    }

    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        if (context.getInstrumentationProvider().activated()) {
            context.getInstrumentationProvider().qIndexSubordLookup(this, index, null);
            Object key = getKey(events, context);
            Set<EventBean> result = index.lookup(key);
            context.getInstrumentationProvider().aIndexSubordLookup(result, key);
            return result;
        }

        Object key = getKey(events, context);
        return index.lookup(key);
    }

    protected Object getKey(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        return factory.evaluator.evaluate(eventsPerStream, true, context);
    }

    public LookupStrategyDesc getStrategyDesc() {
        return factory.getLookupStrategyDesc();
    }

    public String toQueryPlan() {
        return factory.toQueryPlan();
    }
}
