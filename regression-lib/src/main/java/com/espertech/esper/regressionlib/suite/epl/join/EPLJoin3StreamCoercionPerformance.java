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

public class EPLJoin3StreamCoercionPerformance {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinPerfCoercion3waySceneOne());
        execs.add(new EPLJoinPerfCoercion3waySceneTwo());
        execs.add(new EPLJoinPerfCoercion3waySceneThree());
        return execs;
    }

    private static class EPLJoinPerfCoercion3waySceneOne implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s1.intBoxed as value from " +
                "SupportBean(theString='A')#length(1000000) s1," +
                "SupportBean(theString='B')#length(1000000) s2," +
                "SupportBean(theString='C')#length(1000000) s3" +
                " where s1.intBoxed=s2.longBoxed and s1.intBoxed=s3.doubleBoxed";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // preload
            for (int i = 0; i < 10000; i++) {
                sendEvent(env, "B", 0, i, 0);
                sendEvent(env, "C", 0, 0, i);
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 5000; i++) {
                int index = 5000 + i % 1000;
                sendEvent(env, "A", index, 0, 0);
                assertEquals(index, env.listener("s0").assertOneGetNewAndReset().get("value"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            assertTrue("Failed perf test, delta=" + delta, delta < 1500);
            env.undeployAll();
        }
    }

    private static class EPLJoinPerfCoercion3waySceneTwo implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s1.intBoxed as value from " +
                "SupportBean(theString='A')#length(1000000) s1," +
                "SupportBean(theString='B')#length(1000000) s2," +
                "SupportBean(theString='C')#length(1000000) s3" +
                " where s1.intBoxed=s2.longBoxed and s1.intBoxed=s3.doubleBoxed";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // preload
            for (int i = 0; i < 10000; i++) {
                sendEvent(env, "A", i, 0, 0);
                sendEvent(env, "B", 0, i, 0);
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 5000; i++) {
                int index = 5000 + i % 1000;
                sendEvent(env, "C", 0, 0, index);
                assertEquals(index, env.listener("s0").assertOneGetNewAndReset().get("value"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            env.undeployAll();
            assertTrue("Failed perf test, delta=" + delta, delta < 1500);
        }
    }

    private static class EPLJoinPerfCoercion3waySceneThree implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s1.intBoxed as value from " +
                "SupportBean(theString='A')#length(1000000) s1," +
                "SupportBean(theString='B')#length(1000000) s2," +
                "SupportBean(theString='C')#length(1000000) s3" +
                " where s1.intBoxed=s2.longBoxed and s1.intBoxed=s3.doubleBoxed";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // preload
            for (int i = 0; i < 10000; i++) {
                sendEvent(env, "A", i, 0, 0);
                sendEvent(env, "C", 0, 0, i);
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 5000; i++) {
                int index = 5000 + i % 1000;
                sendEvent(env, "B", 0, index, 0);
                assertEquals(index, env.listener("s0").assertOneGetNewAndReset().get("value"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            env.undeployAll();
            assertTrue("Failed perf test, delta=" + delta, delta < 1500);
        }
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intBoxed, long longBoxed, double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        env.sendEventBean(bean);
    }
}
