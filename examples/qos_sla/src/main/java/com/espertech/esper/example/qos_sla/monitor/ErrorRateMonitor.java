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

public class ErrorRateMonitor {
    public ErrorRateMonitor(EPRuntime runtime) {
        String fireEvery10SecondsEPL = "select * from pattern[every timer:at(*, *, *, *, *, */10)]";
        EPStatement fireEvery10Seconds = MonitorUtil.compileDeploy(fireEvery10SecondsEPL, runtime);

        String countEventsEPL = "select count(*) as size from OperationMeasurement(success=false)#time(10 min)";
        EPStatement countEvents = MonitorUtil.compileDeploy(countEventsEPL, runtime);

        fireEvery10Seconds.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                long count = (Long) countEvents.iterator().next().get("size");

                log.info(".update Info, error rate in the last 10 minutes is " + count);
            }
        });
    }

    private static final Logger log = LoggerFactory.getLogger(ErrorRateMonitor.class);
}
