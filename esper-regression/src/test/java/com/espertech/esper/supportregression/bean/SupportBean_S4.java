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

public class SupportBean_S4 implements Serializable {
    private static int idCounter;

    private int id;
    private String p40;
    private String p41;
    private String p42;
    private String p43;

    public static Object[] makeS4(String propOne, String[] propTwo) {
        idCounter++;

        Object[] events = new Object[propTwo.length];
        for (int i = 0; i < propTwo.length; i++) {
            events[i] = new SupportBean_S4(idCounter, propOne, propTwo[i]);
        }
        return events;
    }

    public SupportBean_S4(int id) {
        this.id = id;
    }

    public SupportBean_S4(int id, String p40) {
        this.id = id;
        this.p40 = p40;
    }

    public SupportBean_S4(int id, String p40, String p41) {
        this.id = id;
        this.p40 = p40;
        this.p41 = p41;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getP40() {
        return p40;
    }

    public void setP40(String p40) {
        this.p40 = p40;
    }

    public String getP41() {
        return p41;
    }

    public void setP41(String p41) {
        this.p41 = p41;
    }

    public String getP42() {
        return p42;
    }

    public void setP42(String p42) {
        this.p42 = p42;
    }

    public String getP43() {
        return p43;
    }

    public void setP43(String p43) {
        this.p43 = p43;
    }
}
