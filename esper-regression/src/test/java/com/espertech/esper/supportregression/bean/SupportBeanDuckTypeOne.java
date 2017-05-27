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

public class SupportBeanDuckTypeOne implements SupportBeanDuckType {
    private String stringValue;

    public SupportBeanDuckTypeOne(String stringValue) {
        this.stringValue = stringValue;
    }

    public String makeString() {
        return stringValue;
    }

    public Object makeCommon() {
        return new SupportBeanDuckTypeTwo(-1);
    }

    public double returnDouble() {
        return 12.9876d;
    }
}
