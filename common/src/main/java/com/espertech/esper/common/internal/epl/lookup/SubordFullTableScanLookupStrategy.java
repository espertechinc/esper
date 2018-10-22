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
import com.espertech.esper.common.internal.epl.index.unindexed.UnindexedEventTable;

import java.util.Set;

/**
 * Lookup on an unindexed table returning the full table as matching events.
 */
public class SubordFullTableScanLookupStrategy implements SubordTableLookupStrategy {
    private UnindexedEventTable eventIndex;

    /**
     * Ctor.
     *
     * @param eventIndex - table to use
     */
    public SubordFullTableScanLookupStrategy(UnindexedEventTable eventIndex) {
        this.eventIndex = eventIndex;
    }

    public Set<EventBean> lookup(EventBean[] eventPerStream, ExprEvaluatorContext context) {
        if (context.getInstrumentationProvider().activated()) {
            context.getInstrumentationProvider().qIndexSubordLookup(this, eventIndex, null);
            Set<EventBean> result = lookupInternal();
            context.getInstrumentationProvider().aIndexSubordLookup(result, null);
            return result;
        }
        return lookupInternal();
    }

    private Set<EventBean> lookupInternal() {
        Set<EventBean> result = eventIndex.getEventSet();
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return LookupStrategyDesc.SCAN;
    }
}
