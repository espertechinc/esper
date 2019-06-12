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
package com.espertech.esper.common.internal.support;

import java.io.Serializable;

public class SupportBean_S1 implements Serializable {
    private static final long serialVersionUID = -1197236754579629766L;
    private static int idCounter;

    private int id;
    private String p10;
    private String p11;
    private String p12;
    private String p13;

    public static Object[] makeS1(String propOne, String[] propTwo) {
        idCounter++;

        Object[] events = new Object[propTwo.length];
        for (int i = 0; i < propTwo.length; i++) {
            events[i] = new SupportBean_S1(idCounter, propOne, propTwo[i]);
        }
        return events;
    }


    public SupportBean_S1(int id) {
        this.id = id;
    }

    public SupportBean_S1(int id, String p10) {
        this.id = id;
        this.p10 = p10;
    }

    public SupportBean_S1(int id, String p10, String p11) {
        this.id = id;
        this.p10 = p10;
        this.p11 = p11;
    }

    public SupportBean_S1(int id, String p10, String p11, String p12) {
        this.id = id;
        this.p10 = p10;
        this.p11 = p11;
        this.p12 = p12;
    }

    public SupportBean_S1(int id, String p10, String p11, String p12, String p13) {
        this.id = id;
        this.p10 = p10;
        this.p11 = p11;
        this.p12 = p12;
        this.p13 = p13;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getP10() {
        return p10;
    }

    public void setP10(String p10) {
        this.p10 = p10;
    }

    public String getP11() {
        return p11;
    }

    public void setP11(String p11) {
        this.p11 = p11;
    }

    public String getP12() {
        return p12;
    }

    public void setP12(String p12) {
        this.p12 = p12;
    }

    public String getP13() {
        return p13;
    }

    public void setP13(String p13) {
        this.p13 = p13;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SupportBean_S1 that = (SupportBean_S1) o;

        if (id != that.id) return false;
        if (p10 != null ? !p10.equals(that.p10) : that.p10 != null) return false;
        if (p11 != null ? !p11.equals(that.p11) : that.p11 != null) return false;
        if (p12 != null ? !p12.equals(that.p12) : that.p12 != null) return false;
        return p13 != null ? p13.equals(that.p13) : that.p13 == null;
    }

    public int hashCode() {
        int result = id;
        result = 31 * result + (p10 != null ? p10.hashCode() : 0);
        result = 31 * result + (p11 != null ? p11.hashCode() : 0);
        result = 31 * result + (p12 != null ? p12.hashCode() : 0);
        result = 31 * result + (p13 != null ? p13.hashCode() : 0);
        return result;
    }
}
