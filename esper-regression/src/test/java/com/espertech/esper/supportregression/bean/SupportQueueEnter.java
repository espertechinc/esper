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

public class SupportQueueEnter {
    private int id;
    private String location;
    private String sku;
    private long timeEnter;

    public SupportQueueEnter(int id, String location, String sku, long timeEnter) {
        this.id = id;
        this.location = location;
        this.sku = sku;
        this.timeEnter = timeEnter;
    }

    public int getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public String getSku() {
        return sku;
    }

    public long getTimeEnter() {
        return timeEnter;
    }
}
