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
import com.espertech.esper.example.qos_sla.eventbean.LatencyLimit;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esper.runtime.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynaLatencySpikeMonitor {
    private final EPRuntime runtime;

    public DynaLatencySpikeMonitor(EPRuntime runtime) {
        this.runtime = runtime;
        EPStatement latency = MonitorUtil.compileDeploy("select * from LatencyLimit", runtime);
        latency.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                LatencyLimit limitEvent = (LatencyLimit) newEvents[0].getUnderlying();
                createLatencyCheck(limitEvent, runtime);
            }
        });
    }

    private void createLatencyCheck(LatencyLimit limit, EPRuntime runtime) {
        log.debug("New limit, for operation '" + limit.getOperationName() +
            "' and customer '" + limit.getCustomerId() + "'" +
            " setting threshold " + limit.getLatencyThreshold());

        String filter = "operationName='" + limit.getOperationName() +
            "',customerId='" + limit.getCustomerId() + "'";

        // Alert specific to operation and customer
        String eplMonitor = "select * from OperationMeasurement(" + filter + ", latency > " + limit.getLatencyThreshold() + ")";
        EPStatement spikeLatencyAlert = MonitorUtil.compileDeploy(eplMonitor, runtime);
        spikeLatencyAlert.addListener(new LatencySpikeListener());

        // Stop pattern when the threshold changes
        String eplStop = "select * from LatencyLimit(" + filter + ")";
        EPStatement stopPattern = MonitorUtil.compileDeploy(eplStop, runtime);
        stopPattern.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                try {
                    runtime.getDeploymentService().undeploy(spikeLatencyAlert.getDeploymentId());
                } catch (EPUndeployException e) {
                    log.warn("Failed to undeploy: " + e.getMessage(), e);
                }
            }
        });
    }

    private static final Logger log = LoggerFactory.getLogger(DynaLatencySpikeMonitor.class);
}
