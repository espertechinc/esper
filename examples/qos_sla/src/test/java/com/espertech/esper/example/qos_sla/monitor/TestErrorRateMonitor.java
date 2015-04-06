package com.espertech.esper.example.qos_sla.monitor;

import com.espertech.esper.example.qos_sla.eventbean.*;
import com.espertech.esper.client.*;
import junit.framework.TestCase;

public class TestErrorRateMonitor extends TestCase
{
    private EPRuntime runtime;

    public void setUp()
    {
        ErrorRateMonitor.start();
        runtime = EPServiceProviderManager.getDefaultProvider().getEPRuntime();
    }

    public void testAlert() throws Exception
    {
        for (int i= 0; i < 5; i++)
        {
            sendEvent(false);
        }

        //sleep(11000);

        for (int i= 0; i < 4; i++)
        {
            sendEvent(false);
        }

        //sleep(11000);
        //sleep(11000);
    }

    private void sendEvent(boolean success)
    {
        OperationMeasurement measurement = new OperationMeasurement("myService", "myCustomer", 10000, success);
        runtime.sendEvent(measurement);
    }
}
