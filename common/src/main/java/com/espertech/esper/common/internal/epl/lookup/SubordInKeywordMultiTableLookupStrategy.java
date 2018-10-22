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
import com.espertech.esper.common.internal.epl.join.exec.inkeyword.InKeywordTableLookupUtil;

import java.util.Collection;
import java.util.Set;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordInKeywordMultiTableLookupStrategy implements SubordTableLookupStrategy {
    private final SubordInKeywordMultiTableLookupStrategyFactory factory;
    private final PropertyHashedEventTable[] indexes;
    private final EventBean[] events;

    public SubordInKeywordMultiTableLookupStrategy(SubordInKeywordMultiTableLookupStrategyFactory factory, PropertyHashedEventTable[] indexes) {
        this.factory = factory;
        this.indexes = indexes;
        events = new EventBean[factory.streamCountOuter + 1];
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (context.getInstrumentationProvider().activated()) {
            context.getInstrumentationProvider().qIndexSubordLookup(this, null, null);
            System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);
            Set<EventBean> result = InKeywordTableLookupUtil.multiIndexLookup(factory.evaluator, events, context, indexes);
            context.getInstrumentationProvider().aIndexSubordLookup(result, null);
            return result;
        }

        System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);
        return InKeywordTableLookupUtil.multiIndexLookup(factory.evaluator, events, context, indexes);
    }

    public LookupStrategyDesc getStrategyDesc() {
        return factory.getLookupStrategyDesc();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
