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

public class SupportBeanCtorOne {

    private final String theString;
    private final Integer intBoxed;
    private final int intPrimitive;
    private final boolean boolPrimitive;

    public SupportBeanCtorOne(String theString, Integer intBoxed, int intPrimitive, boolean boolPrimitive) {
        this.theString = theString;
        this.intBoxed = intBoxed;
        this.intPrimitive = intPrimitive;
        this.boolPrimitive = boolPrimitive;
    }

    public SupportBeanCtorOne(String theString, Integer intBoxed, int intPrimitive) {
        this.theString = theString;
        this.intBoxed = intBoxed;
        this.intPrimitive = intPrimitive;
        this.boolPrimitive = false;
    }

    public SupportBeanCtorOne(String theString, Integer intBoxed) {
        this.theString = theString;
        this.intBoxed = intBoxed;
        this.intPrimitive = 99;
        this.boolPrimitive = false;
    }

    public SupportBeanCtorOne(String theString) {
        throw new RuntimeException("This is a test exception");
    }

    public String getTheString() {
        return theString;
    }

    public Integer getIntBoxed() {
        return intBoxed;
    }

    public int getIntPrimitive() {
        return intPrimitive;
    }

    public boolean isBoolPrimitive() {
        return boolPrimitive;
    }
}
