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
package com.espertech.esper.epl.join.exec.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.exec.sorted.SortedAccessStrategy;
import com.espertech.esper.epl.join.exec.sorted.SortedAccessStrategyFactory;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRange;
import com.espertech.esper.epl.join.rep.Cursor;
import com.espertech.esper.epl.join.table.PropertySortedEventTable;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.epl.lookup.LookupStrategyType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.ArrayList;
import java.util.Set;

/**
 * Lookup on an index that is a sorted index on a single property queried as a range.
 * <p>
 * Use the composite strategy if supporting multiple ranges or if range is in combination with unique key.
 */
public class SortedTableLookupStrategy implements JoinExecTableLookupStrategy {
    private final QueryGraphValueEntryRange rangeKeyPair;
    private final PropertySortedEventTable index;
    private final SortedAccessStrategy strategy;

    public SortedTableLookupStrategy(int lookupStream, int numStreams, QueryGraphValueEntryRange rangeKeyPair, Class coercionType, PropertySortedEventTable index) {
        this.rangeKeyPair = rangeKeyPair;
        this.index = index;
        this.strategy = SortedAccessStrategyFactory.make(false, lookupStream, numStreams, rangeKeyPair, coercionType);
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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexJoinLookup(this, index);
            ArrayList<Object> keys = new ArrayList<Object>(2);
            Set<EventBean> result = strategy.lookupCollectKeys(theEvent, index, exprEvaluatorContext, keys);
            InstrumentationHelper.get().aIndexJoinLookup(result, keys.size() > 1 ? keys.toArray() : keys.get(0));
            return result;
        }

        return strategy.lookup(theEvent, index, exprEvaluatorContext);
    }

    public String toString() {
        return "SortedTableLookupStrategy indexProps=" + rangeKeyPair +
                " index=(" + index + ')';
    }

    public LookupStrategyDesc getStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.RANGE, ExprNodeUtilityCore.toExpressionStringsMinPrecedence(rangeKeyPair.getExpressions()));
    }
}
