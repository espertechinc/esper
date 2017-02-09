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

public class SupportBean_S0 implements Serializable {
    private static int idCounter;

    private int id;
    private String p00;
    private String p01;
    private String p02;
    private String p03;

    public static Object[] makeS0(String propOne, String[] propTwo) {
        idCounter++;
        Object[] events = new Object[propTwo.length];
        for (int i = 0; i < propTwo.length; i++) {
            events[i] = new SupportBean_S0(idCounter, propOne, propTwo[i]);
        }
        return events;
    }

    public SupportBean_S0(int id) {
        this.id = id;
    }

    public SupportBean_S0(int id, String p00) {
        this.id = id;
        this.p00 = p00;
    }

    public SupportBean_S0(int id, String p00, String p01) {
        this.id = id;
        this.p00 = p00;
        this.p01 = p01;
    }

    public SupportBean_S0(int id, String p00, String p01, String p02) {
        this.id = id;
        this.p00 = p00;
        this.p01 = p01;
        this.p02 = p02;
    }

    public SupportBean_S0(int id, String p00, String p01, String p02, String p03) {
        this.id = id;
        this.p00 = p00;
        this.p01 = p01;
        this.p02 = p02;
        this.p03 = p03;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getP00() {
        return p00;
    }

    public void setP00(String p00) {
        this.p00 = p00;
    }

    public String getP01() {
        return p01;
    }

    public void setP01(String p01) {
        this.p01 = p01;
    }

    public String getP02() {
        return p02;
    }

    public void setP02(String p02) {
        this.p02 = p02;
    }

    public String getP03() {
        return p03;
    }

    public void setP03(String p03) {
        this.p03 = p03;
    }

    public String toString() {
        return "SupportBean_S0{" +
                "id=" + id +
                ", p00='" + p00 + '\'' +
                ", p01='" + p01 + '\'' +
                ", p02='" + p02 + '\'' +
                ", p03='" + p03 + '\'' +
                '}';
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SupportBean_S0 that = (SupportBean_S0) o;

        if (id != that.id) return false;
        if (p00 != null ? !p00.equals(that.p00) : that.p00 != null) return false;
        if (p01 != null ? !p01.equals(that.p01) : that.p01 != null) return false;
        if (p02 != null ? !p02.equals(that.p02) : that.p02 != null) return false;
        if (p03 != null ? !p03.equals(that.p03) : that.p03 != null) return false;

        return true;
    }

    public int hashCode() {
        int result = id;
        result = 31 * result + (p00 != null ? p00.hashCode() : 0);
        result = 31 * result + (p01 != null ? p01.hashCode() : 0);
        result = 31 * result + (p02 != null ? p02.hashCode() : 0);
        result = 31 * result + (p03 != null ? p03.hashCode() : 0);
        return result;
    }
}
