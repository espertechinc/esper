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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.*;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.*;

public class ExecPatternObserverTimerInterval implements RegressionExecution, SupportBeanConstants {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionOp(epService);
        runAssertionIntervalSpec(epService);
        runAssertionIntervalSpecVariables(epService);
        runAssertionIntervalSpecExpression(epService);
        runAssertionIntervalSpecExpressionWithProperty(epService);
        runAssertionIntervalSpecExpressionWithPropertyArray(epService);
        runAssertionIntervalSpecPreparedStmt(epService);
        runAssertionMonthScoped(epService);
    }

    private void runAssertionOp(EPServiceProvider epService) throws Exception {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        // The wait is done when 2 seconds passed
        testCase = new EventExpressionCase("timer:interval(1999 msec)");
        testCase.add("B1");
        testCaseList.addTest(testCase);

        String text = "select * from pattern [timer:interval(1.999d)]";
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        PatternExpr pattern = Patterns.timerInterval(1.999d);
        model.setFromClause(FromClause.create(PatternStream.create(pattern)));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(text, model.toEPL());
        testCase = new EventExpressionCase(model);
        testCase.add("B1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(2 sec)");
        testCase.add("B1");
        testCaseList.addTest(testCase);

        // 3 seconds (>2001 microseconds) passed
        testCase = new EventExpressionCase("timer:interval(2.001)");
        testCase.add("C1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(2999 milliseconds)");
        testCase.add("C1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(3 seconds)");
        testCase.add("C1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(3.001 seconds)");
        testCase.add("B2");
        testCaseList.addTest(testCase);

        // Try with an all ... repeated timer every 3 seconds
        testCase = new EventExpressionCase("every timer:interval(3.001 sec)");
        testCase.add("B2");
        testCase.add("F1");
        testCase.add("D3");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every timer:interval(5000 msec)");
        testCase.add("A2");
        testCase.add("B3");
        testCaseList.addTest(testCase);


        testCase = new EventExpressionCase("timer:interval(3.999 second) -> b=" + EVENT_B_CLASS);
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(4 sec) -> b=" + EVENT_B_CLASS);
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(4.001 sec) -> b=" + EVENT_B_CLASS);
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(0) -> b=" + EVENT_B_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        // Try with an followed-by as a second argument
        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -> timer:interval(0.001)");
        testCase.add("C1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -> timer:interval(0)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -> timer:interval(1 sec)");
        testCase.add("C1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -> timer:interval(1.001)");
        testCase.add("B2", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        // Try in a 3-way followed by
        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() -> timer:interval(6.000) -> d=" + EVENT_D_CLASS);
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + "() -> timer:interval(2.001) -> d=" + EVENT_D_CLASS + "())");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + "() -> timer:interval(2.000) -> d=" + EVENT_D_CLASS + "())");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        // Try with an "or"
        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() or timer:interval(1.001)");
        testCase.add("B1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() or timer:interval(2.001)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "(id='B3') or timer:interval(8.500)");
        testCase.add("D2");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(8.500) or timer:interval(7.500)");
        testCase.add("F1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(999999 msec) or g=" + EVENT_G_CLASS);
        testCase.add("G1", "g", events.getEvent("G1"));
        testCaseList.addTest(testCase);

        // Try with an "and"
        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() and timer:interval(4000 msec)");
        testCase.add("B2", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() and timer:interval(4001 msec)");
        testCase.add("A2", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(9999999 msec) and b=" + EVENT_B_CLASS);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(1 msec) and b=" + EVENT_B_CLASS + "(id=\"B2\")");
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        // Try with an "within"
        testCase = new EventExpressionCase("timer:interval(3.000) where timer:within(2.000)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:interval(3.000) where timer:within (3.000)");
        testCaseList.addTest(testCase);

        // Run all tests
        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(epService);
    }

    /**
     * As of release 1.6 this no longer updates listeners when the statement is started.
     * The reason is that the dispatch view only gets attached after a pattern started, therefore
     * ZeroDepthEventStream looses the event.
     * There should be no use case requiring this
     * <p>
     * testCase = new EventExpressionCase("not timer:interval(5000 millisecond)");
     * testCase.add(EventCollection.ON_START_EVENT_ID);
     * testCaseList.addTest(testCase);
     *
     * @param epService
     */

    private void runAssertionIntervalSpec(EPServiceProvider epService) {
        // External clocking
        sendTimer(0, epService);

        // Set up a timer:within
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select * from pattern [timer:interval(1 minute 2 seconds)]");

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        sendTimer(62 * 1000 - 1, epService);
        assertFalse(testListener.isInvoked());

        sendTimer(62 * 1000, epService);
        assertTrue(testListener.isInvoked());

        statement.destroy();
    }

    private void runAssertionIntervalSpecVariables(EPServiceProvider epService) {
        // External clocking
        sendTimer(0, epService);

        // Set up a timer:within
        epService.getEPAdministrator().createEPL("create variable double M=1");
        epService.getEPAdministrator().createEPL("create variable double S=2");
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select * from pattern [timer:interval(M minute S seconds)]");

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        sendTimer(62 * 1000 - 1, epService);
        assertFalse(testListener.isInvoked());

        sendTimer(62 * 1000, epService);
        assertTrue(testListener.isInvoked());

        statement.destroy();
    }

    private void runAssertionIntervalSpecExpression(EPServiceProvider epService) {
        // External clocking
        sendTimer(0, epService);

        // Set up a timer:within
        epService.getEPAdministrator().createEPL("create variable double MOne=1");
        epService.getEPAdministrator().createEPL("create variable double SOne=2");
        EPStatement statement = epService.getEPAdministrator().createEPL("select * from pattern [timer:interval(MOne*60+SOne seconds)]");

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        sendTimer(62 * 1000 - 1, epService);
        assertFalse(testListener.isInvoked());

        sendTimer(62 * 1000, epService);
        assertTrue(testListener.isInvoked());

        statement.destroy();
    }

    private void runAssertionIntervalSpecExpressionWithProperty(EPServiceProvider epService) {
        // External clocking
        sendTimer(0, epService);

        // Set up a timer:within
        EPStatement statement = epService.getEPAdministrator().createEPL("select a.theString as id from pattern [every a=SupportBean -> timer:interval(intPrimitive seconds)]");

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        sendTimer(10000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        sendTimer(11999, epService);
        assertFalse(testListener.isInvoked());
        sendTimer(12000, epService);
        assertEquals("E2", testListener.assertOneGetNewAndReset().get("id"));

        sendTimer(12999, epService);
        assertFalse(testListener.isInvoked());
        sendTimer(13000, epService);
        assertEquals("E1", testListener.assertOneGetNewAndReset().get("id"));

        statement.destroy();
    }

    private void runAssertionIntervalSpecExpressionWithPropertyArray(EPServiceProvider epService) {
        // External clocking
        sendTimer(0, epService);

        // Set up a timer:within
        EPStatement statement = epService.getEPAdministrator().createEPL("select a[0].theString as a0id, a[1].theString as a1id from pattern [ [2] a=SupportBean -> timer:interval(a[0].intPrimitive+a[1].intPrimitive seconds)]");

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        sendTimer(10000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        sendTimer(14999, epService);
        assertFalse(testListener.isInvoked());
        sendTimer(15000, epService);
        EPAssertionUtil.assertProps(testListener.assertOneGetNewAndReset(), "a0id,a1id".split(","), "E1,E2".split(","));

        statement.destroy();
    }

    private void runAssertionIntervalSpecPreparedStmt(EPServiceProvider epService) {
        // External clocking
        sendTimer(0, epService);

        // Set up a timer:within
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(
                "select * from pattern [timer:interval(? minute ? seconds)]");

        prepared.setObject(1, 1);
        prepared.setObject(2, 2);
        EPStatement statement = epService.getEPAdministrator().create(prepared);

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        sendTimer(62 * 1000 - 1, epService);
        assertFalse(testListener.isInvoked());

        sendTimer(62 * 1000, epService);
        assertTrue(testListener.isInvoked());

        statement.destroy();
    }

    private void runAssertionMonthScoped(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        epService.getEPAdministrator().createEPL("select * from pattern [timer:interval(1 month)]").addListener(listener);

        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        assertTrue(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendTimer(long timeInMSec, EPServiceProvider epService) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }
}