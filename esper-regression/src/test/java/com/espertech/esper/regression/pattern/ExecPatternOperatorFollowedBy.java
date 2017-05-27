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

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecPatternOperatorFollowedBy implements RegressionExecution, SupportBeanConstants {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOp(epService);
        runAssertionFollowedByWithNot(epService);
        runAssertionFollowedByTimer(epService);
        runAssertionMemoryRFIDEvent(epService);
        runAssertionRFIDZoneExit(epService);
        runAssertionRFIDZoneEnter(epService);
        runAssertionFollowedNotEvery(epService);
        runAssertionFollowedEveryMultiple(epService);
        runAssertionFilterGreaterThen(epService);
        runAssertionFollowedOrPermFalse(epService);
    }

    private void runAssertionOp(EPServiceProvider epService) throws Exception {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -> (d=" + EVENT_D_CLASS + " or not d=" + EVENT_D_CLASS + ")");
        testCase.add("B1", "b", events.getEvent("B1"), "d", null);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -[1000]> (d=" + EVENT_D_CLASS + " or not d=" + EVENT_D_CLASS + ")");
        testCase.add("B1", "b", events.getEvent("B1"), "d", null);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -> every d=" + EVENT_D_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -> d=" + EVENT_D_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -> not d=" + EVENT_D_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " -[1000]> not d=" + EVENT_D_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " -> every d=" + EVENT_D_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " -> d=" + EVENT_D_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " -[10]> d=" + EVENT_D_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + " -> every d=" + EVENT_D_CLASS + ")");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (a_1=" + EVENT_A_CLASS + "() -> b=" + EVENT_B_CLASS + " -> a_2=" + EVENT_A_CLASS + ")");
        testCase.add("A2", "a_1", events.getEvent("A1"), "b", events.getEvent("B1"), "a_2", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("c=" + EVENT_C_CLASS + "() -> d=" + EVENT_D_CLASS + " -> a=" + EVENT_A_CLASS);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (a_1=" + EVENT_A_CLASS + "() -> b=" + EVENT_B_CLASS + "() -> a_2=" + EVENT_A_CLASS + "())");
        testCase.add("A2", "a_1", events.getEvent("A1"), "b", events.getEvent("B1"), "a_2", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (a_1=" + EVENT_A_CLASS + "() -[10]> b=" + EVENT_B_CLASS + "() -[10]> a_2=" + EVENT_A_CLASS + "())");
        testCase.add("A2", "a_1", events.getEvent("A1"), "b", events.getEvent("B1"), "a_2", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every ( every a=" + EVENT_A_CLASS + " -> every b=" + EVENT_B_CLASS + ")");
        testCase.add("B1", "a", events.getEvent("A1"), "b", events.getEvent("B1"));
        testCase.add("B2", "a", events.getEvent("A1"), "b", events.getEvent("B2"));
        testCase.add("B3", "a", events.getEvent("A1"), "b", events.getEvent("B3"));
        testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
        testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
        testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (a=" + EVENT_A_CLASS + "() -> every b=" + EVENT_B_CLASS + "())");
        testCase.add("B1", "a", events.getEvent("A1"), "b", events.getEvent("B1"));
        testCase.add("B2", "a", events.getEvent("A1"), "b", events.getEvent("B2"));
        testCase.add("B3", "a", events.getEvent("A1"), "b", events.getEvent("B3"));
        testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
        testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(epService);
    }

    private void runAssertionFollowedByWithNot(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportBean_B.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("C", SupportBean_C.class.getName());

        String stmt =
                "select * from pattern [" +
                        " every a=A -> (timer:interval(10 seconds) and not (B(id=a.id) or C(id=a.id)))" +
                        "] ";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        assertEquals(StatementType.SELECT, ((EPStatementSPI) statement).getStatementMetadata().getStatementType());
        statement.addListener(listener);

        SupportBean_A eventA;
        EventBean received;
        sendTimer(0, epService);

        // test case where no Completed or Cancel event arrives
        eventA = sendA("A1", epService);
        sendTimer(9999, epService);
        assertFalse(listener.isInvoked());
        sendTimer(10000, epService);
        received = listener.assertOneGetNewAndReset();
        assertEquals(eventA, received.get("a"));

        // test case where Completed event arrives within the time set
        sendTimer(20000, epService);
        sendA("A2", epService);
        sendTimer(29999, epService);
        sendB("A2", epService);
        sendTimer(30000, epService);
        assertFalse(listener.isInvoked());

        // test case where Cancelled event arrives within the time set
        sendTimer(30000, epService);
        sendA("A3", epService);
        sendTimer(30000, epService);
        sendC("A3", epService);
        sendTimer(40000, epService);
        assertFalse(listener.isInvoked());

        // test case where no matching Completed or Cancel event arrives
        eventA = sendA("A4", epService);
        sendB("B4", epService);
        sendC("A5", epService);
        sendTimer(50000, epService);
        received = listener.assertOneGetNewAndReset();
        assertEquals(eventA, received.get("a"));

        statement.destroy();
    }

    private void runAssertionFollowedByTimer(EPServiceProvider epService) throws ParseException {
        epService.getEPAdministrator().getConfiguration().addEventType("CallEvent", SupportCallEvent.class.getName());

        String expression = "select * from pattern " +
                "[every A=CallEvent -> every B=CallEvent(dest=A.dest, startTime in [A.startTime:A.endTime]) where timer:within (7200000)]" +
                "where B.source != A.source";
        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        SupportCallEvent eventOne = sendEvent(epService.getEPRuntime(), 2000002601, "18", "123456789014795", dateToLong("2005-09-26 13:02:53.200"), dateToLong("2005-09-26 13:03:34.400"));
        SupportCallEvent eventTwo = sendEvent(epService.getEPRuntime(), 2000002607, "20", "123456789014795", dateToLong("2005-09-26 13:03:17.300"), dateToLong("2005-09-26 13:03:58.600"));

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertSame(eventOne, theEvent.get("A"));
        assertSame(eventTwo, theEvent.get("B"));

        SupportCallEvent eventThree = sendEvent(epService.getEPRuntime(), 2000002610, "22", "123456789014795", dateToLong("2005-09-26 13:03:31.300"), dateToLong("2005-09-26 13:04:12.100"));
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(2, listener.getLastNewData().length);
        theEvent = listener.getLastNewData()[0];
        assertSame(eventOne, theEvent.get("A"));
        assertSame(eventThree, theEvent.get("B"));
        theEvent = listener.getLastNewData()[1];
        assertSame(eventTwo, theEvent.get("A"));
        assertSame(eventThree, theEvent.get("B"));

        statement.destroy();
    }

    private void runAssertionMemoryRFIDEvent(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("LR", SupportRFIDEvent.class.getName());

        String expression =
                "select 'Tag May Be Broken' as alert, " +
                        "tagMayBeBroken.mac, " +
                        "tagMayBeBroken.zoneID " +
                        "from pattern [" +
                        "every tagMayBeBroken=LR -> (timer:interval(10 sec) and not LR(mac=tagMayBeBroken.mac))" +
                        "]";

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        for (int i = 0; i < 10; i++) {
            /*
            if (i % 1000 == 0)
            {
                log.info(".testMemoryRFIDEvent now at " + i);
            }
            */
            SupportRFIDEvent theEvent = new SupportRFIDEvent("a", "111");
            epService.getEPRuntime().sendEvent(theEvent);

            theEvent = new SupportRFIDEvent("a", "111");
            epService.getEPRuntime().sendEvent(theEvent);
        }

        statement.destroy();
    }

    private void runAssertionRFIDZoneExit(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("LR", SupportRFIDEvent.class.getName());

        /**
         * Every LR event with a zone of '1' activates a new sub-expression after
         * the followed-by operator. The sub-expression instance can end two different ways:
         * It ends when a LR for the same mac and a different exit-zone comes in, or
         * it ends when a LR for the same max and the same zone come in. The latter also starts the
         * sub-expression again.
         */
        String expression =
                "select * " +
                        "from pattern [" +
                        "every a=LR(zoneID='1') -> (b=LR(mac=a.mac,zoneID!='1') and not LR(mac=a.mac,zoneID='1'))" +
                        "]";

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        SupportRFIDEvent theEvent = new SupportRFIDEvent("a", "1");
        epService.getEPRuntime().sendEvent(theEvent);
        assertFalse(listener.isInvoked());

        theEvent = new SupportRFIDEvent("a", "2");
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals(theEvent, listener.assertOneGetNewAndReset().get("b"));

        theEvent = new SupportRFIDEvent("b", "1");
        epService.getEPRuntime().sendEvent(theEvent);
        assertFalse(listener.isInvoked());

        theEvent = new SupportRFIDEvent("b", "1");
        epService.getEPRuntime().sendEvent(theEvent);
        assertFalse(listener.isInvoked());

        theEvent = new SupportRFIDEvent("b", "2");
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals(theEvent, listener.assertOneGetNewAndReset().get("b"));

        statement.destroy();
    }

    private void runAssertionRFIDZoneEnter(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("LR", SupportRFIDEvent.class.getName());

        /**
         * Every LR event with a zone other then '1' activates a new sub-expression after
         * the followed-by operator. The sub-expression instance can end two different ways:
         * It ends when a LR for the same mac and the enter-zone comes in, or
         * it ends when a LR for the same max and the same zone come in. The latter also starts the
         * sub-expression again.
         */
        String expression =
                "select * " +
                        "from pattern [" +
                        "every a=LR(zoneID!='1') -> (b=LR(mac=a.mac,zoneID='1') and not LR(mac=a.mac,zoneID=a.zoneID))" +
                        "]";

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        SupportRFIDEvent theEvent = new SupportRFIDEvent("a", "2");
        epService.getEPRuntime().sendEvent(theEvent);
        assertFalse(listener.isInvoked());

        theEvent = new SupportRFIDEvent("a", "1");
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals(theEvent, listener.assertOneGetNewAndReset().get("b"));

        theEvent = new SupportRFIDEvent("b", "2");
        epService.getEPRuntime().sendEvent(theEvent);
        assertFalse(listener.isInvoked());

        theEvent = new SupportRFIDEvent("b", "2");
        epService.getEPRuntime().sendEvent(theEvent);
        assertFalse(listener.isInvoked());

        theEvent = new SupportRFIDEvent("b", "1");
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals(theEvent, listener.assertOneGetNewAndReset().get("b"));

        statement.destroy();
    }

    private void runAssertionFollowedNotEvery(EPServiceProvider epService) {
        String expression = "select * from pattern [every A=" + SupportBean.class.getName() +
                " -> (timer:interval(1 seconds) and not " + SupportBean_A.class.getName() + ")]";

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Object eventOne = new SupportBean();
        epService.getEPRuntime().sendEvent(eventOne);

        Object eventTwo = new SupportBean();
        epService.getEPRuntime().sendEvent(eventTwo);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(2, listener.getNewDataList().get(0).length);

        statement.destroy();
    }

    private void runAssertionFollowedEveryMultiple(EPServiceProvider epService) {
        String expression = "select * from pattern [every a=" + SupportBean_A.class.getName() +
                " -> b=" + SupportBean_B.class.getName() +
                " -> c=" + SupportBean_C.class.getName() +
                " -> d=" + SupportBean_D.class.getName() +
                "]";

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Object[] events = new Object[10];
        events[0] = new SupportBean_A("A1");
        epService.getEPRuntime().sendEvent(events[0]);

        events[1] = new SupportBean_A("A2");
        epService.getEPRuntime().sendEvent(events[1]);

        events[2] = new SupportBean_B("B1");
        epService.getEPRuntime().sendEvent(events[2]);

        events[3] = new SupportBean_C("C1");
        epService.getEPRuntime().sendEvent(events[3]);
        assertFalse(listener.isInvoked());

        events[4] = new SupportBean_D("D1");
        epService.getEPRuntime().sendEvent(events[4]);
        assertEquals(2, listener.getLastNewData().length);
        String[] fields = new String[]{"a", "b", "c", "d"};
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{events[0], events[2], events[3], events[4]});
        EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{events[1], events[2], events[3], events[4]});

        statement.destroy();
    }

    private void runAssertionFilterGreaterThen(EPServiceProvider epService) {
        // ESPER-411
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        EPStatement statement = epService.getEPAdministrator().createPattern("every a=SupportBean -> b=SupportBean(b.intPrimitive <= a.intPrimitive)");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        assertFalse(listener.isInvoked());

        statement.destroy();
        statement = epService.getEPAdministrator().createPattern("every a=SupportBean -> b=SupportBean(a.intPrimitive >= b.intPrimitive)");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionFollowedOrPermFalse(EPServiceProvider epService) {

        // ESPER-451
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String pattern = "every s=SupportBean(theString='E') -> " +
                "(timer:interval(10) and not SupportBean(theString='C1'))" +
                "or" +
                "(SupportBean(theString='C2') and not timer:interval(10))";
        EPStatement statement = epService.getEPAdministrator().createPattern(pattern);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("E", 0));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10999));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(11000));
        assertTrue(listener.isInvoked());

        statement.destroy();
    }

    private long dateToLong(String dateText) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = format.parse(dateText);
        log.debug(".dateToLong out=" + date.toString());
        return date.getTime();
    }

    private SupportCallEvent sendEvent(EPRuntime runtime, long callId, String source, String destination, long startTime, long endTime) {
        SupportCallEvent theEvent = new SupportCallEvent(callId, source, destination, startTime, endTime);
        runtime.sendEvent(theEvent);
        return theEvent;
    }

    private SupportBean_A sendA(String id, EPServiceProvider epService) {
        SupportBean_A a = new SupportBean_A(id);
        epService.getEPRuntime().sendEvent(a);
        return a;
    }

    private void sendB(String id, EPServiceProvider epService) {
        SupportBean_B b = new SupportBean_B(id);
        epService.getEPRuntime().sendEvent(b);
    }

    private void sendC(String id, EPServiceProvider epService) {
        SupportBean_C c = new SupportBean_C(id);
        epService.getEPRuntime().sendEvent(c);
    }

    private void sendTimer(long time, EPServiceProvider epService) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private final static Logger log = LoggerFactory.getLogger(ExecPatternOperatorFollowedBy.class);
}
