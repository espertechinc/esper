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

/**
 * Aggregation computing an event arrival rate for data windowed-events.
 */
public class AggregatorRate implements AggregationMethod {

    protected final long oneSecondTime;
    protected double accumulator;
    protected long latest;
    protected long oldest;
    protected boolean isSet = false;

    public AggregatorRate(long oneSecondTime) {
        this.oneSecondTime = oneSecondTime;
    }

    public void enter(Object value) {
        if (value.getClass().isArray()) {
            enterValueArr((Object[]) value);
        } else {
            enterValueSingle(value);
        }
    }

    public void leave(Object value) {
        if (value.getClass().isArray()) {
            leaveValueArr((Object[]) value);
        } else {
            leaveValueSingle(value);
        }
    }

    public Object getValue() {
        if (!isSet) return null;
        return (accumulator * oneSecondTime) / (latest - oldest);
    }

    public void clear() {
        accumulator = 0;
        latest = 0;
        oldest = 0;
    }

    public long getOneSecondTime() {
        return oneSecondTime;
    }

    public double getAccumulator() {
        return accumulator;
    }

    public void setAccumulator(double accumulator) {
        this.accumulator = accumulator;
    }

    public long getLatest() {
        return latest;
    }

    public void setLatest(long latest) {
        this.latest = latest;
    }

    public long getOldest() {
        return oldest;
    }

    public void setOldest(long oldest) {
        this.oldest = oldest;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setSet(boolean set) {
        isSet = set;
    }

    protected void enterValueSingle(Object value) {
        accumulator += 1;
        latest = (Long) value;
    }

    protected void enterValueArr(Object[] parameters) {
        Number val = (Number) parameters[1];
        accumulator += val.doubleValue();
        latest = (Long) parameters[0];
    }

    protected void leaveValueArr(Object[] parameters) {
        Number val = (Number) parameters[1];
        accumulator -= val.doubleValue();
        oldest = (Long) parameters[0];
        if (!isSet) isSet = true;
    }

    protected void leaveValueSingle(Object value) {
        accumulator -= 1;
        oldest = (Long) value;
        if (!isSet) isSet = true;
    }
}