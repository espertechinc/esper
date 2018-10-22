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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportIdEventA;
import com.espertech.esper.regressionlib.support.bean.SupportIdEventB;
import com.espertech.esper.regressionlib.support.bean.SupportIdEventC;
import com.espertech.esper.regressionlib.support.bean.SupportIdEventD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertFalse;

public class PatternConsumingPattern {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternOrOp());
        execs.add(new PatternFollowedByOp());
        execs.add(new PatternMatchUntilOp());
        execs.add(new PatternObserverOp());
        execs.add(new PatternAndOp());
        execs.add(new PatternNotOpNotImpacted());
        execs.add(new PatternGuardOp());
        execs.add(new PatternEveryOp());
        execs.add(new PatternCombination());
        execs.add(new PatternInvalid());
        return execs;
    }

    private static class PatternInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            tryInvalidCompile(env, path, "select * from pattern @XX [SupportIdEventA]",
                "Unrecognized pattern-level annotation 'XX' [select * from pattern @XX [SupportIdEventA]]");

            String expected = "Discard-partials and suppress-matches is not supported in a joins, context declaration and on-action ";
            tryInvalidCompile(env, path, "select * from pattern " + TargetEnum.DISCARD_AND_SUPPRESS.getText() + "[SupportIdEventA]#keepall, A#keepall",
                expected + "[select * from pattern @DiscardPartialsOnMatch @SuppressOverlappingMatches [SupportIdEventA]#keepall, A#keepall]");

            env.compileDeploy("create window AWindow#keepall as SupportIdEventA", path);
            tryInvalidCompile(env, path, "on pattern " + TargetEnum.DISCARD_AND_SUPPRESS.getText() + "[SupportIdEventA] select * from AWindow",
                expected + "[on pattern @DiscardPartialsOnMatch @SuppressOverlappingMatches [SupportIdEventA] select * from AWindow]");

            env.undeployAll();
        }
    }

    private static class PatternCombination implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (boolean testsoda : new boolean[]{false, true}) {
                for (TargetEnum target : TargetEnum.values()) {
                    tryAssertionTargetCurrentMatch(env, testsoda, target);
                    tryAssertionTargetNextMatch(env, testsoda, target);
                }
            }

            // test order-by
            String epl = "@name('s0') select * from pattern @DiscardPartialsOnMatch [every a=SupportIdEventA -> SupportIdEventB] order by a.id desc";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportIdEventA("A1", null, null));
            env.sendEventBean(new SupportIdEventA("A2", null, null));
            env.sendEventBean(new SupportIdEventB("B1", null));
            EventBean[] events = env.listener("s0").getAndResetLastNewData();
            EPAssertionUtil.assertPropsPerRow(events, "a.id".split(","), new Object[][]{{"A2"}, {"A1"}});

            env.undeployAll();
        }
    }

    private static class PatternFollowedByOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            runFollowedByOp(env, milestone, "every a1=SupportIdEventA -> a2=SupportIdEventA", false);
            runFollowedByOp(env, milestone, "every a1=SupportIdEventA -> a2=SupportIdEventA", true);
            runFollowedByOp(env, milestone, "every a1=SupportIdEventA -[10]> a2=SupportIdEventA", false);
            runFollowedByOp(env, milestone, "every a1=SupportIdEventA -[10]> a2=SupportIdEventA", true);
        }
    }

    private static class PatternMatchUntilOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionMatchUntilBoundOp(env, milestone, true);
            tryAssertionMatchUntilBoundOp(env, milestone, false);
            tryAssertionMatchUntilWChildMatcher(env, milestone, true);
            tryAssertionMatchUntilWChildMatcher(env, milestone, false);
            tryAssertionMatchUntilRangeOpWTime(env, milestone);    // with time
        }
    }

    private static class PatternObserverOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a.id,b.id".split(",");
            sendTime(env, 0);

            String epl = "@name('s0') select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + " [" +
                "every a=SupportIdEventA -> b=SupportIdEventB -> timer:interval(a.mysec)]";
            env.compileDeploy(epl).addListener("s0");

            sendAEvent(env, "A1", 5);    // 5 seconds for this one

            env.milestone(0);

            sendAEvent(env, "A2", 1);    // 1 seconds for this one
            sendBEvent(env, "B1");

            env.milestone(1);

            sendTime(env, 1000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1"});

            env.milestone(2);

            sendTime(env, 5000);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternAndOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            runAndWAndState(env, milestone, true);
            runAndWAndState(env, milestone, false);

            runAndWChild(env, milestone, true);
            runAndWChild(env, milestone, false);
        }
    }

    private static class PatternNotOpNotImpacted implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a.id".split(",");
            sendTime(env, 0);

            String epl = "@name('s0') select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + " [" +
                "every a=SupportIdEventA -> timer:interval(a.mysec) and not (SupportIdEventB -> SupportIdEventC)]";
            env.compileDeploy(epl).addListener("s0");

            sendAEvent(env, "A1", 5); // 5 sec
            sendAEvent(env, "A2", 1); // 1 sec
            sendBEvent(env, "B1");
            sendTime(env, 1000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2"});

            sendCEvent(env, "C1", null);
            sendTime(env, 5000);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternGuardOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            runGuardOpBeginState(env, milestone, true);
            runGuardOpBeginState(env, milestone, false);
            runGuardOpChildState(env, milestone, true);
            runGuardOpChildState(env, milestone, false);
        }
    }

    private static class PatternOrOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a.id,b.id,c.id".split(",");
            sendTime(env, 0);

            String epl = "@name('s0') select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + " [" +
                "every a=SupportIdEventA -> (b=SupportIdEventB -> c=SupportIdEventC(pc=a.pa)) or timer:interval(1000)]";
            env.compileDeploy(epl).addListener("s0");

            sendAEvent(env, "A1", "x");
            sendAEvent(env, "A2", "y");

            env.milestone(0);

            sendBEvent(env, "B1");

            env.milestone(1);

            sendCEvent(env, "C1", "y");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

            env.milestone(2);

            sendCEvent(env, "C1", "x");
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternEveryOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            tryAssertionEveryBeginState(env, milestone, "");
            tryAssertionEveryBeginState(env, milestone, "-distinct(id)");
            tryAssertionEveryBeginState(env, milestone, "-distinct(id, 10 seconds)");

            tryAssertionEveryChildState(env, milestone, "", true);
            tryAssertionEveryChildState(env, milestone, "", false);
            tryAssertionEveryChildState(env, milestone, "-distinct(id)", true);
            tryAssertionEveryChildState(env, milestone, "-distinct(id)", false);
            tryAssertionEveryChildState(env, milestone, "-distinct(id, 10 seconds)", true);
            tryAssertionEveryChildState(env, milestone, "-distinct(id, 10 seconds)", false);
        }
    }

    private static void tryAssertionEveryChildState(RegressionEnvironment env, AtomicInteger milestone, String everySuffix, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");

        String epl = "@name('s0') select * from pattern " +
            (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
            "every a=SupportIdEventA-> every" + everySuffix + " (b=SupportIdEventB -> c=SupportIdEventC(pc=a.pa))]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "A1", "x");
        sendAEvent(env, "A2", "y");

        env.milestoneInc(milestone);

        sendBEvent(env, "B1");

        env.milestoneInc(milestone);

        sendCEvent(env, "C1", "y");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        env.milestoneInc(milestone);

        sendCEvent(env, "C2", "x");
        if (matchDiscard) {
            assertFalse(env.listener("s0").isInvoked());
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        env.undeployAll();
    }

    private static void tryAssertionEveryBeginState(RegressionEnvironment env, AtomicInteger milestone, String distinct) {
        String[] fields = "a.id,b.id".split(",");

        String epl = "@name('s0') select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + "[" +
            "every a=SupportIdEventA-> every" + distinct + " b=SupportIdEventB]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "A1");

        env.milestoneInc(milestone);

        sendBEvent(env, "B1");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1"});

        env.milestoneInc(milestone);

        sendBEvent(env, "B2");
        assertFalse(env.listener("s0").isInvoked());

        sendAEvent(env, "A2");

        env.milestoneInc(milestone);

        sendBEvent(env, "B3");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B3"});

        env.milestoneInc(milestone);

        sendBEvent(env, "B4");
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private static void runFollowedByOp(RegressionEnvironment env, AtomicInteger milestone, String pattern, boolean matchDiscard) {
        String[] fields = "a1.id,a2.id".split(",");

        String epl = "@name('s0') select * from pattern "
            + (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + "[" + pattern + "]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "E1");
        sendAEvent(env, "E2");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2"});

        env.milestoneInc(milestone);

        sendAEvent(env, "E3");
        if (matchDiscard) {
            assertFalse(env.listener("s0").isInvoked());
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E3"});
        }

        env.milestoneInc(milestone);

        sendAEvent(env, "E4");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", "E4"});

        env.milestoneInc(milestone);

        sendAEvent(env, "E5");
        if (matchDiscard) {
            assertFalse(env.listener("s0").isInvoked());
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", "E5"});
        }

        sendAEvent(env, "E6");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5", "E6"});

        env.undeployAll();
    }

    private static void tryAssertionTargetNextMatch(RegressionEnvironment env, boolean testSoda, TargetEnum target) {

        String[] fields = "a.id,b.id,c.id".split(",");
        String epl = "@name('s0') select * from pattern " + target.getText() + "[every a=SupportIdEventA -> b=SupportIdEventB -> c=SupportIdEventC(pc=a.pa)]";
        env.compileDeploy(testSoda, epl).addListener("s0");

        sendAEvent(env, "A1", "x");
        sendAEvent(env, "A2", "y");
        sendBEvent(env, "B1");
        sendCEvent(env, "C1", "y");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent(env, "C2", "x");
        if (target == TargetEnum.SUPPRESS_ONLY || target == TargetEnum.NONE) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        } else {
            assertFalse(env.listener("s0").isInvoked());
        }

        env.undeployAll();
    }

    private static void tryAssertionMatchUntilBoundOp(RegressionEnvironment env, AtomicInteger milestone, boolean matchDiscard) {
        String[] fields = "a.id,b[0].id,b[1].id".split(",");

        String epl = "@name('s0') select * from pattern " +
            (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + "[" +
            "every a=SupportIdEventA-> [2] b=SupportIdEventB(pb in (a.pa, '-'))]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "A1", "x");
        sendAEvent(env, "A2", "y");

        env.milestoneInc(milestone);

        sendBEvent(env, "B1", "-");  // applies to both matches

        env.milestoneInc(milestone);

        sendBEvent(env, "B2", "y");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "B2"});

        env.milestoneInc(milestone);

        sendBEvent(env, "B3", "x");
        if (matchDiscard) {
            assertFalse(env.listener("s0").isInvoked());
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "B3"});
        }
        env.undeployAll();
    }

    private static void tryAssertionMatchUntilWChildMatcher(RegressionEnvironment env, AtomicInteger milestone, boolean matchDiscard) {
        String[] fields = "a.id,b[0].id,c[0].id".split(",");

        String epl = "@name('s0') select * from pattern " +
            (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
            "every a=SupportIdEventA-> [1] (b=SupportIdEventB -> c=SupportIdEventC(pc=a.pa))]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "A1", "x");
        sendAEvent(env, "A2", "y");

        env.milestoneInc(milestone);

        sendBEvent(env, "B1");

        env.milestoneInc(milestone);

        sendCEvent(env, "C1", "y");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        env.milestoneInc(milestone);

        sendCEvent(env, "C2", "x");
        if (matchDiscard) {
            assertFalse(env.listener("s0").isInvoked());
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }

        env.undeployAll();
    }

    private static void tryAssertionMatchUntilRangeOpWTime(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "a1.id,aarr[0].id".split(",");
        sendTime(env, 0);

        String epl = "@name('s0') select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + "[" +
            "every a1=SupportIdEventA -> ([:100] aarr=SupportIdEventA until (timer:interval(10 sec) and not b=SupportIdEventB))]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "A1");

        env.milestoneInc(milestone);

        sendTime(env, 1000);
        sendAEvent(env, "A2");
        sendTime(env, 10000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2"});

        env.milestoneInc(milestone);

        sendTime(env, 11000);
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private static void tryAssertionTargetCurrentMatch(RegressionEnvironment env, boolean testSoda, TargetEnum target) {

        String[] fields = "a1.id,aarr[0].id,b.id".split(",");
        String epl = "@name('s0') select * from pattern " + target.getText() + "[every a1=SupportIdEventA -> [:10] aarr=SupportIdEventA until b=SupportIdEventB]";
        env.compileDeploy(testSoda, epl).addListener("s0");

        sendAEvent(env, "A1");
        sendAEvent(env, "A2");
        sendBEvent(env, "B1");

        if (target == TargetEnum.SUPPRESS_ONLY || target == TargetEnum.DISCARD_AND_SUPPRESS) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2", "B1"});
        } else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"A1", "A2", "B1"}, {"A2", null, "B1"}});
        }

        env.undeployAll();
    }

    private static void runAndWAndState(RegressionEnvironment env, AtomicInteger milestone, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");

        String epl = "@name('s0') select * from pattern " +
            (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
            "every a=SupportIdEventA-> b=SupportIdEventB and c=SupportIdEventC(pc=a.pa)]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "A1", "x");
        sendAEvent(env, "A2", "y");

        env.milestoneInc(milestone);

        sendBEvent(env, "B1");

        env.milestoneInc(milestone);

        sendCEvent(env, "C1", "y");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        env.milestoneInc(milestone);

        sendCEvent(env, "C2", "x");
        if (matchDiscard) {
            assertFalse(env.listener("s0").isInvoked());
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        env.undeployAll();
    }

    private static void runAndWChild(RegressionEnvironment env, AtomicInteger milestone, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");

        String epl = "@name('s0') select * from pattern " +
            (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
            "every a=SupportIdEventA-> SupportIdEventD and (b=SupportIdEventB -> c=SupportIdEventC(pc=a.pa))]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "A1", "x");

        env.milestoneInc(milestone);

        sendAEvent(env, "A2", "y");
        sendDEvent(env, "D1");

        env.milestoneInc(milestone);

        sendBEvent(env, "B1");

        env.milestoneInc(milestone);

        sendCEvent(env, "C1", "y");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        env.milestoneInc(milestone);

        sendCEvent(env, "C2", "x");
        if (matchDiscard) {
            assertFalse(env.listener("s0").isInvoked());
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }

        env.undeployAll();
    }

    private static void runGuardOpBeginState(RegressionEnvironment env, AtomicInteger milestone, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");

        String epl = "@name('s0') select * from pattern " +
            (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + "[" +
            "every a=SupportIdEventA-> b=SupportIdEventB -> c=SupportIdEventC(pc=a.pa) where timer:within(1)]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "A1", "x");
        sendAEvent(env, "A2", "y");

        env.milestoneInc(milestone);

        sendBEvent(env, "B1");

        env.milestoneInc(milestone);

        sendCEvent(env, "C1", "y");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        env.milestoneInc(milestone);

        sendCEvent(env, "C2", "x");
        if (matchDiscard) {
            assertFalse(env.listener("s0").isInvoked());
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        env.undeployAll();
    }

    private static void runGuardOpChildState(RegressionEnvironment env, AtomicInteger milestone, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");

        String epl = "@name('s0') select * from pattern " +
            (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
            "every a=SupportIdEventA-> (b=SupportIdEventB -> c=SupportIdEventC(pc=a.pa)) where timer:within(1)]";
        env.compileDeploy(epl).addListener("s0");

        sendAEvent(env, "A1", "x");
        sendAEvent(env, "A2", "y");
        sendBEvent(env, "B1");

        env.milestoneInc(milestone);

        sendCEvent(env, "C1", "y");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        env.milestoneInc(milestone);

        sendCEvent(env, "C2", "x");
        if (matchDiscard) {
            assertFalse(env.listener("s0").isInvoked());
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }

        env.undeployAll();
    }

    private static void sendTime(RegressionEnvironment env, long msec) {
        env.advanceTime(msec);
    }

    private static void sendAEvent(RegressionEnvironment env, String id) {
        sendAEvent(id, null, null, env);
    }

    private static void sendAEvent(RegressionEnvironment env, String id, String pa) {
        sendAEvent(id, pa, null, env);
    }

    private static void sendDEvent(RegressionEnvironment env, String id) {
        env.sendEventBean(new SupportIdEventD(id));
    }

    private static void sendAEvent(RegressionEnvironment env, String id, int mysec) {
        sendAEvent(id, null, mysec, env);
    }

    private static void sendAEvent(String id, String pa, Integer mysec, RegressionEnvironment env) {
        env.sendEventBean(new SupportIdEventA(id, pa, mysec));
    }

    private static void sendBEvent(RegressionEnvironment env, String id) {
        sendBEvent(env, id, null);
    }

    private static void sendBEvent(RegressionEnvironment env, String id, String pb) {
        env.sendEventBean(new SupportIdEventB(id, pb));
    }

    private static void sendCEvent(RegressionEnvironment env, String id, String pc) {
        env.sendEventBean(new SupportIdEventC(id, pc));
    }

    private enum TargetEnum {
        DISCARD_ONLY("@DiscardPartialsOnMatch "),
        DISCARD_AND_SUPPRESS("@DiscardPartialsOnMatch @SuppressOverlappingMatches "),
        SUPPRESS_ONLY("@SuppressOverlappingMatches "),
        NONE("");

        private String text;

        private TargetEnum(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}
