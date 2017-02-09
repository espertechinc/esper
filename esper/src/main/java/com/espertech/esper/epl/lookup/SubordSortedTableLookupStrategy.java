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
import com.espertech.esper.epl.join.exec.sorted.SortedAccessStrategy;
import com.espertech.esper.epl.join.table.PropertySortedEventTable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordSortedTableLookupStrategy implements SubordTableLookupStrategy {
    protected final SortedAccessStrategy strategy;

    /**
     * Index to look up in.
     */
    protected final PropertySortedEventTable index;

    private final LookupStrategyDesc strategyDesc;

    public SubordSortedTableLookupStrategy(SortedAccessStrategy strategy, PropertySortedEventTable index, LookupStrategyDesc strategyDesc) {
        this.strategy = strategy;
        this.index = index;
        this.strategyDesc = strategyDesc;
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexSubordLookup(this, index, null);
            ArrayList<Object> keys = new ArrayList<Object>(2);
            Collection<EventBean> result = strategy.lookupCollectKeys(eventsPerStream, index, context, keys);
            InstrumentationHelper.get().aIndexSubordLookup(result, keys.size() > 1 ? keys.toArray() : keys.get(0));
            return result;
        }

        return strategy.lookup(eventsPerStream, index, context);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return strategyDesc;
    }
}
