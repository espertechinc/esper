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

public class SupportDeltaFour {
    private final String k0;
    private final String p0;
    private final String p2;
    private final String p5;

    public SupportDeltaFour(String k0, String p0, String p2, String p5) {
        this.k0 = k0;
        this.p0 = p0;
        this.p2 = p2;
        this.p5 = p5;
    }

    public String getK0() {
        return k0;
    }

    public String getP0() {
        return p0;
    }

    public String getP2() {
        return p2;
    }

    public String getP5() {
        return p5;
    }
}
