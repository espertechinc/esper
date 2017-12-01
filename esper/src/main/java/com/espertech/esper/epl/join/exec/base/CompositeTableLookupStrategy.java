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
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexQuery;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexQueryFactory;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryHashKeyed;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRange;
import com.espertech.esper.epl.join.rep.Cursor;
import com.espertech.esper.epl.join.table.PropertyCompositeEventTable;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.epl.lookup.LookupStrategyType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;

/**
 * Lookup on an nested map structure that represents an index for use with at least one range and possibly multiple ranges
 * and optionally keyed by one or more unique keys.
 * <p>
 * Use the sorted strategy instead if supporting a single range only and no other unique keys are part of the index.
 */
public class CompositeTableLookupStrategy implements JoinExecTableLookupStrategy {
    private final EventType eventType;
    private final PropertyCompositeEventTable index;
    private final CompositeIndexQuery chain;
    private final List<QueryGraphValueEntryRange> rangeKeyPairs;
    private final LookupStrategyDesc lookupStrategyDesc;

    public CompositeTableLookupStrategy(EventType eventType, int lookupStream, List<QueryGraphValueEntryHashKeyed> hashKeys, List<QueryGraphValueEntryRange> rangeKeyPairs, PropertyCompositeEventTable index) {
        this.eventType = eventType;
        this.index = index;
        this.rangeKeyPairs = rangeKeyPairs;
        chain = CompositeIndexQueryFactory.makeJoinSingleLookupStream(false, lookupStream, hashKeys, index.getOptKeyCoercedTypes(), rangeKeyPairs, index.getOptRangeCoercedTypes());

        Deque<String> expressionTexts = new ArrayDeque<String>();
        for (QueryGraphValueEntryRange pair : rangeKeyPairs) {
            ExprNode[] expressions = pair.getExpressions();
            for (ExprNode node : expressions) {
                expressionTexts.add(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(node));
            }
        }
        lookupStrategyDesc = new LookupStrategyDesc(LookupStrategyType.COMPOSITE, expressionTexts.toArray(new String[expressionTexts.size()]));
    }

    /**
     * Returns event type of the lookup event.
     *
     * @return event type of the lookup event
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns index to look up in.
     *
     * @return index to use
     */
    public PropertyCompositeEventTable getIndex() {
        return index;
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexJoinLookup(this, index);
            ArrayList<Object> keys = new ArrayList<Object>(2);
            Set<EventBean> result = chain.getCollectKeys(theEvent, index.getIndex(), context, keys, index.getPostProcessor());
            InstrumentationHelper.get().aIndexJoinLookup(result, keys.size() > 1 ? keys.toArray() : keys.get(0));
            return result;
        }

        Set<EventBean> result = chain.get(theEvent, index.getIndex(), context, index.getPostProcessor());
        if (result != null && result.isEmpty()) {
            return null;
        }
        return result;
    }

    public LookupStrategyDesc getStrategyDesc() {
        return lookupStrategyDesc;
    }

    public String toString() {
        return "CompositeTableLookupStrategy indexProps=" + Arrays.toString(rangeKeyPairs.toArray()) +
                " index=(" + index + ')';
    }
}
