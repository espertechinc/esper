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

public class SupportBean_S6 implements Serializable {
    private static int idCounter;

    private int id;
    private String p60;
    private String p61;
    private String p62;
    private String p63;

    public static Object[] makeS6(String propOne, String[] propTwo) {
        idCounter++;

        Object[] events = new Object[propTwo.length];
        for (int i = 0; i < propTwo.length; i++) {
            events[i] = new SupportBean_S6(idCounter, propOne, propTwo[i]);
        }
        return events;
    }

    public SupportBean_S6(int id) {
        this.id = id;
    }

    public SupportBean_S6(int id, String p60) {
        this.id = id;
        this.p60 = p60;
    }

    public SupportBean_S6(int id, String p60, String p61) {
        this.id = id;
        this.p60 = p60;
        this.p61 = p61;
    }

    public int getId() {
        return id;
    }

    public String getP60() {
        return p60;
    }

    public String getP61() {
        return p61;
    }

    public String getP62() {
        return p62;
    }

    public String getP63() {
        return p63;
    }
}
