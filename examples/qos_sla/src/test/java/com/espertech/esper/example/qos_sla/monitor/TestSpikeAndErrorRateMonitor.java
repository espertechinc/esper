package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import junit.framework.TestCase;

public class TestSpikeAndErrorRateMonitor extends TestCase {
    private EPServiceProvider epService;

    public void setUp() {
        SpikeAndErrorMonitor.start();
        epService = EPServiceProviderManager.getDefaultProvider();
        epService.initialize();
    }

    public void tearDown() throws Exception {
        epService.destroy();
    }

    public void testAlert() throws Exception {
        sendEvent("s1", 30000, false);
    }

    private void sendEvent(String service, long latency, boolean success) {
        OperationMeasurement measurement = new OperationMeasurement(service, "myCustomer", latency, success);
        epService.getEPRuntime().sendEvent(measurement);
    }
}
