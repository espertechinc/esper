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
import com.espertech.esper.epl.join.table.UnindexedEventTable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

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
        return lookupInternal();
    }

    private Set<EventBean> lookupInternal() {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexSubordLookup(this, eventIndex, null);
        }
        Set<EventBean> result = eventIndex.getEventSet();
        if (result.isEmpty()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aIndexSubordLookup(null, null);
            }
            return null;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aIndexSubordLookup(result, null);
        }
        return result;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.FULLTABLESCAN, null);
    }
}
