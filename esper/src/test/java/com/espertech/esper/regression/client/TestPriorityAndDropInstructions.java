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

package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.annotation.Priority;
import com.espertech.esper.client.annotation.Drop;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestPriorityAndDropInstructions extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private SupportUpdateListener listenerTwo;
    private SupportUpdateListener[] listeners;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        listenerTwo = new SupportUpdateListener();

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.getEngineDefaults().getExecution().setPrioritized(true);     // also sets share-views to false

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        listeners = new SupportUpdateListener[10];
        for (int i = 0; i < listeners.length; i++)
        {
            listeners[i] = new SupportUpdateListener();
        }
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
        listenerTwo = null;
        listeners = null;
    }

    @Priority(10)
    @Drop
    public void testSchedulingPriority()
    {
        sendTimer(0,epService);
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Priority(1) select 1 as prio from pattern [every timer:interval(10)]", "s1");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(3) select 3 as prio from pattern [every timer:interval(10)]", "s3");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select 2 as prio from pattern [every timer:interval(10)]", "s2");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(4) select 4 as prio from pattern [every timer:interval(10)]", "s4");
        stmt.addListener(listener);

        sendTimer(10000, epService);
        assertPrio(null, new int[] {4, 3, 2, 1});

        epService.getEPAdministrator().getStatement("s2").destroy();
        stmt = epService.getEPAdministrator().createEPL("select 0 as prio from pattern [every timer:interval(10)]", "s0");
        stmt.addListener(listener);

        sendTimer(20000, epService);
        assertPrio(null, new int[] {4, 3, 1, 0});

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select 2 as prio from pattern [every timer:interval(10)]", "s2");
        stmt.addListener(listener);

        sendTimer(30000, epService);
        assertPrio(null, new int[] {4, 3, 2, 1, 0});

        stmt = epService.getEPAdministrator().createEPL("@Priority(3) select 3 as prio from pattern [every timer:interval(10)]", "s2");
        stmt.addListener(listener);

        sendTimer(40000, epService);
        assertPrio(null, new int[] {4, 3, 3, 2, 1, 0});
    }

    public void testSchedulingDrop()
    {
        sendTimer(0,epService);
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Drop select 1 as prio from pattern [every timer:interval(10)]", "s1");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select 3 as prio from pattern [every timer:interval(10)]", "s3");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("select 2 as prio from pattern [every timer:interval(10)]", "s2");
        stmt.addListener(listener);

        sendTimer(10000, epService);
        assertPrio(null, new int[] {3, 1});
    }

    public void testNamedWindowPriority()
    {
        String stmtText;
        EPStatement stmt;

        stmtText = "create window MyWindow#lastevent as select * from SupportBean";
        stmt = epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "insert into MyWindow select * from SupportBean";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "@Priority(1) on MyWindow e select e.theString as theString, 1 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s1");
        stmt.addListener(listener);

        stmtText = "@Priority(3) on MyWindow e select e.theString as theString, 3 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s3");
        stmt.addListener(listener);

        stmtText = "@Priority(2) on MyWindow e select e.theString as theString, 2 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s2");
        stmt.addListener(listener);

        stmtText = "@Priority(4) on MyWindow e select e.theString as theString, 4 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s4");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertPrio("E1", new int[] {4, 3, 2, 1});

        epService.getEPAdministrator().getStatement("s2").destroy();
        stmt = epService.getEPAdministrator().createEPL("on MyWindow e select e.theString as theString, 0 as prio from MyWindow", "s0");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertPrio("E2", new int[] {4, 3, 1, 0});

        stmtText = "@Priority(2) on MyWindow e select e.theString as theString, 2 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s2");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertPrio("E3", new int[] {4, 3, 2, 1, 0});

        stmtText = "@Priority(3) on MyWindow e select e.theString as theString, 3 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "sx");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        assertPrio("E4", new int[] {4, 3, 3, 2, 1, 0});
    }

    public void testNamedWindowDrop()
    {
        String stmtText;
        EPStatement stmt;

        stmtText = "create window MyWindow#lastevent as select * from SupportBean";
        stmt = epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "insert into MyWindow select * from SupportBean";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "@Drop on MyWindow e select e.theString as theString, 2 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s2");
        stmt.addListener(listener);

        stmtText = "@Priority(3) on MyWindow e select e.theString as theString, 3 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s3");
        stmt.addListener(listener);

        stmtText = "on MyWindow e select e.theString as theString, 0 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s2");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertPrio("E1", new int[] {3, 2});
    }

    public void testPriority()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Priority(1) select *, 1 as prio from SupportBean", "s1");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(3) select *, 3 as prio from SupportBean", "s3");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select *, 2 as prio from SupportBean", "s2");
        stmt.addListener(listener);
        
        stmt = epService.getEPAdministrator().createEPL("@Priority(4) select *, 4 as prio from SupportBean", "s4");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertPrio("E1", new int[] {4, 3, 2, 1});

        epService.getEPAdministrator().getStatement("s2").destroy();
        stmt = epService.getEPAdministrator().createEPL("select *, 0 as prio from SupportBean", "s0");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertPrio("E2", new int[] {4, 3, 1, 0});

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select *, 2 as prio from SupportBean", "s2");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertPrio("E3", new int[] {4, 3, 2, 1, 0});

        stmt = epService.getEPAdministrator().createEPL("@Priority(3) select *, 3 as prio from SupportBean", "sx");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        assertPrio("E4", new int[] {4, 3, 3, 2, 1, 0});
    }

    public void testAddRemoveStmts()
    {
        String stmtSelectText = "insert into ABCStream select * from SupportBean";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtSelectText);
        stmtSelect.addListener(listener);
        
        String stmtOneText = "@Drop select * from SupportBean where intPrimitive = 1";
        EPStatement statementOne = epService.getEPAdministrator().createEPL(stmtOneText);
        statementOne.addListener(listeners[0]);

        String stmtTwoText = "@Drop select * from SupportBean where intPrimitive = 2";
        EPStatement statementTwo = epService.getEPAdministrator().createEPL(stmtTwoText);
        statementTwo.addListener(listeners[1]);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(0, "E1");

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(1, "E2");

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(0, "E3");

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        assertEquals("E4", listener.assertOneGetNewAndReset().get("theString"));
        assertReceivedNone();

        String stmtThreeText = "@Drop select * from SupportBean where intPrimitive = 3";
        EPStatement statementThree = epService.getEPAdministrator().createEPL(stmtThreeText);
        statementThree.addListener(listeners[2]);

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 3));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(2, "E5");

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 1));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(0, "E6");
        
        statementOne.destroy();
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 1));
        assertEquals("E7", listener.assertOneGetNewAndReset().get("theString"));
        assertReceivedNone();

        String stmtSelectTextTwo = "@Priority(50) select * from SupportBean";
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtSelectTextTwo);
        stmtSelectTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new SupportBean("E8", 1));
        assertEquals("E8", listener.assertOneGetNewAndReset().get("theString"));
        assertEquals("E8", listenerTwo.assertOneGetNewAndReset().get("theString"));
        assertReceivedNone();

        epService.getEPRuntime().sendEvent(new SupportBean("E9", 2));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(1, "E9");
    }

    private void assertReceivedSingle(int index, String stringValue)
    {
        for (int i = 0; i < listeners.length; i++)
        {
            if (i == index)
            {
                continue;
            }
            assertFalse(listeners[i].isInvoked());
        }
        assertEquals(stringValue, listeners[index].assertOneGetNewAndReset().get("theString"));
    }

    private void assertPrio(String theString, int[] prioValues)
    {
        EventBean[] events = listener.getNewDataListFlattened();
        assertEquals(prioValues.length, events.length);
        for (int i = 0; i < prioValues.length; i++)
        {
            assertEquals(prioValues[i], events[i].get("prio"));
            if (theString != null)
            {
                assertEquals(theString, events[i].get("theString"));
            }
        }
        listener.reset();
    }

    private void assertReceivedNone()
    {
        for (int i = 0; i < listeners.length; i++)
        {
            assertFalse(listeners[i].isInvoked());
        }
    }

    private void sendTimer(long time, EPServiceProvider epService)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
