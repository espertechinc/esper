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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.*;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.*;

public class ExecPatternGuardTimerWithin implements RegressionExecution, SupportBeanConstants {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOp(epService);
        runAssertionInterval10Min(epService);
        runAssertionInterval10MinVariable(epService);
        runAssertionIntervalPrepared(epService);
        runAssertionWithinFromExpression(epService);
        runAssertionPatternNotFollowedBy(epService);
        runAssertionWithinMayMaxMonthScoped(epService);
    }

    private void runAssertionOp(EPServiceProvider epService) throws Exception {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase = null;

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "(id=\"B1\") where timer:within(2 sec)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "(id=\"B1\") where timer:within(2001 msec)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "(id=\"B1\") where timer:within(1999 msec)");
        testCaseList.addTest(testCase);

        String text = "select * from pattern [b=" + EVENT_B_CLASS + "(id=\"B3\") where timer:within(10.001d)]";
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        Expression filter = Expressions.eq("id", "B3");
        PatternExpr pattern = Patterns.timerWithin(10.001, Patterns.filter(Filter.create(EVENT_B_CLASS, filter), "b"));
        model.setFromClause(FromClause.create(PatternStream.create(pattern)));
        assertEquals(text, model.toEPL());
        testCase = new EventExpressionCase(model);
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "(id=\"B3\") where timer:within(10001 msec)");
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "(id=\"B3\") where timer:within(10 sec)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "(id=\"B3\") where timer:within(9.999)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=" + EVENT_B_CLASS + ") where timer:within(2.001)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=" + EVENT_B_CLASS + ") where timer:within(4.001)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " where timer:within(2.001)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + " where timer:within(2001 msec))");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every ((every b=" + EVENT_B_CLASS + ") where timer:within(2.001))");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every ((every b=" + EVENT_B_CLASS + ") where timer:within(6.001))");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=" + EVENT_B_CLASS + ") where timer:within(11.001)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=" + EVENT_B_CLASS + ") where timer:within(4001 milliseconds)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + ") where timer:within(6.001)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -> d=" + EVENT_D_CLASS + " where timer:within(4001 milliseconds)");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() -> d=" + EVENT_D_CLASS + "() where timer:within(4 sec)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + "() where timer:within (4.001) and d=" + EVENT_D_CLASS + "() where timer:within(6.001))");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() where timer:within (2001 msec) and d=" + EVENT_D_CLASS + "() where timer:within(6001 msec)");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() where timer:within (2001 msec) and d=" + EVENT_D_CLASS + "() where timer:within(6000 msec)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() where timer:within (2000 msec) and d=" + EVENT_D_CLASS + "() where timer:within(6001 msec)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " -> d=" + EVENT_D_CLASS + " where timer:within(4000 msec)");
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + "() -> every d=" + EVENT_D_CLASS + " where timer:within(4000 msec)");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() -> d=" + EVENT_D_CLASS + "() where timer:within(3999 msec)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + "() -> (every d=" + EVENT_D_CLASS + ") where timer:within(2001 msec)");
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + "() -> d=" + EVENT_D_CLASS + "()) where timer:within(6001 msec)");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() where timer:within (2000 msec) or d=" + EVENT_D_CLASS + "() where timer:within(6000 msec)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(b=" + EVENT_B_CLASS + "() where timer:within (2000 msec) or d=" + EVENT_D_CLASS + "() where timer:within(6000 msec)) where timer:within (1999 msec)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + "() where timer:within (2001 msec) and d=" + EVENT_D_CLASS + "() where timer:within(6001 msec))");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() where timer:within (2001 msec) or d=" + EVENT_D_CLASS + "() where timer:within(6001 msec)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "() where timer:within (2000 msec) or d=" + EVENT_D_CLASS + "() where timer:within(6001 msec)");
        testCase.add("D1", "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + "() where timer:within (2001 msec) and every d=" + EVENT_D_CLASS + "() where timer:within(6001 msec)");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=" + EVENT_B_CLASS + ") where timer:within (2000 msec) and every d=" + EVENT_D_CLASS + "() where timer:within(6001 msec)");
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(epService);
    }

    private void runAssertionInterval10Min(EPServiceProvider epService) {
        // External clocking
        sendTimer(0, epService);
        assertEquals(0, epService.getEPRuntime().getCurrentTime());

        // Set up a timer:within
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select * from pattern [(every " + SupportBean.class.getName() +
                        ") where timer:within(1 days 2 hours 3 minutes 4 seconds 5 milliseconds)]");

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        tryAssertion(epService, testListener);

        statement.destroy();
    }

    private void runAssertionInterval10MinVariable(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addVariable("D", double.class, 1);
        epService.getEPAdministrator().getConfiguration().addVariable("H", double.class, 2);
        epService.getEPAdministrator().getConfiguration().addVariable("M", double.class, 3);
        epService.getEPAdministrator().getConfiguration().addVariable("S", double.class, 4);
        epService.getEPAdministrator().getConfiguration().addVariable("MS", double.class, 5);

        // External clocking
        sendTimer(0, epService);

        // Set up a timer:within
        String stmtText = "select * from pattern [(every " + SupportBean.class.getName() +
                ") where timer:within(D days H hours M minutes S seconds MS milliseconds)]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        tryAssertion(epService, testListener);

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());

        statement.destroy();
    }

    private void runAssertionIntervalPrepared(EPServiceProvider epService) {
        // External clocking
        sendTimer(0, epService);

        // Set up a timer:within
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(
                "select * from pattern [(every " + SupportBean.class.getName() +
                        ") where timer:within(? days ? hours ? minutes ? seconds ? milliseconds)]");
        prepared.setObject(1, 1);
        prepared.setObject(2, 2);
        prepared.setObject(3, 3);
        prepared.setObject(4, 4);
        prepared.setObject(5, 5);
        EPStatement statement = epService.getEPAdministrator().create(prepared);

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        tryAssertion(epService, testListener);

        statement.destroy();
    }

    private void runAssertionWithinFromExpression(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // External clocking
        sendTimer(0, epService);

        // Set up a timer:within
        EPStatement statement = epService.getEPAdministrator().createEPL("select b.theString as id from pattern[a=SupportBean -> (every b=SupportBean) where timer:within(a.intPrimitive seconds)]");

        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        // seed
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));

        sendTimer(2000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -1));
        assertEquals("E2", testListener.assertOneGetNewAndReset().get("id"));

        sendTimer(2999, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", -1));
        assertEquals("E3", testListener.assertOneGetNewAndReset().get("id"));

        sendTimer(3000, epService);
        assertFalse(testListener.isInvoked());

        statement.destroy();
    }

    private void runAssertionPatternNotFollowedBy(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SB", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MD", SupportMarketDataBean.class);
        sendTimer(0, epService);

        String stmtText = "select * from pattern [ every(SB -> (MD where timer:within(5 sec))) ]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        sendTimer(6000, epService);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("E5", "M1", 1d));
        assertTrue(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionWithinMayMaxMonthScoped(EPServiceProvider epService) {
        tryAssertionWithinMayMaxMonthScoped(epService, false);
        tryAssertionWithinMayMaxMonthScoped(epService, true);
    }

    private void tryAssertionWithinMayMaxMonthScoped(EPServiceProvider epService, boolean hasMax) {
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern [(every SupportBean) where " +
                (hasMax ? "timer:withinmax(1 month, 10)" : "timer:within(1 month)") +
                "]").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertTrue(listener.getAndClearIsInvoked());

        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertTrue(listener.getAndClearIsInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertFalse(listener.getAndClearIsInvoked());
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener testListener) {
        sendEvent(epService);
        testListener.assertOneGetNewAndReset();

        long time = 24 * 60 * 60 * 1000 + 2 * 60 * 60 * 1000 + 3 * 60 * 1000 + 4 * 1000 + 5;
        sendTimer(time - 1, epService);
        assertEquals(time - 1, epService.getEPRuntime().getCurrentTime());
        sendEvent(epService);
        testListener.assertOneGetNewAndReset();

        sendTimer(time, epService);
        sendEvent(epService);
        assertFalse(testListener.isInvoked());
    }

    private void sendTimer(long timeInMSec, EPServiceProvider epService) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendEvent(EPServiceProvider epService) {
        SupportBean theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
