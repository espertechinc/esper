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

import com.espertech.esper.common.internal.support.SupportEnum;

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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SupportBeanVariantStream that = (SupportBeanVariantStream) o;

        if (boolBoxed != that.boolBoxed) return false;
        if (longPrimitive != that.longPrimitive) return false;
        if (Float.compare(that.doublePrimitive, doublePrimitive) != 0) return false;
        if (theString != null ? !theString.equals(that.theString) : that.theString != null) return false;
        if (intPrimitive != null ? !intPrimitive.equals(that.intPrimitive) : that.intPrimitive != null) return false;
        return enumValue == that.enumValue;
    }

    public int hashCode() {
        int result = theString != null ? theString.hashCode() : 0;
        result = 31 * result + (boolBoxed ? 1 : 0);
        result = 31 * result + (intPrimitive != null ? intPrimitive.hashCode() : 0);
        result = 31 * result + longPrimitive;
        result = 31 * result + (doublePrimitive != +0.0f ? Float.floatToIntBits(doublePrimitive) : 0);
        result = 31 * result + (enumValue != null ? enumValue.hashCode() : 0);
        return result;
    }
}
