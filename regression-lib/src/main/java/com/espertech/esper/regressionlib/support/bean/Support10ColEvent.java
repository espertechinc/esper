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

import java.io.Serializable;

public final class Support10ColEvent implements Serializable {
    private final String groupKey;
    private final int c0;
    private final int c1;
    private final int c2;
    private final int c3;
    private final int c4;
    private final int c5;
    private final int c6;
    private final int c7;
    private final int c8;
    private final int c9;

    public Support10ColEvent(String groupKey, int value) {
        this.groupKey = groupKey;
        this.c0 = value;
        this.c1 = value;
        this.c2 = value;
        this.c3 = value;
        this.c4 = value;
        this.c5 = value;
        this.c6 = value;
        this.c7 = value;
        this.c8 = value;
        this.c9 = value;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public int getC0() {
        return c0;
    }

    public int getC1() {
        return c1;
    }

    public int getC2() {
        return c2;
    }

    public int getC3() {
        return c3;
    }

    public int getC4() {
        return c4;
    }

    public int getC5() {
        return c5;
    }

    public int getC6() {
        return c6;
    }

    public int getC7() {
        return c7;
    }

    public int getC8() {
        return c8;
    }

    public int getC9() {
        return c9;
    }
}
