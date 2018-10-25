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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.support.util.ArrayHandlingUtil;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class EPLOuterJoinVarC3Stream {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinOuterInnerJoinRootS0());
        execs.add(new EPLJoinOuterInnerJoinRootS1());
        execs.add(new EPLJoinOuterInnerJoinRootS2());
        return execs;
    }

    private static class EPLJoinOuterInnerJoinRootS0 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *                  s0
             *           s1 ->      <- s2
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S0#length(1000) as s0 " +
                " right outer join SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S2#length(1000) as s2 on s0.p00 = s2.p20 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static class EPLJoinOuterInnerJoinRootS1 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *                  s0
             *           s1 ->      <- s2
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S1#length(1000) as s1 " +
                " left outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S2#length(1000) as s2 on s0.p00 = s2.p20 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static class EPLJoinOuterInnerJoinRootS2 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *                  s0
             *           s1 ->      <- s2
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S2#length(1000) as s2 " +
                " left outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s2.p20 " +
                " right outer join SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static void tryAssertion(RegressionEnvironment env) {
        // Test s0 ... s1 with 0 rows, s2 with 0 rows
        //
        Object[] s0Events = SupportBean_S0.makeS0("A", new String[]{"A-s0-1"});
        sendEvent(env, s0Events);
        assertFalse(env.listener("s0").isInvoked());

        // Test s0 ... s1 with 1 rows, s2 with 0 rows
        //
        Object[] s1Events = SupportBean_S1.makeS1("B", new String[]{"B-s1-1"});
        sendEventsAndReset(env, s1Events);

        s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-1"});
        sendEvent(env, s0Events);
        assertFalse(env.listener("s0").isInvoked());

        // Test s0 ... s1 with 0 rows, s2 with 1 rows
        //
        Object[] s2Events = SupportBean_S2.makeS2("C", new String[]{"C-s2-1"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("C", new String[]{"C-s0-1"});
        sendEvent(env, s0Events);
        assertFalse(env.listener("s0").isInvoked());

        // Test s0 ... s1 with 1 rows, s2 with 1 rows
        //
        s1Events = SupportBean_S1.makeS1("D", new String[]{"D-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("D", new String[]{"D-s2-1"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("D", new String[]{"D-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]}}, getAndResetNewEvents(env));

        // Test s0 ... s1 with 1 rows, s2 with 2 rows
        //
        s1Events = SupportBean_S1.makeS1("E", new String[]{"E-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("E", new String[]{"E-s2-1", "E-s2-2"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("E", new String[]{"E-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[0], s2Events[1]}}, getAndResetNewEvents(env));

        // Test s0 ... s1 with 2 rows, s2 with 1 rows
        //
        s1Events = SupportBean_S1.makeS1("F", new String[]{"F-s1-1", "F-s1-2"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("F", new String[]{"F-s2-1"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("F", new String[]{"F-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[1], s2Events[0]}}, getAndResetNewEvents(env));

        // Test s0 ... s1 with 2 rows, s2 with 2 rows
        //
        s1Events = SupportBean_S1.makeS1("G", new String[]{"G-s1-1", "G-s1-2"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("G", new String[]{"G-s2-1", "G-s2-2"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("G", new String[]{"G-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[1], s2Events[0]},
            {s0Events[0], s1Events[0], s2Events[1]},
            {s0Events[0], s1Events[1], s2Events[1]}}, getAndResetNewEvents(env));

        // Test s1 ... s0 with 0 rows, s2 with 0 rows
        //
        s1Events = SupportBean_S1.makeS1("H", new String[]{"H-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, s1Events[0], null}}, getAndResetNewEvents(env));

        // Test s1 ... s0 with 1 rows, s2 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("I", new String[]{"I-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("I", new String[]{"I-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, s1Events[0], null}}, getAndResetNewEvents(env));
        // s0 is not expected in this case since s0 requires results in s2 which didn't exist

        // Test s1 ... s0 with 1 rows, s2 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("J", new String[]{"J-s0-1"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("J", new String[]{"J-s2-1"});
        sendEventsAndReset(env, s2Events);

        s1Events = SupportBean_S1.makeS1("J", new String[]{"J-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]}}, getAndResetNewEvents(env));

        // Test s1 ... s0 with 1 rows, s2 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("K", new String[]{"K-s0-1"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("K", new String[]{"K-s2-1", "K-s2-1"});
        sendEventsAndReset(env, s2Events);

        s1Events = SupportBean_S1.makeS1("K", new String[]{"K-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[0], s2Events[1]}}, getAndResetNewEvents(env));


        // Test s1 ... s0 with 2 rows, s2 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("L", new String[]{"L-s0-1", "L-s0-2"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("L", new String[]{"L-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, s1Events[0], null}}, getAndResetNewEvents(env));
        // s0 is not expected in this case since s0 requires results in s2 which didn't exist

        // Test s1 ... s0 with 2 rows, s2 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("M", new String[]{"M-s0-1", "M-s0-2"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("M", new String[]{"M-s2-1"});
        sendEventsAndReset(env, s2Events);

        s1Events = SupportBean_S1.makeS1("M", new String[]{"M-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[1], s1Events[0], s2Events[0]}}, getAndResetNewEvents(env));

        // Test s1 ... s0 with 2 rows, s2 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("N", new String[]{"N-s0-1", "N-s0-2"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("N", new String[]{"N-s2-1", "N-s2-2"});
        sendEventsAndReset(env, s2Events);

        s1Events = SupportBean_S1.makeS1("N", new String[]{"N-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[0], s2Events[1]},
            {s0Events[1], s1Events[0], s2Events[0]},
            {s0Events[1], s1Events[0], s2Events[1]}}, getAndResetNewEvents(env));

        // Test s2 ... s0 with 0 rows, s1 with 0 rows
        //
        s2Events = SupportBean_S2.makeS2("P", new String[]{"P-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, null, s2Events[0]}}, getAndResetNewEvents(env));

        // Test s2 ... s0 with 1 rows, s1 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("Q", new String[]{"Q-s0-1"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("Q", new String[]{"Q-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, null, s2Events[0]}}, getAndResetNewEvents(env));

        // Test s2 ... s0 with 1 rows, s1 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("R", new String[]{"R-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("R", new String[]{"R-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("R", new String[]{"R-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]}}, getAndResetNewEvents(env));

        // Test s2 ... s0 with 1 rows, s1 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("S", new String[]{"S-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("S", new String[]{"S-s1-1", "S-s1-2"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("S", new String[]{"S-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[1], s2Events[0]}}, getAndResetNewEvents(env));

        // Test s2 ... s0 with 2 rows, s1 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("T", new String[]{"T-s0-1", "T-s0-2"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("T", new String[]{"T-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, null, s2Events[0]}}, getAndResetNewEvents(env));   // no s0 events as they depend on s1

        // Test s2 ... s0 with 2 rows, s1 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("U", new String[]{"U-s0-1", "U-s0-2"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("U", new String[]{"U-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("U", new String[]{"U-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[1], s1Events[0], s2Events[0]}}, getAndResetNewEvents(env));

        // Test s2 ... s0 with 2 rows, s1 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("V", new String[]{"V-s0-1", "V-s0-2"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("V", new String[]{"V-s1-1", "V-s1-2"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("V", new String[]{"V-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[1], s2Events[0]},
            {s0Events[1], s1Events[0], s2Events[0]},
            {s0Events[1], s1Events[1], s2Events[0]}}, getAndResetNewEvents(env));

        env.undeployAll();
    }

    private static void sendEventsAndReset(RegressionEnvironment env, Object[] events) {
        sendEvent(env, events);
        env.listener("s0").reset();
    }

    private static void sendEvent(RegressionEnvironment env, Object[] events) {
        for (int i = 0; i < events.length; i++) {
            env.sendEventBean(events[i]);
        }
    }

    private static Object[][] getAndResetNewEvents(RegressionEnvironment env) {
        EventBean[] newEvents = env.listener("s0").getLastNewData();
        assertNotNull("no events received", newEvents);
        env.listener("s0").reset();
        return ArrayHandlingUtil.getUnderlyingEvents(newEvents, new String[]{"s0", "s1", "s2"});
    }
}
