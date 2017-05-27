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

public class SupportBean_StringAlphabetic implements Serializable {
    private final String a;
    private final String b;
    private final String c;
    private final String d;
    private final String e;
    private final String f;
    private final String g;
    private final String h;
    private final String i;

    public SupportBean_StringAlphabetic(String a, String b, String c, String d, String e, String f, String g, String h, String i) {
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

    public SupportBean_StringAlphabetic(String a, String b, String c, String d, String e) {
        this(a, b, c, d, e, null, null, null, null);
    }

    public SupportBean_StringAlphabetic(String a, String b, String c) {
        this(a, b, c, null, null);
    }

    public SupportBean_StringAlphabetic(String a, String b) {
        this(a, b, null);
    }

    public SupportBean_StringAlphabetic(String a) {
        this(a, null, null);
    }

    public String getA() {
        return a;
    }

    public String getB() {
        return b;
    }

    public String getC() {
        return c;
    }

    public String getD() {
        return d;
    }

    public String getE() {
        return e;
    }

    public String getF() {
        return f;
    }

    public String getG() {
        return g;
    }

    public String getH() {
        return h;
    }

    public String getI() {
        return i;
    }
}
