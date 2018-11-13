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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLJoin3StreamOuterJoinCoercionPerformance {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinPerfCoercion3waySceneOne());
        execs.add(new EPLJoinPerfCoercion3waySceneTwo());
        execs.add(new EPLJoinPerfCoercion3waySceneThree());
        execs.add(new EPLJoinPerfCoercion3wayRange());
        return execs;
    }

    private static class EPLJoinPerfCoercion3waySceneOne implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s1.intBoxed as v1, s2.longBoxed as v2, s3.doubleBoxed as v3 from " +
                "SupportBean(theString='A')#length(1000000) s1 " +
                " left outer join " +
                "SupportBean(theString='B')#length(1000000) s2 on s1.intBoxed=s2.longBoxed " +
                " left outer join " +
                "SupportBean(theString='C')#length(1000000) s3 on s1.intBoxed=s3.doubleBoxed";
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
                EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
                assertEquals(index, theEvent.get("v1"));
                assertEquals((long) index, theEvent.get("v2"));
                assertEquals((double) index, theEvent.get("v3"));
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
            String stmtText = "@name('s0') select s1.intBoxed as v1, s2.longBoxed as v2, s3.doubleBoxed as v3 from " +
                "SupportBean(theString='A')#length(1000000) s1 " +
                " left outer join " +
                "SupportBean(theString='B')#length(1000000) s2 on s1.intBoxed=s2.longBoxed " +
                " left outer join " +
                "SupportBean(theString='C')#length(1000000) s3 on s1.intBoxed=s3.doubleBoxed";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // preload
            for (int i = 0; i < 10000; i++) {
                sendEvent(env, "B", 0, i, 0);
                sendEvent(env, "A", i, 0, 0);
            }

            env.listener("s0").reset();
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 5000; i++) {
                int index = 5000 + i % 1000;
                sendEvent(env, "C", 0, 0, index);
                EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
                assertEquals(index, theEvent.get("v1"));
                assertEquals((long) index, theEvent.get("v2"));
                assertEquals((double) index, theEvent.get("v3"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            assertTrue("Failed perf test, delta=" + delta, delta < 1500);
            env.undeployAll();
        }
    }

    private static class EPLJoinPerfCoercion3waySceneThree implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s1.intBoxed as v1, s2.longBoxed as v2, s3.doubleBoxed as v3 from " +
                "SupportBean(theString='A')#length(1000000) s1 " +
                " left outer join " +
                "SupportBean(theString='B')#length(1000000) s2 on s1.intBoxed=s2.longBoxed " +
                " left outer join " +
                "SupportBean(theString='C')#length(1000000) s3 on s1.intBoxed=s3.doubleBoxed";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // preload
            for (int i = 0; i < 10000; i++) {
                sendEvent(env, "A", i, 0, 0);
                sendEvent(env, "C", 0, 0, i);
            }

            env.listener("s0").reset();
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 5000; i++) {
                int index = 5000 + i % 1000;
                sendEvent(env, "B", 0, index, 0);
                EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
                assertEquals(index, theEvent.get("v1"));
                assertEquals((long) index, theEvent.get("v2"));
                assertEquals((double) index, theEvent.get("v3"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            assertTrue("Failed perf test, delta=" + delta, delta < 1500);
            env.undeployAll();
        }
    }

    private static class EPLJoinPerfCoercion3wayRange implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {

            String stmtText = "@name('s0') select * from " +
                "SupportBeanRange#keepall sbr " +
                " left outer join " +
                "SupportBean_ST0#keepall s0 on s0.key0=sbr.key" +
                " left outer join " +
                "SupportBean_ST1#keepall s1 on s1.key1=s0.key0" +
                " where s0.p00 between sbr.rangeStartLong and sbr.rangeEndLong";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // preload
            log.info("Preload");
            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean_ST1("ST1_" + i, "K", i));
            }
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean_ST0("ST0_" + i, "K", i));
            }
            log.info("Preload done");

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                long index = 5000 + i;
                env.sendEventBean(SupportBeanRange.makeLong("R", "K", index, index + 2));
                assertEquals(30, env.listener("s0").getAndResetLastNewData().length);
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            env.sendEventBean(new SupportBean_ST0("ST0X", "K", 5000));
            assertEquals(10, env.listener("s0").getAndResetLastNewData().length);

            env.sendEventBean(new SupportBean_ST1("ST1X", "K", 5004));
            assertEquals(301, env.listener("s0").getAndResetLastNewData().length);

            assertTrue("Failed perf test, delta=" + delta, delta < 500);
            env.undeployAll();
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

    private static final Logger log = LoggerFactory.getLogger(EPLJoin3StreamOuterJoinCoercionPerformance.class);
}
