package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.example.qos_sla.eventbean.*;
import com.espertech.esper.client.*;
import junit.framework.TestCase;

public class TestLatencySpikeMonitor extends TestCase
{
    private EPRuntime runtime;

    public void setUp()
    {
        LatencySpikeMonitor.start();
        runtime = EPServiceProviderManager.getDefaultProvider().getEPRuntime();
    }

    public void testLatencyAlert()
    {
        OperationMeasurement measurement = new OperationMeasurement("svc", "cust", 21000, true);
        runtime.sendEvent(measurement);
    }
}
