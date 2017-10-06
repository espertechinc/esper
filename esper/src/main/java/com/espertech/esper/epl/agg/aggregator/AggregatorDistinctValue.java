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
package com.espertech.esper.epl.agg.aggregator;

import com.espertech.esper.collection.RefCountedSet;

/**
 * AggregationMethod for use on top of another aggregator that handles unique value aggregation (versus all-value aggregation)
 * for the underlying aggregator.
 */
public class AggregatorDistinctValue implements AggregationMethod {
    protected final AggregationMethod inner;
    protected final RefCountedSet<Object> valueSet;

    /**
     * Ctor.
     *
     * @param inner is the aggregator function computing aggregation values
     */
    public AggregatorDistinctValue(AggregationMethod inner) {
        this.inner = inner;
        this.valueSet = new RefCountedSet<Object>();
    }

    public void enter(Object value) {
        // if value not already encountered, enter into aggregate
        if (valueSet.add(value)) {
            inner.enter(value);
        }
    }

    public void leave(Object value) {
        // if last reference to the value is removed, remove from aggregate
        if (valueSet.remove(value)) {
            inner.leave(value);
        }
    }

    public void clear() {
        valueSet.clear();
        inner.clear();
    }

    public Object getValue() {
        return inner.getValue();
    }
}
