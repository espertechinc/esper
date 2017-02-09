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

import java.util.Iterator;
import java.util.Map;

/**
 * Standard deviation always generates double-types numbers.
 */
public class AggregatorAvedev implements AggregationMethod {
    private RefCountedSet<Double> valueSet;
    private double sum;

    public void clear() {
        sum = 0;
        valueSet.clear();
    }

    /**
     * Ctor.
     */
    public AggregatorAvedev() {
        valueSet = new RefCountedSet<Double>();
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }

        double value = ((Number) object).doubleValue();
        valueSet.add(value);
        sum += value;
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }

        double value = ((Number) object).doubleValue();
        valueSet.remove(value);
        sum -= value;
    }

    public Object getValue() {
        int datapoints = valueSet.size();

        if (datapoints == 0) {
            return null;
        }

        double total = 0;
        double avg = sum / datapoints;

        for (Iterator<Map.Entry<Double, Integer>> it = valueSet.entryIterator(); it.hasNext(); ) {
            Map.Entry<Double, Integer> entry = it.next();
            total += entry.getValue() * Math.abs(entry.getKey() - avg);
        }

        return total / datapoints;
    }

    public RefCountedSet<Double> getValueSet() {
        return valueSet;
    }

    public void setValueSet(RefCountedSet<Double> valueSet) {
        this.valueSet = valueSet;
    }

    public double getSum() {
        return sum;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }
}
