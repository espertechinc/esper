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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;

public class SupportGroupSubgroupEvent implements Serializable {
    private String grp;
    private String subGrp;
    private int type;
    private double value;

    public SupportGroupSubgroupEvent() {
    }

    public SupportGroupSubgroupEvent(final String group, final String subGroup, final int type, final double value) {
        grp = group;
        subGrp = subGroup;
        this.type = type;
        this.value = value;
    }

    public String getGrp() {
        return grp;
    }

    public void setGrp(final String group) {
        grp = group;
    }

    public String getSubGrp() {
        return subGrp;
    }

    public void setSubGrp(final String subGroup) {
        subGrp = subGroup;
    }

    public int getType() {
        return type;
    }

    public void setType(final int type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(final double value) {
        this.value = value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SupportGroupSubgroupEvent) {
            final SupportGroupSubgroupEvent evt = (SupportGroupSubgroupEvent) obj;
            return grp.equals(evt.grp) && subGrp.equals(evt.subGrp) && type == evt.type && Math.abs(value - evt.value) < 1e-6;
        }

        return false;
    }

    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "(" + grp + ", " + subGrp + ")@" + type + "=" + value;
    }

}
