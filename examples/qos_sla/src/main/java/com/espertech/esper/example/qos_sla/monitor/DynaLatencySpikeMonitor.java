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

import com.espertech.esper.client.*;
import com.espertech.esper.example.qos_sla.eventbean.LatencyLimit;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynaLatencySpikeMonitor {
    private static EPAdministrator admin;

    private EPStatement spikeLatencyAlert;

    public static void start() {
        admin = EPServiceProviderManager.getDefaultProvider().getEPAdministrator();

        String theEvent = LatencyLimit.class.getName();
        EPStatement latencyAlert = admin.createPattern("every newlimit=" + theEvent);

        latencyAlert.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                LatencyLimit limitEvent = (LatencyLimit) newEvents[0].get("newlimit");
                new DynaLatencySpikeMonitor(limitEvent);
            }
        });
    }

    public DynaLatencySpikeMonitor(LatencyLimit limit) {
        log.debug("New limit, for operation '" + limit.getOperationName() +
                "' and customer '" + limit.getCustomerId() + "'" +
                " setting threshold " + limit.getLatencyThreshold());

        String filter = "operationName='" + limit.getOperationName() +
                "',customerId='" + limit.getCustomerId() + "'";

        // Alert specific to operation and customer
        spikeLatencyAlert = admin.createPattern("every alert=" +
                OperationMeasurement.class.getName() +
                "(" + filter + ", latency>" + limit.getLatencyThreshold() + ")");
        spikeLatencyAlert.addListener(new LatencySpikeListener());

        // Stop pattern when the threshold changes
        String theEvent = LatencyLimit.class.getName();
        EPStatement stopPattern = admin.createPattern(theEvent + "(" + filter + ")");

        stopPattern.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                spikeLatencyAlert.stop();
            }
        });
    }

    private static final Logger log = LoggerFactory.getLogger(DynaLatencySpikeMonitor.class);
}
