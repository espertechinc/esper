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

public class SupportSimpleBeanTwo implements Serializable {
    private String s2;
    private int i2;
    private double d2;
    private long l2;

    public SupportSimpleBeanTwo(String s2, int i2, double d2, long l2) {
        this.s2 = s2;
        this.i2 = i2;
        this.d2 = d2;
        this.l2 = l2;
    }

    public String getS2() {
        return s2;
    }

    public int getI2() {
        return i2;
    }

    public double getD2() {
        return d2;
    }

    public long getL2() {
        return l2;
    }
}
