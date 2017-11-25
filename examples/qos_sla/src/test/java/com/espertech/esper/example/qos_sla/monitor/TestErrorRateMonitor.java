package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import junit.framework.TestCase;

public class TestErrorRateMonitor extends TestCase {
    private EPServiceProvider epService;

    public void setUp() {
        ErrorRateMonitor.start();
        epService = EPServiceProviderManager.getDefaultProvider();
        epService.initialize();
    }

    public void tearDown() throws Exception {
        epService.destroy();
    }

    public void testAlert() throws Exception {
        for (int i = 0; i < 5; i++) {
            sendEvent(false);
        }

        //sleep(11000);

        for (int i = 0; i < 4; i++) {
            sendEvent(false);
        }

        //sleep(11000);
        //sleep(11000);
    }

    private void sendEvent(boolean success) {
        OperationMeasurement measurement = new OperationMeasurement("myService", "myCustomer", 10000, success);
        epService.getEPRuntime().sendEvent(measurement);
    }
}
