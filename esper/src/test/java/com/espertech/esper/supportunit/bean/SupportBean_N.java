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
package com.espertech.esper.supportunit.bean;

import java.io.Serializable;

public class SupportBean_N implements Serializable {
    private int intPrimitive;
    private Integer intBoxed;
    private double doublePrimitive;
    private Double doubleBoxed;
    private boolean boolPrimitive;
    private Boolean boolBoxed;

    public SupportBean_N(int intPrimitive, Integer intBoxed, double doublePrimitive, Double doubleBoxed, boolean boolPrimitive, Boolean boolBoxed) {
        this.intPrimitive = intPrimitive;
        this.intBoxed = intBoxed;
        this.doublePrimitive = doublePrimitive;
        this.doubleBoxed = doubleBoxed;
        this.boolPrimitive = boolPrimitive;
        this.boolBoxed = boolBoxed;
    }

    public SupportBean_N(int intPrimitive, Integer intBoxed) {
        this.intPrimitive = intPrimitive;
        this.intBoxed = intBoxed;
    }

    public int getIntPrimitive() {
        return intPrimitive;
    }

    public Integer getIntBoxed() {
        return intBoxed;
    }

    public double getDoublePrimitive() {
        return doublePrimitive;
    }

    public Double getDoubleBoxed() {
        return doubleBoxed;
    }

    public boolean isBoolPrimitive() {
        return boolPrimitive;
    }

    public Boolean getBoolBoxed() {
        return boolBoxed;
    }

    public String toString() {
        return "intPrim=" + intPrimitive +
                " intBoxed=" + intBoxed +
                " doublePrim=" + doublePrimitive +
                " doubleBoxed=" + doubleBoxed +
                " boolPrim=" + boolPrimitive +
                " boolBoxed=" + boolBoxed;
    }

}
