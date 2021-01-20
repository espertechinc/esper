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

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportBeanReservedKeyword implements Serializable {
    private static final long serialVersionUID = -8999757230563910277L;
    private int seconds;
    private int order;
    private Inner timestamp;
    private int[] group;
    public SupportBeanReservedKeyword innerbean;

    public SupportBeanReservedKeyword(int seconds, int order) {
        this.seconds = seconds;
        this.order = order;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int[] getGroup() {
        return group;
    }

    public void setGroup(int[] group) {
        this.group = group;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public SupportBeanReservedKeyword getInnerbean() {
        return innerbean;
    }

    public void setInnerbean(SupportBeanReservedKeyword innerbean) {
        this.innerbean = innerbean;
    }

    public Inner getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Inner timestamp) {
        this.timestamp = timestamp;
    }

    public static class Inner implements Serializable {
        private static final long serialVersionUID = 6062457966312995327L;
        private int hour;

        public int getHour() {
            return hour;
        }

        public void setHour(int hour) {
            this.hour = hour;
        }
    }
}
