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
package com.espertech.esper.regressionlib.support.extend.aggfunc;

import com.espertech.esper.common.client.hook.aggfunc.AggregationFunction;

public class SupportInvocationCountFunction implements AggregationFunction {
    private static long getValueInvocationCount = 0;

    public static long getGetValueInvocationCount() {
        return getValueInvocationCount;
    }

    public static void incGetValueInvocationCount() {
        getValueInvocationCount++;
    }

    public static void resetGetValueInvocationCount() {
        getValueInvocationCount = 0;
    }

    private int sum;

    public void enter(Object value) {
        int amount = (Integer) value;
        sum += amount;
    }

    public void leave(Object value) {
    }

    public Object getValue() {
        getValueInvocationCount++;
        return sum;
    }

    public void clear() {
    }
}
