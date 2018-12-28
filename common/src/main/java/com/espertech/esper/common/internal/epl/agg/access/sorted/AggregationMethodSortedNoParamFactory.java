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

import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregationMethodSortedKeyedFactory.*;

public class AggregationMethodSortedNoParamFactory {

    public static AggregationMethodSortedNoParamEval makeSortedAggregationNoParam(AggregationMethodSortedEnum method, Class underlyingClass) {
        if (method.getFootprint() != AggregationMethodSortedFootprintEnum.NOPARAM) {
            throw new IllegalStateException("Unrecognized aggregation method " + method);
        }
        if (method == AggregationMethodSortedEnum.FIRSTEVENT) {
            return new AggregationMethodSortedNoParamEval(
                sorted -> firstUnd(sorted.getSorted().firstEntry()),
                sorted -> firstBean(sorted.getSorted().firstEntry()),
                sorted -> firstColl(sorted.getSorted().firstEntry())
            );
        }
        if (method == AggregationMethodSortedEnum.FIRSTEVENTS) {
            return new AggregationMethodSortedNoParamEval(
                sorted -> eventsArrayUnd(sorted.getSorted().firstEntry(), underlyingClass),
                sorted -> null,
                sorted -> eventsColl(sorted.getSorted().firstEntry())
            );
        }
        if (method == AggregationMethodSortedEnum.FIRSTKEY) {
            return new AggregationMethodSortedNoParamEval(
                sorted -> sorted.getSorted().firstKey(),
                sorted -> null,
                sorted -> null
            );
        }
        if (method == AggregationMethodSortedEnum.LASTEVENT) {
            return new AggregationMethodSortedNoParamEval(
                sorted -> firstUnd(sorted.getSorted().lastEntry()),
                sorted -> firstBean(sorted.getSorted().lastEntry()),
                sorted -> firstColl(sorted.getSorted().lastEntry())
            );
        }
        if (method == AggregationMethodSortedEnum.LASTEVENTS) {
            return new AggregationMethodSortedNoParamEval(
                sorted -> eventsArrayUnd(sorted.getSorted().lastEntry(), underlyingClass),
                sorted -> null,
                sorted -> eventsColl(sorted.getSorted().lastEntry())
            );
        }
        if (method == AggregationMethodSortedEnum.LASTKEY) {
            return new AggregationMethodSortedNoParamEval(
                sorted -> sorted.getSorted().lastKey(),
                sorted -> null,
                sorted -> null
            );
        }
        if (method == AggregationMethodSortedEnum.COUNTEVENTS) {
            return new AggregationMethodSortedNoParamEval(
                sorted -> sorted.getSize(),
                sorted -> null,
                sorted -> null
            );
        }
        if (method == AggregationMethodSortedEnum.COUNTKEYS) {
            return new AggregationMethodSortedNoParamEval(
                sorted -> sorted.getSorted().size(),
                sorted -> null,
                sorted -> null
            );
        }
        if (method == AggregationMethodSortedEnum.NAVIGABLEMAPREFERENCE) {
            return new AggregationMethodSortedNoParamEval(
                sorted -> new AggregationMethodSortedWrapperNavigableMap(sorted.getSorted()),
                sorted -> null,
                sorted -> null
            );
        }
        throw new IllegalStateException("Unrecognized aggregation method " + method);
    }
}
