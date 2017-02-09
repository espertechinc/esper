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
 * Standard deviation always generates double-typed numbers.
 */
public class AggregatorStddev implements AggregationMethod {
    protected double mean;
    protected double qn;
    protected long numDataPoints;

    public void clear() {
        mean = 0;
        numDataPoints = 0;
        qn = 0;
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }

        double p = ((Number) object).doubleValue();

        // compute running variance per Knuth's method
        if (numDataPoints == 0) {
            mean = p;
            qn = 0;
            numDataPoints = 1;
        } else {
            numDataPoints++;
            double oldmean = mean;
            mean += (p - mean) / numDataPoints;
            qn += (p - oldmean) * (p - mean);
        }
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }

        double p = ((Number) object).doubleValue();

        // compute running variance per Knuth's method
        if (numDataPoints <= 1) {
            clear();
        } else {
            numDataPoints--;
            double oldmean = mean;
            mean -= (p - mean) / numDataPoints;
            qn -= (p - oldmean) * (p - mean);
        }
    }

    public Object getValue() {
        if (numDataPoints < 2) {
            return null;
        }
        return Math.sqrt(qn / (numDataPoints - 1));
    }

}
