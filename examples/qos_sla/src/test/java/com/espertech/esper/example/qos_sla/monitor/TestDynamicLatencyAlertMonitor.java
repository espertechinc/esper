package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.qos_sla.eventbean.LatencyLimit;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import junit.framework.TestCase;

public class TestDynamicLatencyAlertMonitor extends TestCase {
    private EPRuntime runtime;

    public void setUp() {
        DynaLatencySpikeMonitor.start();
        runtime = EPServiceProviderManager.getDefaultProvider().getEPRuntime();
    }

    public void testLatencyAlert() {
        String services[] = {"s0", "s1", "s2"};
        String customers[] = {"c0", "c1", "c2"};
        long limitSpike[] = {15000, 10000, 10040};

        // Set up limits for 3 services/customer combinations
        for (int i = 0; i < services.length; i++) {
            LatencyLimit limit = new LatencyLimit(services[i], customers[i], limitSpike[i]);
            runtime.sendEvent(limit);
        }

        // Send events
        for (int i = 0; i < 100; i++) {
            for (int index = 0; index < services.length; index++) {
                OperationMeasurement measurement = new OperationMeasurement(services[index], customers[index],
                        9950 + i, true);
                runtime.sendEvent(measurement);
            }
        }

        // Send a new limit
        LatencyLimit limit = new LatencyLimit(services[1], customers[1], 8000);
        runtime.sendEvent(limit);

        // Send a new spike
        OperationMeasurement measurement = new OperationMeasurement(services[1], customers[1], 8001, true);
        runtime.sendEvent(measurement);
    }
}
