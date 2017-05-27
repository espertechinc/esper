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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecViewStartStop implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSameWindowReuse(epService);
        runAssertionStartStop(epService);
        runAssertionAddRemoveListener(epService);
    }

    private void runAssertionSameWindowReuse(EPServiceProvider epService) {
        String epl = "select * from " + SupportBean.class.getName() + "#length(3)";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        // send a couple of events
        sendEvent(epService, 1);
        sendEvent(epService, 2);
        sendEvent(epService, 3);
        sendEvent(epService, 4);

        // create same statement again
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(epl);
        stmtTwo.addListener(testListenerTwo);

        // Send event, no old data should be received
        sendEvent(epService, 5);
        assertNull(testListenerTwo.getLastOldData());

        stmtOne.destroy();
        stmtTwo.destroy();
    }

    private void runAssertionStartStop(EPServiceProvider epService) {
        String epl = "select count(*) as size from " + SupportBean.class.getName();
        EPStatement sizeStmt = epService.getEPAdministrator().createEPL(epl);

        // View created is automatically started
        assertEquals(0L, sizeStmt.iterator().next().get("size"));
        sizeStmt.stop();

        // Send an event, view stopped
        sendEvent(epService);
        assertNull(sizeStmt.iterator());

        // Start view
        sizeStmt.start();
        assertEquals(0L, sizeStmt.iterator().next().get("size"));

        // Send event
        sendEvent(epService);
        assertEquals(1L, sizeStmt.iterator().next().get("size"));

        // Stop view
        sizeStmt.stop();
        assertNull(sizeStmt.iterator());

        // Start again, iterator is zero
        sizeStmt.start();
        assertEquals(0L, sizeStmt.iterator().next().get("size"));

        sizeStmt.destroy();
    }

    private void runAssertionAddRemoveListener(EPServiceProvider epService) {
        String epl = "select count(*) as size from " + SupportBean.class.getName();
        EPStatement sizeStmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();

        // View is started when created

        // Add listener send event
        sizeStmt.addListener(listener);
        assertNull(listener.getLastNewData());
        assertEquals(0L, sizeStmt.iterator().next().get("size"));
        sendEvent(epService);
        assertEquals(1L, listener.getAndResetLastNewData()[0].get("size"));
        assertEquals(1L, sizeStmt.iterator().next().get("size"));

        // Stop view, send event, view
        sizeStmt.stop();
        sendEvent(epService);
        assertNull(sizeStmt.iterator());
        assertNull(listener.getLastNewData());

        // Start again
        sizeStmt.removeListener(listener);
        sizeStmt.addListener(listener);
        sizeStmt.start();

        sendEvent(epService);
        assertEquals(1L, listener.getAndResetLastNewData()[0].get("size"));
        assertEquals(1L, sizeStmt.iterator().next().get("size"));

        // Stop again, leave listeners
        sizeStmt.stop();
        sizeStmt.start();
        sendEvent(epService);
        assertEquals(1L, listener.getAndResetLastNewData()[0].get("size"));

        // Remove listener, send event
        sizeStmt.removeListener(listener);
        sendEvent(epService);
        assertNull(listener.getLastNewData());

        // Add listener back, send event
        sizeStmt.addListener(listener);
        sendEvent(epService);
        assertEquals(3L, listener.getAndResetLastNewData()[0].get("size"));

        sizeStmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService) {
        sendEvent(epService, -1);
    }

    private void sendEvent(EPServiceProvider epService, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
