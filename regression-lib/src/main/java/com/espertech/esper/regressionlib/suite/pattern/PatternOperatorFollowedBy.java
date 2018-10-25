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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.patternassert.*;
import com.espertech.esper.runtime.client.EPEventService;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.*;

public class PatternOperatorFollowedBy {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternOpWHarness());
        execs.add(new PatternFollowedByWithNot());
        execs.add(new PatternFollowedByTimer());
        execs.add(new PatternMemoryRFIDEvent());
        execs.add(new PatternRFIDZoneExit());
        execs.add(new PatternRFIDZoneEnter());
        execs.add(new PatternFollowedNotEvery());
        execs.add(new PatternFollowedEveryMultiple());
        execs.add(new PatternFilterGreaterThen());
        execs.add(new PatternFollowedOrPermFalse());
        return execs;
    }

    private static class PatternOpWHarness implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("b=SupportBean_B -> (d=SupportBean_D or not d=SupportBean_D)");
            testCase.add("B1", "b", events.getEvent("B1"), "d", null);
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B -[1000]> (d=SupportBean_D or not d=SupportBean_D)");
            testCase.add("B1", "b", events.getEvent("B1"), "d", null);
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B -> every d=SupportBean_D");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B -> d=SupportBean_D");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B -> not d=SupportBean_D");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B -[1000]> not d=SupportBean_D");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B -> every d=SupportBean_D");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
            testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B -> d=SupportBean_D");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B -[10]> d=SupportBean_D");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B -> every d=SupportBean_D)");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (a_1=SupportBean_A() -> b=SupportBean_B -> a_2=SupportBean_A)");
            testCase.add("A2", "a_1", events.getEvent("A1"), "b", events.getEvent("B1"), "a_2", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("c=SupportBean_C() -> d=SupportBean_D -> a=SupportBean_A");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (a_1=SupportBean_A() -> b=SupportBean_B() -> a_2=SupportBean_A())");
            testCase.add("A2", "a_1", events.getEvent("A1"), "b", events.getEvent("B1"), "a_2", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (a_1=SupportBean_A() -[10]> b=SupportBean_B() -[10]> a_2=SupportBean_A())");
            testCase.add("A2", "a_1", events.getEvent("A1"), "b", events.getEvent("B1"), "a_2", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every ( every a=SupportBean_A -> every b=SupportBean_B)");
            testCase.add("B1", "a", events.getEvent("A1"), "b", events.getEvent("B1"));
            testCase.add("B2", "a", events.getEvent("A1"), "b", events.getEvent("B2"));
            testCase.add("B3", "a", events.getEvent("A1"), "b", events.getEvent("B3"));
            testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
            testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
            testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (a=SupportBean_A() -> every b=SupportBean_B())");
            testCase.add("B1", "a", events.getEvent("A1"), "b", events.getEvent("B1"));
            testCase.add("B2", "a", events.getEvent("A1"), "b", events.getEvent("B2"));
            testCase.add("B3", "a", events.getEvent("A1"), "b", events.getEvent("B3"));
            testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
            testCase.add("B3", "a", events.getEvent("A2"), "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternFollowedByWithNot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "@name('s0') select * from pattern [" +
                " every a=SupportBean_A -> (timer:interval(10 seconds) and not (SupportBean_B(id=a.id) or SupportBean_C(id=a.id)))" +
                "] ";

            sendTimer(0, env);
            env.compileDeploy(stmt);
            env.addListener("s0");

            SupportBean_A eventA;
            EventBean received;

            // test case where no Completed or Cancel event arrives
            eventA = sendA("A1", env);
            sendTimer(9999, env);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(10000, env);
            received = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(eventA, received.get("a"));

            // test case where Completed event arrives within the time set
            sendTimer(20000, env);
            sendA("A2", env);
            sendTimer(29999, env);
            sendB("A2", env);
            sendTimer(30000, env);
            assertFalse(env.listener("s0").isInvoked());

            // test case where Cancelled event arrives within the time set
            sendTimer(30000, env);
            sendA("A3", env);
            sendTimer(30000, env);
            sendC("A3", env);
            sendTimer(40000, env);
            assertFalse(env.listener("s0").isInvoked());

            // test case where no matching Completed or Cancel event arrives
            eventA = sendA("A4", env);
            sendB("B4", env);
            sendC("A5", env);
            sendTimer(50000, env);
            received = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(eventA, received.get("a"));

            env.undeployAll();
        }
    }

    private static class PatternFollowedByTimer implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select * from pattern " +
                "[every A=SupportCallEvent -> every B=SupportCallEvent(dest=A.dest, startTime in [A.startTime:A.endTime]) where timer:within (7200000)]" +
                "where B.source != A.source";
            env.compileDeploy(expression);

            env.addListener("s0");

            SupportCallEvent eventOne = sendEvent(env.eventService(), 2000002601, "18", "123456789014795", dateToLong("2005-09-26 13:02:53.200"), dateToLong("2005-09-26 13:03:34.400"));
            SupportCallEvent eventTwo = sendEvent(env.eventService(), 2000002607, "20", "123456789014795", dateToLong("2005-09-26 13:03:17.300"), dateToLong("2005-09-26 13:03:58.600"));

            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            TestCase.assertSame(eventOne, theEvent.get("A"));
            TestCase.assertSame(eventTwo, theEvent.get("B"));

            SupportCallEvent eventThree = sendEvent(env.eventService(), 2000002610, "22", "123456789014795", dateToLong("2005-09-26 13:03:31.300"), dateToLong("2005-09-26 13:04:12.100"));
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(2, env.listener("s0").getLastNewData().length);
            theEvent = env.listener("s0").getLastNewData()[0];
            TestCase.assertSame(eventOne, theEvent.get("A"));
            TestCase.assertSame(eventThree, theEvent.get("B"));
            theEvent = env.listener("s0").getLastNewData()[1];
            TestCase.assertSame(eventTwo, theEvent.get("A"));
            TestCase.assertSame(eventThree, theEvent.get("B"));

            env.undeployAll();
        }
    }

    private static class PatternMemoryRFIDEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select 'Tag May Be Broken' as alert, " +
                "tagMayBeBroken.mac, " +
                "tagMayBeBroken.zoneID " +
                "from pattern [" +
                "every tagMayBeBroken=SupportRFIDEvent -> (timer:interval(10 sec) and not SupportRFIDEvent(mac=tagMayBeBroken.mac))" +
                "]";

            env.compileDeploy(expression);

            env.addListener("s0");

            for (int i = 0; i < 10; i++) {
            /*
            if (i % 1000 == 0)
            {
                log.info(".testMemoryRFIDEvent now at " + i);
            }
            */
                SupportRFIDEvent theEvent = new SupportRFIDEvent("a", "111");
                env.sendEventBean(theEvent);

                theEvent = new SupportRFIDEvent("a", "111");
                env.sendEventBean(theEvent);
            }

            env.undeployAll();
        }
    }

    private static class PatternRFIDZoneExit implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            /**
             * Every LR event with a zone of '1' activates a new sub-expression after
             * the followed-by operator. The sub-expression instance can end two different ways:
             * It ends when a LR for the same mac and a different exit-zone comes in, or
             * it ends when a LR for the same max and the same zone come in. The latter also starts the
             * sub-expression again.
             */
            String expression = "@name('s0') select * " +
                "from pattern [" +
                "every a=SupportRFIDEvent(zoneID='1') -> (b=SupportRFIDEvent(mac=a.mac,zoneID!='1') and not SupportRFIDEvent(mac=a.mac,zoneID='1'))" +
                "]";
            env.compileDeploy(expression).addListener("s0");

            SupportRFIDEvent theEvent = new SupportRFIDEvent("a", "1");
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").isInvoked());

            theEvent = new SupportRFIDEvent("a", "2");
            env.sendEventBean(theEvent);
            assertEquals(theEvent, env.listener("s0").assertOneGetNewAndReset().get("b"));

            theEvent = new SupportRFIDEvent("b", "1");
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").isInvoked());

            theEvent = new SupportRFIDEvent("b", "1");
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").isInvoked());

            theEvent = new SupportRFIDEvent("b", "2");
            env.sendEventBean(theEvent);
            assertEquals(theEvent, env.listener("s0").assertOneGetNewAndReset().get("b"));

            env.undeployAll();
        }
    }

    private static class PatternRFIDZoneEnter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Every LR event with a zone other then '1' activates a new sub-expression after
             * the followed-by operator. The sub-expression instance can end two different ways:
             * It ends when a LR for the same mac and the enter-zone comes in, or
             * it ends when a LR for the same max and the same zone come in. The latter also starts the
             * sub-expression again.
             */
            String expression = "@name('s0') select * " +
                "from pattern [" +
                "every a=SupportRFIDEvent(zoneID!='1') -> (b=SupportRFIDEvent(mac=a.mac,zoneID='1') and not SupportRFIDEvent(mac=a.mac,zoneID=a.zoneID))" +
                "]";

            env.compileDeploy(expression);

            env.addListener("s0");

            SupportRFIDEvent theEvent = new SupportRFIDEvent("a", "2");
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").isInvoked());

            theEvent = new SupportRFIDEvent("a", "1");
            env.sendEventBean(theEvent);
            assertEquals(theEvent, env.listener("s0").assertOneGetNewAndReset().get("b"));

            theEvent = new SupportRFIDEvent("b", "2");
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").isInvoked());

            theEvent = new SupportRFIDEvent("b", "2");
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").isInvoked());

            theEvent = new SupportRFIDEvent("b", "1");
            env.sendEventBean(theEvent);
            assertEquals(theEvent, env.listener("s0").assertOneGetNewAndReset().get("b"));

            env.undeployAll();
        }
    }

    private static class PatternFollowedNotEvery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select * from pattern [every A=SupportBean -> (timer:interval(1 seconds) and not SupportBean_A)]";

            env.advanceTime(0);

            env.compileDeploy(expression);

            env.addListener("s0");

            Object eventOne = new SupportBean();
            env.sendEventBean(eventOne);

            Object eventTwo = new SupportBean();
            env.sendEventBean(eventTwo);

            env.advanceTime(1000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(2, env.listener("s0").getNewDataList().get(0).length);

            env.undeployAll();
        }
    }

    private static class PatternFollowedEveryMultiple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select * from pattern [every a=SupportBean_A -> b=SupportBean_B -> c=SupportBean_C -> d=SupportBean_D]";

            env.compileDeploy(expression);

            env.addListener("s0");

            Object[] events = new Object[10];
            events[0] = new SupportBean_A("A1");
            env.sendEventBean(events[0]);

            events[1] = new SupportBean_A("A2");
            env.sendEventBean(events[1]);

            events[2] = new SupportBean_B("B1");
            env.sendEventBean(events[2]);

            events[3] = new SupportBean_C("C1");
            env.sendEventBean(events[3]);
            assertFalse(env.listener("s0").isInvoked());

            events[4] = new SupportBean_D("D1");
            env.sendEventBean(events[4]);
            assertEquals(2, env.listener("s0").getLastNewData().length);
            String[] fields = new String[]{"a", "b", "c", "d"};
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{events[0], events[2], events[3], events[4]});
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[1], fields, new Object[]{events[1], events[2], events[3], events[4]});

            env.undeployAll();
        }
    }

    private static class PatternFilterGreaterThen implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // ESPER-411
            env.compileDeploy("@name('s0') select * from pattern[every a=SupportBean -> b=SupportBean(b.intPrimitive <= a.intPrimitive)]");
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 11));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();

            env.compileDeploy("@name('s0') select * from pattern [every a=SupportBean -> b=SupportBean(a.intPrimitive >= b.intPrimitive)]");
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 11));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternFollowedOrPermFalse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String pattern = "@name('s0') select * from pattern [every s=SupportBean(theString='E') -> " +
                "(timer:interval(10) and not SupportBean(theString='C1'))" +
                "or" +
                "(SupportBean(theString='C2') and not timer:interval(10))]";
            env.compileDeploy(pattern).addListener("s0");

            env.advanceTime(1000);
            env.sendEventBean(new SupportBean("E", 0));

            env.advanceTime(10999);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(11000);
            TestCase.assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static long dateToLong(String dateText) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date date = format.parse(dateText);
            log.debug(".dateToLong out=" + date.toString());
            return date.getTime();
        } catch (java.text.ParseException ex) {
            fail();
        }
        return -1;
    }

    private static SupportCallEvent sendEvent(EPEventService runtime, long callId, String source, String destination, long startTime, long endTime) {
        SupportCallEvent theEvent = new SupportCallEvent(callId, source, destination, startTime, endTime);
        runtime.sendEventBean(theEvent, SupportCallEvent.class.getSimpleName());
        return theEvent;
    }

    private static SupportBean_A sendA(String id, RegressionEnvironment env) {
        SupportBean_A a = new SupportBean_A(id);
        env.sendEventBean(a);
        return a;
    }

    private static void sendB(String id, RegressionEnvironment env) {
        SupportBean_B b = new SupportBean_B(id);
        env.sendEventBean(b);
    }

    private static void sendC(String id, RegressionEnvironment env) {
        SupportBean_C c = new SupportBean_C(id);
        env.sendEventBean(c);
    }

    private static void sendTimer(long time, RegressionEnvironment env) {
        env.advanceTime(time);
    }

    private final static Logger log = LoggerFactory.getLogger(PatternOperatorFollowedBy.class);
}
