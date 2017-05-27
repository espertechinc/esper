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

public class SupportQueueLeave {
    private int id;
    private String location;
    private long timeLeave;

    public SupportQueueLeave(int id, String location, long timeLeave) {
        this.id = id;
        this.location = location;
        this.timeLeave = timeLeave;
    }

    public int getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public long getTimeLeave() {
        return timeLeave;
    }
}
