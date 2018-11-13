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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class EPLSubselectInKeywordPerformance {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectPerformanceInKeywordAsPartOfSubquery());
        execs.add(new EPLSubselectPerformanceWhereClauseCoercion());
        execs.add(new EPLSubselectPerformanceWhereClause());
        return execs;
    }

    private static class EPLSubselectPerformanceInKeywordAsPartOfSubquery implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String eplSingleIndex = "@name('s0') select (select p00 from SupportBean_S0#keepall as s0 where s0.p01 in (s1.p10, s1.p11)) as c0 from SupportBean_S1 as s1";
            env.compileDeployAddListenerMile(eplSingleIndex, "s0", milestone.getAndIncrement());

            tryAssertionPerformanceInKeywordAsPartOfSubquery(env);
            env.undeployAll();

            String eplMultiIdx = "@name('s0') select (select p00 from SupportBean_S0#keepall as s0 where s1.p11 in (s0.p00, s0.p01)) as c0 from SupportBean_S1 as s1";
            env.compileDeployAddListenerMile(eplMultiIdx, "s0", milestone.getAndIncrement());

            tryAssertionPerformanceInKeywordAsPartOfSubquery(env);

            env.undeployAll();
        }
    }

    private static class EPLSubselectPerformanceWhereClauseCoercion implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select intPrimitive from SupportBean(theString='A') as s0 where intPrimitive in (" +
                "select longBoxed from SupportBean(theString='B')#length(10000) where s0.intPrimitive = longBoxed)";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // preload with 10k events
            for (int i = 0; i < 10000; i++) {
                SupportBean bean = new SupportBean();
                bean.setTheString("B");
                bean.setLongBoxed((long) i);
                env.sendEventBean(bean);
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                int index = 5000 + i % 1000;
                SupportBean bean = new SupportBean();
                bean.setTheString("A");
                bean.setIntPrimitive(index);
                env.sendEventBean(bean);
                Assert.assertEquals(index, env.listener("s0").assertOneGetNewAndReset().get("intPrimitive"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            assertTrue("Failed perf test, delta=" + delta, delta < 2000);
            env.undeployAll();
        }
    }

    private static class EPLSubselectPerformanceWhereClause implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 as s0 where p00 in (" +
                "select p10 from SupportBean_S1#length(10000) where s0.p00 = p10)";
            env.compileDeployAddListenerMileZero(stmtText, "s0");


            // preload with 10k events
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean_S1(i, Integer.toString(i)));
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                int index = 5000 + i % 1000;
                env.sendEventBean(new SupportBean_S0(index, Integer.toString(index)));
                Assert.assertEquals(index, env.listener("s0").assertOneGetNewAndReset().get("id"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            assertTrue("Failed perf test, delta=" + delta, delta < 1000);
            env.undeployAll();
        }
    }

    private static void tryAssertionPerformanceInKeywordAsPartOfSubquery(RegressionEnvironment env) {
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportBean_S0(i, "v" + i, "p00_" + i));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 2000; i++) {
            int index = 5000 + i % 1000;
            env.sendEventBean(new SupportBean_S1(index, "x", "p00_" + index));
            Assert.assertEquals("v" + index, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 500);
    }
}
