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
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.bean.SupportBean_C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class EPLJoin3StreamAndPropertyPerformance {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinPerfAllProps());
        execs.add(new EPLJoinPerfPartialProps());
        execs.add(new EPLJoinPerfPartialStreams());
        return execs;
    }

    private static class EPLJoinPerfAllProps implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            // Statement where all streams are reachable from each other via properties
            String stmt = "@name('s0') select * from " +
                "SupportBean_A()#length(1000000) s1," +
                "SupportBean_B()#length(1000000) s2," +
                "SupportBean_C()#length(1000000) s3" +
                " where s1.id=s2.id and s2.id=s3.id and s1.id=s3.id";
            tryJoinPerf3Streams(env, stmt);
        }
    }

    private static class EPLJoinPerfPartialProps implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            // Statement where the s1 stream is not reachable by joining s2 to s3 and s3 to s1
            String stmt = "@name('s0') select * from " +
                "SupportBean_A#length(1000000) s1," +
                "SupportBean_B#length(1000000) s2," +
                "SupportBean_C#length(1000000) s3" +
                " where s1.id=s2.id and s2.id=s3.id";   // ==> therefore s1.id = s3.id
            tryJoinPerf3Streams(env, stmt);
        }
    }

    private static class EPLJoinPerfPartialStreams implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String methodName = ".testPerfPartialStreams";

            // Statement where the s1 stream is not reachable by joining s2 to s3 and s3 to s1
            String epl = "@name('s0') select * from " +
                "SupportBean_A#length(1000000) s1," +
                "SupportBean_B#length(1000000) s2," +
                "SupportBean_C#length(1000000) s3" +
                " where s1.id=s2.id";   // ==> stream s3 no properties supplied, full s3 scan
            env.compileDeployAddListenerMileZero(epl, "s0");

            // preload s3 with just 1 event
            sendEvent(env, new SupportBean_C("GE_0"));

            // Send events for each stream
            log.info(methodName + " Preloading events");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                sendEvent(env, new SupportBean_A("CSCO_" + i));
                sendEvent(env, new SupportBean_B("IBM_" + i));
            }
            log.info(methodName + " Done preloading");

            long endTime = System.currentTimeMillis();
            log.info(methodName + " delta=" + (endTime - startTime));

            // Stay below 500, no index would be 4 sec plus
            assertTrue((endTime - startTime) < 500);
            env.undeployAll();
        }
    }

    private static void tryJoinPerf3Streams(RegressionEnvironment env, String epl) {
        String methodName = ".tryJoinPerf3Streams";

        env.compileDeployAddListenerMileZero(epl, "s0");

        // Send events for each stream
        log.info(methodName + " Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            sendEvent(env, new SupportBean_A("CSCO_" + i));
            sendEvent(env, new SupportBean_B("IBM_" + i));
            sendEvent(env, new SupportBean_C("GE_" + i));
        }
        log.info(methodName + " Done preloading");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stay below 500, no index would be 4 sec plus
        assertTrue((endTime - startTime) < 500);

        env.undeployAll();
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }

    private static final Logger log = LoggerFactory.getLogger(EPLJoin3StreamAndPropertyPerformance.class);
}
