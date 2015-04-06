package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.example.qos_sla.eventbean.*;
import com.espertech.esper.client.*;
import junit.framework.TestCase;

public class TestAverageLatencyAlertMonitor extends TestCase
{
    private EPRuntime runtime;

    public void setUp()
    {
        // This code runs as part of the automated regression test suite; Therefore disable internal timer theading to safe resources
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);

        AverageLatencyMonitor.start();
        runtime = EPServiceProviderManager.getDefaultProvider(config).getEPRuntime();
    }

    public void testLatencyAlert()
    {
        String services[] = {"s0", "s1", "s2"};
        String customers[] = {"c0", "c1", "c2"};

        for (int i = 0; i < 100; i++)
        {
            for (int index = 0; index < services.length; index++)
            {
                OperationMeasurement measurement = new OperationMeasurement(services[index], customers[index],
                        9950 + i, true);
                runtime.sendEvent(measurement);
            }
        }

        // This should generate an alert
        OperationMeasurement measurement = new OperationMeasurement(services[0], customers[0], 10000, true);
        runtime.sendEvent(measurement);

        // This should generate an alert
        measurement = new OperationMeasurement(services[1], customers[1], 10001, true);
        runtime.sendEvent(measurement);

        // This should not generate an alert
        measurement = new OperationMeasurement(services[2], customers[2], 9999, true);
        runtime.sendEvent(measurement);
    }
}
