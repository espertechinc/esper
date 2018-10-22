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

public class SupportLowerUpperCompareAggregationFunction implements AggregationFunction {
    private static Object[] lastEnterParameters;
    private int count;

    public static Object[] getLastEnterParameters() {
        return lastEnterParameters;
    }

    public static void setLastEnterParameters(Object[] lastEnterParameters) {
        SupportLowerUpperCompareAggregationFunction.lastEnterParameters = lastEnterParameters;
    }

    public void enter(Object value) {
        Object[] parameters = (Object[]) value;
        lastEnterParameters = parameters;
        int lower = (Integer) parameters[0];
        int upper = (Integer) parameters[1];
        int val = (Integer) parameters[2];
        if ((val >= lower) && (val <= upper)) {
            count++;
        }
    }

    public void leave(Object value) {
        Object[] parameters = (Object[]) value;
        int lower = (Integer) parameters[0];
        int upper = (Integer) parameters[1];
        int val = (Integer) parameters[2];
        if ((val >= lower) && (val <= upper)) {
            count--;
        }
    }

    public void clear() {
        count = 0;
    }

    public Object getValue() {
        return count;
    }
}
