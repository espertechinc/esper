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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.bean.SupportBean_C;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.*;

public class ExecJoinSingleOp3Stream implements RegressionExecution {
    private final static String EVENT_A = SupportBean_A.class.getName();
    private final static String EVENT_B = SupportBean_B.class.getName();
    private final static String EVENT_C = SupportBean_C.class.getName();

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionJoinUniquePerId(epService);
        runAssertionJoinUniquePerIdOM(epService);
        runAssertionJoinUniquePerIdCompile(epService);
    }

    private void runAssertionJoinUniquePerId(EPServiceProvider epService) {
        String epl = "select * from " +
                EVENT_A + "#length(3) as streamA," +
                EVENT_B + "#length(3) as streamB," +
                EVENT_C + "#length(3) as streamC" +
                " where (streamA.id = streamB.id) " +
                "   and (streamB.id = streamC.id)" +
                "   and (streamA.id = streamC.id)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runJoinUniquePerId(epService, listener);

        stmt.destroy();
    }

    private void runAssertionJoinUniquePerIdOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        FromClause fromClause = FromClause.create(
                FilterStream.create(EVENT_A, "streamA").addView(View.create("length", Expressions.constant(3))),
                FilterStream.create(EVENT_B, "streamB").addView(View.create("length", Expressions.constant(3))),
                FilterStream.create(EVENT_C, "streamC").addView(View.create("length", Expressions.constant(3))));
        model.setFromClause(fromClause);
        model.setWhereClause(Expressions.and(
                Expressions.eqProperty("streamA.id", "streamB.id"),
                Expressions.eqProperty("streamB.id", "streamC.id"),
                Expressions.eqProperty("streamA.id", "streamC.id")));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String epl = "select * from " +
                EVENT_A + "#length(3) as streamA, " +
                EVENT_B + "#length(3) as streamB, " +
                EVENT_C + "#length(3) as streamC " +
                "where streamA.id=streamB.id " +
                "and streamB.id=streamC.id " +
                "and streamA.id=streamC.id";

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);
        assertEquals(epl, model.toEPL());

        runJoinUniquePerId(epService, updateListener);

        stmt.destroy();
    }

    private void runAssertionJoinUniquePerIdCompile(EPServiceProvider epService) throws Exception {
        String epl = "select * from " +
                EVENT_A + "#length(3) as streamA, " +
                EVENT_B + "#length(3) as streamB, " +
                EVENT_C + "#length(3) as streamC " +
                "where streamA.id=streamB.id " +
                "and streamB.id=streamC.id " +
                "and streamA.id=streamC.id";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        EPStatement srmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        srmt.addListener(listener);
        assertEquals(epl, model.toEPL());

        runJoinUniquePerId(epService, listener);

        srmt.destroy();
    }

    private void runJoinUniquePerId(EPServiceProvider epService, SupportUpdateListener listener) {
        SupportBean_A[] eventsA = new SupportBean_A[10];
        SupportBean_B[] eventsB = new SupportBean_B[10];
        SupportBean_C[] eventsC = new SupportBean_C[10];
        for (int i = 0; i < eventsA.length; i++) {
            eventsA[i] = new SupportBean_A(Integer.toString(i));
            eventsB[i] = new SupportBean_B(Integer.toString(i));
            eventsC[i] = new SupportBean_C(Integer.toString(i));
        }

        // Test sending a C event
        sendEvent(epService, eventsA[0]);
        sendEvent(epService, eventsB[0]);
        assertNull(listener.getLastNewData());
        sendEvent(epService, eventsC[0]);
        assertEventsReceived(listener, eventsA[0], eventsB[0], eventsC[0]);

        // Test sending a B event
        sendEvent(epService, new Object[]{eventsA[1], eventsB[2], eventsC[3]});
        sendEvent(epService, eventsC[1]);
        assertNull(listener.getLastNewData());
        sendEvent(epService, eventsB[1]);
        assertEventsReceived(listener, eventsA[1], eventsB[1], eventsC[1]);

        // Test sending a C event
        sendEvent(epService, new Object[]{eventsA[4], eventsA[5], eventsB[4], eventsB[3]});
        assertNull(listener.getLastNewData());
        sendEvent(epService, eventsC[4]);
        assertEventsReceived(listener, eventsA[4], eventsB[4], eventsC[4]);
        assertNull(listener.getLastNewData());
    }

    private void assertEventsReceived(SupportUpdateListener updateListener, SupportBean_A eventA, SupportBean_B eventB, SupportBean_C eventC) {
        assertEquals(1, updateListener.getLastNewData().length);
        assertSame(eventA, updateListener.getLastNewData()[0].get("streamA"));
        assertSame(eventB, updateListener.getLastNewData()[0].get("streamB"));
        assertSame(eventC, updateListener.getLastNewData()[0].get("streamC"));
        updateListener.reset();
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEvent(EPServiceProvider epService, Object[] events) {
        for (int i = 0; i < events.length; i++) {
            epService.getEPRuntime().sendEvent(events[i]);
        }
    }
}
