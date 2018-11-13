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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLJoin2StreamSimpleCoercionPerformance {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinPerformanceCoercionForward());
        execs.add(new EPLJoinPerformanceCoercionBack());
        return execs;
    }

    private static class EPLJoinPerformanceCoercionForward implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmt = "@name('s0') select A.longBoxed as value from " +
                "SupportBean(theString='A')#length(1000000) as A," +
                "SupportBean(theString='B')#length(1000000) as B" +
                " where A.longBoxed=B.intPrimitive";
            env.compileDeployAddListenerMileZero(stmt, "s0");

            // preload
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(makeSupportEvent("A", 0, i));
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 5000; i++) {
                int index = 5000 + i % 1000;
                env.sendEventBean(makeSupportEvent("B", index, 0));
                assertEquals((long) index, env.listener("s0").assertOneGetNewAndReset().get("value"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            env.undeployAll();
            assertTrue("Failed perf test, delta=" + delta, delta < 1500);
        }
    }

    private static class EPLJoinPerformanceCoercionBack implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmt = "@name('s0') select A.intPrimitive as value from " +
                "SupportBean(theString='A')#length(1000000) as A," +
                "SupportBean(theString='B')#length(1000000) as B" +
                " where A.intPrimitive=B.longBoxed";
            env.compileDeployAddListenerMileZero(stmt, "s0");

            // preload
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(makeSupportEvent("A", i, 0));
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 5000; i++) {
                int index = 5000 + i % 1000;
                env.sendEventBean(makeSupportEvent("B", 0, index));
                assertEquals(index, env.listener("s0").assertOneGetNewAndReset().get("value"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            env.undeployAll();
            assertTrue("Failed perf test, delta=" + delta, delta < 1500);
        }
    }

    private static Object makeSupportEvent(String theString, int intPrimitive, long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setLongBoxed(longBoxed);
        return bean;
    }
}
