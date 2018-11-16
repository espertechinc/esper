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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceHealthMonitor {
    public ServiceHealthMonitor(EPRuntime runtime) {
        String epl = "select * from OperationMeasurement(success=false)\n" +
            "  match_recognize (\n" +
            "    measures a as a_events" +
            "    pattern (a a a))";
        EPStatement statement = MonitorUtil.compileDeploy(epl, runtime);
        statement.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                log.debug(".update Alert, detected 3 errors in a row");
            }
        });
    }

    private static final Logger log = LoggerFactory.getLogger(ServiceHealthMonitor.class);
}
