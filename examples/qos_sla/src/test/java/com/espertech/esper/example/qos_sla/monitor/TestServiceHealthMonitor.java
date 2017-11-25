package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import junit.framework.TestCase;

public class TestServiceHealthMonitor extends TestCase {
    private EPServiceProvider epService;

    public void setUp() {
        ServiceHealthMonitor.start();
        epService = EPServiceProviderManager.getDefaultProvider();
    }

    public void tearDown() throws Exception {
        epService.destroy();
    }

    public void testLatencyAlert() {
        sendEvent(false);
        sendEvent(false);
        sendEvent(false);
    }

    private void sendEvent(boolean success) {
        OperationMeasurement measurement = new OperationMeasurement("myService", "myCustomer", 10000, success);
        epService.getEPRuntime().sendEvent(measurement);
    }
}
