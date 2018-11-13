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
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLJoin2StreamAndPropertyPerformance {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoin2StreamAndPropertyPerfRemoveStream());
        execs.add(new EPLJoin2StreamAndPropertyPerf2Properties());
        execs.add(new EPLJoin2StreamAndPropertyPerf3Properties());
        return execs;
    }

    private static class EPLJoin2StreamAndPropertyPerfRemoveStream implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {

            MyStaticEval.setCountCalled(0);
            MyStaticEval.setWaitTimeMSec(0);
            env.advanceTime(0);

            String epl = "@name('s0') select * from SupportBean#time(1) as sb, " +
                " SupportBean_S0#keepall as s0 " +
                " where myStaticEvaluator(sb.theString, s0.p00)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean_S0(1, "x"));
            assertEquals(0, MyStaticEval.getCountCalled());

            env.sendEventBean(new SupportBean("y", 10));
            assertEquals(1, MyStaticEval.getCountCalled());
            assertTrue(env.listener("s0").isInvoked());

            // this would be observed as hanging if there was remove-stream evaluation
            MyStaticEval.setWaitTimeMSec(10000000);
            env.advanceTime(100000);

            env.undeployAll();
        }
    }

    private static class EPLJoin2StreamAndPropertyPerf2Properties implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String methodName = ".testPerformanceJoinNoResults";

            String epl = "@name('s0') select * from " +
                "SupportMarketDataBean#length(1000000)," +
                "SupportBean#length(1000000)" +
                " where symbol=theString and volume=longBoxed";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // Send events for each stream
            log.info(methodName + " Preloading events");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                sendEvent(env, makeMarketEvent("IBM_" + i, 1));
                sendEvent(env, makeSupportEvent("CSCO_" + i, 2));
            }
            log.info(methodName + " Done preloading");

            long endTime = System.currentTimeMillis();
            log.info(methodName + " delta=" + (endTime - startTime));

            // Stay at 250, belwo 500ms
            assertTrue((endTime - startTime) < 500);
            env.undeployAll();
        }
    }

    private static class EPLJoin2StreamAndPropertyPerf3Properties implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String methodName = ".testPerformanceJoinNoResults";

            String epl = "@name('s0') select * from " +
                "SupportMarketDataBean()#length(1000000)," +
                "SupportBean#length(1000000)" +
                " where symbol=theString and volume=longBoxed and doublePrimitive=price";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // Send events for each stream
            log.info(methodName + " Preloading events");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                sendEvent(env, makeMarketEvent("IBM_" + i, 1));
                sendEvent(env, makeSupportEvent("CSCO_" + i, 2));
            }
            log.info(methodName + " Done preloading");

            long endTime = System.currentTimeMillis();
            log.info(methodName + " delta=" + (endTime - startTime));

            // Stay at 250, belwo 500ms
            assertTrue((endTime - startTime) < 500);
            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }

    private static Object makeSupportEvent(String id, long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(id);
        bean.setLongBoxed(longBoxed);
        return bean;
    }

    private static Object makeMarketEvent(String id, long volume) {
        return new SupportMarketDataBean(id, 0, (long) volume, "");
    }

    public static class MyStaticEval {
        private static int countCalled = 0;
        private static long waitTimeMSec;

        public static int getCountCalled() {
            return countCalled;
        }

        public static void setCountCalled(int countCalled) {
            MyStaticEval.countCalled = countCalled;
        }

        public static long getWaitTimeMSec() {
            return waitTimeMSec;
        }

        public static void setWaitTimeMSec(long waitTimeMSec) {
            MyStaticEval.waitTimeMSec = waitTimeMSec;
        }

        public static boolean myStaticEvaluator(String a, String b) {
            try {
                Thread.sleep(waitTimeMSec);
                countCalled++;
            } catch (InterruptedException ex) {
                return false;
            }
            return true;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(EPLJoin2StreamAndPropertyPerformance.class);
}
