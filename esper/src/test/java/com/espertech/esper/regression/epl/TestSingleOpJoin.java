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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestSingleOpJoin extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    private SupportBean_A eventsA[] = new SupportBean_A[10];
    private SupportBean_A eventsASetTwo[] = new SupportBean_A[10];
    private SupportBean_B eventsB[] = new SupportBean_B[10];
    private SupportBean_B eventsBSetTwo[] = new SupportBean_B[10];

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String eventA = SupportBean_A.class.getName();
        String eventB = SupportBean_B.class.getName();

        String joinStatement = "select irstream * from " +
            eventA + "()#length(3) as streamA," +
            eventB + "()#length(3) as streamB" +
            " where streamA.id = streamB.id";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        assertEquals(SupportBean_A.class, joinView.getEventType().getPropertyType("streamA"));
        assertEquals(SupportBean_B.class, joinView.getEventType().getPropertyType("streamB"));
        assertEquals(2, joinView.getEventType().getPropertyNames().length);

        for (int i = 0; i < eventsA.length; i++)
        {
            eventsA[i] = new SupportBean_A(Integer.toString(i));
            eventsASetTwo[i] = new SupportBean_A(Integer.toString(i));
            eventsB[i] = new SupportBean_B(Integer.toString(i));
            eventsBSetTwo[i] = new SupportBean_B(Integer.toString(i));
        }
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testJoinUniquePerId()
    {
        sendEvent(eventsA[0]);
        sendEvent(eventsB[1]);
        assertNull(updateListener.getLastNewData());

        // Test join new B with id 0
        sendEvent(eventsB[0]);
        assertSame(eventsA[0], updateListener.getLastNewData()[0].get("streamA"));
        assertSame(eventsB[0], updateListener.getLastNewData()[0].get("streamB"));
        assertNull(updateListener.getLastOldData());
        updateListener.reset();

        // Test join new A with id 1
        sendEvent(eventsA[1]);
        assertSame(eventsA[1], updateListener.getLastNewData()[0].get("streamA"));
        assertSame(eventsB[1], updateListener.getLastNewData()[0].get("streamB"));
        assertNull(updateListener.getLastOldData());
        updateListener.reset();

        sendEvent(eventsA[2]);
        assertNull(updateListener.getLastOldData());

        // Test join old A id 0 leaves length window of 3 events
        sendEvent(eventsA[3]);
        assertSame(eventsA[0], updateListener.getLastOldData()[0].get("streamA"));
        assertSame(eventsB[0], updateListener.getLastOldData()[0].get("streamB"));
        assertNull(updateListener.getLastNewData());
        updateListener.reset();

        // Test join old B id 1 leaves window
        sendEvent(eventsB[4]);
        assertNull(updateListener.getLastOldData());
        sendEvent(eventsB[5]);
        assertSame(eventsA[1], updateListener.getLastOldData()[0].get("streamA"));
        assertSame(eventsB[1], updateListener.getLastOldData()[0].get("streamB"));
        assertNull(updateListener.getLastNewData());
    }

    public void testJoinNonUniquePerId()
    {
        sendEvent(eventsA[0]);
        sendEvent(eventsA[1]);
        sendEvent(eventsASetTwo[0]);
        assertTrue(updateListener.getLastOldData() == null && updateListener.getLastNewData() == null);

        sendEvent(eventsB[0]); // Event B id 0 joins to A id 0 twice
        EventBean[] data = updateListener.getLastNewData();
        assertTrue(eventsASetTwo[0] == data[0].get("streamA") || eventsASetTwo[0] == data[1].get("streamA"));    // Order arbitrary
        assertSame(eventsB[0], data[0].get("streamB"));
        assertTrue(eventsA[0] == data[0].get("streamA") || eventsA[0] == data[1].get("streamA"));
        assertSame(eventsB[0], data[1].get("streamB"));
        assertNull(updateListener.getLastOldData());
        updateListener.reset();

        sendEvent(eventsB[2]);
        sendEvent(eventsBSetTwo[0]);  // Ignore events generated
        updateListener.reset();

        sendEvent(eventsA[3]);  // Pushes A id 0 out of window, which joins to B id 0 twice
        data = updateListener.getLastOldData();
        assertSame(eventsA[0], updateListener.getLastOldData()[0].get("streamA"));
        assertTrue(eventsB[0] == data[0].get("streamB") || eventsB[0] == data[1].get("streamB"));    // B order arbitrary
        assertSame(eventsA[0], updateListener.getLastOldData()[1].get("streamA"));
        assertTrue(eventsBSetTwo[0] == data[0].get("streamB") || eventsBSetTwo[0] == data[1].get("streamB"));
        assertNull(updateListener.getLastNewData());
        updateListener.reset();

        sendEvent(eventsBSetTwo[2]);  // Pushes B id 0 out of window, which joins to A set two id 0
        assertSame(eventsASetTwo[0], updateListener.getLastOldData()[0].get("streamA"));
        assertSame(eventsB[0], updateListener.getLastOldData()[0].get("streamB"));
        assertEquals(1, updateListener.getLastOldData().length);
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
