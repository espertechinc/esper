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

public class SupportBean_S3 implements Serializable {
    private static int idCounter;

    private int id;
    private String p30;
    private String p31;
    private String p32;
    private String p33;

    public static Object[] makeS3(String propOne, String[] propTwo) {
        idCounter++;

        Object[] events = new Object[propTwo.length];
        for (int i = 0; i < propTwo.length; i++) {
            events[i] = new SupportBean_S3(idCounter, propOne, propTwo[i]);
        }
        return events;
    }

    public SupportBean_S3(int id) {
        this.id = id;
    }

    public SupportBean_S3(int id, String p30) {
        this.id = id;
        this.p30 = p30;
    }

    public SupportBean_S3(int id, String p30, String p31) {
        this.id = id;
        this.p30 = p30;
        this.p31 = p31;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getP30() {
        return p30;
    }

    public void setP30(String p30) {
        this.p30 = p30;
    }

    public String getP31() {
        return p31;
    }

    public void setP31(String p31) {
        this.p31 = p31;
    }

    public String getP32() {
        return p32;
    }

    public void setP32(String p32) {
        this.p32 = p32;
    }

    public String getP33() {
        return p33;
    }

    public void setP33(String p33) {
        this.p33 = p33;
    }
}
