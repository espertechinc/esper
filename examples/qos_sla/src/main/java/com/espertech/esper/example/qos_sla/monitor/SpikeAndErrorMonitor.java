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
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpikeAndErrorMonitor {
    private SpikeAndErrorMonitor() {
    }

    public static void start() {
        EPAdministrator admin = EPServiceProviderManager.getDefaultProvider().getEPAdministrator();

        String eventName = OperationMeasurement.class.getName();

        EPStatement myPattern = admin.createPattern(
                "every spike=" + eventName + "(latency>20000) or every error=" + eventName + "(success=false)");

        myPattern.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                OperationMeasurement spike = (OperationMeasurement) newEvents[0].get("spike");
                OperationMeasurement error = (OperationMeasurement) newEvents[0].get("error");

                if (spike != null) {
                    log.debug(".update spike=" + spike.toString());
                }
                if (error != null) {
                    log.debug(".update error=" + error.toString());
                }
            }
        });
    }

    private static final Logger log = LoggerFactory.getLogger(SpikeAndErrorMonitor.class);
}
