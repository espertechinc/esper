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

// don't make serializable
public class SupportBeanErrorTestingOne {
    public SupportBeanErrorTestingOne() {
        throw new RuntimeException("Default ctor manufactured test exception");
    }

    public void setValue(String value) {
        throw new RuntimeException("Setter manufactured test exception");
    }

    public String getValue() {
        throw new RuntimeException("Getter manufactured test exception");
    }
}
