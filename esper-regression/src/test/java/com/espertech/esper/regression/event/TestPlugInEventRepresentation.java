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

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.plugin.PlugInEventBeanReflectorContext;
import com.espertech.esper.plugin.PlugInEventRepresentationContext;
import com.espertech.esper.plugin.PlugInEventTypeHandlerContext;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class TestPlugInEventRepresentation extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener[] listeners;

    public void setUp()
    {
        listeners = new SupportUpdateListener[5];
        for (int i = 0; i < listeners.length; i++)
        {
            listeners[i] = new SupportUpdateListener();
        }
    }

    protected void tearDown() throws Exception {
        listeners = null;
    }

    /*
     * Use case 1: static event type resolution, no event object reflection (static event type assignment)
     * Use case 2: static event type resolution, dynamic event object reflection and event type assignment
     *   a) Register all representations with URI via configuration
     *   b) Register event type name and specify the list of URI to use for resolving:
     *     // at engine initialization time it obtain instances of an EventType for each name
     *   c) Create statement using the registered event type name
     *   d) Get EventSender to send in that specific type of event
     */
    public void testPreConfigStaticTypeResolution() throws Exception
    {
        if (SupportConfigFactory.skipTest(TestPlugInEventRepresentation.class)) {
            return;
        }
        Configuration configuration = getConfiguration();
        configuration.addPlugInEventType("TestTypeOne", new URI[] {new URI("type://properties/test1/testtype")}, "t1");
        configuration.addPlugInEventType("TestTypeTwo", new URI[] {new URI("type://properties/test2")}, "t2");
        configuration.addPlugInEventType("TestTypeThree", new URI[] {new URI("type://properties/test3")}, "t3");
        configuration.addPlugInEventType("TestTypeFour", new URI[] {new URI("type://properties/test2/x"), new URI("type://properties/test3")}, "t4");

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        runAssertionCaseStatic(epService);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testRuntimeConfigStaticTypeResolution() throws Exception
    {
        if (SupportConfigFactory.skipTest(TestPlugInEventRepresentation.class)) {
            return;
        }
        Configuration configuration = getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        ConfigurationOperations runtimeConfig = epService.getEPAdministrator().getConfiguration();
        runtimeConfig.addPlugInEventType("TestTypeOne", new URI[] {new URI("type://properties/test1/testtype")}, "t1");
        runtimeConfig.addPlugInEventType("TestTypeTwo", new URI[] {new URI("type://properties/test2")}, "t2");
        runtimeConfig.addPlugInEventType("TestTypeThree", new URI[] {new URI("type://properties/test3")}, "t3");
        runtimeConfig.addPlugInEventType("TestTypeFour", new URI[] {new URI("type://properties/test2/x"), new URI("type://properties/test3")}, "t4");

        runAssertionCaseStatic(epService);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    /*
     * Use case 3: dynamic event type resolution
     *   a) Register all representations with URI via configuration
     *   b) Via configuration, set a list of URIs to use for resolving new event type names
     *   c) Compile statement with an event type name that is not defined yet, each of the representations are asked to accept, in URI hierarchy order
     *     admin.createEPL("select a, b, c from MyEventType");
     *    // engine asks each event representation to create an EventType, takes the first valid one
     *   d) Get EventSender to send in that specific type of event, or a URI-list dynamic reflection sender
     */
    public void testRuntimeConfigDynamicTypeResolution() throws Exception
    {
        if (SupportConfigFactory.skipTest(TestPlugInEventRepresentation.class)) {
            return;
        }
        Configuration configuration = getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        URI[] uriList = new URI[] {new URI("type://properties/test2/myresolver")};
        epService.getEPAdministrator().getConfiguration().setPlugInEventTypeResolutionURIs(uriList);

        runAssertionCaseDynamic(epService);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testStaticConfigDynamicTypeResolution() throws Exception
    {
        if (SupportConfigFactory.skipTest(TestPlugInEventRepresentation.class)) {
            return;
        }
        URI[] uriList = new URI[] {new URI("type://properties/test2/myresolver")};
        Configuration configuration = getConfiguration();
        configuration.setPlugInEventTypeResolutionURIs(uriList);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        runAssertionCaseDynamic(epService);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testInvalid() throws Exception
    {
        if (SupportConfigFactory.skipTest(TestPlugInEventRepresentation.class)) {
            return;
        }
        Configuration configuration = getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        try
        {
            epService.getEPRuntime().getEventSender(new URI[0]);
            fail();
        }
        catch (EventTypeException ex)
        {
            assertEquals("Event sender for resolution URIs '[]' did not return at least one event representation's event factory", ex.getMessage());
        }
    }

    public void testContextContents() throws Exception
    {
        if (SupportConfigFactory.skipTest(TestPlugInEventRepresentation.class)) {
            return;
        }
        Configuration configuration = getConfiguration();
        configuration.addPlugInEventRepresentation(new URI("type://test/support"), SupportEventRepresentation.class.getName(), "abc");
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        PlugInEventRepresentationContext initContext = SupportEventRepresentation.getInitContext();
        assertEquals(new URI("type://test/support"), initContext.getEventRepresentationRootURI());
        assertEquals("abc", initContext.getRepresentationInitializer());
        assertNotNull(initContext.getEventAdapterService());

        ConfigurationOperations runtimeConfig = epService.getEPAdministrator().getConfiguration();
        runtimeConfig.addPlugInEventType("TestTypeOne", new URI[] {new URI("type://test/support?a=b&c=d")}, "t1");

        PlugInEventTypeHandlerContext context = SupportEventRepresentation.getAcceptTypeContext();
        assertEquals(new URI("type://test/support?a=b&c=d"), context.getEventTypeResolutionURI());
        assertEquals("t1", context.getTypeInitializer());
        assertEquals("TestTypeOne", context.getEventTypeName());

        context = SupportEventRepresentation.getEventTypeContext();
        assertEquals(new URI("type://test/support?a=b&c=d"), context.getEventTypeResolutionURI());
        assertEquals("t1", context.getTypeInitializer());
        assertEquals("TestTypeOne", context.getEventTypeName());

        epService.getEPRuntime().getEventSender(new URI[] {new URI("type://test/support?a=b")});
        PlugInEventBeanReflectorContext contextBean = SupportEventRepresentation.getEventBeanContext();
        assertEquals("type://test/support?a=b", contextBean.getResolutionURI().toString());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runAssertionCaseDynamic(EPServiceProvider epService) throws Exception
    {
        // type resolved for each by the first event representation picking both up, i.e. the one with "r2" since that is the most specific URI
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TestTypeOne");
        stmt.addListener(listeners[0]);
        stmt = epService.getEPAdministrator().createEPL("select * from TestTypeTwo");
        stmt.addListener(listeners[1]);

        // static senders
        EventSender sender = epService.getEPRuntime().getEventSender("TestTypeOne");
        sender.sendEvent(makeProperties(new String[][] {{"r2", "A"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[0].assertOneGetNewAndReset(), new Object[]{"A"});
        assertFalse(listeners[0].isInvoked());

        sender = epService.getEPRuntime().getEventSender("TestTypeTwo");
        sender.sendEvent(makeProperties(new String[][] {{"r2", "B"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[1].assertOneGetNewAndReset(), new Object[]{"B"});
    }

    private Configuration getConfiguration() throws URISyntaxException
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addPlugInEventRepresentation(new URI("type://properties"), MyPlugInEventRepresentation.class.getName(), "r3");
        configuration.addPlugInEventRepresentation(new URI("type://properties/test1"), MyPlugInEventRepresentation.class.getName(), "r1");
        configuration.addPlugInEventRepresentation(new URI("type://properties/test2"), MyPlugInEventRepresentation.class.getName(), "r2");
        return configuration;
    }

    private void runAssertionCaseStatic(EPServiceProvider epService) throws URISyntaxException
    {
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
        sender.sendEvent(makeProperties(new String[][] {{"r1", "A"}, {"t1", "B"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[0].assertOneGetNewAndReset(), new Object[]{"A", "B"});
        assertFalse(listeners[3].isInvoked() || listeners[1].isInvoked() || listeners[2].isInvoked());

        sender = epService.getEPRuntime().getEventSender("TestTypeTwo");
        sender.sendEvent(makeProperties(new String[][] {{"r2", "C"}, {"t2", "D"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[1].assertOneGetNewAndReset(), new Object[]{"C", "D"});
        assertFalse(listeners[3].isInvoked() || listeners[0].isInvoked() || listeners[2].isInvoked());

        sender = epService.getEPRuntime().getEventSender("TestTypeThree");
        sender.sendEvent(makeProperties(new String[][] {{"r3", "E"}, {"t3", "F"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[2].assertOneGetNewAndReset(), new Object[]{"E", "F"});
        assertFalse(listeners[3].isInvoked() || listeners[1].isInvoked() || listeners[0].isInvoked());

        sender = epService.getEPRuntime().getEventSender("TestTypeFour");
        sender.sendEvent(makeProperties(new String[][] {{"r2", "G"}, {"t4", "H"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[3].assertOneGetNewAndReset(), new Object[]{"G", "H"});
        assertFalse(listeners[0].isInvoked() || listeners[1].isInvoked() || listeners[2].isInvoked());

        // dynamic sender - decides on event type thus a particular update listener should see the event
        URI[] uriList = new URI[] {new URI("type://properties/test1"), new URI("type://properties/test2")};
        EventSender dynamicSender = epService.getEPRuntime().getEventSender(uriList);
        dynamicSender.sendEvent(makeProperties(new String[][] {{"r3", "I"}, {"t3", "J"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[2].assertOneGetNewAndReset(), new Object[]{"I", "J"});
        dynamicSender.sendEvent(makeProperties(new String[][] {{"r1", "K"}, {"t1", "L"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[0].assertOneGetNewAndReset(), new Object[]{"K", "L"});
        dynamicSender.sendEvent(makeProperties(new String[][] {{"r2", "M"}, {"t2", "N"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[1].assertOneGetNewAndReset(), new Object[]{"M", "N"});
        dynamicSender.sendEvent(makeProperties(new String[][] {{"r2", "O"}, {"t4", "P"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[3].assertOneGetNewAndReset(), new Object[]{"O", "P"});
        dynamicSender.sendEvent(makeProperties(new String[][] {{"r2", "O"}, {"t3", "P"}}));
        assertNoneReceived();

        uriList = new URI[] {new URI("type://properties/test2")};
        dynamicSender = epService.getEPRuntime().getEventSender(uriList);
        dynamicSender.sendEvent(makeProperties(new String[][] {{"r1", "I"}, {"t1", "J"}}));
        assertNoneReceived();
        dynamicSender.sendEvent(makeProperties(new String[][] {{"r2", "Q"}, {"t2", "R"}}));
        EPAssertionUtil.assertAllPropsSortedByName(listeners[1].assertOneGetNewAndReset(), new Object[]{"Q", "R"});
    }

    private void assertNoneReceived()
    {
        for (int i = 0; i < listeners.length; i++)
        {
            assertFalse(listeners[i].isInvoked());            
        }
    }

    private Properties makeProperties(String[][] values)
    {
        Properties theEvent = new Properties();
        for (int i = 0; i < values.length; i++)
        {
            theEvent.put(values[i][0], values[i][1]);
        }
        return theEvent;
    }
}
