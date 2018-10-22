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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;


public class EPLOuterJoinUnidirectional {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoin2Stream());
        execs.add(new EPLJoin3StreamAllUnidirectional(false));
        execs.add(new EPLJoin3StreamAllUnidirectional(true));
        execs.add(new EPLJoin3StreamMixed());
        execs.add(new EPLJoin4StreamWhereClause());
        execs.add(new EPLJoinOuterInvalid());
        return execs;
    }

    public static class EPLJoinOuterInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // all: unidirectional and full-outer-join

            // no-view-declared
            tryInvalidCompile(env,
                "select * from SupportBean_A unidirectional full outer join SupportBean_B#keepall unidirectional",
                "The unidirectional keyword requires that no views are declared onto the stream (applies to stream 1)");

            // not-all-unidirectional
            tryInvalidCompile(env,
                "select * from SupportBean_A unidirectional full outer join SupportBean_B unidirectional full outer join SupportBean_C#keepall",
                "The unidirectional keyword must either apply to a single stream or all streams in a full outer join");

            // no iterate
            SupportMessageAssertUtil.tryInvalidIterate(env,
                "@name('s0') select * from SupportBean_A unidirectional full outer join SupportBean_B unidirectional",
                "Iteration over a unidirectional join is not supported");
        }
    }

    private static class EPLJoin2Stream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select a.id as aid, b.id as bid from SupportBean_A as a unidirectional " +
                "full outer join SupportBean_B as b unidirectional";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean_A("A1"));
            assertReceived2Stream(env, "A1", null);

            env.sendEventBean(new SupportBean_B("B1"));
            assertReceived2Stream(env, null, "B1");

            env.sendEventBean(new SupportBean_B("B2"));
            assertReceived2Stream(env, null, "B2");

            env.sendEventBean(new SupportBean_A("A2"));
            assertReceived2Stream(env, "A2", null);

            env.undeployAll();
        }
    }

    private static class EPLJoin3StreamAllUnidirectional implements RegressionExecution {
        private final boolean soda;

        public EPLJoin3StreamAllUnidirectional(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select * from SupportBean_A as a unidirectional " +
                "full outer join SupportBean_B as b unidirectional " +
                "full outer join SupportBean_C as c unidirectional";

            env.compileDeploy(soda, epl).addListener("s0").milestone(0);

            env.sendEventBean(new SupportBean_A("A1"));
            assertReceived3Stream(env, "A1", null, null);

            env.sendEventBean(new SupportBean_C("C1"));
            assertReceived3Stream(env, null, null, "C1");

            env.sendEventBean(new SupportBean_C("C2"));
            assertReceived3Stream(env, null, null, "C2");

            env.sendEventBean(new SupportBean_A("A2"));
            assertReceived3Stream(env, "A2", null, null);

            env.sendEventBean(new SupportBean_B("B1"));
            assertReceived3Stream(env, null, "B1", null);

            env.sendEventBean(new SupportBean_B("B2"));
            assertReceived3Stream(env, null, "B2", null);

            env.undeployAll();
        }
    }

    private static class EPLJoin3StreamMixed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyCWindow#keepall as SupportBean_C;\n" +
                "insert into MyCWindow select * from SupportBean_C;\n" +
                "@name('s0') select a.id as aid, b.id as bid, MyCWindow.id as cid, SupportBean_D.id as did " +
                "from pattern[every a=SupportBean_A -> b=SupportBean_B] t1 unidirectional " +
                "full outer join " +
                "MyCWindow unidirectional " +
                "full outer join " +
                "SupportBean_D unidirectional;\n";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean_C("c1"));
            assertReceived3StreamMixed(env, null, null, "c1", null);

            env.sendEventBean(new SupportBean_A("a1"));
            env.sendEventBean(new SupportBean_B("b1"));
            assertReceived3StreamMixed(env, "a1", "b1", null, null);

            env.sendEventBean(new SupportBean_A("a2"));
            env.sendEventBean(new SupportBean_B("b2"));
            assertReceived3StreamMixed(env, "a2", "b2", null, null);

            env.sendEventBean(new SupportBean_D("d1"));
            assertReceived3StreamMixed(env, null, null, null, "d1");

            env.undeployAll();
        }
    }

    private static class EPLJoin4StreamWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean_A as a unidirectional " +
                "full outer join SupportBean_B as b unidirectional " +
                "full outer join SupportBean_C as c unidirectional " +
                "full outer join SupportBean_D as d unidirectional " +
                "where coalesce(a.id,b.id,c.id,d.id) in ('YES')";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendAssert(env, new SupportBean_A("A1"), false);
            sendAssert(env, new SupportBean_A("YES"), true);
            sendAssert(env, new SupportBean_C("YES"), true);
            sendAssert(env, new SupportBean_C("C1"), false);
            sendAssert(env, new SupportBean_D("YES"), true);
            sendAssert(env, new SupportBean_B("YES"), true);
            sendAssert(env, new SupportBean_B("B1"), false);

            env.undeployAll();
        }
    }

    private static void sendAssert(RegressionEnvironment env, SupportBeanAtoFBase event, boolean b) {
        env.sendEventBean(event);
        assertEquals(b, env.listener("s0").getAndClearIsInvoked());
    }

    private static void assertReceived2Stream(RegressionEnvironment env, String a, String b) {
        String[] fields = "aid,bid".split(",");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{a, b});
    }

    private static void assertReceived3Stream(RegressionEnvironment env, String a, String b, String c) {
        String[] fields = "a.id,b.id,c.id".split(",");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{a, b, c});
    }

    private static void assertReceived3StreamMixed(RegressionEnvironment env, String a, String b, String c, String d) {
        String[] fields = "aid,bid,cid,did".split(",");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{a, b, c, d});
    }
}
