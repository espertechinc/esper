/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.regression.event.plugin;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventSender;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static com.espertech.esper.regression.event.plugin.ExecEventPlugInConfigRuntimeTypeResolution.configureURIs;
import static org.junit.Assert.assertFalse;

public class ExecEventPlugInConfigStaticTypeResolution implements RegressionExecution {
    /*
     * Use case 1: static event type resolution, no event object reflection (static event type assignment)
     * Use case 2: static event type resolution, dynamic event object reflection and event type assignment
     *   a) Register all representations with URI via configuration
     *   b) Register event type name and specify the list of URI to use for resolving:
     *     // at engine initialization time it obtain instances of an EventType for each name
     *   c) Create statement using the registered event type name
     *   d) Get EventSender to send in that specific type of event
     */

    public void configure(Configuration configuration) throws Exception {
        configureURIs(configuration);

        configuration.addPlugInEventType("TestTypeOne", new URI[]{new URI("type://properties/test1/testtype")}, "t1");
        configuration.addPlugInEventType("TestTypeTwo", new URI[]{new URI("type://properties/test2")}, "t2");
        configuration.addPlugInEventType("TestTypeThree", new URI[]{new URI("type://properties/test3")}, "t3");
        configuration.addPlugInEventType("TestTypeFour", new URI[]{new URI("type://properties/test2/x"), new URI("type://properties/test3")}, "t4");
    }

    public void run(EPServiceProvider epService) throws Exception {
        if (SupportConfigFactory.skipTest(ExecEventPlugInConfigStaticTypeResolution.class)) {
            return;
        }
        runAssertionCaseStatic(epService);
    }

    public static void runAssertionCaseStatic(EPServiceProvider epService) throws URISyntaxException {
        SupportUpdateListener[] listeners = SupportUpdateListener.makeListeners(5);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TestTypeOne");
        stmt.addListener(listeners[0]);
        stmt = epService.getEPAdministrator().createEPL("select * from TestTypeTwo");
        stmt.addListener(listeners[1]);
        stmt = epService.getEPAdministrator().createEPL("select * from TestTypeThree");
        stmt.addListener(listeners[2]);
        stmt = epService.getEPAdministrator().createEPL("select * from TestTypeFour");
        stmt.addListener(listeners[3]);

        // static senders
        EventSender sender = epService.getEPRuntime().getEventSender("TestTypeOne");
        sender.sendEvent(makeProperties(new String[][]{{"r1", "A"}, {"t1", "B"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[0].assertOneGetNewAndReset(), new Object[]{"A", "B"});
        assertFalse(listeners[3].isInvoked() || listeners[1].isInvoked() || listeners[2].isInvoked());

        sender = epService.getEPRuntime().getEventSender("TestTypeTwo");
        sender.sendEvent(makeProperties(new String[][]{{"r2", "C"}, {"t2", "D"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[1].assertOneGetNewAndReset(), new Object[]{"C", "D"});
        assertFalse(listeners[3].isInvoked() || listeners[0].isInvoked() || listeners[2].isInvoked());

        sender = epService.getEPRuntime().getEventSender("TestTypeThree");
        sender.sendEvent(makeProperties(new String[][]{{"r3", "E"}, {"t3", "F"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[2].assertOneGetNewAndReset(), new Object[]{"E", "F"});
        assertFalse(listeners[3].isInvoked() || listeners[1].isInvoked() || listeners[0].isInvoked());

        sender = epService.getEPRuntime().getEventSender("TestTypeFour");
        sender.sendEvent(makeProperties(new String[][]{{"r2", "G"}, {"t4", "H"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[3].assertOneGetNewAndReset(), new Object[]{"G", "H"});
        assertFalse(listeners[0].isInvoked() || listeners[1].isInvoked() || listeners[2].isInvoked());

        // dynamic sender - decides on event type thus a particular update listener should see the event
        URI[] uriList = new URI[]{new URI("type://properties/test1"), new URI("type://properties/test2")};
        EventSender dynamicSender = epService.getEPRuntime().getEventSender(uriList);
        dynamicSender.sendEvent(makeProperties(new String[][]{{"r3", "I"}, {"t3", "J"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[2].assertOneGetNewAndReset(), new Object[]{"I", "J"});
        dynamicSender.sendEvent(makeProperties(new String[][]{{"r1", "K"}, {"t1", "L"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[0].assertOneGetNewAndReset(), new Object[]{"K", "L"});
        dynamicSender.sendEvent(makeProperties(new String[][]{{"r2", "M"}, {"t2", "N"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[1].assertOneGetNewAndReset(), new Object[]{"M", "N"});
        dynamicSender.sendEvent(makeProperties(new String[][]{{"r2", "O"}, {"t4", "P"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[3].assertOneGetNewAndReset(), new Object[]{"O", "P"});
        dynamicSender.sendEvent(makeProperties(new String[][]{{"r2", "O"}, {"t3", "P"}}));
        assertNoneReceived(listeners);

        uriList = new URI[]{new URI("type://properties/test2")};
        dynamicSender = epService.getEPRuntime().getEventSender(uriList);
        dynamicSender.sendEvent(makeProperties(new String[][]{{"r1", "I"}, {"t1", "J"}}));
        assertNoneReceived(listeners);
        dynamicSender.sendEvent(makeProperties(new String[][]{{"r2", "Q"}, {"t2", "R"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[1].assertOneGetNewAndReset(), new Object[]{"Q", "R"});
    }

    private static void assertNoneReceived(SupportUpdateListener[] listeners) {
        for (int i = 0; i < listeners.length; i++) {
            assertFalse(listeners[i].isInvoked());
        }
    }

    private static Properties makeProperties(String[][] values) {
        Properties theEvent = new Properties();
        for (int i = 0; i < values.length; i++) {
            theEvent.put(values[i][0], values[i][1]);
        }
        return theEvent;
    }
}
