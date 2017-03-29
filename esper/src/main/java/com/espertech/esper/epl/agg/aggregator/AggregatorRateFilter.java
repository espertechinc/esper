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

public class AggregatorRateFilter extends AggregatorRate {

    public AggregatorRateFilter(long oneSecondTime) {
        super(oneSecondTime);
    }

    @Override
    public void enter(Object value) {
        Object[] arr = (Object[]) value;
        Boolean pass = (Boolean) arr[arr.length - 1];
        if (pass != null && pass) {
            if (arr.length == 2) {
                super.enterValueSingle(arr[0]);
            } else {
                super.enterValueArr(arr);
            }
        }
    }

    @Override
    public void leave(Object value) {
        Object[] arr = (Object[]) value;
        Boolean pass = (Boolean) arr[arr.length - 1];
        if (pass != null && pass) {
            if (arr.length == 2) {
                super.leaveValueSingle(arr[0]);
            } else {
                super.leaveValueArr(arr);
            }
        }
    }
}