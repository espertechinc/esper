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
package com.espertech.esper.regressionlib.suite.epl.subselect;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.support.bean.SupportBean_S3;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class EPLSubselectFilteredPerformance {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectPerformanceOneCriteria());
        execs.add(new EPLSubselectPerformanceTwoCriteria());
        execs.add(new EPLSubselectPerformanceJoin3CriteriaSceneOne());
        execs.add(new EPLSubselectPerformanceJoin3CriteriaSceneTwo());
        return execs;
    }

    private static class EPLSubselectPerformanceOneCriteria implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select p10 from SupportBean_S1#length(100000) where id = s0.id) as value from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // preload with 10k events
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean_S1(i, Integer.toString(i)));
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                int index = 5000 + i % 1000;
                env.sendEventBean(new SupportBean_S0(index, Integer.toString(index)));
                Assert.assertEquals(Integer.toString(index), env.listener("s0").assertOneGetNewAndReset().get("value"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            assertTrue("Failed perf test, delta=" + delta, delta < 1000);
            env.undeployAll();
        }
    }

    private static class EPLSubselectPerformanceTwoCriteria implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select p10 from SupportBean_S1#length(100000) where s0.id = id and p10 = s0.p00) as value from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // preload with 10k events
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean_S1(i, Integer.toString(i)));
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                int index = 5000 + i % 1000;
                env.sendEventBean(new SupportBean_S0(index, Integer.toString(index)));
                Assert.assertEquals(Integer.toString(index), env.listener("s0").assertOneGetNewAndReset().get("value"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            assertTrue("Failed perf test, delta=" + delta, delta < 1000);
            env.undeployAll();
        }
    }

    private static class EPLSubselectPerformanceJoin3CriteriaSceneOne implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select p00 from SupportBean_S0#length(100000) where p00 = s1.p10 and p01 = s2.p20 and p02 = s3.p30) as value " +
                "from SupportBean_S1#length(100000) as s1, SupportBean_S2#length(100000) as s2, SupportBean_S3#length(100000) as s3 where s1.id = s2.id and s2.id = s3.id";
            tryPerfJoin3Criteria(env, stmtText);
        }
    }

    private static class EPLSubselectPerformanceJoin3CriteriaSceneTwo implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select p00 from SupportBean_S0#length(100000) where p01 = s2.p20 and p00 = s1.p10 and p02 = s3.p30 and id >= 0) as value " +
                "from SupportBean_S3#length(100000) as s3, SupportBean_S1#length(100000) as s1, SupportBean_S2#length(100000) as s2 where s2.id = s3.id and s1.id = s2.id";
            tryPerfJoin3Criteria(env, stmtText);
        }
    }

    private static void tryPerfJoin3Criteria(RegressionEnvironment env, String stmtText) {
        env.compileDeployAddListenerMileZero(stmtText, "s0");

        // preload with 10k events
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportBean_S0(i, Integer.toString(i), Integer.toString(i + 1), Integer.toString(i + 2)));
        }

        long startTime = System.currentTimeMillis();
        for (int index = 0; index < 5000; index++) {
            env.sendEventBean(new SupportBean_S1(index, Integer.toString(index)));
            env.sendEventBean(new SupportBean_S2(index, Integer.toString(index + 1)));
            env.sendEventBean(new SupportBean_S3(index, Integer.toString(index + 2)));
            Assert.assertEquals(Integer.toString(index), env.listener("s0").assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
        env.undeployAll();
    }
}
