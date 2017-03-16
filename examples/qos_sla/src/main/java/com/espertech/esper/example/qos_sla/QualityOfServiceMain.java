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
package com.espertech.esper.example.qos_sla;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.qos_sla.eventbean.LatencyLimit;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import com.espertech.esper.example.qos_sla.monitor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class QualityOfServiceMain implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(QualityOfServiceMain.class);

    private final boolean continuousSimulation;

    public static void main(String[] args) {
        new QualityOfServiceMain(false).run();
    }

    public QualityOfServiceMain(boolean continuousSimulation) {
        this.continuousSimulation = continuousSimulation;
    }

    public void run() {
        log.info("Setting up EPL");
        DynaLatencySpikeMonitor.start();
        AverageLatencyMonitor.start();
        ErrorRateMonitor.start();
        LatencySpikeMonitor.start();
        ServiceHealthMonitor.start();
        SpikeAndErrorMonitor.start();

        EPRuntime runtime = EPServiceProviderManager.getDefaultProvider().getEPRuntime();

        log.info("Sending new limits");
        String[] services = {"s0", "s1", "s2"};
        String[] customers = {"c0", "c1", "c2"};
        long[] limitSpike = {15000, 10000, 10040};

        // Set up limits for 3 services/customer combinations
        for (int i = 0; i < services.length; i++) {
            LatencyLimit limit = new LatencyLimit(services[i], customers[i], limitSpike[i]);
            runtime.sendEvent(limit);
        }

        // Send events
        log.info("Sending 100k of random events");
        Random random = new Random();
        for (int i = 0; i < 100000; i++) {
            for (int index = 0; index < services.length; index++) {
                int delta = random.nextInt(1000000) - 999700;
                OperationMeasurement measurement = new OperationMeasurement(services[index], customers[index],
                        9950 + delta, true);
                runtime.sendEvent(measurement);

                if (continuousSimulation) { // if running continously and not from command line simply send with delay
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        log.debug("Interrupted", e);
                        break;
                    }
                }
            }
            if (Thread.interrupted()) {
                break;
            }
        }

        log.info("Finally, sending a new limit and a artificial spike");
        // Send a new limit
        LatencyLimit limit = new LatencyLimit(services[1], customers[1], 8000);
        runtime.sendEvent(limit);

        // Send a new spike
        OperationMeasurement measurement = new OperationMeasurement(services[1], customers[1], 8001, true);
        runtime.sendEvent(measurement);
    }
}

