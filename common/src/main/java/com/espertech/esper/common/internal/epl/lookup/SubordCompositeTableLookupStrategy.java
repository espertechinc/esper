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
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordCompositeTableLookupStrategy implements SubordTableLookupStrategy {
    private final SubordCompositeTableLookupStrategyFactory factory;
    private final PropertyCompositeEventTable index;

    public SubordCompositeTableLookupStrategy(SubordCompositeTableLookupStrategyFactory factory, PropertyCompositeEventTable index) {
        this.factory = factory;
        this.index = index;
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (context.getInstrumentationProvider().activated()) {
            context.getInstrumentationProvider().qIndexSubordLookup(this, index, null);
            ArrayList<Object> keys = new ArrayList<Object>(2); // can collect nulls
            Collection<EventBean> result = factory.innerIndexQuery.getCollectKeys(eventsPerStream, index.getIndex(), context, keys, index.getPostProcessor());
            context.getInstrumentationProvider().aIndexSubordLookup(result, keys.size() > 1 ? keys.toArray() : keys.get(0));
            return result;
        }

        return factory.innerIndexQuery.get(eventsPerStream, index.getIndex(), context, index.getPostProcessor());
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return factory.getLookupStrategyDesc();
    }
}
