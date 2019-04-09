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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PatternOperatorEveryDistinct {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternEveryDistinctSimple());
        execs.add(new PatternEveryDistinctWTime());
        execs.add(new PatternExpireSeenBeforeKey());
        execs.add(new PatternEveryDistinctOverFilter());
        execs.add(new PatternRepeatOverDistinct());
        execs.add(new PatternTimerWithinOverDistinct());
        execs.add(new PatternEveryDistinctOverRepeat());
        execs.add(new PatternEveryDistinctOverTimerWithin());
        execs.add(new PatternEveryDistinctOverAnd());
        execs.add(new PatternEveryDistinctOverOr());
        execs.add(new PatternEveryDistinctOverNot());
        execs.add(new PatternEveryDistinctOverFollowedBy());
        execs.add(new PatternEveryDistinctWithinFollowedBy());
        execs.add(new PatternFollowedByWithDistinct());
        execs.add(new PatternInvalid());
        execs.add(new PatternMonthScoped());
        execs.add(new PatternEveryDistinctMultikeyWArray());
        return execs;
    }

    public static class PatternEveryDistinctMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from pattern[every-distinct(a.array) a=SupportEventWithIntArray]");
            env.addListener("s0");

            sendAssertReceived(env, "E1", new int[]{1, 2}, true);
            sendAssertReceived(env, "E2", new int[]{1, 2}, false);
            sendAssertReceived(env, "E3", new int[]{1}, true);
            sendAssertReceived(env, "E4", new int[]{}, true);
            sendAssertReceived(env, "E5", null, true);

            env.milestone(0);

            sendAssertReceived(env, "E10", new int[]{1, 2}, false);
            sendAssertReceived(env, "E11", new int[]{1}, false);
            sendAssertReceived(env, "E12", new int[]{}, false);
            sendAssertReceived(env, "E13", null, false);

            env.undeployAll();
        }

        private void sendAssertReceived(RegressionEnvironment env, String id, int[] array, boolean received) {
            env.sendEventBean(new SupportEventWithIntArray(id, array));
            assertEquals(received, env.listener("s0").getAndClearIsInvoked());
        }
    }

    public static class PatternEveryDistinctSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0".split(",");

            String epl = "@Name('s0') select a.theString as c0 from pattern [every-distinct(a.theString) a=SupportBean]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            sendSupportBean(env, "E1");
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendSupportBean(env, "E1");
            Assert.assertFalse(env.listener("s0").isInvoked());
            sendSupportBean(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            sendSupportBean(env, "E1");
            sendSupportBean(env, "E2");
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            sendSupportBean(env, "E1");
            sendSupportBean(env, "E2");
            Assert.assertFalse(env.listener("s0").isInvoked());
            sendSupportBean(env, "E3");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            env.undeployAll();
        }
    }

    public static class PatternEveryDistinctWTime implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0".split(",");

            env.advanceTime(0);
            String epl = "@Name('s0') select a.theString as c0 from pattern [every-distinct(a.theString, 5 sec) a=SupportBean]";
            env.compileDeploy(epl).addListener("s0");

            env.advanceTime(15000);

            env.milestone(0);

            sendSupportBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            sendSupportBean(env, "E1");
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            env.advanceTime(18000);

            sendSupportBean(env, "E1");
            Assert.assertFalse(env.listener("s0").isInvoked());
            sendSupportBean(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            env.advanceTime(19999);
            sendSupportBean(env, "E1");
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            env.advanceTime(20000);

            sendSupportBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            sendSupportBean(env, "E2");
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            sendSupportBean(env, "E1");
            Assert.assertFalse(env.listener("s0").isInvoked());
            sendSupportBean(env, "E2");
            Assert.assertFalse(env.listener("s0").isInvoked());
            sendSupportBean(env, "E3");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            env.undeployAll();
        }
    }

    private static class PatternExpireSeenBeforeKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive, 1 sec) a=SupportBean(theString like 'A%')]";
            env.compileDeploy(expression).addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean("A1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A1"});

            env.milestone(1);

            env.sendEventBean(new SupportBean("A2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            env.sendEventBean(new SupportBean("A3", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A3"});

            env.milestone(3);

            env.sendEventBean(new SupportBean("A4", 1));
            env.sendEventBean(new SupportBean("A5", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            env.advanceTime(1000);

            env.sendEventBean(new SupportBean("A4", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A4"});
            env.sendEventBean(new SupportBean("A5", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A5"});

            env.milestone(5);

            env.sendEventBean(new SupportBean("A6", 1));
            env.advanceTime(1999);
            env.sendEventBean(new SupportBean("A7", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);

            env.advanceTime(2000);
            env.sendEventBean(new SupportBean("A7", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A7"});

            env.undeployAll();
        }
    }

    private static class PatternEveryDistinctOverFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String expression = "@name('s0') select * from pattern [every-distinct(intPrimitive) a=SupportBean]";
            runEveryDistinctOverFilter(env, expression, milestone);

            expression = "@name('s0') select * from pattern [every-distinct(intPrimitive,2 minutes) a=SupportBean]";
            runEveryDistinctOverFilter(env, expression, milestone);
        }

        private static void runEveryDistinctOverFilter(RegressionEnvironment env, String expression, AtomicInteger milestone) {
            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E1", 1));
            Assert.assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E3", 2));
            Assert.assertEquals("E3", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E4", 3));
            Assert.assertEquals("E4", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E5", 2));
            env.sendEventBean(new SupportBean("E6", 3));
            env.sendEventBean(new SupportBean("E7", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E8", 0));
            Assert.assertEquals("E8", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.eplToModelCompileDeploy(expression);

            env.undeployAll();
        }
    }

    private static class PatternRepeatOverDistinct implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expression = "@name('s0') select * from pattern [[2] every-distinct(a.intPrimitive) a=SupportBean]";
            runRepeatOverDistinct(env, expression, milestone);

            expression = "@name('s0') select * from pattern [[2] every-distinct(a.intPrimitive, 1 hour) a=SupportBean]";
            runRepeatOverDistinct(env, expression, milestone);
        }

        private static void runRepeatOverDistinct(RegressionEnvironment env, String expression, AtomicInteger milestone) {
            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E3", 2));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("E1", theEvent.get("a[0].theString"));
            Assert.assertEquals("E3", theEvent.get("a[1].theString"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E4", 3));
            env.sendEventBean(new SupportBean("E5", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternEveryDistinctOverRepeat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expression = "@name('s0') select * from pattern [every-distinct(a[0].intPrimitive) [2] a=SupportBean]";
            runEveryDistinctOverRepeat(env, expression, milestone);

            expression = "@name('s0') select * from pattern [every-distinct(a[0].intPrimitive, a[0].intPrimitive, 1 hour) [2] a=SupportBean]";
            runEveryDistinctOverRepeat(env, expression, milestone);
        }

        private static void runEveryDistinctOverRepeat(RegressionEnvironment env, String expression, AtomicInteger milestone) {

            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 1));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("E1", theEvent.get("a[0].theString"));
            Assert.assertEquals("E2", theEvent.get("a[1].theString"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E3", 1));
            env.sendEventBean(new SupportBean("E4", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E5", 2));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E6", 1));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("E5", theEvent.get("a[0].theString"));
            Assert.assertEquals("E6", theEvent.get("a[1].theString"));

            env.undeployAll();
        }
    }

    private static class PatternTimerWithinOverDistinct implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            // for 10 seconds, look for every distinct A
            String expression = "@name('s0') select * from pattern [(every-distinct(a.intPrimitive) a=SupportBean) where timer:within(10 sec)]";
            runTimerWithinOverDistinct(env, expression, milestone);

            expression = "@name('s0') select * from pattern [(every-distinct(a.intPrimitive, 2 days 2 minutes) a=SupportBean) where timer:within(10 sec)]";
            runTimerWithinOverDistinct(env, expression, milestone);
        }

        private static void runTimerWithinOverDistinct(RegressionEnvironment env, String expression, AtomicInteger milestone) {

            sendTimer(0, env);
            env.compileDeploy(expression).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            Assert.assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E3", 2));
            Assert.assertEquals("E3", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.milestoneInc(milestone);

            sendTimer(11000, env);
            env.sendEventBean(new SupportBean("E4", 3));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E5", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternEveryDistinctOverTimerWithin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive) (a=SupportBean where timer:within(10 sec))]";
            runEveryDistinctOverTimerWithin(env, expression, milestone);

            expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive, 1 hour) (a=SupportBean where timer:within(10 sec))]";
            runEveryDistinctOverTimerWithin(env, expression, milestone);
        }

        private static void runEveryDistinctOverTimerWithin(RegressionEnvironment env, String expression, AtomicInteger milestone) {

            sendTimer(0, env);
            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E1", 1));
            Assert.assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.sendEventBean(new SupportBean("E2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimer(5000, env);
            env.sendEventBean(new SupportBean("E3", 2));
            Assert.assertEquals("E3", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.milestoneInc(milestone);

            sendTimer(10000, env);
            env.sendEventBean(new SupportBean("E4", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E5", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E6", 2));
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(15000, env);
            env.sendEventBean(new SupportBean("E7", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimer(20000, env);
            env.sendEventBean(new SupportBean("E8", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimer(25000, env);
            env.sendEventBean(new SupportBean("E9", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimer(50000, env);
            env.sendEventBean(new SupportBean("E10", 1));
            Assert.assertEquals("E10", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.sendEventBean(new SupportBean("E11", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E12", 2));
            Assert.assertEquals("E12", env.listener("s0").assertOneGetNewAndReset().get("a.theString"));

            env.sendEventBean(new SupportBean("E13", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternEveryDistinctOverAnd implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive, b.intPrimitive) (a=SupportBean(theString like 'A%') and b=SupportBean(theString like 'B%'))]";
            runEveryDistinctOverAnd(env, expression, milestone);

            expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive, b.intPrimitive, 1 hour) (a=SupportBean(theString like 'A%') and b=SupportBean(theString like 'B%'))]";
            runEveryDistinctOverAnd(env, expression, milestone);
        }

        private static void runEveryDistinctOverAnd(RegressionEnvironment env, String expression, AtomicInteger milestone) {

            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A1", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B1"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A2", 1));
            env.sendEventBean(new SupportBean("B2", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("A3", 2));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B3", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A3", "B3"});

            env.sendEventBean(new SupportBean("A4", 1));
            env.sendEventBean(new SupportBean("B4", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A4", "B4"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A5", 2));
            env.sendEventBean(new SupportBean("B5", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A6", 2));
            env.sendEventBean(new SupportBean("B6", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A6", "B6"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A7", 2));
            env.sendEventBean(new SupportBean("B7", 20));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternEveryDistinctOverOr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expression = "@name('s0') select * from pattern [every-distinct(coalesce(a.intPrimitive, 0) + coalesce(b.intPrimitive, 0)) (a=SupportBean(theString like 'A%') or b=SupportBean(theString like 'B%'))]";
            runEveryDistinctOverOr(env, expression, milestone);

            expression = "@name('s0') select * from pattern [every-distinct(coalesce(a.intPrimitive, 0) + coalesce(b.intPrimitive, 0), 1 hour) (a=SupportBean(theString like 'A%') or b=SupportBean(theString like 'B%'))]";
            runEveryDistinctOverOr(env, expression, milestone);
        }

        private static void runEveryDistinctOverOr(RegressionEnvironment env, String expression, AtomicInteger milestone) {

            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", null});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B1", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{null, "B1"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B2", 1));
            env.sendEventBean(new SupportBean("A2", 2));
            env.sendEventBean(new SupportBean("A3", 2));
            env.sendEventBean(new SupportBean("B3", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B4", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{null, "B4"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B5", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{null, "B5"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B6", 3));
            env.sendEventBean(new SupportBean("A4", 3));
            env.sendEventBean(new SupportBean("A5", 4));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternEveryDistinctOverNot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive) (a=SupportBean(theString like 'A%') and not SupportBean(theString like 'B%'))]";
            runEveryDistinctOverNot(env, expression, milestone);

            expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive, 1 hour) (a=SupportBean(theString like 'A%') and not SupportBean(theString like 'B%'))]";
            runEveryDistinctOverNot(env, expression, milestone);
        }

        private static void runEveryDistinctOverNot(RegressionEnvironment env, String expression, AtomicInteger milestone) {

            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A1"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A3", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A3"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B1", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A4", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A4"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A5", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternEveryDistinctOverFollowedBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive + b.intPrimitive) (a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%'))]";
            runEveryDistinctOverFollowedBy(env, expression, milestone);

            expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive + b.intPrimitive, 1 hour) (a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%'))]";
            runEveryDistinctOverFollowedBy(env, expression, milestone);
        }

        private static void runEveryDistinctOverFollowedBy(RegressionEnvironment env, String expression, AtomicInteger milestone) {
            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A1", 1));
            assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(new SupportBean("B1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B1"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A2", 1));
            env.sendEventBean(new SupportBean("B2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A3", 10));
            env.sendEventBean(new SupportBean("B3", -8));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A4", 2));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B4", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A4", "B4"});

            env.sendEventBean(new SupportBean("A5", 3));
            env.sendEventBean(new SupportBean("B5", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternEveryDistinctWithinFollowedBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expression = "@name('s0') select * from pattern [(every-distinct(a.intPrimitive) a=SupportBean(theString like 'A%')) -> b=SupportBean(intPrimitive=a.intPrimitive)]";
            runEveryDistinctWithinFollowedBy(env, expression, milestone);

            expression = "@name('s0') select * from pattern [(every-distinct(a.intPrimitive, 2 hours 1 minute) a=SupportBean(theString like 'A%')) -> b=SupportBean(intPrimitive=a.intPrimitive)]";
            runEveryDistinctWithinFollowedBy(env, expression, milestone);
        }

        private static void runEveryDistinctWithinFollowedBy(RegressionEnvironment env, String expression, AtomicInteger milestone) {
            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A1", 1));
            env.sendEventBean(new SupportBean("B1", 0));
            assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(new SupportBean("B2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B2"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A2", 2));
            env.sendEventBean(new SupportBean("A3", 3));
            env.sendEventBean(new SupportBean("A4", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A3", "B3"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B4", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B5", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A2", "B5"});

            env.sendEventBean(new SupportBean("A5", 2));
            env.sendEventBean(new SupportBean("B6", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A6", 4));
            env.sendEventBean(new SupportBean("B7", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A6", "B7"});

            env.undeployAll();
        }
    }

    private static class PatternFollowedByWithDistinct implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive) a=SupportBean(theString like 'A%') -> every-distinct(b.intPrimitive) b=SupportBean(theString like 'B%')]";
            runFollowedByWithDistinct(env, expression, milestone);

            expression = "@name('s0') select * from pattern [every-distinct(a.intPrimitive, 1 day) a=SupportBean(theString like 'A%') -> every-distinct(b.intPrimitive) b=SupportBean(theString like 'B%')]";
            runFollowedByWithDistinct(env, expression, milestone);
        }

        private static void runFollowedByWithDistinct(RegressionEnvironment env, String expression, AtomicInteger milestone) {
            env.compileDeploy(expression).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A1", 1));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B1"});
            env.sendEventBean(new SupportBean("B2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B2"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B3", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("A2", 1));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B4", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B4"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("A3", 2));
            env.sendEventBean(new SupportBean("B5", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A3", "B5"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B6", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("B7", 3));
            EventBean[] events = env.listener("s0").getAndResetLastNewData();
            EPAssertionUtil.assertPropsPerRowAnyOrder(events, "a.theString,b.theString".split(","),
                new Object[][]{{"A1", "B7"}, {"A3", "B7"}});

            env.undeployAll();
        }
    }

    private static class PatternInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalid(env, "a=SupportBean_A->every-distinct(a.intPrimitive) SupportBean_B",
                "Failed to validate pattern every-distinct expression 'a.intPrimitive': Failed to resolve property 'a.intPrimitive' to a stream or nested property in a stream");

            tryInvalid(env, "every-distinct(dummy) SupportBean_A",
                "Failed to validate pattern every-distinct expression 'dummy': Property named 'dummy' is not valid in any stream ");

            tryInvalid(env, "every-distinct(2 sec) SupportBean_A",
                "Every-distinct node requires one or more distinct-value expressions that each return non-constant result values");
        }
    }

    private static class PatternMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a.theString,a.intPrimitive".split(",");

            sendCurrentTime(env, "2002-02-01T09:00:00.000");
            String epl = "@name('s0') select * from pattern [every-distinct(theString, 1 month) a=SupportBean]";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.sendEventBean(new SupportBean("E1", 2));
            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            env.sendEventBean(new SupportBean("E1", 3));
            assertFalse(env.listener("s0").isInvoked());

            sendCurrentTime(env, "2002-03-01T09:00:00.000");

            env.sendEventBean(new SupportBean("E1", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 4});

            env.undeployAll();
        }
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void tryInvalid(RegressionEnvironment env, String statement, String message) {
        tryInvalidCompile(env, "select * from pattern[" + statement + "]", message);
    }

    private static void sendTimer(long timeInMSec, RegressionEnvironment env) {
        env.advanceTime(timeInMSec);
    }

    private static void sendSupportBean(RegressionEnvironment env, String string) {
        env.sendEventBean(new SupportBean(string, 0));
    }
}
