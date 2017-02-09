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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexQuery;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexQueryFactory;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryHashKeyed;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRange;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.PropertyCompositeEventTable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Index lookup strategy into a poll-based cache result.
 */
public class HistoricalIndexLookupStrategyComposite implements HistoricalIndexLookupStrategy {
    private final CompositeIndexQuery chain;

    public HistoricalIndexLookupStrategyComposite(int lookupStream, List<QueryGraphValueEntryHashKeyed> hashKeys, Class[] keyCoercionTypes, List<QueryGraphValueEntryRange> rangeKeyPairs, Class[] rangeCoercionTypes) {
        chain = CompositeIndexQueryFactory.makeJoinSingleLookupStream(false, lookupStream, hashKeys, keyCoercionTypes, rangeKeyPairs, rangeCoercionTypes);
    }

    public Iterator<EventBean> lookup(EventBean lookupEvent, EventTable[] indexTable, ExprEvaluatorContext context) {
        // The table may not be indexed as the cache may not actively cache, in which case indexing doesn't makes sense
        if (indexTable[0] instanceof PropertyCompositeEventTable) {
            PropertyCompositeEventTable table = (PropertyCompositeEventTable) indexTable[0];
            Map<Object, Object> index = table.getIndex();

            Set<EventBean> events = chain.get(lookupEvent, index, context, table.getPostProcessor());
            if (events != null) {
                return events.iterator();
            }
            return null;
        }

        return indexTable[0].iterator();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " chain " + chain.getClass().getSimpleName();
    }
}
