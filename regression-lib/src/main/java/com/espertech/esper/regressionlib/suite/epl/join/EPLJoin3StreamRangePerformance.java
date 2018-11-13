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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLJoin3StreamRangePerformance {
    private final static Logger log = LoggerFactory.getLogger(EPLJoin3StreamRangePerformance.class);

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinPerf3StreamKeyAndRange());
        execs.add(new EPLJoinPerf3StreamRangeOnly());
        execs.add(new EPLJoinPerf3StreamUnidirectionalKeyAndRange());
        return execs;
    }

    /**
     * This join algorithm profits from merge join cartesian indicated via @hint.
     */
    private static class EPLJoinPerf3StreamKeyAndRange implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window ST0#keepall as SupportBean_ST0;\n" +
                "@Name('I1') insert into ST0 select * from SupportBean_ST0;\n" +
                "create window ST1#keepall as SupportBean_ST1;\n" +
                "@Name('I2') insert into ST1 select * from SupportBean_ST1;\n";
            env.compileDeploy(epl, path).milestone(0);

            // Preload
            log.info("Preloading events");
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean_ST0("ST0", "G", i));
                env.sendEventBean(new SupportBean_ST1("ST1", "G", i));
            }
            log.info("Done preloading");

            String eplQuery = "@name('s0') @Hint('PREFER_MERGE_JOIN') select * from SupportBeanRange#lastevent a " +
                "inner join ST0 st0 on st0.key0 = a.key " +
                "inner join ST1 st1 on st1.key1 = a.key " +
                "where " +
                "st0.p00 between rangeStart and rangeEnd and st1.p10 between rangeStart and rangeEnd";
            tryAssertion(env, path, eplQuery);

            eplQuery = "@name('s0') @Hint('PREFER_MERGE_JOIN') select * from SupportBeanRange#lastevent a, ST0 st0, ST1 st1 " +
                "where st0.key0 = a.key and st1.key1 = a.key and " +
                "st0.p00 between rangeStart and rangeEnd and st1.p10 between rangeStart and rangeEnd";
            tryAssertion(env, path, eplQuery);

            env.undeployAll();
        }
    }

    /**
     * This join algorithm uses merge join cartesian (not nested iteration).
     */
    private static class EPLJoinPerf3StreamRangeOnly implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window ST0#keepall as SupportBean_ST0;\n" +
                "@Name('I1') insert into ST0 select * from SupportBean_ST0;\n" +
                "create window ST1#keepall as SupportBean_ST1;\n" +
                "@Name('I2') insert into ST1 select * from SupportBean_ST1;\n";
            env.compileDeploy(epl, path).milestone(0);

            // Preload
            log.info("Preloading events");
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean_ST0("ST0", "ST0", i));
                env.sendEventBean(new SupportBean_ST1("ST1", "ST1", i));
            }
            log.info("Done preloading");

            String eplQuery = "@name('s0') select * from SupportBeanRange#lastevent a, ST0 st0, ST1 st1 " +
                "where st0.p00 between rangeStart and rangeEnd and st1.p10 between rangeStart and rangeEnd";
            env.compileDeploy(eplQuery, path).addListener("s0").milestone(1);

            // Repeat
            log.info("Querying");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R", "R", 100, 101));
                assertEquals(4, env.listener("s0").getAndResetLastNewData().length);
            }
            log.info("Done Querying");
            long endTime = System.currentTimeMillis();
            log.info("delta=" + (endTime - startTime));

            assertTrue((endTime - startTime) < 1000);
            env.undeployAll();
        }
    }

    /**
     * This join algorithm profits from nested iteration execution.
     */
    private static class EPLJoinPerf3StreamUnidirectionalKeyAndRange implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window SBR#keepall as SupportBeanRange;\n" +
                "@Name('I1') insert into SBR select * from SupportBeanRange;\n" +
                "create window ST1#keepall as SupportBean_ST1;\n" +
                "@Name('I2') insert into ST1 select * from SupportBean_ST1;\n";
            env.compileDeploy(epl, path).milestone(0);

            // Preload
            log.info("Preloading events");
            env.sendEventBean(new SupportBeanRange("ST1", "G", 4000, 4004));
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean_ST1("ST1", "G", i));
            }
            log.info("Done preloading");

            String eplQuery = "@name('s0') select * from SupportBean_ST0 st0 unidirectional, SBR a, ST1 st1 " +
                "where st0.key0 = a.key and st1.key1 = a.key and " +
                "st1.p10 between rangeStart and rangeEnd";
            env.compileDeploy(eplQuery, path).addListener("s0").milestone(1);

            // Repeat
            log.info("Querying");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 500; i++) {
                env.sendEventBean(new SupportBean_ST0("ST0", "G", -1));
                assertEquals(5, env.listener("s0").getAndResetLastNewData().length);
            }
            log.info("Done Querying");
            long delta = System.currentTimeMillis() - startTime;
            log.info("delta=" + delta);

            // This works best with a nested iteration join (and not a cardinal join)
            assertTrue("delta=" + delta, delta < 500);
            env.undeployAll();
        }
    }

    private static void tryAssertion(RegressionEnvironment env, RegressionPath path, String epl) {

        env.compileDeploy(epl, path).addListener("s0");

        // Repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            env.sendEventBean(new SupportBeanRange("R", "G", 100, 101));
            assertEquals(4, env.listener("s0").getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        assertTrue((endTime - startTime) < 500);
        env.undeployModuleContaining("s0");
    }
}
