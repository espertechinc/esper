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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Iterator;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecClientIsolationUnit implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_A", SupportBean_A.class);
        configuration.getEngineDefaults().getViewResources().setShareViews(false);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMovePattern(epService);
        runAssertionInvalid(epService);
        runAssertionIsolateFilter(epService);
        runAssertionDestroy(epService);
        runAssertionIsolatedSchedule(epService);
        runAssertionInsertInto(epService);
        runAssertionIsolateMultiple(epService);
        runAssertionStartStop(epService);
        runAssertionNamedWindowTakeCreate(epService);
        runAssertionNamedWindowTimeCatchup(epService);
        runAssertionCurrentTimestamp(epService);
        runAssertionUpdate(epService);
        runAssertionSuspend(epService);
        runAssertionCreateStmt(epService);
        runAssertionSubscriberNamedWindowConsumerIterate(epService);
        runAssertionEventSender(epService);
    }

    private void runAssertionMovePattern(EPServiceProvider epService) {
        EPServiceProviderIsolated isolatedService = epService.getEPServiceIsolated("Isolated");
        EPStatement stmt = isolatedService.getEPAdministrator().createEPL("select * from pattern [every (a=SupportBean -> b=SupportBean(theString=a.theString)) where timer:within(1 day)]", "TestStatement", null);
        isolatedService.getEPRuntime().sendEvent(new CurrentTimeEvent(System.currentTimeMillis() + 1000));
        isolatedService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        isolatedService.getEPAdministrator().removeStatement(stmt);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertTrue(listener.getIsInvokedAndReset());
        stmt.destroy();
        isolatedService.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('A') select * from SupportBean");
        EPServiceProviderIsolated unitOne = epService.getEPServiceIsolated("i1");
        EPServiceProviderIsolated unitTwo = epService.getEPServiceIsolated("i2");
        unitOne.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_INTERNAL));
        unitOne.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));

        unitOne.getEPAdministrator().addStatement(stmt);
        try {
            unitTwo.getEPAdministrator().addStatement(stmt);
            fail();
        } catch (EPServiceIsolationException ex) {
            assertEquals("Statement named 'A' already in service isolation under 'i1'", ex.getMessage());
        }

        try {
            unitTwo.getEPAdministrator().removeStatement(stmt);
            fail();
        } catch (EPServiceIsolationException ex) {
            assertEquals("Statement named 'A' not in this service isolation but under service isolation 'A'", ex.getMessage());
        }

        unitOne.getEPAdministrator().removeStatement(stmt);

        try {
            unitOne.getEPAdministrator().removeStatement(stmt);
            fail();
        } catch (EPServiceIsolationException ex) {
            assertEquals("Statement named 'A' is not currently in service isolation", ex.getMessage());
        }

        try {
            unitTwo.getEPAdministrator().removeStatement(new EPStatement[]{null});
            fail();
        } catch (EPServiceIsolationException ex) {
            assertEquals("Illegal argument, a null value was provided in the statement list", ex.getMessage());
        }

        try {
            unitTwo.getEPAdministrator().addStatement(new EPStatement[]{null});
            fail();
        } catch (EPServiceIsolationException ex) {
            assertEquals("Illegal argument, a null value was provided in the statement list", ex.getMessage());
        }

        stmt.destroy();
        unitOne.destroy();
        unitTwo.destroy();
    }

    private void runAssertionIsolateFilter(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from pattern [every a=SupportBean -> b=SupportBean(theString=a.theString)]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.getAndClearIsInvoked());

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"i1"}, epService.getEPServiceIsolatedNames());

        // send fake to wrong place
        unit.getEPRuntime().sendEvent(new SupportBean("E1", -1));

        unit.getEPAdministrator().addStatement(stmt);

        // send to 'wrong' engine
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertFalse(listener.getAndClearIsInvoked());

        // send to 'right' engine
        unit.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,a.intPrimitive,b.intPrimitive".split(","), new Object[]{"E1", 1, 3});

        // send second pair, and a fake to the wrong place
        unit.getEPRuntime().sendEvent(new SupportBean("E2", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -1));

        unit.getEPAdministrator().removeStatement(stmt);

        // send to 'wrong' engine
        unit.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        assertFalse(listener.getAndClearIsInvoked());

        // send to 'right' engine
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 6));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,a.intPrimitive,b.intPrimitive".split(","), new Object[]{"E2", 4, 6});

        epService.getEPAdministrator().destroyAllStatements();
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"i1"}, epService.getEPServiceIsolatedNames());
        epService.getEPServiceIsolated("i1").destroy();
        EPAssertionUtil.assertEqualsAnyOrder(new Object[0], epService.getEPServiceIsolatedNames());
    }

    private void runAssertionDestroy(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') select * from SupportBean", null, null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");

        EPStatement stmtTwo = unit.getEPAdministrator().createEPL("@Name('B') select * from SupportBean", null, null);
        stmtTwo.addListener(listener);
        unit.getEPAdministrator().addStatement(stmtOne);

        unit.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(2, listener.getNewDataListFlattened().length);
        listener.reset();

        unit.destroy();

        unit.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(0, listener.getNewDataListFlattened().length);
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(2, listener.getNewDataListFlattened().length);

        stmtOne.destroy();
        stmtTwo.destroy();
    }

    private void runAssertionIsolatedSchedule(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        sendTimerUnisolated(epService, 100000);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from pattern [every a=SupportBean -> timer:interval(10)]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimerUnisolated(epService, 105000);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        sendTimerIso(0, unit);
        unit.getEPAdministrator().addStatement(stmt);

        sendTimerIso(9999, unit);
        assertFalse(listener.isInvoked());

        sendTimerIso(10000, unit);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"E1"});

        sendTimerIso(11000, unit);
        unit.getEPRuntime().sendEvent(new SupportBean("E2", 1));

        sendTimerUnisolated(epService, 120000);
        assertFalse(listener.isInvoked());

        unit.getEPAdministrator().removeStatement(stmt);

        sendTimerUnisolated(epService, 129999);
        assertFalse(listener.isInvoked());

        sendTimerUnisolated(epService, 130000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"E2"});

        sendTimerIso(30000, unit);
        assertFalse(listener.isInvoked());

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInsertInto(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyStream select * from SupportBean");
        SupportUpdateListener listenerInsert = new SupportUpdateListener();
        stmtInsert.addListener(listenerInsert);

        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select * from MyStream");
        SupportUpdateListener listenerSelect = new SupportUpdateListener();
        stmtSelect.addListener(listenerSelect);

        // unit takes "insert"
        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        unit.getEPAdministrator().addStatement(stmtInsert);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listenerSelect.getAndClearIsInvoked());
        assertFalse(listenerInsert.getAndClearIsInvoked());

        unit.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertFalse(listenerSelect.getAndClearIsInvoked());
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E2"});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listenerSelect.getAndClearIsInvoked());
        assertFalse(listenerInsert.getAndClearIsInvoked());    // is there a remaining event that gets flushed with the last one?

        // unit returns insert
        unit.getEPAdministrator().removeStatement(stmtInsert);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E4"});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E4"});

        unit.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        assertFalse(listenerSelect.getAndClearIsInvoked());
        assertFalse(listenerInsert.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E6"});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E6"});

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIsolateMultiple(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        String[] fields = new String[]{"theString", "sumi"};
        int count = 4;
        SupportUpdateListener[] listeners = new SupportUpdateListener[count];
        for (int i = 0; i < count; i++) {
            String epl = "@Name('S" + i + "') select theString, sum(intPrimitive) as sumi from SupportBean(theString='" + i + "')#time(10)";
            listeners[i] = new SupportUpdateListener();
            epService.getEPAdministrator().createEPL(epl).addListener(listeners[i]);
        }

        EPStatement[] statements = new EPStatement[2];
        statements[0] = epService.getEPAdministrator().getStatement("S0");
        statements[1] = epService.getEPAdministrator().getStatement("S2");

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        unit.getEPAdministrator().addStatement(statements);

        // send to unisolated
        for (int i = 0; i < count; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean(Integer.toString(i), i));
        }
        assertFalse(listeners[0].isInvoked());
        assertFalse(listeners[2].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"1", 1});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"3", 3});

        // send to isolated
        for (int i = 0; i < count; i++) {
            unit.getEPRuntime().sendEvent(new SupportBean(Integer.toString(i), i));
        }
        assertFalse(listeners[1].isInvoked());
        assertFalse(listeners[3].isInvoked());
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"0", 0});
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, new Object[]{"2", 2});

        unit.getEPRuntime().sendEvent(new SupportBean(Integer.toString(2), 2));
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, new Object[]{"2", 4});

        // return
        unit.getEPAdministrator().removeStatement(statements);

        // send to unisolated
        for (int i = 0; i < count; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean(Integer.toString(i), i));
        }
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"0", 0});
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"1", 2});
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, new Object[]{"2", 6});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"3", 6});

        // send to isolated
        for (int i = 0; i < count; i++) {
            unit.getEPRuntime().sendEvent(new SupportBean(Integer.toString(i), i));
            assertFalse(listeners[i].isInvoked());
        }

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStartStop(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        String[] fields = new String[]{"theString"};
        String epl = "select theString from SupportBean#time(60)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        unit.getEPAdministrator().addStatement(stmt);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        unit.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E2"}});

        stmt.stop();

        unit.getEPAdministrator().removeStatement(stmt);

        stmt.start();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        unit.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E3"}});

        unit.getEPAdministrator().addStatement(stmt);

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 0));
        unit.getEPRuntime().sendEvent(new SupportBean("E6", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E3"}, {"E6"}});

        unit.getEPAdministrator().removeStatement(stmt);

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 0));
        unit.getEPRuntime().sendEvent(new SupportBean("E8", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E3"}, {"E6"}, {"E7"}});

        stmt.stop();

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowTakeCreate(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        String[] fields = new String[]{"theString"};
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("@Name('create') create window MyWindow#keepall as SupportBean");
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("@Name('insert') insert into MyWindow select * from SupportBean");
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL("@Name('delete') on SupportBean_A delete from MyWindow where theString = id");
        EPStatement stmtConsume = epService.getEPAdministrator().createEPL("@Name('consume') select irstream * from MyWindow");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtConsume.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1"}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        unit.getEPAdministrator().addStatement(epService.getEPAdministrator().getStatement("create"));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        unit.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1"}});
        assertFalse(listener.isInvoked());

        unit.getEPAdministrator().addStatement(stmtInsert);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        unit.getEPRuntime().sendEvent(new SupportBean("E5", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1"}, {"E5"}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});    // yes receives it

        // Note: Named window is global across isolation units: they are a relation and not a stream.

        // The insert-into to a named window is a stream that can be isolated from the named window.
        // The streams of on-select and on-delete can be isolated, however they select or change the named window even if that is isolated.
        // Consumers to a named window always receive all changes to a named window (regardless of whether the consuming statement is isolated or not), even if the window itself was isolated.
        //
        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E5"}});

        unit.getEPAdministrator().addStatement(stmtDelete);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E5"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E5"}});

        unit.getEPRuntime().sendEvent(new SupportBean_A("E5"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, null);

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowTimeCatchup(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        sendTimerUnisolated(epService, 100000);
        String[] fields = new String[]{"theString"};
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("@Name('create') create window MyWindow#time(10) as SupportBean");
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("@Name('insert') insert into MyWindow select * from SupportBean");

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        sendTimerIso(0, unit);
        unit.getEPAdministrator().addStatement(new EPStatement[]{stmtCreate, stmtInsert});

        sendTimerIso(1000, unit);
        unit.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        sendTimerIso(2000, unit);
        unit.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        sendTimerIso(9000, unit);
        unit.getEPRuntime().sendEvent(new SupportBean("E3", 3));

        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
        unit.getEPAdministrator().removeStatement(new EPStatement[]{stmtCreate});

        sendTimerUnisolated(epService, 101000);    // equivalent to 10000  (E3 is 1 seconds old)

        sendTimerUnisolated(epService, 102000);    // equivalent to 11000  (E3 is 2 seconds old)
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E2"}, {"E3"}});

        sendTimerUnisolated(epService, 103000);    // equivalent to 12000  (E3 is 3 seconds old)
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E3"}});

        sendTimerUnisolated(epService, 109000);    // equivalent to 18000 (E3 is 9 seconds old)
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E3"}});

        unit.getEPAdministrator().addStatement(new EPStatement[]{stmtCreate});

        sendTimerIso(9999, unit);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E3"}});

        sendTimerIso(10000, unit);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, null);

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCurrentTimestamp(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        sendTimerUnisolated(epService, 5000);
        String[] fields = new String[]{"ct"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select current_timestamp() as ct from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5000L});

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        sendTimerIso(100000, unit);
        unit.getEPAdministrator().addStatement(new EPStatement[]{stmt});

        unit.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100000L});

        epService.getEPAdministrator().destroyAllStatements();

        stmt = epService.getEPAdministrator().createEPL("select theString as ct from SupportBean where current_timestamp() >= 10000");
        stmt.addListener(listener);

        unit.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());

        sendTimerUnisolated(epService, 10000);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        unit.getEPAdministrator().addStatement(stmt);

        unit.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select theString as ct from SupportBean where current_timestamp() >= 120000");
        stmt.addListener(listener);
        unit.getEPAdministrator().addStatement(new EPStatement[]{stmt});

        unit.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        assertFalse(listener.isInvoked());

        sendTimerIso(120000, unit);

        unit.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUpdate(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        sendTimerUnisolated(epService, 5000);
        String[] fields = new String[]{"theString"};
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into NewStream select * from SupportBean");
        EPStatement stmtUpd = epService.getEPAdministrator().createEPL("update istream NewStream set theString = 'X'");
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select * from NewStream");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"X"});

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        unit.getEPAdministrator().addStatement(new EPStatement[]{stmtSelect});

        unit.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());

        /**
         * Update statements apply to a stream even if the statement is not isolated.
         */
        unit.getEPAdministrator().addStatement(new EPStatement[]{stmtInsert});
        unit.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"X"});

        unit.getEPAdministrator().addStatement(stmtUpd);
        unit.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"X"});

        stmtUpd.stop();

        unit.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSuspend(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        sendTimerUnisolated(epService, 1000);
        String[] fields = new String[]{"theString"};
        String epl = "select irstream theString from SupportBean#time(10)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimerUnisolated(epService, 2000);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));

        sendTimerUnisolated(epService, 3000);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));

        sendTimerUnisolated(epService, 7000);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));

        sendTimerUnisolated(epService, 8000);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select 'x' as theString from pattern [timer:interval(10)]");
        stmtTwo.addListener(listener);

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        unit.getEPAdministrator().addStatement(new EPStatement[]{stmt, stmtTwo});
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{stmt.getName(), stmtTwo.getName()}, unit.getEPAdministrator().getStatementNames());
        assertEquals("i1", stmt.getServiceIsolated());
        assertEquals("i1", stmt.getServiceIsolated());

        listener.reset();
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        sendTimerUnisolated(epService, 15000);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        unit.getEPAdministrator().removeStatement(new EPStatement[]{stmt, stmtTwo});
        EPAssertionUtil.assertEqualsAnyOrder(new Object[0], unit.getEPAdministrator().getStatementNames());
        assertNull(stmt.getServiceIsolated());
        assertNull(stmt.getServiceIsolated());

        sendTimerUnisolated(epService, 18999);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        sendTimerUnisolated(epService, 19000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E2"}, {"E3"}});

        sendTimerUnisolated(epService, 23999);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E3"}});

        sendTimerUnisolated(epService, 24000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendTimerUnisolated(epService, 25000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"x"});

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCreateStmt(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("i1");
        sendTimerUnisolated(epService, 100000);
        sendTimerIso(1000, unit);

        String[] fields = new String[]{"ct"};
        EPStatement stmt = unit.getEPAdministrator().createEPL("select current_timestamp() as ct from pattern[every timer:interval(10)]", null, null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimerIso(10999, unit);
        assertFalse(listener.isInvoked());

        sendTimerIso(11000, unit);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11000L});

        sendTimerIso(15000, unit);

        unit.getEPAdministrator().removeStatement(stmt);

        sendTimerIso(21000, unit);
        assertFalse(listener.isInvoked());

        sendTimerUnisolated(epService, 106000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{106000L});

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubscriberNamedWindowConsumerIterate(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        EPServiceProviderIsolated isolatedService = epService.getEPServiceIsolated("isolatedStmts");
        isolatedService.getEPRuntime().sendEvent(new CurrentTimeEvent(System.currentTimeMillis()));

        SupportSubscriber subscriber = new SupportSubscriber();
        final EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from SupportBean");
        stmtOne.setSubscriber(subscriber);

        final EPStatement stmtTwo = isolatedService.getEPAdministrator().createEPL("select * from MyWindow", null, null);
        isolatedService.getEPAdministrator().addStatement(stmtOne);

        final Iterator<EventBean> iter = stmtTwo.iterator();
        while (iter.hasNext()) {
            final EventBean theEvent = iter.next();
            isolatedService.getEPRuntime().sendEvent(theEvent.getUnderlying());
        }

        assertTrue(subscriber.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventSender(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientIsolationUnit.class)) {
            return;
        }

        EPServiceProviderIsolated unit = epService.getEPServiceIsolated("other1");
        EventSender sender = unit.getEPRuntime().getEventSender("SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        unit.getEPAdministrator().createEPL("select * from SupportBean", null, null).addListener(listener);
        sender.sendEvent(new SupportBean());
        assertTrue(listener.isInvoked());

        unit.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendTimerUnisolated(EPServiceProvider epService, long millis) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(millis));
    }

    private void sendTimerIso(long millis, EPServiceProviderIsolated unit) {
        unit.getEPRuntime().sendEvent(new CurrentTimeEvent(millis));
    }
}
