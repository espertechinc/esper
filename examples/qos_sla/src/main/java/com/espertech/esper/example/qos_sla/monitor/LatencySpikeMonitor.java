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
package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;

public class LatencySpikeMonitor {
    private LatencySpikeMonitor() {
    }

    public static void start() {
        EPAdministrator admin = EPServiceProviderManager.getDefaultProvider().getEPAdministrator();

        EPStatement latencyAlert = admin.createPattern(
                "every alert=" + OperationMeasurement.class.getName() + "(latency > 20000)");

        latencyAlert.addListener(new LatencySpikeListener());
    }
}
