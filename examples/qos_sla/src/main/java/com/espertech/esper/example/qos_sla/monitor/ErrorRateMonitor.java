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

public class ErrorRateMonitor {
    private ErrorRateMonitor() {
    }

    public static void start() {
        EPAdministrator admin = EPServiceProviderManager.getDefaultProvider().getEPAdministrator();

        EPStatement pattern = admin.createPattern("every timer:at(*, *, *, *, *, */10)");
        final EPStatement view = admin.createEPL("select count(*) as size from " + OperationMeasurement.class.getName() +
                "(success=false)#time(10 min)");

        pattern.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                long count = (Long) view.iterator().next().get("size");

                log.info(".update Info, error rate in the last 10 minutes is " + count);
            }
        });
    }

    private static final Logger log = LoggerFactory.getLogger(ErrorRateMonitor.class);
}
