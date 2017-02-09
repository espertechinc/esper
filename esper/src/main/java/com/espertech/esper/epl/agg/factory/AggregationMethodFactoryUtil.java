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
package com.espertech.esper.epl.agg.factory;

import com.espertech.esper.epl.agg.aggregator.*;

public class AggregationMethodFactoryUtil {
    public static AggregationMethod makeDistinctAggregator(AggregationMethod aggregationMethod, boolean hasFilter) {
        if (hasFilter) {
            return new AggregatorDistinctValueFilter(aggregationMethod);
        }
        return new AggregatorDistinctValue(aggregationMethod);
    }

    public static AggregationMethod makeFirstEver(boolean hasFilter) {
        if (hasFilter) {
            return new AggregatorFirstEverFilter();
        }
        return new AggregatorFirstEver();
    }

    public static AggregationMethod makeLastEver(boolean hasFilter) {
        if (hasFilter) {
            return new AggregatorLastEverFilter();
        }
        return new AggregatorLastEver();
    }
}
