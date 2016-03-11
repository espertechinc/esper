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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportStmtAwareUpdateListener;
import com.espertech.esper.view.StatementStopCallback;
import junit.framework.TestCase;

public class TestEPStatement extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testListenerWithReplay() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from SupportBean.win:length(2)");
        assertFalse(stmt.getStatementContext().isStatelessSelect());

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
        stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from SupportBean.win:length(2)");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        stmt.addListenerWithReplay(listener);
        assertEquals("E1", listener.assertOneGetNewAndReset().get("theString"));
        stmt.destroy();
        listener.reset();

        // test 2 events
        stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from SupportBean.win:length(2)");
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
        try
        {
            stmt.addListenerWithReplay(listener);
            fail();
        }
        catch (IllegalStateException ex)
        {
            //
        }

        stmt.removeAllListeners();
        stmt.removeListener(listener);
        stmt.removeListener(new SupportStmtAwareUpdateListener());
        stmt.setSubscriber(new SupportSubscriber());

        stmt.getAnnotations();
        stmt.getState();
        stmt.getSubscriber();

        try
        {
            stmt.addListener(listener);
            fail();
        }
        catch (IllegalStateException ex)
        {
            //
        }
        try
        {
            stmt.addListener(new SupportStmtAwareUpdateListener());
            fail();
        }
        catch (IllegalStateException ex)
        {
            //
        }

        // test named window and having-clause
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(
                "create window SupportBeanWindow.win:keepall() as SupportBean;\n" +
                "insert into SupportBeanWindow select * from SupportBean;\n");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPStatement stmtWHaving = epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBeanWindow having intPrimitive > 4000");
        stmtWHaving.addListenerWithReplay(listener);
    }

    public void testStartedDestroy()
    {
        sendTimer(1000);

        String text = "select * from " + SupportBean.class.getName();
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(text, "s1");
        assertTrue(stmt.getStatementContext().isStatelessSelect());
        assertEquals(1000l, stmt.getTimeLastStateChange());
        assertEquals(false, stmt.isDestroyed());
        assertEquals(false, stmt.isStopped());
        assertEquals(true, stmt.isStarted());

        stmt.addListener(listener);
        sendEvent();
        listener.assertOneGetNewAndReset();

        sendTimer(2000);
        stmt.destroy();
        assertEquals(2000l, stmt.getTimeLastStateChange());
        assertEquals(true, stmt.isDestroyed());
        assertEquals(false, stmt.isStopped());
        assertEquals(false, stmt.isStarted());

        sendEvent();
        assertFalse(listener.isInvoked());

        assertStmtDestroyed(stmt, text);
    }

    public void testStopDestroy()
    {
        sendTimer(5000);
        String text = "select * from " + SupportBean.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(text, "s1");
        assertEquals(false, stmt.isDestroyed());
        assertEquals(false, stmt.isStopped());
        assertEquals(true, stmt.isStarted());
        assertEquals(5000l, stmt.getTimeLastStateChange());
        stmt.addListener(listener);
        sendEvent();
        listener.assertOneGetNewAndReset();

        sendTimer(6000);
        stmt.stop();
        assertEquals(6000l, stmt.getTimeLastStateChange());
        assertEquals(false, stmt.isDestroyed());
        assertEquals(true, stmt.isStopped());
        assertEquals(false, stmt.isStarted());

        sendEvent();
        assertFalse(listener.isInvoked());

        sendTimer(7000);
        stmt.destroy();
        assertEquals(true, stmt.isDestroyed());
        assertEquals(false, stmt.isStopped());
        assertEquals(false, stmt.isStarted());
        assertEquals(7000l, stmt.getTimeLastStateChange());
        sendEvent();
        assertFalse(listener.isInvoked());

        assertStmtDestroyed(stmt, text);

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
    }

    private void assertStmtDestroyed(EPStatement stmt, String text)
    {
        assertEquals(EPStatementState.DESTROYED, stmt.getState());
        assertEquals(text, stmt.getText());
        assertEquals("s1", stmt.getName());
        assertNull(epService.getEPAdministrator().getStatement("s1"));
        EPAssertionUtil.assertEqualsAnyOrder(new String[0], epService.getEPAdministrator().getStatementNames());

        try
        {
            stmt.destroy();
            fail();
        }
        catch (IllegalStateException ex)
        {
            // expected
            assertEquals("Statement already destroyed", ex.getMessage());
        }

        try
        {
            stmt.start();
            fail();
        }
        catch (IllegalStateException ex)
        {
            // expected
            assertEquals("Cannot start statement, statement is in destroyed state", ex.getMessage());
        }

        try
        {
            stmt.stop();
            fail();
        }
        catch (IllegalStateException ex)
        {
            // expected
            assertEquals("Cannot stop statement, statement is in destroyed state", ex.getMessage());
        }
    }
    
    private void sendEvent()
    {
        SupportBean bean = new SupportBean();
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private class StopCallbackImpl implements StatementStopCallback {
        private boolean stopped = false;

        public void statementStopped() {
            stopped = true;
        }

        public boolean isStopped() {
            return stopped;
        }

        public void setStopped(boolean stopped) {
            this.stopped = stopped;
        }
    }
}
