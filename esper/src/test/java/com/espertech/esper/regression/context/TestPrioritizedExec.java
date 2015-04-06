/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.context;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestPrioritizedExec extends TestCase {

    private EPServiceProvider epService;

    public void testPrioritizedExec() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getExecution().setPrioritized(true);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType(Event.class);

        sendTimeEvent("2002-05-1T10:00:00.000");
        
        String epl =
            "\n @Name('ctx') create context RuleActivityTime as start (0, 9, *, *, *) end (0, 17, *, *, *);" +
            "\n @Name('window') context RuleActivityTime create window EventsWindow.std:firstunique(productID) as Event;" +
            "\n @Name('variable') create variable boolean IsOutputTriggered_2 = false;" +
            "\n @Name('A') insert into EventsWindow select * from Event(not exists (select * from EventsWindow));" +
            "\n @Name('B') insert into EventsWindow select * from Event(not exists (select * from EventsWindow));" +
            "\n @Name('C') insert into EventsWindow select * from Event(not exists (select * from EventsWindow));" +
            "\n @Name('D') insert into EventsWindow select * from Event(not exists (select * from EventsWindow));" +
            "\n @Name('out') context RuleActivityTime select * from EventsWindow";

        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getStatement("out").addListener(new SupportUpdateListener());

        epService.getEPRuntime().sendEvent(new Event("A1"));
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }
    
    private void sendTimeEvent(String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    public static class Event {
        private final String productID;

        public Event(String productId) {
            this.productID = productId;
        }

        public String getProductID() {
            return productID;
        }
    }
}
