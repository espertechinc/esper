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

public class AggregatorNthFilter extends AggregatorNth {

    public AggregatorNthFilter(int sizeBuf) {
        super(sizeBuf);
    }

    @Override
    public void enter(Object value) {
        Object[] arr = (Object[]) value;
        Boolean pass = (Boolean) arr[arr.length - 1];
        if (pass != null && pass) {
            super.enterValues(arr);
        }
    }

    @Override
    public void leave(Object value) {
        Object[] arr = (Object[]) value;
        Boolean pass = (Boolean) arr[arr.length - 1];
        if (pass != null && pass) {
            super.leave(arr);
        }
    }
}