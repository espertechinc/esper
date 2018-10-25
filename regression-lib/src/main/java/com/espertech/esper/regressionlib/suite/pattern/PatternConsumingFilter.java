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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class PatternConsumingFilter {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternFollowedBy());
        execs.add(new PatternAnd());
        execs.add(new PatternFilterAndSceneTwo());
        execs.add(new PatternOr());
        execs.add(new PatternInvalid());
        return execs;
    }

    private static class PatternFollowedBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a,b".split(",");
            String pattern = "@name('s0') select a.theString as a, b.theString as b from pattern[every a=SupportBean -> b=SupportBean@consume]";
            env.compileDeploy(pattern).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2"});

            env.sendEventBean(new SupportBean("E3", 0));
            env.sendEventBean(new SupportBean("E4", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", "E4"});

            env.sendEventBean(new SupportBean("E5", 0));
            env.sendEventBean(new SupportBean("E6", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5", "E6"});

            env.undeployAll();
        }
    }

    private static class PatternAnd implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a,b".split(",");
            String pattern = "@name('s0') select a.theString as a, b.theString as b from pattern[every (a=SupportBean and b=SupportBean)]";
            env.compileDeploy(pattern).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1"});
            env.undeployAll();

            pattern = "@name('s0') select a.theString as a, b.theString as b from pattern [every (a=SupportBean and b=SupportBean(intPrimitive=10)@consume(2))]";
            env.compileDeploy(pattern).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E1"});

            env.sendEventBean(new SupportBean("E3", 1));
            env.sendEventBean(new SupportBean("E4", 1));
            env.sendEventBean(new SupportBean("E5", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", "E5"});
            env.undeployAll();

            // test SODA
            env.eplToModelCompileDeploy(pattern).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E1"});

            env.undeployAll();
        }
    }

    public static class PatternFilterAndSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Name('A') select a.theString as a, b.theString as b from pattern[a=SupportBean and b=SupportBean(theString='A')@consume]";
            env.compileDeploy(epl).addListener("A");

            String[] fields = new String[]{"a", "b"};
            env.sendEventBean(new SupportBean("A", 10));
            assertFalse(env.listener("A").isInvoked());

            env.sendEventBean(new SupportBean("X", 10));
            EPAssertionUtil.assertProps(env.listener("A").assertOneGetNew(), fields, new Object[]{"X", "A"});

            env.undeployAll();
        }
    }

    private static class PatternOr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a,b".split(",");
            tryAssertion(env, fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean or b=SupportBean] order by a asc",
                new Object[][]{{null, "E1"}, {"E1", null}});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean@consume(1) or every b=SupportBean@consume(1)] order by a asc",
                new Object[][]{{null, "E1"}, {"E1", null}});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean@consume(2) or b=SupportBean@consume(1)] order by a asc",
                new Object[]{"E1", null});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean@consume(1) or b=SupportBean@consume(2)] order by a asc",
                new Object[]{null, "E1"});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean or b=SupportBean@consume(2)] order by a asc",
                new Object[]{null, "E1"});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean@consume(1) or b=SupportBean] order by a asc",
                new Object[]{"E1", null});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean(intPrimitive=11)@consume(1) or b=SupportBean] order by a asc",
                new Object[]{null, "E1"});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean(intPrimitive=10)@consume(1) or b=SupportBean] order by a asc",
                new Object[]{"E1", null});

            fields = "a,b,c".split(",");
            tryAssertion(env, fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(1) or b=SupportBean@consume(2) or c=SupportBean@consume(3)] order by a,b,c",
                new Object[][]{{null, null, "E1"}});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(1) or every b=SupportBean@consume(2) or every c=SupportBean@consume(2)] order by a,b,c",
                new Object[][]{{null, null, "E1"}, {null, "E1", null}});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(2) or every b=SupportBean@consume(2) or every c=SupportBean@consume(2)] order by a,b,c",
                new Object[][]{{null, null, "E1"}, {null, "E1", null}, {"E1", null, null}});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(2) or every b=SupportBean@consume(2) or every c=SupportBean@consume(1)] order by a,b,c",
                new Object[][]{{null, "E1", null}, {"E1", null, null}});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(2) or every b=SupportBean@consume(1) or every c=SupportBean@consume(2)] order by a,b,c",
                new Object[][]{{null, null, "E1"}, {"E1", null, null}});

            tryAssertion(env, fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(0) or every b=SupportBean or every c=SupportBean] order by a,b,c",
                new Object[][]{{null, null, "E1"}, {null, "E1", null}, {"E1", null, null}});
        }
    }

    private static class PatternInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern[every a=SupportBean@consume()]",
                "Incorrect syntax near ')' expecting any of the following tokens {IntegerLiteral, FloatingPointLiteral} but found a closing parenthesis ')' at line 1 column 50, please check the filter specification within the pattern expression within the from clause [select * from pattern[every a=SupportBean@consume()]]");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern[every a=SupportBean@consume(-1)]",
                "Incorrect syntax near '-' expecting any of the following tokens {IntegerLiteral, FloatingPointLiteral} but found a minus '-' at line 1 column 50, please check the filter specification within the pattern expression within the from clause [select * from pattern[every a=SupportBean@consume(-1)]]");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern[every a=SupportBean@xx]",
                "Unexpected pattern filter @ annotation, expecting 'consume' but received 'xx' [select * from pattern[every a=SupportBean@xx]]");
        }
    }

    private static void tryAssertion(RegressionEnvironment env, String[] fields, String pattern, Object expected) {

        env.compileDeploy("@name('s0') " + pattern).addListener("s0");
        env.sendEventBean(new SupportBean("E1", 10));

        if (expected instanceof Object[][]) {
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, (Object[][]) expected);
        } else {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, (Object[]) expected);
        }
        env.undeployAll();
    }
}


