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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecEPLSelectJoin implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportBean_B.class);

        runAssertionJoinUniquePerId(epService);
        runAssertionJoinNonUniquePerId(epService);
    }

    private void runAssertionJoinUniquePerId(EPServiceProvider epService) {
        SelectJoinHolder holder = setupStmt(epService);

        sendEvent(epService, holder.eventsA[0]);
        sendEvent(epService, holder.eventsB[1]);
        assertNull(holder.listener.getLastNewData());

        // Test join new B with id 0
        sendEvent(epService, holder.eventsB[0]);
        assertSame(holder.eventsA[0], holder.listener.getLastNewData()[0].get("streamA"));
        assertSame(holder.eventsB[0], holder.listener.getLastNewData()[0].get("streamB"));
        assertNull(holder.listener.getLastOldData());
        holder.listener.reset();

        // Test join new A with id 1
        sendEvent(epService, holder.eventsA[1]);
        assertSame(holder.eventsA[1], holder.listener.getLastNewData()[0].get("streamA"));
        assertSame(holder.eventsB[1], holder.listener.getLastNewData()[0].get("streamB"));
        assertNull(holder.listener.getLastOldData());
        holder.listener.reset();

        sendEvent(epService, holder.eventsA[2]);
        assertNull(holder.listener.getLastOldData());

        // Test join old A id 0 leaves length window of 3 events
        sendEvent(epService, holder.eventsA[3]);
        assertSame(holder.eventsA[0], holder.listener.getLastOldData()[0].get("streamA"));
        assertSame(holder.eventsB[0], holder.listener.getLastOldData()[0].get("streamB"));
        assertNull(holder.listener.getLastNewData());
        holder.listener.reset();

        // Test join old B id 1 leaves window
        sendEvent(epService, holder.eventsB[4]);
        assertNull(holder.listener.getLastOldData());
        sendEvent(epService, holder.eventsB[5]);
        assertSame(holder.eventsA[1], holder.listener.getLastOldData()[0].get("streamA"));
        assertSame(holder.eventsB[1], holder.listener.getLastOldData()[0].get("streamB"));
        assertNull(holder.listener.getLastNewData());

        holder.stmt.destroy();
    }

    private void runAssertionJoinNonUniquePerId(EPServiceProvider epService) {
        SelectJoinHolder holder = setupStmt(epService);

        sendEvent(epService, holder.eventsA[0]);
        sendEvent(epService, holder.eventsA[1]);
        sendEvent(epService, holder.eventsASetTwo[0]);
        assertTrue(holder.listener.getLastOldData() == null && holder.listener.getLastNewData() == null);

        sendEvent(epService, holder.eventsB[0]); // Event B id 0 joins to A id 0 twice
        EventBean[] data = holder.listener.getLastNewData();
        assertTrue(holder.eventsASetTwo[0] == data[0].get("streamA") || holder.eventsASetTwo[0] == data[1].get("streamA"));    // Order arbitrary
        assertSame(holder.eventsB[0], data[0].get("streamB"));
        assertTrue(holder.eventsA[0] == data[0].get("streamA") || holder.eventsA[0] == data[1].get("streamA"));
        assertSame(holder.eventsB[0], data[1].get("streamB"));
        assertNull(holder.listener.getLastOldData());
        holder.listener.reset();

        sendEvent(epService, holder.eventsB[2]);
        sendEvent(epService, holder.eventsBSetTwo[0]);  // Ignore events generated
        holder.listener.reset();

        sendEvent(epService, holder.eventsA[3]);  // Pushes A id 0 out of window, which joins to B id 0 twice
        data = holder.listener.getLastOldData();
        assertSame(holder.eventsA[0], holder.listener.getLastOldData()[0].get("streamA"));
        assertTrue(holder.eventsB[0] == data[0].get("streamB") || holder.eventsB[0] == data[1].get("streamB"));    // B order arbitrary
        assertSame(holder.eventsA[0], holder.listener.getLastOldData()[1].get("streamA"));
        assertTrue(holder.eventsBSetTwo[0] == data[0].get("streamB") || holder.eventsBSetTwo[0] == data[1].get("streamB"));
        assertNull(holder.listener.getLastNewData());
        holder.listener.reset();

        sendEvent(epService, holder.eventsBSetTwo[2]);  // Pushes B id 0 out of window, which joins to A set two id 0
        assertSame(holder.eventsASetTwo[0], holder.listener.getLastOldData()[0].get("streamA"));
        assertSame(holder.eventsB[0], holder.listener.getLastOldData()[0].get("streamB"));
        assertEquals(1, holder.listener.getLastOldData().length);

        holder.stmt.destroy();
    }

    private SelectJoinHolder setupStmt(EPServiceProvider epService) {
        SelectJoinHolder holder = new SelectJoinHolder();

        String epl = "select irstream * from A#length(3) as streamA, B#length(3) as streamB where streamA.id = streamB.id";
        holder.stmt = epService.getEPAdministrator().createEPL(epl);
        holder.listener = new SupportUpdateListener();
        holder.stmt.addListener(holder.listener);

        assertEquals(SupportBean_A.class, holder.stmt.getEventType().getPropertyType("streamA"));
        assertEquals(SupportBean_B.class, holder.stmt.getEventType().getPropertyType("streamB"));
        assertEquals(2, holder.stmt.getEventType().getPropertyNames().length);

        holder.eventsA = new SupportBean_A[10];
        holder.eventsASetTwo = new SupportBean_A[10];
        holder.eventsB = new SupportBean_B[10];
        holder.eventsBSetTwo = new SupportBean_B[10];
        for (int i = 0; i < holder.eventsA.length; i++) {
            holder.eventsA[i] = new SupportBean_A(Integer.toString(i));
            holder.eventsASetTwo[i] = new SupportBean_A(Integer.toString(i));
            holder.eventsB[i] = new SupportBean_B(Integer.toString(i));
            holder.eventsBSetTwo[i] = new SupportBean_B(Integer.toString(i));
        }
        return holder;
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private static class SelectJoinHolder {
        EPStatement stmt;
        SupportUpdateListener listener;
        SupportBean_A[] eventsA;
        SupportBean_A[] eventsASetTwo;
        SupportBean_B[] eventsB;
        SupportBean_B[] eventsBSetTwo;

    }
}
