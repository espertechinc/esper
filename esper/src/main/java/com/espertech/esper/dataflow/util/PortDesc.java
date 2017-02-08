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
package com.espertech.esper.dataflow.util;

import java.lang.reflect.Method;

public class PortDesc {
    private final int operator;
    private final Method optionalMethod;

    public PortDesc(int operator, Method optionalMethod) {
        this.operator = operator;
        this.optionalMethod = optionalMethod;
    }

    public int getOperator() {
        return operator;
    }

    public Method getOptionalMethod() {
        return optionalMethod;
    }

    public String toString() {
        return "{" +
                "operator=" + operator +
                ", method=" + optionalMethod +
                '}';
    }
}
