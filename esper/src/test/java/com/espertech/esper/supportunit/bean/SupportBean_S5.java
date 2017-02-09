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
package com.espertech.esper.supportunit.bean;

import java.io.Serializable;

public class SupportBean_S5 implements Serializable {
    private static int idCounter;

    private int id;
    private String p50;
    private String p51;
    private String p52;
    private String p53;

    public static Object[] makeS5(String propOne, String[] propTwo) {
        idCounter++;

        Object[] events = new Object[propTwo.length];
        for (int i = 0; i < propTwo.length; i++) {
            events[i] = new SupportBean_S5(idCounter, propOne, propTwo[i]);
        }
        return events;
    }

    public SupportBean_S5(int id) {
        this.id = id;
    }

    public SupportBean_S5(int id, String p50) {
        this.id = id;
        this.p50 = p50;
    }

    public SupportBean_S5(int id, String p50, String p51) {
        this.id = id;
        this.p50 = p50;
        this.p51 = p51;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getP50() {
        return p50;
    }

    public void setP50(String p50) {
        this.p50 = p50;
    }

    public String getP51() {
        return p51;
    }

    public String getP52() {
        return p52;
    }

    public String getP53() {
        return p53;
    }
}
