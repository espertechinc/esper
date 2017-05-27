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

public class SupportRevisionFull implements ISupportRevisionFull {
    private final String k0;
    private final String p0;
    private final String p1;
    private final String p2;
    private final String p3;
    private final String p4;
    private final String p5;

    public SupportRevisionFull(String k0, String p0, String p1, String p2, String p3, String p4, String p5) {
        this.k0 = k0;
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
        this.p5 = p5;
    }

    public SupportRevisionFull(String k0, String p1, String p5) {
        this.k0 = k0;
        this.p0 = null;
        this.p1 = p1;
        this.p2 = null;
        this.p3 = null;
        this.p4 = null;
        this.p5 = p5;
    }

    public String getK0() {
        return k0;
    }

    public String getP0() {
        return p0;
    }

    public String getP1() {
        return p1;
    }

    public String getP2() {
        return p2;
    }

    public String getP3() {
        return p3;
    }

    public String getP4() {
        return p4;
    }

    public String getP5() {
        return p5;
    }
}
