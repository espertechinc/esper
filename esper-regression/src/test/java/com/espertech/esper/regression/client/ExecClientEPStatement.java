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

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportStmtAwareUpdateListener;
import com.espertech.esper.view.StatementStopCallback;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecClientEPStatement implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionListenerWithReplay(epService);
        runAssertionStartedDestroy(epService);
        runAssertionStopDestroy(epService);
    }

    private void runAssertionListenerWithReplay(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from SupportBean#length(2)");
        assertFalse(stmt.getStatementContext().isStatelessSelect());
        SupportUpdateListener listener = new SupportUpdateListener();

        // test empty statement
        stmt.addListenerWithReplay(listener);
        assertTrue(listener.isInvoked());
        assertEquals(1, listener.getNewDataList().size());
        assertNull(listener.getNewDataList().get(0));
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("theString"));
        stmt.destroy();
        listener.reset();

        // test 1 event
        stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from SupportBean#length(2)");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        stmt.addListenerWithReplay(listener);
        assertEquals("E1", listener.assertOneGetNewAndReset().get("theString"));
        stmt.destroy();
        listener.reset();

        // test 2 events
        stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from SupportBean#length(2)");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        stmt.addListenerWithReplay(listener);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"E1"}, {"E2"}});

        // test stopped statement and destroyed statement
        listener.reset();
        stmt.stop();
        stmt.removeAllListeners();

        stmt.addListenerWithReplay(listener);
        assertTrue(listener.isInvoked());
        assertEquals(1, listener.getNewDataList().size());
        assertNull(listener.getNewDataList().get(0));
        listener.reset();

        // test destroyed
        listener.reset();
        stmt.destroy();
        try {
            stmt.addListenerWithReplay(listener);
            fail();
        } catch (IllegalStateException ex) {
            //
        }

        stmt.removeAllListeners();
        stmt.removeListener(listener);
        stmt.removeListener(new SupportStmtAwareUpdateListener());
        stmt.setSubscriber(new SupportSubscriber());

        stmt.getAnnotations();
        stmt.getState();
        stmt.getSubscriber();

        try {
            stmt.addListener(listener);
            fail();
        } catch (IllegalStateException ex) {
            //
        }
        try {
            stmt.addListener(new SupportStmtAwareUpdateListener());
            fail();
        } catch (IllegalStateException ex) {
            //
        }

        // test named window and having-clause
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(
                "create window SupportBeanWindow#keepall as SupportBean;\n" +
                        "insert into SupportBeanWindow select * from SupportBean;\n");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPStatement stmtWHaving = epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBeanWindow having intPrimitive > 4000");
        stmtWHaving.addListenerWithReplay(listener);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStartedDestroy(EPServiceProvider epService) {
        sendTimer(epService, 1000);

        String text = "select * from " + SupportBean.class.getName();
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(text, "s1");
        assertTrue(stmt.getStatementContext().isStatelessSelect());
        assertEquals(1000L, stmt.getTimeLastStateChange());
        assertEquals(false, stmt.isDestroyed());
        assertEquals(false, stmt.isStopped());
        assertEquals(true, stmt.isStarted());

        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        sendEvent(epService);
        listener.assertOneGetNewAndReset();

        sendTimer(epService, 2000);
        stmt.destroy();
        assertEquals(2000L, stmt.getTimeLastStateChange());
        assertEquals(true, stmt.isDestroyed());
        assertEquals(false, stmt.isStopped());
        assertEquals(false, stmt.isStarted());

        sendEvent(epService);
        assertFalse(listener.isInvoked());

        assertStmtDestroyed(epService, stmt, text);
    }

    private void runAssertionStopDestroy(EPServiceProvider epService) {
        sendTimer(epService, 5000);
        String text = "select * from " + SupportBean.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(text, "s1");
        assertEquals(false, stmt.isDestroyed());
        assertEquals(false, stmt.isStopped());
        assertEquals(true, stmt.isStarted());
        assertEquals(5000L, stmt.getTimeLastStateChange());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        sendEvent(epService);
        listener.assertOneGetNewAndReset();

        sendTimer(epService, 6000);
        stmt.stop();
        assertEquals(6000L, stmt.getTimeLastStateChange());
        assertEquals(false, stmt.isDestroyed());
        assertEquals(true, stmt.isStopped());
        assertEquals(false, stmt.isStarted());

        sendEvent(epService);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 7000);
        stmt.destroy();
        assertEquals(true, stmt.isDestroyed());
        assertEquals(false, stmt.isStopped());
        assertEquals(false, stmt.isStarted());
        assertEquals(7000L, stmt.getTimeLastStateChange());
        sendEvent(epService);
        assertFalse(listener.isInvoked());

        assertStmtDestroyed(epService, stmt, text);

        // test fire-stop service
        EPStatementSPI spiOne = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from java.lang.Object");
        StopCallbackImpl callbackOne = new StopCallbackImpl();
        spiOne.getStatementContext().getStatementStopService().addSubscriber(callbackOne);
        spiOne.destroy();
        assertTrue(callbackOne.isStopped());

        EPStatementSPI spiTwo = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from java.lang.Object");
        StopCallbackImpl callbackTwo = new StopCallbackImpl();
        spiTwo.getStatementContext().getStatementStopService().addSubscriber(callbackTwo);
        spiTwo.stop();
        assertTrue(callbackTwo.isStopped());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertStmtDestroyed(EPServiceProvider epService, EPStatement stmt, String text) {
        assertEquals(EPStatementState.DESTROYED, stmt.getState());
        assertEquals(text, stmt.getText());
        assertEquals("s1", stmt.getName());
        assertNull(epService.getEPAdministrator().getStatement("s1"));
        EPAssertionUtil.assertEqualsAnyOrder(new String[0], epService.getEPAdministrator().getStatementNames());

        try {
            stmt.destroy();
            fail();
        } catch (IllegalStateException ex) {
            // expected
            assertEquals("Statement already destroyed", ex.getMessage());
        }

        try {
            stmt.start();
            fail();
        } catch (IllegalStateException ex) {
            // expected
            assertEquals("Cannot start statement, statement is in destroyed state", ex.getMessage());
        }

        try {
            stmt.stop();
            fail();
        } catch (IllegalStateException ex) {
            // expected
            assertEquals("Cannot stop statement, statement is in destroyed state", ex.getMessage());
        }
    }

    private void sendEvent(EPServiceProvider epService) {
        SupportBean bean = new SupportBean();
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private class StopCallbackImpl implements StatementStopCallback {
        private boolean stopped = false;

        public void statementStopped() {
            stopped = true;
        }

        boolean isStopped() {
            return stopped;
        }
    }
}
