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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class EPLJoin2StreamRangePerformance {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinPerfKeyAndRangeOuterJoin());
        execs.add(new EPLJoinPerfRelationalOp());
        execs.add(new EPLJoinPerfKeyAndRange());
        execs.add(new EPLJoinPerfKeyAndRangeInverted());
        execs.add(new EPLJoinPerfUnidirectionalRelOp());
        return execs;
    }

    private static class EPLJoinPerfKeyAndRangeOuterJoin implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window SBR#keepall as SupportBeanRange;\n" +
                "@Name('I1') insert into SBR select * from SupportBeanRange;\n" +
                "create window SB#keepall as SupportBean;\n" +
                "@Name('I2') insert into SB select * from SupportBean;\n";
            env.compileDeploy(epl, path).milestone(0);

            // Preload
            log.info("Preloading events");
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean("G", i));
                env.sendEventBean(new SupportBeanRange("R", "G", i - 1, i + 2));
            }
            log.info("Done preloading");

            // create
            String eplQuery = "@name('s0') select * " +
                "from SB sb " +
                "full outer join " +
                "SBR sbr " +
                "on theString = key " +
                "where intPrimitive between rangeStart and rangeEnd";
            env.compileDeploy(eplQuery, path).addListener("s0").milestone(1);

            // Repeat
            log.info("Querying");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBean("G", 9990));
                assertEquals(4, env.listener("s0").getAndResetLastNewData().length);

                env.sendEventBean(new SupportBeanRange("R", "G", 4, 10));
                assertEquals(7, env.listener("s0").getAndResetLastNewData().length);
            }
            log.info("Done Querying");
            long endTime = System.currentTimeMillis();
            log.info("delta=" + (endTime - startTime));

            env.undeployAll();
            assertTrue((endTime - startTime) < 500);
        }
    }

    private static class EPLJoinPerfRelationalOp implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window SBR#keepall as SupportBeanRange;\n" +
                "@Name('I1') insert into SBR select * from SupportBeanRange;\n" +
                "create window SB#keepall as SupportBean;\n" +
                "@Name('I2') insert into SB select * from SupportBean";
            env.compileDeploy(epl, path).milestone(0);

            // Preload
            log.info("Preloading events");
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
                env.sendEventBean(new SupportBeanRange("E", i, -1));
            }
            log.info("Done preloading");

            // start query
            String eplQuery = "@name('s0') select * from SBR a, SB b where a.rangeStart < b.intPrimitive";
            env.compileDeploy(eplQuery, path).addListener("s0").milestone(1);

            // Repeat
            log.info("Querying");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBean("B", 10));
                assertEquals(10, env.listener("s0").getAndResetLastNewData().length);

                env.sendEventBean(new SupportBeanRange("R", 9990, -1));
                assertEquals(9, env.listener("s0").getAndResetLastNewData().length);
            }
            log.info("Done Querying");
            long endTime = System.currentTimeMillis();
            log.info("delta=" + (endTime - startTime));

            env.undeployAll();
            assertTrue((endTime - startTime) < 500);
        }
    }

    private static class EPLJoinPerfKeyAndRange implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window SBR#keepall as SupportBeanRange;\n" +
                "@Name('I1') insert into SBR select * from SupportBeanRange;\n" +
                "create window SB#keepall as SupportBean;\n" +
                "@Name('I2') insert into SB select * from SupportBean;\n";
            env.compileDeploy(epl, path).milestone(0);

            // Preload
            log.info("Preloading events");
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    env.sendEventBean(new SupportBean(Integer.toString(i), j));
                    env.sendEventBean(new SupportBeanRange("R", Integer.toString(i), j - 1, j + 1));
                }
            }
            log.info("Done preloading");

            // start query
            String eplQuery = "@name('s0') select * from SBR sbr, SB sb where sbr.key = sb.theString and sb.intPrimitive between sbr.rangeStart and sbr.rangeEnd";
            env.compileDeploy(eplQuery, path).addListener("s0").milestone(1);

            // repeat
            log.info("Querying");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBean("55", 10));
                assertEquals(3, env.listener("s0").getAndResetLastNewData().length);

                env.sendEventBean(new SupportBeanRange("R", "56", 12, 20));
                assertEquals(9, env.listener("s0").getAndResetLastNewData().length);
            }
            log.info("Done Querying");
            long endTime = System.currentTimeMillis();
            log.info("delta=" + (endTime - startTime));

            // test no event found
            env.sendEventBean(new SupportBeanRange("R", "56", 2000, 3000));
            env.sendEventBean(new SupportBeanRange("R", "X", 2000, 3000));
            assertFalse(env.listener("s0").isInvoked());

            assertTrue("delta=" + (endTime - startTime), (endTime - startTime) < 1500);

            // delete all events
            env.compileDeploy("on SupportBean delete from SBR;\n" +
                "on SupportBean delete from SB;\n", path);
            env.sendEventBean(new SupportBean("D", -1));

            env.undeployAll();
        }
    }

    private static class EPLJoinPerfKeyAndRangeInverted implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window SB#keepall as SupportBean;\n" +
                "@Name('I2') insert into SB select * from SupportBean";
            env.compileDeploy(epl, path).milestone(0);

            // Preload
            log.info("Preloading events");
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean("E", i));
            }
            log.info("Done preloading");

            // start query
            String eplQuery = "@name('s0') select * from SupportBeanRange#lastevent sbr, SB sb where sbr.key = sb.theString and sb.intPrimitive not in [sbr.rangeStart:sbr.rangeEnd]";
            env.compileDeploy(eplQuery, path).addListener("s0").milestone(1);

            // repeat
            log.info("Querying");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R", "E", 5, 9995));
                assertEquals(9, env.listener("s0").getAndResetLastNewData().length);
            }
            log.info("Done Querying");
            long endTime = System.currentTimeMillis();
            log.info("delta=" + (endTime - startTime));

            assertTrue((endTime - startTime) < 500);
            env.undeployAll();
        }
    }

    private static class EPLJoinPerfUnidirectionalRelOp implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            RegressionPath path = new RegressionPath();

            String epl = "create window SB#keepall as SupportBean;\n" +
                "@Name('I') insert into SB select * from SupportBean;\n";
            env.compileDeploy(epl, path).milestone(0);

            // Preload
            log.info("Preloading events");
            for (int i = 0; i < 100000; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }
            log.info("Done preloading");

            // Test range
            String rangeEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive between r.rangeStart and r.rangeEnd";
            String rangeEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive between r.rangeStart and r.rangeEnd";
            String rangeEplThree = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange#lastevent r, SB a " +
                "where a.intPrimitive between r.rangeStart and r.rangeEnd";
            String rangeEplFour = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange#lastevent r " +
                "where a.intPrimitive between r.rangeStart and r.rangeEnd";
            String rangeEplFive = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a\n" +
                "where a.intPrimitive >= r.rangeStart and a.intPrimitive <= r.rangeEnd";
            String rangeEplSix = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive <= r.rangeEnd and a.intPrimitive >= r.rangeStart";
            AssertionCallback rangeCallback = new AssertionCallback() {
                public Object getEvent(int iteration) {
                    return new SupportBeanRange("E", iteration + 50000, iteration + 50100);
                }

                public Object[] getExpectedValue(int iteration) {
                    return new Object[]{50000 + iteration, 50100 + iteration};
                }
            };
            tryAssertion(env, path, milestone, rangeEplOne, 100, rangeCallback);
            tryAssertion(env, path, milestone, rangeEplTwo, 100, rangeCallback);
            tryAssertion(env, path, milestone, rangeEplThree, 100, rangeCallback);
            tryAssertion(env, path, milestone, rangeEplFour, 100, rangeCallback);
            tryAssertion(env, path, milestone, rangeEplFive, 100, rangeCallback);
            tryAssertion(env, path, milestone, rangeEplSix, 100, rangeCallback);

            // Test Greater-Equals
            String geEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive >= r.rangeStart and a.intPrimitive <= 99200";
            String geEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive >= r.rangeStart and a.intPrimitive <= 99200";
            AssertionCallback geCallback = new AssertionCallback() {
                public Object getEvent(int iteration) {
                    return new SupportBeanRange("E", iteration + 99000, null);
                }

                public Object[] getExpectedValue(int iteration) {
                    return new Object[]{99000 + iteration, 99200};
                }
            };
            tryAssertion(env, path, milestone, geEplOne, 100, geCallback);
            tryAssertion(env, path, milestone, geEplTwo, 100, geCallback);

            // Test Greater-Then
            String gtEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= 99200";
            String gtEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= 99200";
            String gtEplThree = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange#lastevent r, SB a " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= 99200";
            String gtEplFour = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange#lastevent r " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= 99200";
            AssertionCallback gtCallback = new AssertionCallback() {
                public Object getEvent(int iteration) {
                    return new SupportBeanRange("E", iteration + 99000, null);
                }

                public Object[] getExpectedValue(int iteration) {
                    return new Object[]{99001 + iteration, 99200};
                }
            };
            tryAssertion(env, path, milestone, gtEplOne, 100, gtCallback);
            tryAssertion(env, path, milestone, gtEplTwo, 100, gtCallback);
            tryAssertion(env, path, milestone, gtEplThree, 100, gtCallback);
            tryAssertion(env, path, milestone, gtEplFour, 100, gtCallback);

            // Test Less-Then
            String ltEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive < r.rangeStart and a.intPrimitive > 100";
            String ltEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive < r.rangeStart and a.intPrimitive > 100";
            AssertionCallback ltCallback = new AssertionCallback() {
                public Object getEvent(int iteration) {
                    return new SupportBeanRange("E", iteration + 500, null);
                }

                public Object[] getExpectedValue(int iteration) {
                    return new Object[]{101, 499 + iteration};
                }
            };
            tryAssertion(env, path, milestone, ltEplOne, 100, ltCallback);
            tryAssertion(env, path, milestone, ltEplTwo, 100, ltCallback);

            // Test Less-Equals
            String leEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive <= r.rangeStart and a.intPrimitive > 100";
            String leEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive <= r.rangeStart and a.intPrimitive > 100";
            AssertionCallback leCallback = new AssertionCallback() {
                public Object getEvent(int iteration) {
                    return new SupportBeanRange("E", iteration + 500, null);
                }

                public Object[] getExpectedValue(int iteration) {
                    return new Object[]{101, 500 + iteration};
                }
            };
            tryAssertion(env, path, milestone, leEplOne, 100, leCallback);
            tryAssertion(env, path, milestone, leEplTwo, 100, leCallback);

            // Test open range
            String openEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive < r.rangeEnd";
            String openEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive in (r.rangeStart:r.rangeEnd)";
            AssertionCallback openCallback = new AssertionCallback() {
                public Object getEvent(int iteration) {
                    return new SupportBeanRange("E", iteration + 3, iteration + 7);
                }

                public Object[] getExpectedValue(int iteration) {
                    return new Object[]{iteration + 4, iteration + 6};
                }
            };
            tryAssertion(env, path, milestone, openEplOne, 100, openCallback);
            tryAssertion(env, path, milestone, openEplTwo, 100, openCallback);

            // Test half-open range
            String hopenEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive >= r.rangeStart and a.intPrimitive < r.rangeEnd";
            String hopenEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive in [r.rangeStart:r.rangeEnd)";
            AssertionCallback halfOpenCallback = new AssertionCallback() {
                public Object getEvent(int iteration) {
                    return new SupportBeanRange("E", iteration + 3, iteration + 7);
                }

                public Object[] getExpectedValue(int iteration) {
                    return new Object[]{iteration + 3, iteration + 6};
                }
            };
            tryAssertion(env, path, milestone, hopenEplOne, 100, halfOpenCallback);
            tryAssertion(env, path, milestone, hopenEplTwo, 100, halfOpenCallback);

            // Test half-closed range
            String hclosedEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= r.rangeEnd";
            String hclosedEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive in (r.rangeStart:r.rangeEnd]";
            AssertionCallback halfClosedCallback = new AssertionCallback() {
                public Object getEvent(int iteration) {
                    return new SupportBeanRange("E", iteration + 3, iteration + 7);
                }

                public Object[] getExpectedValue(int iteration) {
                    return new Object[]{iteration + 4, iteration + 7};
                }
            };
            tryAssertion(env, path, milestone, hclosedEplOne, 100, halfClosedCallback);
            tryAssertion(env, path, milestone, hclosedEplTwo, 100, halfClosedCallback);

            // Test inverted closed range
            String invertedClosedEPLOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive not in [r.rangeStart:r.rangeEnd]";
            String invertedClosedEPLTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive not between r.rangeStart and r.rangeEnd";
            AssertionCallback invertedClosedCallback = new AssertionCallback() {
                public Object getEvent(int iteration) {
                    return new SupportBeanRange("E", 20, 99990);
                }

                public Object[] getExpectedValue(int iteration) {
                    return new Object[]{0, 99999};
                }
            };
            tryAssertion(env, path, milestone, invertedClosedEPLOne, 100, invertedClosedCallback);
            tryAssertion(env, path, milestone, invertedClosedEPLTwo, 100, invertedClosedCallback);

            // Test inverted open range
            String invertedOpenEPLOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive not in (r.rangeStart:r.rangeEnd)";
            tryAssertion(env, path, milestone, invertedOpenEPLOne, 100, invertedClosedCallback);

            env.undeployAll();
        }
    }

    private static void tryAssertion(RegressionEnvironment env, RegressionPath path, AtomicInteger milestone, String epl, int numLoops, AssertionCallback assertionCallback) {
        String[] fields = "mini,maxi".split(",");

        env.compileDeploy("@name('s0')" + epl, path).addListener("s0").milestoneInc(milestone);

        // Send range query events
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numLoops; i++) {
            //if (i % 10 == 0) {
            //    log.info("At loop #" + i);
            //}
            env.sendEventBean(assertionCallback.getEvent(i));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, assertionCallback.getExpectedValue(i));
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        assertTrue((endTime - startTime) < 1500);
        env.undeployModuleContaining("s0");
    }

    private static final Logger log = LoggerFactory.getLogger(EPLJoin2StreamRangePerformance.class);

    private static interface AssertionCallback {
        public Object getEvent(int iteration);

        public Object[] getExpectedValue(int iteration);
    }
}
