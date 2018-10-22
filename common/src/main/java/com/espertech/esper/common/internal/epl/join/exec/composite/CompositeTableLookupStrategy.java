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
package com.espertech.esper.common.internal.epl.join.exec.composite;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.rep.Cursor;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyType;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.ArrayList;
import java.util.Set;

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

    public CompositeTableLookupStrategy(EventType eventType, int lookupStream, ExprEvaluator hashKeys, QueryGraphValueEntryRange[] rangeKeyPairs, PropertyCompositeEventTable index) {
        this.eventType = eventType;
        this.index = index;
        chain = CompositeIndexQueryFactory.makeJoinSingleLookupStream(false, lookupStream, hashKeys, rangeKeyPairs);
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
        InstrumentationCommon instrumentationCommon = context.getInstrumentationProvider();
        if (instrumentationCommon.activated()) {
            instrumentationCommon.qIndexJoinLookup(this, index);
            ArrayList<Object> keys = new ArrayList<Object>(2);
            Set<EventBean> result = chain.getCollectKeys(theEvent, index.getIndex(), context, keys, index.getPostProcessor());
            instrumentationCommon.aIndexJoinLookup(result, keys.size() > 1 ? keys.toArray() : keys.get(0));
            return result;
        }

        Set<EventBean> result = chain.get(theEvent, index.getIndex(), context, index.getPostProcessor());
        if (result != null && result.isEmpty()) {
            return null;
        }
        return result;
    }

    public LookupStrategyType getLookupStrategyType() {
        return LookupStrategyType.COMPOSITE;
    }
}
