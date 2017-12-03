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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.filterspec.DoubleRange;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index for filter parameter constants for the range operators (range open/closed/half).
 * The implementation is based on the SortedMap implementation of TreeMap and stores only expression
 * parameter values of type DoubleRange.
 */
public final class FilterParamIndexDoubleRange extends FilterParamIndexDoubleRangeBase {
    public FilterParamIndexDoubleRange(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock, FilterOperator filterOperator) {
        super(lookupable, readWriteLock, filterOperator);
        if (!(filterOperator.isRangeOperator())) {
            throw new IllegalArgumentException("Invalid filter operator " + filterOperator);
        }
    }

    public final void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        Object objAttributeValue = lookupable.getGetter().get(theEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, objAttributeValue);
        }

        if (objAttributeValue == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        }

        double attributeValue = ((Number) objAttributeValue).doubleValue();

        DoubleRange rangeStart = new DoubleRange(attributeValue - largestRangeValueDouble, attributeValue);
        DoubleRange rangeEnd = new DoubleRange(attributeValue, Double.MAX_VALUE);

        SortedMap<DoubleRange, EventEvaluator> subMap = ranges.subMap(rangeStart, rangeEnd);

        // For not including either endpoint
        // A bit awkward to duplicate the loop code, however better than checking the boolean many times over
        // This may be a bit of an early performance optimization - the optimizer after all may do this better
        if (this.getFilterOperator() == FilterOperator.RANGE_OPEN) {
            // include neither endpoint
            for (Map.Entry<DoubleRange, EventEvaluator> entry : subMap.entrySet()) {
                if ((attributeValue > entry.getKey().getMin()) &&
                        (attributeValue < entry.getKey().getMax())) {
                    entry.getValue().matchEvent(theEvent, matches);
                }
            }
        } else if (this.getFilterOperator() == FilterOperator.RANGE_CLOSED) {
            // include all endpoints
            for (Map.Entry<DoubleRange, EventEvaluator> entry : subMap.entrySet()) {
                if ((attributeValue >= entry.getKey().getMin()) &&
                        (attributeValue <= entry.getKey().getMax())) {
                    entry.getValue().matchEvent(theEvent, matches);
                }
            }
        } else if (this.getFilterOperator() == FilterOperator.RANGE_HALF_CLOSED) {
            // include high endpoint not low endpoint
            for (Map.Entry<DoubleRange, EventEvaluator> entry : subMap.entrySet()) {
                if ((attributeValue > entry.getKey().getMin()) &&
                        (attributeValue <= entry.getKey().getMax())) {
                    entry.getValue().matchEvent(theEvent, matches);
                }
            }
        } else if (this.getFilterOperator() == FilterOperator.RANGE_HALF_OPEN) {
            // include low endpoint not high endpoint
            for (Map.Entry<DoubleRange, EventEvaluator> entry : subMap.entrySet()) {
                if ((attributeValue >= entry.getKey().getMin()) &&
                        (attributeValue < entry.getKey().getMax())) {
                    entry.getValue().matchEvent(theEvent, matches);
                }
            }
        } else {
            throw new IllegalStateException("Invalid filter operator " + this.getFilterOperator());
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(null);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(FilterParamIndexDoubleRange.class);
}
