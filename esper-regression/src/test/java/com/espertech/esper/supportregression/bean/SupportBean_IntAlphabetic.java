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

public class SupportBean_IntAlphabetic implements Serializable {
    private final int a;
    private final int b;
    private final int c;
    private final int d;
    private final int e;
    private final int f;
    private final int g;
    private final int h;
    private final int i;

    public SupportBean_IntAlphabetic(int a, int b, int c, int d, int e, int f, int g, int h, int i) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
        this.g = g;
        this.h = h;
        this.i = i;
    }

    public SupportBean_IntAlphabetic(int a, int b, int c, int d, int e) {
        this(a, b, c, d, e, -1, -1, -1, -1);
    }

    public SupportBean_IntAlphabetic(int a, int b, int c, int d) {
        this(a, b, c, d, -1);
    }

    public SupportBean_IntAlphabetic(int a, int b, int c) {
        this(a, b, c, -1, -1);
    }

    public SupportBean_IntAlphabetic(int a, int b) {
        this(a, b, -1);
    }

    public SupportBean_IntAlphabetic(int a) {
        this(a, -1);
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public int getC() {
        return c;
    }

    public int getD() {
        return d;
    }

    public int getE() {
        return e;
    }

    public int getF() {
        return f;
    }

    public int getG() {
        return g;
    }

    public int getH() {
        return h;
    }

    public int getI() {
        return i;
    }
}
