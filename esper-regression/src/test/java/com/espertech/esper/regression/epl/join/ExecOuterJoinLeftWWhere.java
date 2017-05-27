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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecOuterJoinLeftWWhere implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionWhereNotNullIs(epService);
        runAssertionWhereNotNullNE(epService);
        runAssertionWhereNullIs(epService);
        runAssertionWhereNullEq(epService);
        runAssertionWhereJoinOrNull(epService);
        runAssertionWhereJoin(epService);
        runAssertionEventType(epService);
    }

    private void runAssertionWhereNotNullIs(EPServiceProvider epService) {
        EPStatement stmt = setupStatement(epService, "where s1.p11 is not null");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryWhereNotNull(epService, listener);
        stmt.destroy();
    }

    private void runAssertionWhereNotNullNE(EPServiceProvider epService) {
        EPStatement stmt = setupStatement(epService, "where s1.p11 is not null");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryWhereNotNull(epService, listener);
        stmt.destroy();
    }

    private void runAssertionWhereNullIs(EPServiceProvider epService) {
        EPStatement stmt = setupStatement(epService, "where s1.p11 is null");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryWhereNull(epService, listener);
        stmt.destroy();
    }

    private void runAssertionWhereNullEq(EPServiceProvider epService) {
        EPStatement stmt = setupStatement(epService, "where s1.p11 is null");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryWhereNull(epService, listener);
        stmt.destroy();
    }

    private void runAssertionWhereJoinOrNull(EPServiceProvider epService) {
        EPStatement stmt = setupStatement(epService, "where s0.p01 = s1.p11 or s1.p11 is null");
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        SupportBean_S0 eventS0 = new SupportBean_S0(0, "0", "[a]");
        sendEvent(eventS0, epService);
        compareEvent(updateListener.assertOneGetNewAndReset(), eventS0, null);

        // Send events to test the join for multiple rows incl. null value
        SupportBean_S1 s1_1 = new SupportBean_S1(1000, "5", "X");
        SupportBean_S1 s1_2 = new SupportBean_S1(1001, "5", "Y");
        SupportBean_S1 s1_3 = new SupportBean_S1(1002, "5", "X");
        SupportBean_S1 s1_4 = new SupportBean_S1(1003, "5", null);
        SupportBean_S0 s0 = new SupportBean_S0(1, "5", "X");
        sendEvent(epService, new Object[]{s1_1, s1_2, s1_3, s1_4, s0});

        assertEquals(3, updateListener.getLastNewData().length);
        Object[] received = new Object[3];
        for (int i = 0; i < 3; i++) {
            assertSame(s0, updateListener.getLastNewData()[i].get("s0"));
            received[i] = updateListener.getLastNewData()[i].get("s1");
        }
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{s1_1, s1_3, s1_4}, received);

        stmt.destroy();
    }

    private void runAssertionWhereJoin(EPServiceProvider epService) {
        EPStatement stmt = setupStatement(epService, "where s0.p01 = s1.p11");
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        SupportBean_S0[] eventsS0 = new SupportBean_S0[15];
        SupportBean_S1[] eventsS1 = new SupportBean_S1[15];
        int count = 100;
        for (int i = 0; i < eventsS0.length; i++) {
            eventsS0[i] = new SupportBean_S0(count++, Integer.toString(i));
        }
        count = 200;
        for (int i = 0; i < eventsS1.length; i++) {
            eventsS1[i] = new SupportBean_S1(count++, Integer.toString(i));
        }

        // Send S0[0] p01=a
        eventsS0[0].setP01("[a]");
        sendEvent(eventsS0[0], epService);
        assertFalse(updateListener.isInvoked());

        // Send S1[1] p11=b
        eventsS1[1].setP11("[b]");
        sendEvent(eventsS1[1], epService);
        assertFalse(updateListener.isInvoked());

        // Send S0[1] p01=c, no match expected
        eventsS0[1].setP01("[c]");
        sendEvent(eventsS0[1], epService);
        assertFalse(updateListener.isInvoked());

        // Send S1[2] p11=d
        eventsS1[2].setP11("[d]");
        sendEvent(eventsS1[2], epService);
        // Send S0[2] p01=d
        eventsS0[2].setP01("[d]");
        sendEvent(eventsS0[2], epService);
        compareEvent(updateListener.assertOneGetNewAndReset(), eventsS0[2], eventsS1[2]);

        // Send S1[3] and S0[3] with differing props, no match expected 
        eventsS1[3].setP11("[e]");
        sendEvent(eventsS1[3], epService);
        eventsS0[3].setP01("[e1]");
        sendEvent(eventsS0[3], epService);
        assertFalse(updateListener.isInvoked());

        stmt.destroy();
    }

    private EPStatement setupStatement(EPServiceProvider epService, String whereClause) {
        String joinStatement = "select * from " +
                SupportBean_S0.class.getName() + "#length(5) as s0 " +
                "left outer join " +
                SupportBean_S1.class.getName() + "#length(5) as s1" +
                " on s0.p00 = s1.p10 " +
                whereClause;

        return epService.getEPAdministrator().createEPL(joinStatement);
    }

    private void runAssertionEventType(EPServiceProvider epService) {
        EPStatement outerJoinView = setupStatement(epService, "");
        EventType type = outerJoinView.getEventType();
        assertEquals(SupportBean_S0.class, type.getPropertyType("s0"));
        assertEquals(SupportBean_S1.class, type.getPropertyType("s1"));
    }

    private void tryWhereNotNull(EPServiceProvider epService, SupportUpdateListener updateListener) {
        SupportBean_S1 s1_1 = new SupportBean_S1(1000, "5", "X");
        SupportBean_S1 s1_2 = new SupportBean_S1(1001, "5", null);
        SupportBean_S1 s1_3 = new SupportBean_S1(1002, "6", null);
        sendEvent(epService, new Object[]{s1_1, s1_2, s1_3});
        assertFalse(updateListener.isInvoked());

        SupportBean_S0 s0 = new SupportBean_S0(1, "5", "X");
        sendEvent(s0, epService);
        compareEvent(updateListener.assertOneGetNewAndReset(), s0, s1_1);
    }

    private void tryWhereNull(EPServiceProvider epService, SupportUpdateListener updateListener) {
        SupportBean_S1 s1_1 = new SupportBean_S1(1000, "5", "X");
        SupportBean_S1 s1_2 = new SupportBean_S1(1001, "5", null);
        SupportBean_S1 s1_3 = new SupportBean_S1(1002, "6", null);
        sendEvent(epService, new Object[]{s1_1, s1_2, s1_3});
        assertFalse(updateListener.isInvoked());

        SupportBean_S0 s0 = new SupportBean_S0(1, "5", "X");
        sendEvent(s0, epService);
        compareEvent(updateListener.assertOneGetNewAndReset(), s0, s1_2);
    }

    private void compareEvent(EventBean receivedEvent, SupportBean_S0 expectedS0, SupportBean_S1 expectedS1) {
        assertSame(expectedS0, receivedEvent.get("s0"));
        assertSame(expectedS1, receivedEvent.get("s1"));
    }

    private void sendEvent(EPServiceProvider epService, Object[] events) {
        for (int i = 0; i < events.length; i++) {
            sendEvent(events[i], epService);
        }
    }

    private void sendEvent(Object theEvent, EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
