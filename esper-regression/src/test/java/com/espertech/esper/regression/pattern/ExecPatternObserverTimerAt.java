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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.*;
import com.espertech.esper.util.SerializableObjectCopier;

import java.util.*;

import static org.junit.Assert.*;

public class ExecPatternObserverTimerAt implements RegressionExecution, SupportBeanConstants {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class.getName());
        configuration.addVariable("VMIN", int.class, 0);
        configuration.addVariable("VHOUR", int.class, 8);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOp(epService);
        runAssertionAtWeekdays(epService);
        runAssertionAtWeekdaysPrepared(epService);
        runAssertionAtWeekdaysVariable(epService);
        runAssertionExpression(epService);
        runAssertionPropertyAndSODAAndTimezone(epService);
        runAssertionEvery15thMonth(epService);
    }

    private void runAssertionEvery15thMonth(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("select * from pattern[every timer:at(*,*,*,*/15,*)]").destroy();
    }

    private void runAssertionOp(EPServiceProvider epService) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2005, 3, 9, 8, 00, 00);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();

        /**
         // Start a 2004-12-9 8:00:00am and send events every 10 minutes
         "A1"    8:10
         "B1"    8:20
         "C1"    8:30
         "B2"    8:40
         "A2"    8:50
         "D1"    9:00
         "E1"    9:10
         "F1"    9:20
         "D2"    9:30
         "B3"    9:40
         "G1"    9:50
         "D3"   10:00
         */

        EventCollection testData = EventCollectionFactory.getEventSetOne(startTime, 1000 * 60 * 10);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase = null;

        testCase = new EventExpressionCase("timer:at(10, 8, *, *, *)");
        testCase.add("A1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(10, 8, *, *, *, 1)");
        testCase.add("B1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(5, 8, *, *, *)");
        testCase.add("A1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(10, 8, *, *, *, *)");
        testCase.add("A1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(25, 9, *, *, *)");
        testCase.add("D2");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(11, 8, *, *, *)");
        testCase.add("B1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(19, 8, *, *, *, 59)");
        testCase.add("B1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every timer:at(* / 5, *, *, *, *, *)");
        addAll(testCase);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every timer:at(*, *, *, *, *, * / 10)");
        addAll(testCase);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(20, 8, *, *, *, 20)");
        testCase.add("C1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every timer:at(*, *, *, *, *)");
        addAll(testCase);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every timer:at(*, *, *, *, *, *)");
        addAll(testCase);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every timer:at(* / 9, *, *, *, *, *)");
        addAll(testCase);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every timer:at(* / 10, *, *, *, *, *)");
        addAll(testCase);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every timer:at(* / 30, *, *, *, *)");
        testCase.add("C1");
        testCase.add("D1");
        testCase.add("D2");
        testCase.add("D3");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(10, 9, *, *, *, 10) or timer:at(30, 9, *, *, *, *)");
        testCase.add("F1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "(id='B3') -> timer:at(20, 9, *, *, *, *)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + "(id='B3') -> timer:at(45, 9, *, *, *, *)");
        testCase.add("G1", "b", testData.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(59, 8, *, *, *, 59) -> d=" + EVENT_D_CLASS);
        testCase.add("D1", "d", testData.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(*, 9, *, *, *, 59) -> d=" + EVENT_D_CLASS);
        testCase.add("D2", "d", testData.getEvent("D2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(22, 8, *, *, *) -> b=" + EVENT_B_CLASS + "(id='B3') -> timer:at(55, *, *, *, *)");
        testCase.add("D3", "b", testData.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(40, *, *, *, *, 1) and b=" + EVENT_B_CLASS);
        testCase.add("A2", "b", testData.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(40, 9, *, *, *, 1) or d=" + EVENT_D_CLASS + "(id=\"D3\")");
        testCase.add("G1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(22, 8, *, *, *) -> b=" + EVENT_B_CLASS + "() -> timer:at(55, 8, *, *, *)");
        testCase.add("D1", "b", testData.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(22, 8, *, *, *, 1) where timer:within(1 second)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(22, 8, *, *, *, 1) where timer:within(31 minutes)");
        testCase.add("C1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(*, 9, *, *, *) and timer:at(55, *, *, *, *)");
        testCase.add("D1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("timer:at(40, 8, *, *, *, 1) and b=" + EVENT_B_CLASS);
        testCase.add("A2", "b", testData.getEvent("B1"));
        testCaseList.addTest(testCase);

        String text = "select * from pattern [timer:at(10,8,*,*,*,*)]";
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        PatternExpr pattern = Patterns.timerAt(10, 8, null, null, null, null);
        model.setFromClause(FromClause.create(PatternStream.create(pattern)));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(text, model.toEPL());
        testCase = new EventExpressionCase(model);
        testCase.add("A1");
        testCaseList.addTest(testCase);

        /**
         * As of release 1.6 this no longer updates listeners when the statement is started.
         * The reason is that the dispatch view only gets attached after a pattern started, therefore
         * ZeroDepthEventStream looses the event.
         * There should be no use case requiring this
         *
         testCase = new EventExpressionCase("not timer:at(22, 8, *, *, *, 1)");
         testCase.add(EventCollection.ON_START_EVENT_ID);
         testCaseList.addTest(testCase);
         */

        // Run all tests
        PatternTestHarness util = new PatternTestHarness(testData, testCaseList, this.getClass());
        util.runTest(epService);
    }

    private void runAssertionAtWeekdays(EPServiceProvider epService) {
        String expression = "select * from pattern [every timer:at(0,8,*,*,[1,2,3,4,5])]";

        Calendar cal = GregorianCalendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008
        sendTimer(cal.getTimeInMillis(), epService);

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        if (!InstrumentationHelper.ENABLED) {
            tryAssertion(epService, listener);
        }

        statement.destroy();
    }

    private void runAssertionAtWeekdaysPrepared(EPServiceProvider epService) {
        String expression = "select * from pattern [every timer:at(?,?,*,*,[1,2,3,4,5])]";

        Calendar cal = GregorianCalendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008
        sendTimer(cal.getTimeInMillis(), epService);

        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(expression);
        prepared.setObject(1, 0);
        prepared.setObject(2, 8);
        EPStatement statement = epService.getEPAdministrator().create(prepared);

        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.endTest();
        } // excluding assertion, too many steps

        tryAssertion(epService, listener);

        statement.destroy();
    }

    private void runAssertionAtWeekdaysVariable(EPServiceProvider epService) {
        String expression = "select * from pattern [every timer:at(VMIN,VHOUR,*,*,[1,2,3,4,5])]";

        Calendar cal = GregorianCalendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008
        sendTimer(cal.getTimeInMillis(), epService);

        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(expression);
        EPStatement statement = epService.getEPAdministrator().create(prepared);

        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        if (!InstrumentationHelper.ENABLED) {
            tryAssertion(epService, listener);
        }

        statement.destroy();
    }

    private void runAssertionExpression(EPServiceProvider epService) {
        String expression = "select * from pattern [every timer:at(7+1-8,4+4,*,*,[1,2,3,4,5])]";

        Calendar cal = GregorianCalendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008
        sendTimer(cal.getTimeInMillis(), epService);

        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(expression);
        EPStatement statement = epService.getEPAdministrator().create(prepared);

        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.endTest();
        } // excluding assertion, too many steps

        tryAssertion(epService, listener);

        statement.destroy();
    }

    private void runAssertionPropertyAndSODAAndTimezone(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        sendTimeEvent("2008-08-3T06:00:00.000", epService);
        String expression = "select * from pattern [a=SupportBean -> every timer:at(2*a.intPrimitive,*,*,*,*)]";
        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));

        sendTimeEvent("2008-08-3T06:39:59.000", epService);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimeEvent("2008-08-3T06:40:00.000", epService);
        assertTrue(listener.getAndClearIsInvoked());
        statement.destroy();

        // test SODA
        String epl = "select * from pattern [every timer:at(*/VFREQ,VMIN:VMAX,1 last,*,[8,2:VMAX,*/VREQ])]";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());

        // test timezone
        if (TimeZone.getDefault().getRawOffset() == -5 * 60 * 60 * 1000) {    // asserting only in EST timezone, see schedule util tests
            sendTimeEvent("2008-01-4T06:50:00.000", epService);
            epService.getEPAdministrator().createEPL("select * from pattern [timer:at(0, 5, 4, 1, *, 0, 'PST')]").addListener(listener);

            sendTimeEvent("2008-01-4T07:59:59.999", epService);
            assertFalse(listener.getAndClearIsInvoked());

            sendTimeEvent("2008-01-4T08:00:00.000", epService);
            assertTrue(listener.getAndClearIsInvoked());
        }
        epService.getEPAdministrator().createEPL("select * from pattern [timer:at(0, 5, 4, 8, *, 0, 'xxx')]").addListener(listener);
        epService.getEPAdministrator().createEPL("select * from pattern [timer:at(0, 5, 4, 8, *, 0, *)]").addListener(listener);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008

        List<String> invocations = new ArrayList<String>();
        for (int i = 0; i < 24 * 60 * 7; i++) { // run for 1 week
            cal.add(Calendar.MINUTE, 1);
            sendTimer(cal.getTimeInMillis(), epService);

            if (listener.getAndClearIsInvoked()) {
                // System.out.println("invoked at calendar " + cal.getTime().toString());
                invocations.add(cal.getTime().toString());
            }
        }
        String[] expectedResult = new String[5];
        cal.set(2008, 7, 4, 8, 0, 0); //"Mon Aug 04 08:00:00 EDT 2008"
        expectedResult[0] = cal.getTime().toString();
        cal.set(2008, 7, 5, 8, 0, 0); //"Tue Aug 05 08:00:00 EDT 2008"
        expectedResult[1] = cal.getTime().toString();
        cal.set(2008, 7, 6, 8, 0, 0); //"Wed Aug 06 08:00:00 EDT 2008"
        expectedResult[2] = cal.getTime().toString();
        cal.set(2008, 7, 7, 8, 0, 0); //"Thu Aug 07 08:00:00 EDT 2008"
        expectedResult[3] = cal.getTime().toString();
        cal.set(2008, 7, 8, 8, 0, 0); //"Fri Aug 08 08:00:00 EDT 2008"
        expectedResult[4] = cal.getTime().toString();
        EPAssertionUtil.assertEqualsExactOrder(expectedResult, invocations.toArray());
    }

    private void sendTimeEvent(String time, EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendTimer(long timeInMSec, EPServiceProvider epService) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void addAll(EventExpressionCase desc) {
        desc.add("A1");
        desc.add("B1");
        desc.add("C1");
        desc.add("B2");
        desc.add("A2");
        desc.add("D1");
        desc.add("E1");
        desc.add("F1");
        desc.add("D2");
        desc.add("B3");
        desc.add("G1");
        desc.add("D3");
    }
}