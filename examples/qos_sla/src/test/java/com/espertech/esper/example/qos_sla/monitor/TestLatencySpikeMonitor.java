package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import junit.framework.TestCase;

public class TestLatencySpikeMonitor extends TestCase {
    private EPServiceProvider epService;

    public void setUp() {
        LatencySpikeMonitor.start();
        epService = EPServiceProviderManager.getDefaultProvider();
        epService.initialize();
    }

    public void tearDown() throws Exception {
        epService.destroy();
    }

    public void testLatencyAlert() {
        OperationMeasurement measurement = new OperationMeasurement("svc", "cust", 21000, true);
        epService.getEPRuntime().sendEvent(measurement);
    }
}
