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
package com.espertech.esper.example.runtimeconfig;

import com.espertech.esper.epl.agg.aggregator.AggregationMethod;

public class MyConcatAggregationFunction implements AggregationMethod {
    private final static char DELIMITER = ' ';
    private StringBuilder builder;
    private String delimiter;

    public MyConcatAggregationFunction() {
        super();
        builder = new StringBuilder();
        delimiter = "";
    }

    public void enter(Object value) {
        if (value != null) {
            builder.append(delimiter);
            builder.append(value.toString());
            delimiter = String.valueOf(DELIMITER);
        }
    }

    public void leave(Object value) {
        if (value != null) {
            builder.delete(0, value.toString().length() + 1);
        }
    }

    public Object getValue() {
        return builder.toString();
    }

    public void clear() {
        builder = new StringBuilder();
        delimiter = "";
    }
}
