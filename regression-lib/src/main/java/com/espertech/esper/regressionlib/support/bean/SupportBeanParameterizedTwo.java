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

public class SupportBeanParameterizedTwo<A, B> {
    private final A one;
    private final B two;

    public SupportBeanParameterizedTwo(A one, B two) {
        this.one = one;
        this.two = two;
    }

    public A getOne() {
        return one;
    }

    public B getTwo() {
        return two;
    }
}
