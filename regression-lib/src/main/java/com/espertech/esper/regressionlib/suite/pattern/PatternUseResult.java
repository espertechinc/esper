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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportTradeEvent;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import com.espertech.esper.regressionlib.support.patternassert.*;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import static org.junit.Assert.*;

public class PatternUseResult {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternNumeric());
        execs.add(new PatternObjectId());
        execs.add(new PatternFollowedByFilter());
        execs.add(new PatternPatternTypeCacheForRepeat());
        execs.add(new PatternBooleanExprRemoveConsiderTag());
        execs.add(new PatternBooleanExprRemoveConsiderArrayTag());
        return execs;
    }

    private static class PatternBooleanExprRemoveConsiderArrayTag implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select sb[1].intPrimitive as c0 from pattern[every [2] sb=SupportBean -> SupportBean_A(id like sb[1].theString)]";
            env.compileDeploy(epl).addListener("s0");

            for (int i = 0; i < 6; i++) {
                env.sendEventBean(new SupportBean("X" + i, i));
                env.sendEventBean(new SupportBean("Y" + i, i));
            }

            env.milestone(0);

            sendBeanAAssert(env, "Y2", 2, 5);
            sendBeanAMiss(env, "Y2");

            env.milestone(1);

            sendBeanAAssert(env, "Y1", 1, 4);
            sendBeanAMiss(env, "Y1,Y2");

            env.milestone(2);

            sendBeanAAssert(env, "Y4", 4, 3);
            sendBeanAMiss(env, "Y1,Y2,Y4");

            env.milestone(3);

            sendBeanAAssert(env, "Y0", 0, 2);
            sendBeanAMiss(env, "Y0,Y1,Y2,Y4");

            env.milestone(4);

            sendBeanAAssert(env, "Y5", 5, 1);
            sendBeanAMiss(env, "Y0,Y1,Y2,Y4,Y5");

            env.milestone(5);

            sendBeanAAssert(env, "Y3", 3, 0);
            sendBeanAMiss(env, "Y0,Y1,Y2,Y3,Y4,Y5");

            env.undeployAll();
        }
    }

    private static class PatternBooleanExprRemoveConsiderTag implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select sb.intPrimitive as c0 from pattern[every sb=SupportBean -> SupportBean_A(id like sb.theString)]";
            env.compileDeploy(epl).addListener("s0");

            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }
            env.milestone(0);

            sendBeanAAssert(env, "E5", 5, 9);
            sendBeanAMiss(env, "E5");

            env.milestone(1);

            sendBeanAAssert(env, "E3", 3, 8);
            sendBeanAMiss(env, "E5,E3");

            env.milestone(2);

            sendBeanAAssert(env, "E1", 1, 7);
            sendBeanAAssert(env, "E8", 8, 6);

            env.milestone(3);

            sendBeanAAssert(env, "E4", 4, 5);
            sendBeanAMiss(env, "E1,E3,E4,E5,E8");

            sendBeanAAssert(env, "E2", 2, 4);
            sendBeanAAssert(env, "E9", 9, 3);
            sendBeanAAssert(env, "E7", 7, 2);
            sendBeanAMiss(env, "E1,E2,E3,E4,E5,E7,E8,E9");

            sendBeanAAssert(env, "E0", 0, 1);

            env.milestone(4);

            sendBeanAAssert(env, "E6", 6, 0);

            env.milestone(5);

            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean_A("E" + i));
            }
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternPatternTypeCacheForRepeat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // UEJ-229-28464 bug fix for type reuse for dissimilar types
            String epl = "create objectarray schema TypeOne(symbol string, price double);\n" +
                "create objectarray schema TypeTwo(symbol string, market string, price double);\n" +
                "\n" +
                "@Name('Out2') select a[0].symbol from pattern [ [2] a=TypeOne ]\n;" +
                "@Name('Out3') select a[0].market from pattern [ [2] a=TypeTwo ];";
            env.compileDeployWBusPublicType(epl, new RegressionPath());

            env.addListener("Out2");
            env.addListener("Out3");

            env.sendEventObjectArray(new Object[]{"GE", 10}, "TypeOne");
            env.sendEventObjectArray(new Object[]{"GE", 10}, "TypeOne");
            assertTrue(env.listener("Out2").getIsInvokedAndReset());

            env.sendEventObjectArray(new Object[]{"GE", "m1", 5}, "TypeTwo");
            env.sendEventObjectArray(new Object[]{"GE", "m2", 5}, "TypeTwo");
            assertTrue(env.listener("Out3").getIsInvokedAndReset());

            env.undeployAll();
        }
    }

    private static class PatternNumeric implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getSetThreeExternalClock(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("na=SupportBean_N -> nb=SupportBean_N(doublePrimitive = na.doublePrimitive)");
            testCase.add("N6", "na", events.getEvent("N1"), "nb", events.getEvent("N6"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N(intPrimitive=87) -> nb=SupportBean_N(intPrimitive > na.intPrimitive)");
            testCase.add("N8", "na", events.getEvent("N3"), "nb", events.getEvent("N8"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N(intPrimitive=87) -> nb=SupportBean_N(intPrimitive < na.intPrimitive)");
            testCase.add("N4", "na", events.getEvent("N3"), "nb", events.getEvent("N4"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N(intPrimitive=66) -> every nb=SupportBean_N(intPrimitive >= na.intPrimitive)");
            testCase.add("N3", "na", events.getEvent("N2"), "nb", events.getEvent("N3"));
            testCase.add("N4", "na", events.getEvent("N2"), "nb", events.getEvent("N4"));
            testCase.add("N8", "na", events.getEvent("N2"), "nb", events.getEvent("N8"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N(boolBoxed=false) -> every nb=SupportBean_N(boolPrimitive = na.boolPrimitive)");
            testCase.add("N4", "na", events.getEvent("N2"), "nb", events.getEvent("N4"));
            testCase.add("N5", "na", events.getEvent("N2"), "nb", events.getEvent("N5"));
            testCase.add("N8", "na", events.getEvent("N2"), "nb", events.getEvent("N8"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every na=SupportBean_N -> every nb=SupportBean_N(intPrimitive=na.intPrimitive)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every na=SupportBean_N() -> every nb=SupportBean_N(doublePrimitive=na.doublePrimitive)");
            testCase.add("N5", "na", events.getEvent("N2"), "nb", events.getEvent("N5"));
            testCase.add("N6", "na", events.getEvent("N1"), "nb", events.getEvent("N6"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every na=SupportBean_N(boolBoxed=false) -> every nb=SupportBean_N(boolBoxed=na.boolBoxed)");
            testCase.add("N5", "na", events.getEvent("N2"), "nb", events.getEvent("N5"));
            testCase.add("N8", "na", events.getEvent("N2"), "nb", events.getEvent("N8"));
            testCase.add("N8", "na", events.getEvent("N5"), "nb", events.getEvent("N8"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N(boolBoxed=false) -> nb=SupportBean_N(intPrimitive<na.intPrimitive)" +
                " -> nc=SupportBean_N(intPrimitive > nb.intPrimitive)");
            testCase.add("N6", "na", events.getEvent("N2"), "nb", events.getEvent("N5"), "nc", events.getEvent("N6"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N(intPrimitive=86) -> nb=SupportBean_N(intPrimitive<na.intPrimitive)" +
                " -> nc=SupportBean_N(intPrimitive > na.intPrimitive)");
            testCase.add("N8", "na", events.getEvent("N4"), "nb", events.getEvent("N5"), "nc", events.getEvent("N8"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N(intPrimitive=86) -> (nb=SupportBean_N(intPrimitive<na.intPrimitive)" +
                " or nc=SupportBean_N(intPrimitive > na.intPrimitive))");
            testCase.add("N5", "na", events.getEvent("N4"), "nb", events.getEvent("N5"), "nc", null);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N(intPrimitive=86) -> (nb=SupportBean_N(intPrimitive>na.intPrimitive)" +
                " or nc=SupportBean_N(intBoxed < na.intBoxed))");
            testCase.add("N8", "na", events.getEvent("N4"), "nb", events.getEvent("N8"), "nc", null);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N(intPrimitive=86) -> (nb=SupportBean_N(intPrimitive>na.intPrimitive)" +
                " and nc=SupportBean_N(intBoxed < na.intBoxed))");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N() -> every nb=SupportBean_N(doublePrimitive in [0:na.doublePrimitive])");
            testCase.add("N4", "na", events.getEvent("N1"), "nb", events.getEvent("N4"));
            testCase.add("N6", "na", events.getEvent("N1"), "nb", events.getEvent("N6"));
            testCase.add("N7", "na", events.getEvent("N1"), "nb", events.getEvent("N7"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N() -> every nb=SupportBean_N(doublePrimitive in (0:na.doublePrimitive))");
            testCase.add("N4", "na", events.getEvent("N1"), "nb", events.getEvent("N4"));
            testCase.add("N7", "na", events.getEvent("N1"), "nb", events.getEvent("N7"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N() -> every nb=SupportBean_N(intPrimitive in (na.intPrimitive:na.doublePrimitive))");
            testCase.add("N7", "na", events.getEvent("N1"), "nb", events.getEvent("N7"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("na=SupportBean_N() -> every nb=SupportBean_N(intPrimitive in (na.intPrimitive:60))");
            testCase.add("N6", "na", events.getEvent("N1"), "nb", events.getEvent("N6"));
            testCase.add("N7", "na", events.getEvent("N1"), "nb", events.getEvent("N7"));
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternObjectId implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getSetFourExternalClock(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("X1=SupportBean_S0() -> X2=SupportBean_S0(p00=X1.p00)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("X1=SupportBean_S0(p00='B') -> X2=SupportBean_S0(p00=X1.p00)");
            testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("X1=SupportBean_S0(p00='B') -> every X2=SupportBean_S0(p00=X1.p00)");
            testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
            testCase.add("e11", "X1", events.getEvent("e2"), "X2", events.getEvent("e11"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every X1=SupportBean_S0(p00='B') -> every X2=SupportBean_S0(p00=X1.p00)");
            testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
            testCase.add("e11", "X1", events.getEvent("e2"), "X2", events.getEvent("e11"));
            testCase.add("e11", "X1", events.getEvent("e6"), "X2", events.getEvent("e11"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every X1=SupportBean_S0() -> X2=SupportBean_S0(p00=X1.p00)");
            testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
            testCase.add("e8", "X1", events.getEvent("e3"), "X2", events.getEvent("e8"));
            testCase.add("e10", "X1", events.getEvent("e9"), "X2", events.getEvent("e10"));
            testCase.add("e11", "X1", events.getEvent("e6"), "X2", events.getEvent("e11"));
            testCase.add("e12", "X1", events.getEvent("e7"), "X2", events.getEvent("e12"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every X1=SupportBean_S0() -> every X2=SupportBean_S0(p00=X1.p00)");
            testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
            testCase.add("e8", "X1", events.getEvent("e3"), "X2", events.getEvent("e8"));
            testCase.add("e10", "X1", events.getEvent("e9"), "X2", events.getEvent("e10"));
            testCase.add("e11", "X1", events.getEvent("e2"), "X2", events.getEvent("e11"));
            testCase.add("e11", "X1", events.getEvent("e6"), "X2", events.getEvent("e11"));
            testCase.add("e12", "X1", events.getEvent("e7"), "X2", events.getEvent("e12"));
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternFollowedByFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select * from pattern [" +
                "every tradeevent1=SupportTradeEvent(userId in ('U1000','U1001','U1002') ) -> " +
                "(tradeevent2=SupportTradeEvent(userId in ('U1000','U1001','U1002') and " +
                "  userId != tradeevent1.userId and " +
                "  ccypair = tradeevent1.ccypair and " +
                "  direction = tradeevent1.direction) -> " +
                " tradeevent3=SupportTradeEvent(userId in ('U1000','U1001','U1002') and " +
                "  userId != tradeevent1.userId and " +
                "  userId != tradeevent2.userId and " +
                "  ccypair = tradeevent1.ccypair and " +
                "  direction = tradeevent1.direction)" +
                ") where timer:within(600 sec)]";

            env.compileDeploy(expression);
            MyUpdateListener listener = new MyUpdateListener();
            env.statement("s0").addListener(listener);

            Random random = new Random();
            String[] users = {"U1000", "U1001", "U1002"};
            String[] ccy = {"USD", "JPY", "EUR"};
            String[] direction = {"B", "S"};

            for (int i = 0; i < 100; i++) {
                SupportTradeEvent theEvent = new
                    SupportTradeEvent(i, users[random.nextInt(users.length)],
                    ccy[random.nextInt(ccy.length)], direction[random.nextInt(direction.length
                )]);
                env.sendEventBean(theEvent);
            }

            assertEquals(0, listener.badMatchCount);
            env.undeployAll();
        }
    }

    private static class MyUpdateListener implements UpdateListener {
        private int badMatchCount;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            if (newEvents != null) {
                for (EventBean eventBean : newEvents) {
                    handleEvent(eventBean);
                }
            }
        }

        private void handleEvent(EventBean eventBean) {
            SupportTradeEvent tradeevent1 = (SupportTradeEvent)
                eventBean.get("tradeevent1");
            SupportTradeEvent tradeevent2 = (SupportTradeEvent)
                eventBean.get("tradeevent2");
            SupportTradeEvent tradeevent3 = (SupportTradeEvent)
                eventBean.get("tradeevent3");

            if (tradeevent1.getUserId().equals(tradeevent2.getUserId()) ||
                tradeevent1.getUserId().equals(tradeevent3.getUserId()) ||
                tradeevent2.getUserId().equals(tradeevent3.getUserId())) {
                /*
                System.out.println("Bad Match : ");
                System.out.println(tradeevent1);
                System.out.println(tradeevent2);
                System.out.println(tradeevent3 + "\n");
                */
                badMatchCount++;
            }
        }
    }

    private static void sendBeanAAssert(RegressionEnvironment env, String id, int intPrimitiveExpected, int numFiltersRemaining) {
        env.sendEventBean(new SupportBean_A(id));
        final String[] fields = "c0".split(",");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{intPrimitiveExpected});
        assertEquals(numFiltersRemaining, SupportFilterHelper.getFilterCount(env.statement("s0"), "SupportBean_A"));
    }

    private static void sendBeanAMiss(RegressionEnvironment env, String idCSV) {
        for (String id : idCSV.split(",")) {
            env.sendEventBean(new SupportBean_A(id));
            assertFalse(env.listener("s0").isInvoked());
        }
    }
}
