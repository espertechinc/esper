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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.*;

public class CompositeAccessStrategyRangeNormal extends CompositeAccessStrategyRangeBase implements CompositeAccessStrategy {

    private boolean allowReverseRange;

    public CompositeAccessStrategyRangeNormal(boolean isNWOnTrigger, int lookupStream, int numStreams, ExprEvaluator start, boolean includeStart, ExprEvaluator end, boolean includeEnd, boolean allowReverseRange) {
        super(isNWOnTrigger, lookupStream, numStreams, start, includeStart, end, includeEnd);
        this.allowReverseRange = allowReverseRange;
    }

    public Set<EventBean> lookup(EventBean theEvent, Map parent, Set<EventBean> result, CompositeIndexQuery next, ExprEvaluatorContext context, ArrayList<Object> optionalKeyCollector, CompositeIndexQueryResultPostProcessor postProcessor) {
        Object comparableStart = super.evaluateLookupStart(theEvent, context);
        if (optionalKeyCollector != null) {
            optionalKeyCollector.add(comparableStart);
        }
        if (comparableStart == null) {
            return null;
        }
        Object comparableEnd = super.evaluateLookupEnd(theEvent, context);
        if (optionalKeyCollector != null) {
            optionalKeyCollector.add(comparableEnd);
        }
        if (comparableEnd == null) {
            return null;
        }
        TreeMap index = (TreeMap) parent;

        SortedMap<Object, Set<EventBean>> submap;
        try {
            submap = index.subMap(comparableStart, includeStart, comparableEnd, includeEnd);
        } catch (IllegalArgumentException ex) {
            if (allowReverseRange) {
                submap = index.subMap(comparableEnd, includeStart, comparableStart, includeEnd);
            } else {
                return null;
            }
        }

        return CompositeIndexQueryRange.handle(theEvent, submap, null, result, next, postProcessor);
    }

    public Collection<EventBean> lookup(EventBean[] eventPerStream, Map parent, Collection<EventBean> result, CompositeIndexQuery next, ExprEvaluatorContext context, ArrayList<Object> optionalKeyCollector, CompositeIndexQueryResultPostProcessor postProcessor) {
        Object comparableStart = super.evaluatePerStreamStart(eventPerStream, context);
        if (optionalKeyCollector != null) {
            optionalKeyCollector.add(comparableStart);
        }
        if (comparableStart == null) {
            return null;
        }
        Object comparableEnd = super.evaluatePerStreamEnd(eventPerStream, context);
        if (optionalKeyCollector != null) {
            optionalKeyCollector.add(comparableEnd);
        }
        if (comparableEnd == null) {
            return null;
        }
        TreeMap index = (TreeMap) parent;

        SortedMap<Object, Set<EventBean>> submap;
        try {
            submap = index.subMap(comparableStart, includeStart, comparableEnd, includeEnd);
        } catch (IllegalArgumentException ex) {
            if (allowReverseRange) {
                submap = index.subMap(comparableEnd, includeStart, comparableStart, includeEnd);
            } else {
                return null;
            }
        }

        return CompositeIndexQueryRange.handle(eventPerStream, submap, null, result, next, postProcessor);
    }
}
