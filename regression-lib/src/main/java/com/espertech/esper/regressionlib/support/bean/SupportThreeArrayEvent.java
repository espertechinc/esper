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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;

public class SupportThreeArrayEvent implements Serializable {
    private final String id;
    private final int value;
    private final int[] intArray;
    private final long[] longArray;
    private final double[] doubleArray;

    public SupportThreeArrayEvent(String id, int value, int[] intArray, long[] longArray, double[] doubleArray) {
        this.id = id;
        this.value = value;
        this.intArray = intArray;
        this.longArray = longArray;
        this.doubleArray = doubleArray;
    }

    public String getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    public int[] getIntArray() {
        return intArray;
    }

    public long[] getLongArray() {
        return longArray;
    }

    public double[] getDoubleArray() {
        return doubleArray;
    }
}
