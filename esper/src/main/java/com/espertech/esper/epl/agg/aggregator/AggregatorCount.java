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
 * Counts all datapoints including null values.
 */
public class AggregatorCount implements AggregationMethod {
    protected long numDataPoints;

    public void clear() {
        numDataPoints = 0;
    }

    public void enter(Object object) {
        numDataPoints++;
    }

    public void leave(Object object) {
        if (numDataPoints > 0) {
            numDataPoints--;
        }
    }

    public Object getValue() {
        return numDataPoints;
    }

}
