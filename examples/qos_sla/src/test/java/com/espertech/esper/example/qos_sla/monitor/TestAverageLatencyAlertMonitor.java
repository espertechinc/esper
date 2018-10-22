package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import junit.framework.TestCase;

public class TestAverageLatencyAlertMonitor extends TestCase {
    private EPRuntime runtime;

    public void setUp() {
        // This code runs as part of the automated regression test suite; Therefore disable internal timer theading to safe resources
        Configuration config = MonitorUtil.getConfiguration();
        config.getRuntime().getThreading().setInternalTimerEnabled(false);

        runtime = EPRuntimeProvider.getDefaultRuntime(config);
        runtime.initialize();

        new AverageLatencyMonitor(runtime);
    }

    public void tearDown() throws Exception {
        runtime.destroy();
    }

    public void testLatencyAlert() {
        String services[] = {"s0", "s1", "s2"};
        String customers[] = {"c0", "c1", "c2"};

        for (int i = 0; i < 100; i++) {
            for (int index = 0; index < services.length; index++) {
                OperationMeasurement measurement = new OperationMeasurement(services[index], customers[index],
                    9950 + i, true);
                runtime.getEventService().sendEventBean(measurement, "OperationMeasurement");
            }
        }

        // This should generate an alert
        OperationMeasurement measurement = new OperationMeasurement(services[0], customers[0], 10000, true);
        runtime.getEventService().sendEventBean(measurement, "OperationMeasurement");

        // This should generate an alert
        measurement = new OperationMeasurement(services[1], customers[1], 10001, true);
        runtime.getEventService().sendEventBean(measurement, "OperationMeasurement");

        // This should not generate an alert
        measurement = new OperationMeasurement(services[2], customers[2], 9999, true);
        runtime.getEventService().sendEventBean(measurement, "OperationMeasurement");
    }
}
