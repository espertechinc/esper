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
package com.espertech.esper.supportunit.epl;

import com.espertech.esper.epl.agg.aggregator.AggregationMethod;

import java.io.Serializable;

public class SupportPluginAggregationMethodOne implements AggregationMethod, Serializable {
    private int count;

    public void clear() {
        count = 0;
    }

    public void enter(Object value) {
        count--;
    }

    public void leave(Object value) {
        count++;
    }

    public Object getValue() {
        return count;
    }

}
