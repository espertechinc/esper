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
package com.espertech.esper.epl.join.exec.sorted;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.PropertySortedEventTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class SortedAccessStrategyRangeInverted extends SortedAccessStrategyRangeBase implements SortedAccessStrategy {

    public SortedAccessStrategyRangeInverted(boolean isNWOnTrigger, int lookupStream, int numStreams, ExprEvaluator start, boolean includeStart, ExprEvaluator end, boolean includeEnd) {
        super(isNWOnTrigger, lookupStream, numStreams, start, includeStart, end, includeEnd);
    }

    public Set<EventBean> lookup(EventBean theEvent, PropertySortedEventTable index, ExprEvaluatorContext context) {
        return index.lookupRangeInverted(super.evaluateLookupStart(theEvent, context), includeStart, super.evaluateLookupEnd(theEvent, context), includeEnd);
    }

    public Set<EventBean> lookupCollectKeys(EventBean theEvent, PropertySortedEventTable index, ExprEvaluatorContext context, ArrayList<Object> keys) {
        Object start = super.evaluateLookupStart(theEvent, context);
        keys.add(start);
        Object end = super.evaluateLookupEnd(theEvent, context);
        keys.add(end);
        return index.lookupRangeInverted(start, includeStart, end, includeEnd);
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, PropertySortedEventTable index, ExprEvaluatorContext context) {
        return index.lookupRangeInvertedColl(super.evaluatePerStreamStart(eventsPerStream, context), includeStart, super.evaluatePerStreamEnd(eventsPerStream, context), includeEnd);
    }

    public Collection<EventBean> lookupCollectKeys(EventBean[] eventsPerStream, PropertySortedEventTable index, ExprEvaluatorContext context, ArrayList<Object> keys) {
        Object start = super.evaluatePerStreamStart(eventsPerStream, context);
        keys.add(start);
        Object end = super.evaluatePerStreamEnd(eventsPerStream, context);
        keys.add(end);
        return index.lookupRangeInvertedColl(start, includeStart, end, includeEnd);
    }
}

