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

public class SupportBeanInt implements Serializable {
    private String id;
    private int p00;
    private int p01;
    private int p02;
    private int p03;
    private int p04;
    private int p05;

    public SupportBeanInt(String id, int p00, int p01, int p02, int p03, int p04, int p05) {
        this.id = id;
        this.p00 = p00;
        this.p01 = p01;
        this.p02 = p02;
        this.p03 = p03;
        this.p04 = p04;
        this.p05 = p05;
    }

    public String getId() {
        return id;
    }

    public int getP00() {
        return p00;
    }

    public int getP01() {
        return p01;
    }

    public int getP02() {
        return p02;
    }

    public int getP03() {
        return p03;
    }

    public int getP04() {
        return p04;
    }

    public int getP05() {
        return p05;
    }
}
