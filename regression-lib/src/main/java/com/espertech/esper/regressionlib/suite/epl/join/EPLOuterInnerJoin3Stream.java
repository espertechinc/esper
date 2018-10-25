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
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class EPLOuterInnerJoin3Stream {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinFullJoinVariantThree());
        execs.add(new EPLJoinFullJoinVariantTwo());
        execs.add(new EPLJoinFullJoinVariantOne());
        execs.add(new EPLJoinLeftJoinVariantThree());
        execs.add(new EPLJoinLeftJoinVariantTwo());
        execs.add(new EPLJoinRightJoinVariantOne());
        return execs;
    }

    private static class EPLJoinFullJoinVariantThree implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from " +
                "SupportBean_S1#keepall as s1 inner join " +
                "SupportBean_S2#length(1000) as s2 on s1.p10 = s2.p20 " +
                "full outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s1.p10";

            tryAssertionFull(env, joinStatement);
        }
    }

    private static class EPLJoinFullJoinVariantTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from " +
                "SupportBean_S2#length(1000) as s2 " +
                "inner join " + "SupportBean_S1#keepall as s1 on s1.p10 = s2.p20" +
                " full outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s1.p10";

            tryAssertionFull(env, joinStatement);
        }
    }

    private static class EPLJoinFullJoinVariantOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from " +
                "SupportBean_S0#length(1000) as s0 " +
                "full outer join " + "SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10" +
                " inner join " + "SupportBean_S2#length(1000) as s2 on s1.p10 = s2.p20";

            tryAssertionFull(env, joinStatement);
        }
    }

    private static class EPLJoinLeftJoinVariantThree implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from " +
                "SupportBean_S1#keepall as s1 left outer join " +
                "SupportBean_S0#length(1000) as s0 on s0.p00 = s1.p10 " +
                "inner join " + "SupportBean_S2#length(1000) as s2 on s1.p10 = s2.p20";

            tryAssertionFull(env, joinStatement);
        }
    }

    private static class EPLJoinLeftJoinVariantTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from " +
                "SupportBean_S2#length(1000) as s2 " +
                "inner join " + "SupportBean_S1#keepall as s1 on s1.p10 = s2.p20" +
                " left outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s1.p10";

            tryAssertionFull(env, joinStatement);
        }
    }

    private static class EPLJoinRightJoinVariantOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from " +
                "SupportBean_S0#length(1000) as s0 " +
                "right outer join " + "SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10" +
                " inner join " + "SupportBean_S2#length(1000) as s2 on s1.p10 = s2.p20";

            tryAssertionFull(env, joinStatement);
        }
    }

    private static void tryAssertionFull(RegressionEnvironment env, String expression) {
        String[] fields = "s0.id, s0.p00, s1.id, s1.p10, s2.id, s2.p20".split(",");

        env.eplToModelCompileDeploy(expression).addListener("s0");

        // s1, s2, s0
        env.sendEventBean(new SupportBean_S1(100, "A_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(200, "A_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, 100, "A_1", 200, "A_1"});

        env.sendEventBean(new SupportBean_S0(0, "A_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0, "A_1", 100, "A_1", 200, "A_1"});

        // s1, s0, s2
        env.sendEventBean(new SupportBean_S1(103, "D_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(203, "D_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, 103, "D_1", 203, "D_1"});

        env.sendEventBean(new SupportBean_S0(3, "D_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, "D_1", 103, "D_1", 203, "D_1"});

        // s2, s1, s0
        env.sendEventBean(new SupportBean_S2(201, "B_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(101, "B_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, 101, "B_1", 201, "B_1"});

        env.sendEventBean(new SupportBean_S0(1, "B_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "B_1", 101, "B_1", 201, "B_1"});

        // s2, s0, s1
        env.sendEventBean(new SupportBean_S2(202, "C_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(2, "C_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(102, "C_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "C_1", 102, "C_1", 202, "C_1"});

        // s0, s1, s2
        env.sendEventBean(new SupportBean_S0(4, "E_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(104, "E_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(204, "E_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4, "E_1", 104, "E_1", 204, "E_1"});

        // s0, s2, s1
        env.sendEventBean(new SupportBean_S0(5, "F_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(205, "F_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(105, "F_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, "F_1", 105, "F_1", 205, "F_1"});

        env.undeployAll();
    }
}
