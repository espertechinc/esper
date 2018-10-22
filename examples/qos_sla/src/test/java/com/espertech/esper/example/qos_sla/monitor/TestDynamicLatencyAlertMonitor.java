package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.example.qos_sla.eventbean.LatencyLimit;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import junit.framework.TestCase;

public class TestDynamicLatencyAlertMonitor extends TestCase {
    private EPRuntime runtime;

    public void setUp() {
        // This code runs as part of the automated regression test suite; Therefore disable internal timer theading to safe resources
        Configuration config = MonitorUtil.getConfiguration();
        config.getRuntime().getThreading().setInternalTimerEnabled(false);

        runtime = EPRuntimeProvider.getDefaultRuntime(config);
        runtime.initialize();

        new DynaLatencySpikeMonitor(runtime);
    }

    public void testLatencyAlert() {
        String services[] = {"s0", "s1", "s2"};
        String customers[] = {"c0", "c1", "c2"};
        long limitSpike[] = {15000, 10000, 10040};

        // Set up limits for 3 services/customer combinations
        for (int i = 0; i < services.length; i++) {
            LatencyLimit limit = new LatencyLimit(services[i], customers[i], limitSpike[i]);
            runtime.getEventService().sendEventBean(limit, "LatencyLimit");
        }

        // Send events
        for (int i = 0; i < 100; i++) {
            for (int index = 0; index < services.length; index++) {
                OperationMeasurement measurement = new OperationMeasurement(services[index], customers[index],
                    9950 + i, true);
                runtime.getEventService().sendEventBean(measurement, "OperationMeasurement");
            }
        }

        // Send a new limit
        LatencyLimit limit = new LatencyLimit(services[1], customers[1], 8000);
        runtime.getEventService().sendEventBean(limit, "LatencyLimit");

        // Send a new spike
        OperationMeasurement measurement = new OperationMeasurement(services[1], customers[1], 8001, true);
        runtime.getEventService().sendEventBean(measurement, "OperationMeasurement");
    }
}
