package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import junit.framework.TestCase;

public class TestAverageLatencyAlertMonitor extends TestCase {
    private EPServiceProvider epService;

    public void setUp() {
        // This code runs as part of the automated regression test suite; Therefore disable internal timer theading to safe resources
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);

        AverageLatencyMonitor.start();
        epService = EPServiceProviderManager.getDefaultProvider(config);
    }

    public void tearDown() throws Exception {
        epService.destroy();
    }

    public void testLatencyAlert() {
        String services[] = {"s0", "s1", "s2"};
        String customers[] = {"c0", "c1", "c2"};

        for (int i = 0; i < 100; i++) {
            for (int index = 0; index < services.length; index++) {
                OperationMeasurement measurement = new OperationMeasurement(services[index], customers[index],
                        9950 + i, true);
                epService.getEPRuntime().sendEvent(measurement);
            }
        }

        // This should generate an alert
        OperationMeasurement measurement = new OperationMeasurement(services[0], customers[0], 10000, true);
        epService.getEPRuntime().sendEvent(measurement);

        // This should generate an alert
        measurement = new OperationMeasurement(services[1], customers[1], 10001, true);
        epService.getEPRuntime().sendEvent(measurement);

        // This should not generate an alert
        measurement = new OperationMeasurement(services[2], customers[2], 9999, true);
        epService.getEPRuntime().sendEvent(measurement);
    }
}
