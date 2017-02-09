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
package com.espertech.esper.example.servershell;

public class SampleEvent {
    private String ipAddress;
    private double duration;

    public SampleEvent(String ipAddress, double duration) {
        this.ipAddress = ipAddress;
        this.duration = duration;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public double getDuration() {
        return duration;
    }
}
