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

import com.espertech.esper.schedule.TimeProvider;

import java.util.ArrayDeque;

/**
 * Aggregation computing an event arrival rate for with and without data window.
 */
public class AggregatorRateEver implements AggregationMethod {

    protected final long interval;
    protected final long oneSecondTime;
    protected final ArrayDeque<Long> points;
    protected boolean hasLeave = false;
    protected final TimeProvider timeProvider;

    /**
     * Ctor.
     *
     * @param interval     rate interval
     * @param timeProvider time
     * @param oneSecondTime number of ticks for one second
     */
    public AggregatorRateEver(long interval, long oneSecondTime, TimeProvider timeProvider) {
        this.interval = interval;
        this.oneSecondTime = oneSecondTime;
        this.timeProvider = timeProvider;
        points = new ArrayDeque<Long>();
    }

    public void clear() {
        points.clear();
    }

    public void enter(Object object) {
        long timestamp = timeProvider.getTime();
        points.add(timestamp);
        removeFromHead(timestamp);
    }

    public void leave(Object object) {
        // This is an "ever" aggregator and is designed for use in non-window env
    }

    public Object getValue() {
        if (!points.isEmpty()) {
            long newest = points.getLast();
            removeFromHead(newest);
        }
        if (!hasLeave) {
            return null;
        }
        if (points.isEmpty()) {
            return 0d;
        }
        return (points.size() * oneSecondTime * 1d) / interval;
    }

    private void removeFromHead(long timestamp) {
        if (points.size() > 1) {
            while (true) {
                long first = points.getFirst();
                long delta = timestamp - first;
                if (delta >= interval) {
                    points.remove();
                    hasLeave = true;
                } else if (delta == interval) {
                    hasLeave = true;
                    break;
                } else {
                    break;
                }
                if (points.isEmpty()) {
                    break;
                }
            }
        }
    }

}