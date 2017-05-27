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

public class SupportBeanErrorTestingTwo {
    private String value;

    public SupportBeanErrorTestingTwo() {
        value = "default";
    }

    public void setValue(String value) {
        throw new RuntimeException("Setter manufactured test exception");
    }

    public String getValue() {
        return value;
    }
}
