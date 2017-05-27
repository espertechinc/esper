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
package com.espertech.esper.supportregression.multithread;

import com.espertech.esper.epl.agg.aggregator.AggregationMethod;

import java.util.ArrayList;
import java.util.List;

public class MyIntListAggregation implements AggregationMethod {

    private List<Integer> values = new ArrayList<Integer>();

    @Override
    public void enter(Object value) {
        values.add((Integer) value);
    }

    @Override
    public void leave(Object value) {
    }

    @Override
    public Object getValue() {
        return new ArrayList<Integer>(values);
    }

    @Override
    public void clear() {
        values.clear();
    }
}
