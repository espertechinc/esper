package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.example.qos_sla.eventbean.OperationMeasurement;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import junit.framework.TestCase;

public class TestLatencySpikeMonitor extends TestCase {
    private EPRuntime runtime;

    public void setUp() {
        // This code runs as part of the automated regression test suite; Therefore disable internal timer theading to safe resources
        Configuration config = MonitorUtil.getConfiguration();
        config.getRuntime().getThreading().setInternalTimerEnabled(false);

        runtime = EPRuntimeProvider.getDefaultRuntime(config);
        runtime.initialize();

        new LatencySpikeMonitor(runtime);
    }

    public void tearDown() throws Exception {
        runtime.destroy();
    }

    public void testLatencyAlert() {
        OperationMeasurement measurement = new OperationMeasurement("svc", "cust", 21000, true);
        runtime.getEventService().sendEventBean(measurement, "OperationMeasurement");
    }
}
