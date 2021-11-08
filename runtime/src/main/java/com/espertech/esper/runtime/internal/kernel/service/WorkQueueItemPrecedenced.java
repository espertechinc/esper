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
package com.espertech.esper.runtime.internal.kernel.service;

public class WorkQueueItemPrecedenced implements Comparable<WorkQueueItemPrecedenced> {
    private Object latchOrBean;
    private final int precedence;

    public WorkQueueItemPrecedenced(Object latchOrBean, int precedence) {
        this.latchOrBean = latchOrBean;
        this.precedence = precedence;
    }

    public int getPrecedence() {
        return precedence;
    }

    public void setLatchOrBean(Object latchOrBean) {
        this.latchOrBean = latchOrBean;
    }

    public Object getLatchOrBean() {
        return latchOrBean;
    }

    public int compareTo(WorkQueueItemPrecedenced o) {
        return Integer.compare(o.precedence, precedence);
    }
}
