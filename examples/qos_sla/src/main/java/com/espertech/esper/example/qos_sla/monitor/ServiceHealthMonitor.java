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

public class ServiceHealthMonitor {
    private ServiceHealthMonitor() {
    }

    public static void start() {
        EPAdministrator admin = EPServiceProviderManager.getDefaultProvider().getEPAdministrator();

        String theEvent = OperationMeasurement.class.getName();

        EPStatement statView = admin.createPattern("every (" +
                theEvent + "(success=false)->" +
                theEvent + "(success=false)->" +
                theEvent + "(success=false))");

        statView.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                log.debug(".update Alert, detected 3 erros in a row");
            }
        });
    }

    private static final Logger log = LoggerFactory.getLogger(ServiceHealthMonitor.class);
}
