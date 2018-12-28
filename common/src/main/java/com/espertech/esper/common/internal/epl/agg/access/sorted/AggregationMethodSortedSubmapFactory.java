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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethod;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.lang.reflect.Array;
import java.util.*;

public class AggregationMethodSortedSubmapFactory {

    public static AggregationMultiFunctionAggregationMethod makeSortedAggregationSubmap(ExprEvaluator fromKeyEval,
                                                                                        ExprEvaluator fromInclusiveEval,
                                                                                        ExprEvaluator toKeyEval,
                                                                                        ExprEvaluator toInclusiveEval,
                                                                                        AggregationMethodSortedEnum method,
                                                                                        Class underlyingClass) {
        if (method.getFootprint() != AggregationMethodSortedFootprintEnum.SUBMAP) {
            throw new IllegalStateException("Unrecognized aggregation method " + method);
        }
        if (method == AggregationMethodSortedEnum.EVENTSBETWEEN) {
            return new AggregationMethodSortedEventsBetweenEval(fromKeyEval, fromInclusiveEval, toKeyEval, toInclusiveEval,
                submap -> underlyingEvents(submap, underlyingClass),
                submap -> collEvents(submap)
            );
        }
        if (method == AggregationMethodSortedEnum.SUBMAP) {
            return new AggregationMethodSortedSubmapEval(fromKeyEval, fromInclusiveEval, toKeyEval, toInclusiveEval, underlyingClass);
        }
        throw new IllegalStateException("Unrecognized aggregation method " + method);
    }

    private static Collection collEvents(NavigableMap<Object, Object> submap) {
        if (submap.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayDeque<EventBean> events = new ArrayDeque<>(4);
        for (Map.Entry<Object, Object> entry : submap.entrySet()) {
            AggregatorAccessSortedImpl.checkedPayloadAddAll(events, entry.getValue());
        }
        return events;
    }

    private static Object underlyingEvents(NavigableMap<Object, Object> submap, Class underlyingClass) {
        if (submap.isEmpty()) {
            return Array.newInstance(underlyingClass, 0);
        }

        ArrayDeque<EventBean> events = new ArrayDeque<>(4);
        for (Map.Entry<Object, Object> entry : submap.entrySet()) {
            AggregatorAccessSortedImpl.checkedPayloadAddAll(events, entry.getValue());
        }

        Object array = Array.newInstance(underlyingClass, events.size());
        int index = 0;
        for (EventBean event : events) {
            Array.set(array, index++, event.getUnderlying());
        }
        return array;
    }
}
