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
import java.util.Properties;

import static com.espertech.esper.regression.event.plugin.ExecEventPlugInConfigRuntimeTypeResolution.configureURIs;
import static org.junit.Assert.assertFalse;

public class ExecEventPlugInRuntimeConfigDynamicTypeResolution implements RegressionExecution {

/*
 * Use case 3: dynamic event type resolution
 *   a) Register all representations with URI via configuration
 *   b) Via configuration, set a list of URIs to use for resolving new event type names
 *   c) Compile statement with an event type name that is not defined yet, each of the representations are asked to accept, in URI hierarchy order
 *     admin.createEPL("select a, b, c from MyEventType");
 *    // engine asks each event representation to create an EventType, takes the first valid one
 *   d) Get EventSender to send in that specific type of event, or a URI-list dynamic reflection sender
 */

    public void configure(Configuration configuration) throws Exception {
        configureURIs(configuration);
    }

    public void run(EPServiceProvider epService) throws Exception {

        if (SupportConfigFactory.skipTest(ExecEventPlugInRuntimeConfigDynamicTypeResolution.class)) {
            return;
        }

        URI[] uriList = new URI[]{new URI("type://properties/test2/myresolver")};
        epService.getEPAdministrator().getConfiguration().setPlugInEventTypeResolutionURIs(uriList);

        runAssertionCaseDynamic(epService);
    }

    public static void runAssertionCaseDynamic(EPServiceProvider epService) throws Exception {
        // type resolved for each by the first event representation picking both up, i.e. the one with "r2" since that is the most specific URI
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TestTypeOne");
        SupportUpdateListener[] listeners = SupportUpdateListener.makeListeners(5);
        stmt.addListener(listeners[0]);
        stmt = epService.getEPAdministrator().createEPL("select * from TestTypeTwo");
        stmt.addListener(listeners[1]);

        // static senders
        EventSender sender = epService.getEPRuntime().getEventSender("TestTypeOne");
        sender.sendEvent(makePropertiesFromStrings(new String[][]{{"r2", "A"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[0].assertOneGetNewAndReset(), new Object[]{"A"});
        assertFalse(listeners[0].isInvoked());

        sender = epService.getEPRuntime().getEventSender("TestTypeTwo");
        sender.sendEvent(makePropertiesFromStrings(new String[][]{{"r2", "B"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[1].assertOneGetNewAndReset(), new Object[]{"B"});
    }

    private static Properties makePropertiesFromStrings(String[][] values) {
        Properties theEvent = new Properties();
        for (int i = 0; i < values.length; i++) {
            theEvent.put(values[i][0], values[i][1]);
        }
        return theEvent;
    }
}
