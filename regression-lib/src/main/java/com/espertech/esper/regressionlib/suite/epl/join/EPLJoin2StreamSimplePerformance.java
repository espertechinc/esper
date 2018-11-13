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
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLJoin2StreamSimplePerformance {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinPerformanceJoinNoResults());
        execs.add(new EPLJoinJoinPerformanceStreamA());
        execs.add(new EPLJoinJoinPerformanceStreamB());
        return execs;
    }

    private static class EPLJoinPerformanceJoinNoResults implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            setupStatement(env);
            String methodName = ".testPerformanceJoinNoResults";

            // Send events for each stream
            log.info(methodName + " Preloading events");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                sendEvent(env, makeMarketEvent("IBM_" + i));
                sendEvent(env, makeSupportEvent("CSCO_" + i));
            }
            log.info(methodName + " Done preloading");

            long endTime = System.currentTimeMillis();
            log.info(methodName + " delta=" + (endTime - startTime));

            // Stay below 50 ms
            assertTrue((endTime - startTime) < 500);
            env.undeployAll();
        }
    }

    private static class EPLJoinJoinPerformanceStreamA implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            setupStatement(env);
            String methodName = ".testJoinPerformanceStreamA";

            // Send 100k events
            log.info(methodName + " Preloading events");
            for (int i = 0; i < 50000; i++) {
                sendEvent(env, makeMarketEvent("IBM_" + i));
            }
            log.info(methodName + " Done preloading");

            long startTime = System.currentTimeMillis();
            sendEvent(env, makeSupportEvent("IBM_10"));
            long endTime = System.currentTimeMillis();
            log.info(methodName + " delta=" + (endTime - startTime));

            assertEquals(1, env.listener("s0").getLastNewData().length);
            // Stay below 50 ms
            assertTrue((endTime - startTime) < 50);
            env.undeployAll();
        }
    }

    private static class EPLJoinJoinPerformanceStreamB implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String methodName = ".testJoinPerformanceStreamB";
            setupStatement(env);

            // Send 100k events
            log.info(methodName + " Preloading events");
            for (int i = 0; i < 50000; i++) {
                sendEvent(env, makeSupportEvent("IBM_" + i));
            }
            log.info(methodName + " Done preloading");

            long startTime = System.currentTimeMillis();

            env.listener("s0").reset();
            sendEvent(env, makeMarketEvent("IBM_" + 10));

            long endTime = System.currentTimeMillis();
            log.info(methodName + " delta=" + (endTime - startTime));

            assertEquals(1, env.listener("s0").getLastNewData().length);
            // Stay below 50 ms
            assertTrue((endTime - startTime) < 25);
            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }

    private static Object makeSupportEvent(String id) {
        SupportBean bean = new SupportBean();
        bean.setTheString(id);
        return bean;
    }

    private static Object makeMarketEvent(String id) {
        return new SupportMarketDataBean(id, 0, (long) 0, "");
    }

    private static void setupStatement(RegressionEnvironment env) {
        String epl = "@name('s0') select * from " +
            "SupportMarketDataBean#length(1000000)," +
            "SupportBean#length(1000000)" +
            " where symbol=theString";
        env.compileDeployAddListenerMileZero(epl, "s0");
    }

    private static final Logger log = LoggerFactory.getLogger(EPLJoin2StreamSimplePerformance.class);
}
