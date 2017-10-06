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
 * Count all non-null values.
 */
public class AggregatorCountNonNull implements AggregationMethod {
    protected long cnt;

    public AggregatorCountNonNull() {
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        cnt++;
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }
        if (cnt > 0) {
            cnt--;
        }
    }

    public void clear() {
        cnt = 0;
    }

    public Object getValue() {
        return cnt;
    }
}
