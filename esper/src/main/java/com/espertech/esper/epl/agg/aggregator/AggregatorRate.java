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
            final Object[] parameters = (Object[]) value;
            Number val = (Number) parameters[1];
            accumulator += val.doubleValue();
            latest = (Long) parameters[0];
        } else {
            accumulator += 1;
            latest = (Long) value;
        }
    }

    public void leave(Object value) {
        if (value.getClass().isArray()) {
            final Object[] parameters = (Object[]) value;
            Number val = (Number) parameters[1];
            accumulator -= val.doubleValue();
            oldest = (Long) parameters[0];
        } else {
            accumulator -= 1;
            oldest = (Long) value;
        }
        if (!isSet) isSet = true;
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

}