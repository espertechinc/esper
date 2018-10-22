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

public class OperationMeasurement {
    private String operationName;
    private String customerId;
    private long latency;
    private boolean success;

    public OperationMeasurement(String operationName, String customerId, long latency,
                                boolean success) {
        this.operationName = operationName;
        this.customerId = customerId;
        this.latency = latency;
        this.success = success;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public long getLatency() {
        return latency;
    }

    public boolean isSuccess() {
        return success;
    }
}
