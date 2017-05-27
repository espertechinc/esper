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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UnmatchedListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExecClientUnmatchedListener implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        runAssertionUnmatchedSendEvent(epService);
        runAssertionUnmatchedCreateStatement(epService);
        runAssertionUnmatchedInsertInto(epService);
    }

    private void runAssertionUnmatchedSendEvent(EPServiceProvider epService) {
        MyUnmatchedListener listener = new MyUnmatchedListener();
        epService.getEPRuntime().setUnmatchedListener(listener);

        // no statement, should be unmatched
        SupportBean theEvent = sendEvent(epService, "E1");
        assertEquals(1, listener.getReceived().size());
        assertSame(theEvent, listener.getReceived().get(0).getUnderlying());
        listener.reset();

        // no unmatched listener
        epService.getEPRuntime().setUnmatchedListener(null);
        sendEvent(epService, "E1");
        assertEquals(0, listener.getReceived().size());

        // create statement and re-register unmatched listener
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from " + SupportBean.class.getName());
        epService.getEPRuntime().setUnmatchedListener(listener);
        sendEvent(epService, "E1");
        assertEquals(0, listener.getReceived().size());

        // stop statement
        stmt.stop();
        theEvent = sendEvent(epService, "E1");
        assertEquals(1, listener.getReceived().size());
        assertSame(theEvent, listener.getReceived().get(0).getUnderlying());
        listener.reset();

        // start statement
        stmt.start();
        sendEvent(epService, "E1");
        assertEquals(0, listener.getReceived().size());

        // destroy statement
        stmt.destroy();
        theEvent = sendEvent(epService, "E1");
        assertEquals(1, listener.getReceived().size());
        assertSame(theEvent, listener.getReceived().get(0).getUnderlying());
    }

    private void runAssertionUnmatchedCreateStatement(EPServiceProvider epService) {
        UnmatchListenerCreateStmt listener = new UnmatchListenerCreateStmt(epService);
        epService.getEPRuntime().setUnmatchedListener(listener);

        // no statement, should be unmatched
        sendEvent(epService, "E1");
        assertEquals(1, listener.getReceived().size());
        listener.reset();

        sendEvent(epService, "E1");
        assertEquals(0, listener.getReceived().size());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUnmatchedInsertInto(EPServiceProvider epService) {
        MyUnmatchedListener listener = new MyUnmatchedListener();
        epService.getEPRuntime().setUnmatchedListener(listener);

        // create insert into
        EPStatement insertInto = epService.getEPAdministrator().createEPL("insert into MyEvent select theString from SupportBean");

        // no statement, should be unmatched
        sendEvent(epService, "E1");
        assertEquals(1, listener.getReceived().size());
        assertEquals("E1", listener.getReceived().get(0).get("theString"));
        listener.reset();

        // stop insert into, now SupportBean itself is unmatched
        insertInto.stop();
        SupportBean theEvent = sendEvent(epService, "E2");
        assertEquals(1, listener.getReceived().size());
        assertSame(theEvent, listener.getReceived().get(0).getUnderlying());
        listener.reset();

        // start insert-into
        sendEvent(epService, "E3");
        assertEquals(1, listener.getReceived().size());
        assertEquals("E3", listener.getReceived().get(0).get("theString"));
        listener.reset();
    }

    private SupportBean sendEvent(EPServiceProvider epService, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    public class MyUnmatchedListener implements UnmatchedListener {
        private List<EventBean> received;

        MyUnmatchedListener() {
            this.received = new ArrayList<>();
        }

        public void update(EventBean theEvent) {
            received.add(theEvent);
        }

        public List<EventBean> getReceived() {
            return received;
        }

        public void reset() {
            received.clear();
        }
    }

    public static class UnmatchListenerCreateStmt implements UnmatchedListener {

        private List<EventBean> received;
        private final EPServiceProvider engine;

        private UnmatchListenerCreateStmt(EPServiceProvider engine) {
            this.engine = engine;
            this.received = new ArrayList<>();
        }

        public void update(EventBean theEvent) {
            received.add(theEvent);
            engine.getEPAdministrator().createEPL("select * from SupportBean");
        }

        public List<EventBean> getReceived() {
            return received;
        }

        public void reset() {
            received.clear();
        }
    }
}
