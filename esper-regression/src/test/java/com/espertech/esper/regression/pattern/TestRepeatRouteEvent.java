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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;

public class TestRepeatRouteEvent extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    /**
     * Test route of an event within a listener.
     * The Listener when it receives an event will generate a single new event
     * that it routes back into the runtime, up to X number of times.
     */
    public void testRouteSingle() throws Exception
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();} // excluded
        String viewExpr = "every tag=" + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(viewExpr);

        SingleRouteUpdateListener listener = new SingleRouteUpdateListener();
        patternStmt.addListener(listener);

        // Send first event that triggers the loop
        sendEvent(0);

        // Should have fired X times
        assertEquals(1000, listener.getCount());

        // test route map and XML doc - smoke test
        patternStmt.addListener(new StatementAwareUpdateListener() {

            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider)
            {
                Node theEvent = getXMLEvent("<root><value>5</value></root>");
                epServiceProvider.getEPRuntime().route(theEvent);
                epServiceProvider.getEPRuntime().route(new HashMap(), "MyMap");
            }
        });
    }

    /**
     * Test route of multiple events within a listener.
     * The Listener when it receives an event will generate multiple new events
     * that it routes back into the runtime, up to X number of times.
     */
    public void testRouteCascade()
    {
        String viewExpr = "every tag=" + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(viewExpr);

        CascadeRouteUpdateListener listener = new CascadeRouteUpdateListener();
        patternStmt.addListener(listener);

        // Send first event that triggers the loop
        sendEvent(2);       // the 2 translates to number of new events routed

        // Should have fired X times
        assertEquals(9, listener.getCountReceived());
        assertEquals(8, listener.getCountRouted());

        //  Num    Received         Routes      Num
        //  2             1           2         3
        //  3             2           6         4
        //  4             6             -
    }

    public void testRouteTimer()
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();} // excluded
        String viewExpr = "every tag=" + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(viewExpr);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        // define time-based pattern and listener
        viewExpr = "timer:at(*,*,*,*,*,*)";
        EPStatement atPatternStmt = epService.getEPAdministrator().createPattern(viewExpr);
        SingleRouteUpdateListener timeListener = new SingleRouteUpdateListener();
        atPatternStmt.addListener(timeListener);

        // register regular listener
        SingleRouteUpdateListener eventListener = new SingleRouteUpdateListener();
        patternStmt.addListener(eventListener);

        assertEquals(0, timeListener.getCount());
        assertEquals(0, eventListener.getCount());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));

        assertEquals(1, timeListener.getCount());
        assertEquals(1000, eventListener.getCount());
    }

    private SupportBean sendEvent(int intValue)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intValue);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }

    private SupportBean routeEvent(int intValue)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intValue);
        epService.getEPRuntime().route(theEvent);
        return theEvent;
    }

    private Node getXMLEvent(String xml)
    {
        try
        {
            StringReader reader = new StringReader(xml);
            InputSource source = new InputSource(reader);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);
            return simpleDoc;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    class SingleRouteUpdateListener implements UpdateListener {

        private int count = 0;

        public void update(EventBean[] newEvents, EventBean[] oldEvents)
        {
            count++;
            if (count < 1000)
            {
                routeEvent(0);
            }
        }

        public int getCount()
        {
            return count;
        }
    };

    class CascadeRouteUpdateListener implements UpdateListener {

        private int countReceived = 0;
        private int countRouted = 0;

        public void update(EventBean[] newEvents, EventBean[] oldEvents)
        {
            countReceived++;
            SupportBean theEvent = (SupportBean) (newEvents[0].get("tag"));
            int numNewEvents = theEvent.getIntPrimitive();

            for (int i = 0; i < numNewEvents; i++)
            {
                if (numNewEvents < 4)
                {
                    routeEvent(numNewEvents + 1);
                    countRouted++;
                }
            }
        }

        public int getCountReceived()
        {
            return countReceived;
        }

        public int getCountRouted()
        {
            return countRouted;
        }
    };
}
