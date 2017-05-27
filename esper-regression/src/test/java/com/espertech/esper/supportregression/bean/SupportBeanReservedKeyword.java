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

public class SupportBeanReservedKeyword {
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

    public static class Inner {
        private int hour;

        public int getHour() {
            return hour;
        }

        public void setHour(int hour) {
            this.hour = hour;
        }
    }
}
