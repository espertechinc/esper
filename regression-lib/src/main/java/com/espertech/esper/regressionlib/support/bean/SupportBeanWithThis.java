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

public class SupportBeanWithThis {
    private final String theString;
    private final int intPrimitive;

    public SupportBeanWithThis(String theString, int intPrimitive) {
        this.theString = theString;
        this.intPrimitive = intPrimitive;
    }

    public SupportBeanWithThis getThis() {
        return this;
    }

    public String getTheString() {
        return theString;
    }

    public int getIntPrimitive() {
        return intPrimitive;
    }
}
