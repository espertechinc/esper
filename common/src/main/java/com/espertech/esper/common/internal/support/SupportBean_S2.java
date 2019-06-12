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

public class SupportBean_S2 implements Serializable {
    private static final long serialVersionUID = -8786400418416365802L;
    private static int idCounter;

    private int id;
    private String p20;
    private String p21;
    private String p22;
    private String p23;

    public static Object[] makeS2(String propOne, String[] propTwo) {
        idCounter++;

        Object[] events = new Object[propTwo.length];
        for (int i = 0; i < propTwo.length; i++) {
            events[i] = new SupportBean_S2(idCounter, propOne, propTwo[i]);
        }
        return events;
    }

    public SupportBean_S2(int id) {
        this.id = id;
    }

    public SupportBean_S2(int id, String p20) {
        this.id = id;
        this.p20 = p20;
    }

    public SupportBean_S2(int id, String p20, String p21) {
        this.id = id;
        this.p20 = p20;
        this.p21 = p21;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getP20() {
        return p20;
    }

    public void setP20(String p20) {
        this.p20 = p20;
    }

    public String getP21() {
        return p21;
    }

    public void setP21(String p21) {
        this.p21 = p21;
    }

    public String getP22() {
        return p22;
    }

    public void setP22(String p22) {
        this.p22 = p22;
    }

    public String getP23() {
        return p23;
    }

    public void setP23(String p23) {
        this.p23 = p23;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SupportBean_S2 that = (SupportBean_S2) o;

        if (id != that.id) return false;
        if (p20 != null ? !p20.equals(that.p20) : that.p20 != null) return false;
        if (p21 != null ? !p21.equals(that.p21) : that.p21 != null) return false;
        if (p22 != null ? !p22.equals(that.p22) : that.p22 != null) return false;
        return p23 != null ? p23.equals(that.p23) : that.p23 == null;
    }

    public int hashCode() {
        int result = id;
        result = 31 * result + (p20 != null ? p20.hashCode() : 0);
        result = 31 * result + (p21 != null ? p21.hashCode() : 0);
        result = 31 * result + (p22 != null ? p22.hashCode() : 0);
        result = 31 * result + (p23 != null ? p23.hashCode() : 0);
        return result;
    }
}
