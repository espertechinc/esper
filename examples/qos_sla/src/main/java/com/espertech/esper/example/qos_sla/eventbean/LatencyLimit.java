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
package com.espertech.esper.example.qos_sla.eventbean;

public class LatencyLimit {
    private String operationName;
    private String customerId;
    private long latencyThreshold;

    public LatencyLimit(String operationName, String customerId, long latencyThreshold) {
        this.operationName = operationName;
        this.customerId = customerId;
        this.latencyThreshold = latencyThreshold;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public long getLatencyThreshold() {
        return latencyThreshold;
    }
}
