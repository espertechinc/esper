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

public class SupportSimpleBeanOne implements Serializable {
    private String s1;
    private int i1;
    private double d1;
    private long l1;

    public SupportSimpleBeanOne(String s1, int i1, double d1, long l1) {
        this.s1 = s1;
        this.i1 = i1;
        this.d1 = d1;
        this.l1 = l1;
    }

    public String getS1() {
        return s1;
    }

    public int getI1() {
        return i1;
    }

    public double getD1() {
        return d1;
    }

    public long getL1() {
        return l1;
    }
}
