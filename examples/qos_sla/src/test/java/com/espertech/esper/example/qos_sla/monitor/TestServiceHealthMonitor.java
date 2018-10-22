package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import junit.framework.TestCase;

public class TestServiceHealthMonitor extends TestCase {
    private EPRuntime runtime;

    public void setUp() {
        // This code runs as part of the automated regression test suite; Therefore disable internal timer theading to safe resources
        Configuration config = MonitorUtil.getConfiguration();
        config.getRuntime().getThreading().setInternalTimerEnabled(false);

        runtime = EPRuntimeProvider.getDefaultRuntime(config);
        runtime.initialize();

        new ServiceHealthMonitor(runtime);
    }

    public void tearDown() throws Exception {
        runtime.destroy();
    }

    public void testLatencyAlert() {
        sendEvent(false);
        sendEvent(false);
        sendEvent(false);
    }

    private void sendEvent(boolean success) {
        OperationMeasurement measurement = new OperationMeasurement("myService", "myCustomer", 10000, success);
        runtime.getEventService().sendEventBean(measurement, "OperationMeasurement");
    }
}
