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
package com.espertech.esper.supportregression.bean;

import java.io.Serializable;

// For testing variant streams to act as a variant of SupportBean
public class SupportBeanVariantStream implements Serializable {
    private String theString;
    private boolean boolBoxed;
    private Integer intPrimitive;
    private int longPrimitive;
    private float doublePrimitive;
    private SupportEnum enumValue;

    public SupportBeanVariantStream(String theString) {
        this.theString = theString;
    }

    public SupportBeanVariantStream(String theString, boolean boolBoxed, Integer intPrimitive, int longPrimitive, float doublePrimitive, SupportEnum enumValue) {
        this.theString = theString;
        this.boolBoxed = boolBoxed;
        this.intPrimitive = intPrimitive;
        this.longPrimitive = longPrimitive;
        this.doublePrimitive = doublePrimitive;
        this.enumValue = enumValue;
    }

    public String getTheString() {
        return theString;
    }

    public boolean isBoolBoxed() {
        return boolBoxed;
    }

    public Integer getIntPrimitive() {
        return intPrimitive;
    }

    public int getLongPrimitive() {
        return longPrimitive;
    }

    public float getDoublePrimitive() {
        return doublePrimitive;
    }

    public SupportEnum getEnumValue() {
        return enumValue;
    }
}
