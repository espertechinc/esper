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
package com.espertech.esper.common.internal.epl.join.exec.sorted;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.sorted.PropertySortedEventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.rep.Cursor;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyType;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.ArrayList;
import java.util.Set;

/**
 * Lookup on an index that is a sorted index on a single property queried as a range.
 * <p>
 * Use the composite strategy if supporting multiple ranges or if range is in combination with unique key.
 */
public class SortedTableLookupStrategy implements JoinExecTableLookupStrategy {
    private final PropertySortedEventTable index;
    private final SortedAccessStrategy strategy;

    public SortedTableLookupStrategy(int lookupStream, int numStreams, QueryGraphValueEntryRange rangeKeyPair, PropertySortedEventTable index) {
        this.index = index;
        this.strategy = SortedAccessStrategyFactory.make(false, lookupStream, numStreams, rangeKeyPair);
    }

    /**
     * Returns index to look up in.
     *
     * @return index to use
     */
    public PropertySortedEventTable getIndex() {
        return index;
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        if (instrumentationCommon.activated()) {
            instrumentationCommon.qIndexJoinLookup(this, index);
            ArrayList<Object> keys = new ArrayList<Object>(2);
            Set<EventBean> result = strategy.lookupCollectKeys(theEvent, index, exprEvaluatorContext, keys);
            instrumentationCommon.aIndexJoinLookup(result, keys.size() > 1 ? keys.toArray() : keys.get(0));
            return result;
        }

        return strategy.lookup(theEvent, index, exprEvaluatorContext);
    }

    public LookupStrategyType getLookupStrategyType() {
        return LookupStrategyType.RANGE;
    }
}
