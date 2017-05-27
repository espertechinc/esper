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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.Drop;
import com.espertech.esper.client.annotation.Priority;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecClientPriorityAndDropInstructions implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.getEngineDefaults().getExecution().setPrioritized(true);     // also sets share-views to false
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSchedulingPriority(epService);
        runAssertionSchedulingDrop(epService);
        runAssertionNamedWindowPriority(epService);
        runAssertionNamedWindowDrop(epService);
        runAssertionPriority(epService);
        runAssertionAddRemoveStmts(epService);
    }

    @Priority(10)
    @Drop
    private void runAssertionSchedulingPriority(EPServiceProvider epService) {
        sendTimer(0, epService);
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Priority(1) select 1 as prio from pattern [every timer:interval(10)]", "s1");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(3) select 3 as prio from pattern [every timer:interval(10)]", "s3");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select 2 as prio from pattern [every timer:interval(10)]", "s2");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(4) select 4 as prio from pattern [every timer:interval(10)]", "s4");
        stmt.addListener(listener);

        sendTimer(10000, epService);
        assertPrio(listener, null, new int[]{4, 3, 2, 1});

        epService.getEPAdministrator().getStatement("s2").destroy();
        stmt = epService.getEPAdministrator().createEPL("select 0 as prio from pattern [every timer:interval(10)]", "s0");
        stmt.addListener(listener);

        sendTimer(20000, epService);
        assertPrio(listener, null, new int[]{4, 3, 1, 0});

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select 2 as prio from pattern [every timer:interval(10)]", "s2");
        stmt.addListener(listener);

        sendTimer(30000, epService);
        assertPrio(listener, null, new int[]{4, 3, 2, 1, 0});

        stmt = epService.getEPAdministrator().createEPL("@Priority(3) select 3 as prio from pattern [every timer:interval(10)]", "s2");
        stmt.addListener(listener);

        sendTimer(40000, epService);
        assertPrio(listener, null, new int[]{4, 3, 3, 2, 1, 0});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSchedulingDrop(EPServiceProvider epService) {
        sendTimer(0, epService);
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Drop select 1 as prio from pattern [every timer:interval(10)]", "s1");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select 3 as prio from pattern [every timer:interval(10)]", "s3");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("select 2 as prio from pattern [every timer:interval(10)]", "s2");
        stmt.addListener(listener);

        sendTimer(10000, epService);
        assertPrio(listener, null, new int[]{3, 1});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowPriority(EPServiceProvider epService) {
        String stmtText;
        EPStatement stmt;

        stmtText = "create window MyWindow#lastevent as select * from SupportBean";
        stmt = epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "insert into MyWindow select * from SupportBean";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "@Priority(1) on MyWindow e select e.theString as theString, 1 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s1");
        SupportUpdateListener listener = new SupportUpdateListener();
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
        assertPrio(listener, "E1", new int[]{4, 3, 2, 1});

        epService.getEPAdministrator().getStatement("s2").destroy();
        stmt = epService.getEPAdministrator().createEPL("on MyWindow e select e.theString as theString, 0 as prio from MyWindow", "s0");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertPrio(listener, "E2", new int[]{4, 3, 1, 0});

        stmtText = "@Priority(2) on MyWindow e select e.theString as theString, 2 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s2");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertPrio(listener, "E3", new int[]{4, 3, 2, 1, 0});

        stmtText = "@Priority(3) on MyWindow e select e.theString as theString, 3 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "sx");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        assertPrio(listener, "E4", new int[]{4, 3, 3, 2, 1, 0});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowDrop(EPServiceProvider epService) {
        String stmtText;
        EPStatement stmt;

        stmtText = "create window MyWindow#lastevent as select * from SupportBean";
        stmt = epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "insert into MyWindow select * from SupportBean";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "@Drop on MyWindow e select e.theString as theString, 2 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s2");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        stmtText = "@Priority(3) on MyWindow e select e.theString as theString, 3 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s3");
        stmt.addListener(listener);

        stmtText = "on MyWindow e select e.theString as theString, 0 as prio from MyWindow";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "s2");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertPrio(listener, "E1", new int[]{3, 2});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPriority(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Priority(1) select *, 1 as prio from SupportBean", "s1");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(3) select *, 3 as prio from SupportBean", "s3");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select *, 2 as prio from SupportBean", "s2");
        stmt.addListener(listener);

        stmt = epService.getEPAdministrator().createEPL("@Priority(4) select *, 4 as prio from SupportBean", "s4");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertPrio(listener, "E1", new int[]{4, 3, 2, 1});

        epService.getEPAdministrator().getStatement("s2").destroy();
        stmt = epService.getEPAdministrator().createEPL("select *, 0 as prio from SupportBean", "s0");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertPrio(listener, "E2", new int[]{4, 3, 1, 0});

        stmt = epService.getEPAdministrator().createEPL("@Priority(2) select *, 2 as prio from SupportBean", "s2");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertPrio(listener, "E3", new int[]{4, 3, 2, 1, 0});

        stmt = epService.getEPAdministrator().createEPL("@Priority(3) select *, 3 as prio from SupportBean", "sx");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        assertPrio(listener, "E4", new int[]{4, 3, 3, 2, 1, 0});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAddRemoveStmts(EPServiceProvider epService) {
        String stmtSelectText = "insert into ABCStream select * from SupportBean";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtSelectText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        String stmtOneText = "@Drop select * from SupportBean where intPrimitive = 1";
        EPStatement statementOne = epService.getEPAdministrator().createEPL(stmtOneText);
        SupportUpdateListener[] listeners = SupportUpdateListener.makeListeners(10);
        statementOne.addListener(listeners[0]);

        String stmtTwoText = "@Drop select * from SupportBean where intPrimitive = 2";
        EPStatement statementTwo = epService.getEPAdministrator().createEPL(stmtTwoText);
        statementTwo.addListener(listeners[1]);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(listeners, 0, "E1");

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(listeners, 1, "E2");

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(listeners, 0, "E3");

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        assertEquals("E4", listener.assertOneGetNewAndReset().get("theString"));
        assertReceivedNone(listeners);

        String stmtThreeText = "@Drop select * from SupportBean where intPrimitive = 3";
        EPStatement statementThree = epService.getEPAdministrator().createEPL(stmtThreeText);
        statementThree.addListener(listeners[2]);

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 3));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(listeners, 2, "E5");

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 1));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(listeners, 0, "E6");

        statementOne.destroy();
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 1));
        assertEquals("E7", listener.assertOneGetNewAndReset().get("theString"));
        assertReceivedNone(listeners);

        String stmtSelectTextTwo = "@Priority(50) select * from SupportBean";
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtSelectTextTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtSelectTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new SupportBean("E8", 1));
        assertEquals("E8", listener.assertOneGetNewAndReset().get("theString"));
        assertEquals("E8", listenerTwo.assertOneGetNewAndReset().get("theString"));
        assertReceivedNone(listeners);

        epService.getEPRuntime().sendEvent(new SupportBean("E9", 2));
        assertFalse(listener.isInvoked());
        assertReceivedSingle(listeners, 1, "E9");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertReceivedSingle(SupportUpdateListener[] listeners, int index, String stringValue) {
        for (int i = 0; i < listeners.length; i++) {
            if (i == index) {
                continue;
            }
            assertFalse(listeners[i].isInvoked());
        }
        assertEquals(stringValue, listeners[index].assertOneGetNewAndReset().get("theString"));
    }

    private void assertPrio(SupportUpdateListener listener, String theString, int[] prioValues) {
        EventBean[] events = listener.getNewDataListFlattened();
        assertEquals(prioValues.length, events.length);
        for (int i = 0; i < prioValues.length; i++) {
            assertEquals(prioValues[i], events[i].get("prio"));
            if (theString != null) {
                assertEquals(theString, events[i].get("theString"));
            }
        }
        listener.reset();
    }

    private void assertReceivedNone(SupportUpdateListener[] listeners) {
        for (int i = 0; i < listeners.length; i++) {
            assertFalse(listeners[i].isInvoked());
        }
    }

    private void sendTimer(long time, EPServiceProvider epService) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
