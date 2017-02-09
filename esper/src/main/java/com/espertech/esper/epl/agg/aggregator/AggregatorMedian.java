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

import com.espertech.esper.collection.SortedDoubleVector;

/**
 * Median aggregation.
 */
public class AggregatorMedian implements AggregationMethod {
    protected SortedDoubleVector vector;

    public void clear() {
        vector.clear();
    }

    /**
     * Ctor.
     */
    public AggregatorMedian() {
        this.vector = new SortedDoubleVector();
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        double value = ((Number) object).doubleValue();
        vector.add(value);
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }
        double value = ((Number) object).doubleValue();
        vector.remove(value);
    }

    public Object getValue() {
        if (vector.size() == 0) {
            return null;
        }
        if (vector.size() == 1) {
            return vector.getValue(0);
        }

        int middle = vector.size() >> 1;
        if (vector.size() % 2 == 0) {
            return (vector.getValue(middle - 1) + vector.getValue(middle)) / 2;
        } else {
            return vector.getValue(middle);
        }
    }

}
