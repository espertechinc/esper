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
import com.espertech.esper.regressionlib.support.bean.SupportBean_S3;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class EPLOuterInnerJoin4Stream {
    private final static String[] FIELDS = "s0.id, s0.p00, s1.id, s1.p10, s2.id, s2.p20, s3.id, s3.p30".split(",");

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinFullMiddleJoinVariantTwo());
        execs.add(new EPLJoinFullMiddleJoinVariantOne());
        execs.add(new EPLJoinFullSidedJoinVariantTwo());
        execs.add(new EPLJoinFullSidedJoinVariantOne());
        execs.add(new EPLJoinStarJoinVariantTwo());
        execs.add(new EPLJoinStarJoinVariantOne());
        return execs;
    }

    private static class EPLJoinFullMiddleJoinVariantTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from SupportBean_S3#keepall s3 " +
                " inner join SupportBean_S2#keepall s2 on s3.p30 = s2.p20 " +
                " full outer join SupportBean_S1#keepall s1 on s2.p20 = s1.p10 " +
                " inner join SupportBean_S0#keepall s0 on s1.p10 = s0.p00";

            tryAssertionMiddle(env, joinStatement);
        }
    }

    private static class EPLJoinFullMiddleJoinVariantOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from SupportBean_S0#keepall s0 " +
                " inner join SupportBean_S1#keepall s1 on s0.p00 = s1.p10 " +
                " full outer join SupportBean_S2#keepall s2 on s1.p10 = s2.p20 " +
                " inner join SupportBean_S3#keepall s3 on s2.p20 = s3.p30";

            tryAssertionMiddle(env, joinStatement);
        }
    }

    private static class EPLJoinFullSidedJoinVariantTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from SupportBean_S3#keepall s3 " +
                " full outer join SupportBean_S2#keepall s2 on s3.p30 = s2.p20 " +
                " full outer join SupportBean_S1#keepall s1 on s2.p20 = s1.p10 " +
                " inner join SupportBean_S0#keepall s0 on s1.p10 = s0.p00";

            tryAssertionSided(env, joinStatement);
        }
    }

    private static class EPLJoinFullSidedJoinVariantOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from SupportBean_S0#keepall s0 " +
                " inner join SupportBean_S1#keepall s1 on s0.p00 = s1.p10 " +
                " full outer join SupportBean_S2#keepall s2 on s1.p10 = s2.p20 " +
                " full outer join SupportBean_S3#keepall s3 on s2.p20 = s3.p30";

            tryAssertionSided(env, joinStatement);
        }
    }

    private static class EPLJoinStarJoinVariantTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from SupportBean_S0#keepall s0 " +
                " left outer join SupportBean_S1#keepall s1 on s0.p00 = s1.p10 " +
                " full outer join SupportBean_S2#keepall s2 on s0.p00 = s2.p20 " +
                " inner join SupportBean_S3#keepall s3 on s0.p00 = s3.p30";

            tryAssertionStar(env, joinStatement);
        }
    }

    private static class EPLJoinStarJoinVariantOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from SupportBean_S3#keepall s3 " +
                " inner join SupportBean_S0#keepall s0 on s0.p00 = s3.p30 " +
                " full outer join SupportBean_S2#keepall s2 on s0.p00 = s2.p20 " +
                " left outer join SupportBean_S1#keepall s1 on s1.p10 = s0.p00";

            tryAssertionStar(env, joinStatement);
        }
    }

    private static void tryAssertionMiddle(RegressionEnvironment env, String expression) {
        String[] fields = "s0.id, s0.p00, s1.id, s1.p10, s2.id, s2.p20, s3.id, s3.p30".split(",");

        env.compileDeployAddListenerMileZero(expression, "s0");

        // s0, s1, s2, s3
        env.sendEventBean(new SupportBean_S0(0, "A"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(100, "A"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(200, "A"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(300, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0, "A", 100, "A", 200, "A", 300, "A"});

        // s0, s2, s3, s1
        env.sendEventBean(new SupportBean_S0(1, "B"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(201, "B"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(301, "B"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(101, "B"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "B", 101, "B", 201, "B", 301, "B"});

        // s2, s3, s1, s0
        env.sendEventBean(new SupportBean_S2(202, "C"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(302, "C"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(102, "C"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(2, "C"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "C", 102, "C", 202, "C", 302, "C"});

        // s1, s2, s0, s3
        env.sendEventBean(new SupportBean_S1(103, "D"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(203, "D"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(3, "D"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(303, "D"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, "D", 103, "D", 203, "D", 303, "D"});

        env.undeployAll();
    }

    private static void tryAssertionSided(RegressionEnvironment env, String expression) {
        env.compileDeployAddListenerMileZero(expression, "s0");

        // s0, s1, s2, s3
        env.sendEventBean(new SupportBean_S0(0, "A"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(100, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{0, "A", 100, "A", null, null, null, null});

        env.sendEventBean(new SupportBean_S2(200, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{0, "A", 100, "A", 200, "A", null, null});

        env.sendEventBean(new SupportBean_S3(300, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{0, "A", 100, "A", 200, "A", 300, "A"});

        // s0, s2, s3, s1
        env.sendEventBean(new SupportBean_S0(1, "B"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(201, "B"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(301, "B"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(101, "B"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{1, "B", 101, "B", 201, "B", 301, "B"});

        // s2, s3, s1, s0
        env.sendEventBean(new SupportBean_S2(202, "C"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(302, "C"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(102, "C"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(2, "C"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{2, "C", 102, "C", 202, "C", 302, "C"});

        // s1, s2, s0, s3
        env.sendEventBean(new SupportBean_S1(103, "D"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(203, "D"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(3, "D"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{3, "D", 103, "D", 203, "D", null, null});

        env.sendEventBean(new SupportBean_S3(303, "D"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{3, "D", 103, "D", 203, "D", 303, "D"});

        env.undeployAll();
    }

    private static void tryAssertionStar(RegressionEnvironment env, String expression) {
        env.compileDeployAddListenerMileZero(expression, "s0");

        // s0, s1, s2, s3
        env.sendEventBean(new SupportBean_S0(0, "A"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(100, "A"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(200, "A"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(300, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{0, "A", 100, "A", 200, "A", 300, "A"});

        // s0, s2, s3, s1
        env.sendEventBean(new SupportBean_S0(1, "B"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(201, "B"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(301, "B"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{1, "B", null, null, 201, "B", 301, "B"});

        env.sendEventBean(new SupportBean_S1(101, "B"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{1, "B", 101, "B", 201, "B", 301, "B"});

        // s2, s3, s1, s0
        env.sendEventBean(new SupportBean_S2(202, "C"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(302, "C"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(102, "C"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(2, "C"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{2, "C", 102, "C", 202, "C", 302, "C"});

        // s1, s2, s0, s3
        env.sendEventBean(new SupportBean_S1(103, "D"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(203, "D"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(3, "D"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S3(303, "D"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{3, "D", 103, "D", 203, "D", 303, "D"});

        // s3, s0, s1, s2
        env.sendEventBean(new SupportBean_S3(304, "E"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(4, "E"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{4, "E", null, null, null, null, 304, "E"});

        env.sendEventBean(new SupportBean_S1(104, "E"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{4, "E", 104, "E", null, null, 304, "E"});

        env.sendEventBean(new SupportBean_S2(204, "E"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{4, "E", 104, "E", 204, "E", 304, "E"});

        env.undeployAll();
    }
}
