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
package com.espertech.esper.example.ohlc;

public class OHLCBarValue {
    private long minuteValue;
    private Double first;
    private Double last;
    private Double max;
    private Double min;

    public OHLCBarValue(long minuteValue, Double first, Double last, Double max, Double min) {
        this.minuteValue = minuteValue;
        this.first = first;
        this.last = last;
        this.max = max;
        this.min = min;
    }

    public long getMinuteValue() {
        return minuteValue;
    }

    public Double getFirst() {
        return first;
    }

    public Double getLast() {
        return last;
    }

    public Double getMax() {
        return max;
    }

    public Double getMin() {
        return min;
    }
}
