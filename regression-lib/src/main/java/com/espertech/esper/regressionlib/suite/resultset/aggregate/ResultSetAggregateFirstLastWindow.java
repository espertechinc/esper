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
package com.espertech.esper.regressionlib.suite.resultset.aggregate;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ResultSetAggregateFirstLastWindow {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateStar());
        execs.add(new ResultSetAggregateUnboundedSimple());
        execs.add(new ResultSetAggregateUnboundedStream());
        execs.add(new ResultSetAggregateWindowedUnGrouped());
        execs.add(new ResultSetAggregateWindowedGrouped());
        execs.add(new ResultSetAggregateFirstLastIndexed());
        execs.add(new ResultSetAggregatePrevNthIndexedFirstLast());
        execs.add(new ResultSetAggregateInvalid());
        execs.add(new ResultSetAggregateSubquery());
        execs.add(new ResultSetAggregateMethodAndAccessTogether());
        execs.add(new ResultSetAggregateTypeAndColNameAndEquivalency());
        execs.add(new ResultSetAggregateJoin2Access());
        execs.add(new ResultSetAggregateOuterJoin1Access());
        execs.add(new ResultSetAggregateBatchWindow());
        execs.add(new ResultSetAggregateBatchWindowGrouped());
        execs.add(new ResultSetAggregateFirstLastWindowNoGroup());
        execs.add(new ResultSetAggregateFirstLastWindowGroup());
        execs.add(new ResultSetAggregateWindowAndSumWGroup());
        execs.add(new ResultSetAggregateOutputRateLimiting());
        execs.add(new ResultSetAggregateOnDelete());
        execs.add(new ResultSetAggregateLastMaxMixedOnSelect());
        execs.add(new ResultSetAggregateLateInitialize());
        execs.add(new ResultSetAggregateMixedNamedWindow());
        execs.add(new ResultSetAggregateNoParamChainedAndProperty());
        execs.add(new ResultSetAggregateOnDemandQuery());
        return execs;
    }

    public static class ResultSetAggregateFirstLastWindowGroup implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@Name('s0') select theString, " +
                "first(theString) as firststring, " +
                "last(theString) as laststring, " +
                "first(intPrimitive) as firstint, " +
                "last(intPrimitive) as lastint, " +
                "window(intPrimitive) as allint " +
                "from SupportBean#length(5) group by theString order by theString asc";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "theString,firststring,firstint,laststring,lastint,allint".split(",");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10, "E1", 10, new int[]{10}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 11, "E2", 11, new int[]{11}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10, "E1", 12, new int[]{10, 12}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 13));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 11, "E2", 13, new int[]{11, 13}});

            env.milestone(3);

            env.sendEventBean(new SupportBean("E2", 14));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 11, "E2", 14, new int[]{11, 13, 14}});

            env.milestone(4);

            env.sendEventBean(new SupportBean("E1", 15));  // push out E1/10
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 12, "E1", 15, new int[]{12, 15}});

            env.milestone(5);

            env.sendEventBean(new SupportBean("E1", 16));  // push out E2/11 --> 2 events
            EventBean[] received = env.listener("s0").getAndResetLastNewData();
            EPAssertionUtil.assertPropsPerRow(received, fields,
                new Object[][]{
                    new Object[]{"E1", "E1", 12, "E1", 16, new int[]{12, 15, 16}},
                    new Object[]{"E2", "E2", 13, "E2", 14, new int[]{13, 14}}
                });

            env.undeployAll();
        }
    }

    public static class ResultSetAggregateFirstLastWindowNoGroup implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@Name('s0') select " +
                "first(theString) as firststring, " +
                "last(theString) as laststring, " +
                "first(intPrimitive) as firstint, " +
                "last(intPrimitive) as lastint, " +
                "window(intPrimitive) as allint " +
                "from SupportBean.win:length(2)";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "firststring,firstint,laststring,lastint,allint".split(",");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "E1", 10, new int[]{10}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "E2", 11, new int[]{10, 11}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E3", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 11, "E3", 12, new int[]{11, 12}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E4", 13));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 12, "E4", 13, new int[]{12, 13}});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateNoParamChainedAndProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select first().property as val0, first().myMethod() as val1, window() as val2 from SupportEventPropertyWithMethod#lastevent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportEventPropertyWithMethod("p1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{"p1", "abc"});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateLastMaxMixedOnSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindowOne#keepall as SupportBean;\n" +
                "insert into MyWindowOne select * from SupportBean(theString like 'A%');\n" +
                "@name('s0') on SupportBean(theString like 'B%') select last(mw.intPrimitive) as li, max(mw.intPrimitive) as mi from MyWindowOne mw;";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "li,mi".split(",");

            env.sendEventBean(new SupportBean("A1", 10));
            env.sendEventBean(new SupportBean("B1", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 10});

            env.milestone(0);

            for (int i = 11; i < 20; i++) {
                env.sendEventBean(new SupportBean("A1", i));
                env.sendEventBean(new SupportBean("Bx", -1));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{i, i});
            }

            env.milestone(1);

            env.sendEventBean(new SupportBean("A1", 1));
            env.sendEventBean(new SupportBean("B1", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 19});

            env.sendEventBean(new SupportBean("A1", 2));
            env.sendEventBean(new SupportBean("B1", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, 19});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregatePrevNthIndexedFirstLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "prev(intPrimitive, 0) as p0, " +
                "prev(intPrimitive, 1) as p1, " +
                "prev(intPrimitive, 2) as p2, " +
                "nth(intPrimitive, 0) as n0, " +
                "nth(intPrimitive, 1) as n1, " +
                "nth(intPrimitive, 2) as n2, " +
                "last(intPrimitive, 0) as l1, " +
                "last(intPrimitive, 1) as l2, " +
                "last(intPrimitive, 2) as l3 " +
                "from SupportBean#length(3)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            String[] fields = "p0,p1,p2,n0,n1,n2,l1,l2,l3".split(",");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, null, null, 10, null, null, 10, null, null});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11, 10, null, 11, 10, null, 11, 10, null});

            env.sendEventBean(new SupportBean("E3", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{12, 11, 10, 12, 11, 10, 12, 11, 10});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E4", 13));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{13, 12, 11, 13, 12, 11, 13, 12, 11});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateFirstLastIndexed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') select " +
                "first(intPrimitive, 0) as f0, " +
                "first(intPrimitive, 1) as f1, " +
                "first(intPrimitive, 2) as f2, " +
                "first(intPrimitive, 3) as f3, " +
                "last(intPrimitive, 0) as l0, " +
                "last(intPrimitive, 1) as l1, " +
                "last(intPrimitive, 2) as l2, " +
                "last(intPrimitive, 3) as l3 " +
                "from SupportBean#length(3)";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionFirstLastIndexed(env, milestone);

            // test join
            env.undeployAll();
            epl += ", SupportBean_A#lastevent";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));

            tryAssertionFirstLastIndexed(env, milestone);

            // test variable
            env.undeployAll();

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('var') create variable int indexvar = 2", path);
            epl = "@name('s0') select first(intPrimitive, indexvar) as f0 from SupportBean#keepall";
            env.compileDeploy(epl, path).addListener("s0");

            String[] fields = "f0".split(",");
            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E1", 11));
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean("E1", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{12});

            env.runtime().getVariableService().setVariableValue(env.deploymentId("var"), "indexvar", 0);
            env.sendEventBean(new SupportBean("E1", 13));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10});
            env.undeployAll();

            // test as part of function
            env.compileDeploy("select Math.abs(last(intPrimitive)) from SupportBean").undeployAll();

        }
    }

    private static class ResultSetAggregateInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "select window(distinct intPrimitive) from SupportBean",
                "Incorrect syntax near '(' ('distinct' is a reserved keyword) at line 1 column 13 near reserved keyword 'distinct' [");

            tryInvalidCompile(env, "select window(sa.intPrimitive + sb.intPrimitive) from SupportBean#lastevent sa, SupportBean#lastevent sb",
                "Failed to validate select-clause expression 'window(sa.intPrimitive+sb.intPrimitive)': The 'window' aggregation function requires that any child expressions evaluate properties of the same stream; Use 'firstever' or 'lastever' or 'nth' instead [select window(sa.intPrimitive + sb.intPrimitive) from SupportBean#lastevent sa, SupportBean#lastevent sb]");

            tryInvalidCompile(env, "select last(*) from SupportBean#lastevent sa, SupportBean#lastevent sb",
                "Failed to validate select-clause expression 'last(*)': The 'last' aggregation function requires that in joins or subqueries the stream-wildcard (stream-alias.*) syntax is used instead [select last(*) from SupportBean#lastevent sa, SupportBean#lastevent sb]");

            tryInvalidCompile(env, "select theString, (select first(*) from SupportBean#lastevent sa) from SupportBean#lastevent sb",
                "Failed to plan subquery number 1 querying SupportBean: Failed to validate select-clause expression 'first(*)': The 'first' aggregation function requires that in joins or subqueries the stream-wildcard (stream-alias.*) syntax is used instead [select theString, (select first(*) from SupportBean#lastevent sa) from SupportBean#lastevent sb]");

            tryInvalidCompile(env, "select window(x.*) from SupportBean#lastevent",
                "Failed to validate select-clause expression 'window(x.*)': Stream by name 'x' could not be found among all streams [select window(x.*) from SupportBean#lastevent]");

            tryInvalidCompile(env, "select window(*) from SupportBean x",
                "Failed to validate select-clause expression 'window(*)': The 'window' aggregation function requires that the aggregated events provide a remove stream; Please define a data window onto the stream or use 'firstever', 'lastever' or 'nth' instead [select window(*) from SupportBean x]");
            tryInvalidCompile(env, "select window(x.*) from SupportBean x",
                "Failed to validate select-clause expression 'window(x.*)': The 'window' aggregation function requires that the aggregated events provide a remove stream; Please define a data window onto the stream or use 'firstever', 'lastever' or 'nth' instead [select window(x.*) from SupportBean x]");
            tryInvalidCompile(env, "select window(x.intPrimitive) from SupportBean x",
                "Failed to validate select-clause expression 'window(x.intPrimitive)': The 'window' aggregation function requires that the aggregated events provide a remove stream; Please define a data window onto the stream or use 'firstever', 'lastever' or 'nth' instead [select window(x.intPrimitive) from SupportBean x]");

            tryInvalidCompile(env, "select window(x.intPrimitive, 10) from SupportBean#keepall x",
                "Failed to validate select-clause expression 'window(x.intPrimitive,10)': The 'window' aggregation function does not accept an index expression; Use 'first' or 'last' instead [");

            tryInvalidCompile(env, "select first(x.*, 10d) from SupportBean#lastevent as x",
                "Failed to validate select-clause expression 'first(x.*,10.0)': The 'first' aggregation function requires an index expression that returns an integer value [select first(x.*, 10d) from SupportBean#lastevent as x]");
        }
    }

    private static class ResultSetAggregateSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select id, (select window(sb.*) from SupportBean#length(2) as sb) as w from SupportBean_A";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "id,w".split(",");

            env.sendEventBean(new SupportBean_A("A1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", null});

            env.milestone(0);

            SupportBean beanOne = sendEvent(env, "E1", 0, 1);
            env.sendEventBean(new SupportBean_A("A2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", new Object[]{beanOne}});

            env.milestone(1);

            SupportBean beanTwo = sendEvent(env, "E2", 0, 1);
            env.sendEventBean(new SupportBean_A("A3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A3", new Object[]{beanOne, beanTwo}});

            env.milestone(2);

            SupportBean beanThree = sendEvent(env, "E2", 0, 1);
            env.sendEventBean(new SupportBean_A("A4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A4", new Object[]{beanTwo, beanThree}});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMethodAndAccessTogether implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') select sum(intPrimitive) as si, window(sa.intPrimitive) as wi from SupportBean#length(2) as sa";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "si,wi".split(",");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, intArray(1)});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, intArray(1, 2)});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, intArray(2, 3)});

            env.milestoneInc(milestone);

            env.undeployAll();

            epl = "@name('s0') select sum(intPrimitive) as si, window(sa.intPrimitive) as wi from SupportBean#keepall as sa group by theString";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, intArray(1)});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, intArray(2)});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, intArray(2, 3)});

            env.sendEventBean(new SupportBean("E1", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, intArray(1, 4)});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateOutputRateLimiting implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select sum(intPrimitive) as si, window(sa.intPrimitive) as wi from SupportBean#keepall as sa output every 2 events";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "si,wi".split(",");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {1, intArray(1)},
                {3, intArray(1, 2)},
            });

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", 3));
            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {6, intArray(1, 2, 3)},
                {10, intArray(1, 2, 3, 4)},
            });

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateTypeAndColNameAndEquivalency implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "first(sa.doublePrimitive + sa.intPrimitive), " +
                "first(sa.intPrimitive), " +
                "window(sa.*), " +
                "last(*) from SupportBean#length(2) as sa";
            env.compileDeploy(epl).addListener("s0");

            Object[][] rows = new Object[][]{
                {"first(sa.doublePrimitive+sa.intPrimitive)", Double.class},
                {"first(sa.intPrimitive)", Integer.class},
                {"window(sa.*)", SupportBean[].class},
                {"last(*)", SupportBean.class},
            };
            for (int i = 0; i < rows.length; i++) {
                EventPropertyDescriptor prop = env.statement("s0").getEventType().getPropertyDescriptors()[i];
                assertEquals(rows[i][0], prop.getPropertyName());
                assertEquals(rows[i][1], prop.getPropertyType());
            }

            env.undeployAll();

            epl = "@name('s0') select " +
                "first(sa.doublePrimitive + sa.intPrimitive) as f1, " +
                "first(sa.intPrimitive) as f2, " +
                "window(sa.*) as w1, " +
                "last(*) as l1 " +
                "from SupportBean#length(2) as sa";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionType(env, false);

            env.undeployAll();

            epl = "@name('s0') select " +
                "first(sa.doublePrimitive + sa.intPrimitive) as f1, " +
                "first(sa.intPrimitive) as f2, " +
                "window(sa.*) as w1, " +
                "last(*) as l1 " +
                "from SupportBean#length(2) as sa " +
                "having SupportStaticMethodLib.alwaysTrue({first(sa.doublePrimitive + sa.intPrimitive), " +
                "first(sa.intPrimitive), window(sa.*), last(*)})";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionType(env, true);

            env.undeployAll();
        }

        private void tryAssertionType(RegressionEnvironment env, boolean isCheckStatic) {
            String[] fields = "f1,f2,w1,l1".split(",");
            SupportStaticMethodLib.getInvocations().clear();

            SupportBean beanOne = sendEvent(env, "E1", 10d, 100);
            Object[] expected = new Object[]{110d, 100, new Object[]{beanOne}, beanOne};
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);
            if (isCheckStatic) {
                Object[] parameters = SupportStaticMethodLib.getInvocations().get(0);
                SupportStaticMethodLib.getInvocations().clear();
                EPAssertionUtil.assertEqualsExactOrder(expected, parameters);
            }
        }
    }

    private static class ResultSetAggregateJoin2Access implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "sa.id as ast, " +
                "sb.id as bst, " +
                "first(sa.id) as fas, " +
                "window(sa.id) as was, " +
                "last(sa.id) as las, " +
                "first(sb.id) as fbs, " +
                "window(sb.id) as wbs, " +
                "last(sb.id) as lbs " +
                "from SupportBean_A#length(2) as sa, SupportBean_B#length(2) as sb " +
                "order by ast, bst";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "ast,bst,fas,was,las,fbs,wbs,lbs".split(",");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_B("B1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "A1", split("A1"), "A1", "B1", split("B1"), "B1"});

            env.milestone(0);

            env.sendEventBean(new SupportBean_A("A2"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
                new Object[][]{
                    {"A2", "B1", "A1", split("A1,A2"), "A2", "B1", split("B1"), "B1"}
                });

            env.milestone(1);

            env.sendEventBean(new SupportBean_A("A3"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
                new Object[][]{
                    {"A3", "B1", "A2", split("A2,A3"), "A3", "B1", split("B1"), "B1"}
                });

            env.milestone(2);

            env.sendEventBean(new SupportBean_B("B2"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
                new Object[][]{
                    {"A2", "B2", "A2", split("A2,A3"), "A3", "B1", split("B1,B2"), "B2"},
                    {"A3", "B2", "A2", split("A2,A3"), "A3", "B1", split("B1,B2"), "B2"}
                });

            env.milestone(3);

            env.sendEventBean(new SupportBean_B("B3"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
                new Object[][]{
                    {"A2", "B3", "A2", split("A2,A3"), "A3", "B2", split("B2,B3"), "B3"},
                    {"A3", "B3", "A2", split("A2,A3"), "A3", "B2", split("B2,B3"), "B3"}
                });

            env.milestone(4);

            env.sendEventBean(new SupportBean_A("A4"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
                new Object[][]{
                    {"A4", "B2", "A3", split("A3,A4"), "A4", "B2", split("B2,B3"), "B3"},
                    {"A4", "B3", "A3", split("A3,A4"), "A4", "B2", split("B2,B3"), "B3"}
                });

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateOuterJoin1Access implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "sa.id as aid, " +
                "sb.id as bid, " +
                "first(sb.p10) as fb, " +
                "window(sb.p10) as wb, " +
                "last(sb.p10) as lb " +
                "from SupportBean_S0#keepall as sa " +
                "left outer join " +
                "SupportBean_S1#keepall as sb " +
                "on sa.id = sb.id";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "aid,bid,fb,wb,lb".split(",");

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1, null, null, null, null});

            env.sendEventBean(new SupportBean_S1(1, "A"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1, 1, "A", split("A"), "A"});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(2, "B"));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(2, "A"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{2, 2, "A", split("A,B"), "B"});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(3, "C"));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(3, "C"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{3, 3, "A", split("A,B,C"), "C"});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateBatchWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream " +
                "first(theString) as fs, " +
                "window(theString) as ws, " +
                "last(theString) as ls " +
                "from SupportBean#length_batch(2) as sb";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "fs,ws,ls".split(",");

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{null, null, null});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E1", split("E1,E2"), "E2"});
            env.listener("s0").reset();

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", 0));
            env.sendEventBean(new SupportBean("E4", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E1", split("E1,E2"), "E2"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E3", split("E3,E4"), "E4"});
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean("E5", 0));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E6", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E3", split("E3,E4"), "E4"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E5", split("E5,E6"), "E6"});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateBatchWindowGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "theString, " +
                "first(intPrimitive) as fi, " +
                "window(intPrimitive) as wi, " +
                "last(intPrimitive) as li " +
                "from SupportBean#length_batch(6) as sb group by theString order by theString asc";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "theString,fi,wi,li".split(",");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 11));
            env.sendEventBean(new SupportBean("E3", 30));

            env.sendEventBean(new SupportBean("E3", 31));
            assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(new SupportBean("E1", 12));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, intArray(10, 11, 12), 12},
                {"E2", 20, intArray(20), 20},
                {"E3", 30, intArray(30, 31), 31}
            });

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 13));
            env.sendEventBean(new SupportBean("E1", 14));
            env.sendEventBean(new SupportBean("E1", 15));
            env.sendEventBean(new SupportBean("E1", 16));
            env.sendEventBean(new SupportBean("E1", 17));
            env.sendEventBean(new SupportBean("E1", 18));
            EventBean[] result = env.listener("s0").getAndResetLastNewData();
            EPAssertionUtil.assertPropsPerRow(result, fields, new Object[][]{
                {"E1", 13, intArray(13, 14, 15, 16, 17, 18), 18},
                {"E2", null, null, null},
                {"E3", null, null, null}
            });

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateLateInitialize implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindowTwo#keepall as select * from SupportBean;\n" +
                "insert into MyWindowTwo select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));

            String[] fields = "firststring,windowstring,laststring".split(",");
            epl = "@name('s0') select " +
                "first(theString) as firststring, " +
                "window(theString) as windowstring, " +
                "last(theString) as laststring " +
                "from MyWindowTwo";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean("E3", 30));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1,E2,E3"), "E3"});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateOnDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "firststring,windowstring,laststring".split(",");
            String epl = "create window MyWindowThree#keepall as select * from SupportBean;\n" +
                "insert into MyWindowThree select * from SupportBean;\n" +
                "on SupportBean_A delete from MyWindowThree where theString = id;\n" +
                "@name('s0') select " +
                "first(theString) as firststring, " +
                "window(theString) as windowstring, " +
                "last(theString) as laststring " +
                "from MyWindowThree";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1"), "E1"});

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1,E2"), "E2"});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", 30));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1,E2,E3"), "E3"});

            env.sendEventBean(new SupportBean_A("E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1,E3"), "E3"});

            env.sendEventBean(new SupportBean_A("E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1"), "E1"});

            env.milestone(1);

            env.sendEventBean(new SupportBean_A("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

            env.sendEventBean(new SupportBean("E4", 40));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", split("E4"), "E4"});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E5", 50));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", split("E4,E5"), "E5"});

            env.sendEventBean(new SupportBean_A("E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5", split("E5"), "E5"});

            env.milestone(3);

            env.sendEventBean(new SupportBean("E6", 60));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5", split("E5,E6"), "E6"});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateOnDemandQuery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindowFour#keepall as select * from SupportBean;\n" +
                "insert into MyWindowFour select * from SupportBean;";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean("E3", 30));
            env.sendEventBean(new SupportBean("E3", 31));
            env.sendEventBean(new SupportBean("E1", 11));
            env.sendEventBean(new SupportBean("E1", 12));

            EPCompiled qc = env.compileFAF("select first(intPrimitive) as f, window(intPrimitive) as w, last(intPrimitive) as l from MyWindowFour as s", path);
            EPFireAndForgetPreparedQuery q = env.runtime().getFireAndForgetService().prepareQuery(qc);
            EPAssertionUtil.assertPropsPerRow(q.execute().getArray(), "f,w,l".split(","),
                new Object[][]{{10, intArray(10, 20, 30, 31, 11, 12), 12}});

            env.sendEventBean(new SupportBean("E1", 13));
            EPAssertionUtil.assertPropsPerRow(q.execute().getArray(), "f,w,l".split(","),
                new Object[][]{{10, intArray(10, 20, 30, 31, 11, 12, 13), 13}});

            qc = env.compileFAF("select theString as s, first(intPrimitive) as f, window(intPrimitive) as w, last(intPrimitive) as l from MyWindowFour as s group by theString order by theString asc", path);
            q = env.runtime().getFireAndForgetService().prepareQuery(qc);
            Object[][] expected = new Object[][]{
                {"E1", 10, intArray(10, 11, 12, 13), 13},
                {"E2", 20, intArray(20), 20},
                {"E3", 30, intArray(30, 31), 31}
            };
            EPAssertionUtil.assertPropsPerRow(q.execute().getArray(), "s,f,w,l".split(","), expected);
            EPAssertionUtil.assertPropsPerRow(q.execute().getArray(), "s,f,w,l".split(","), expected);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateStar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') select " +
                "first(*) as firststar, " +
                "first(sb.*) as firststarsb, " +
                "last(*) as laststar, " +
                "last(sb.*) as laststarsb, " +
                "window(*) as windowstar, " +
                "window(sb.*) as windowstarsb, " +
                "firstever(*) as firsteverstar, " +
                "lastever(*) as lasteverstar " +
                "from SupportBean#length(2) as sb";
            env.compileDeploy(epl).addListener("s0");

            EventPropertyDescriptor[] props = env.statement("s0").getEventType().getPropertyDescriptors();
            for (int i = 0; i < props.length; i++) {
                assertEquals(i == 4 || i == 5 ? SupportBean[].class : SupportBean.class, props[i].getPropertyType());
            }

            tryAssertionStar(env, milestone);
            env.undeployAll();

            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertionStar(env, milestone);

            env.undeployAll();
        }
    }

    public static class ResultSetAggregateUnboundedSimple implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@Name('s0') select first(theString) as c0, last(theString) as c1 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1".split(",");

            env.milestone(0);

            sendSupportBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1"});

            env.milestone(1);

            sendSupportBean(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2"});

            env.milestone(2);

            sendSupportBean(env, "E3");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3"});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateUnboundedStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "first(theString) as f1, " +
                "first(sb.*) as f2, " +
                "first(*) as f3, " +
                "last(theString) as l1, " +
                "last(sb.*) as l2, " +
                "last(*) as l3 " +
                "from SupportBean as sb";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "f1,f2,f3,l1,l2,l3".split(",");

            SupportBean beanOne = sendEvent(env, "E1", 1d, 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", beanOne, beanOne, "E1", beanOne, beanOne});

            env.milestone(0);

            SupportBean beanTwo = sendEvent(env, "E2", 2d, 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", beanOne, beanOne, "E2", beanTwo, beanTwo});

            env.milestone(1);

            SupportBean beanThree = sendEvent(env, "E3", 3d, 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", beanOne, beanOne, "E3", beanThree, beanThree});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateWindowedUnGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') select " +
                "first(theString) as firststring, " +
                "last(theString) as laststring, " +
                "first(intPrimitive) as firstint, " +
                "last(intPrimitive) as lastint, " +
                "window(intPrimitive) as allint " +
                "from SupportBean#length(2)";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionUngrouped(env, milestone);

            env.undeployAll();

            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertionUngrouped(env, milestone);

            env.undeployAll();

            // test null-value provided
            epl = "@name('s0') select window(intBoxed).take(10) from SupportBean#length(2)";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean("E1", 1));

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateWindowedGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') select " +
                "theString, " +
                "first(theString) as firststring, " +
                "last(theString) as laststring, " +
                "first(intPrimitive) as firstint, " +
                "last(intPrimitive) as lastint, " +
                "window(intPrimitive) as allint " +
                "from SupportBean#length(5) " +
                "group by theString order by theString";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionGrouped(env, milestone);

            env.undeployAll();

            // SODA
            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertionGrouped(env, milestone);

            env.undeployAll();

            // test hints
            String newEPL = "@Hint('disable_reclaim_group') " + epl;
            env.compileDeploy(newEPL).addListener("s0");

            tryAssertionGrouped(env, milestone);

            // test hints
            env.undeployAll();
            newEPL = "@Hint('reclaim_group_aged=10,reclaim_group_freq=5') " + epl;
            env.compileDeploy(newEPL).addListener("s0");

            tryAssertionGrouped(env, milestone);

            env.undeployAll();

            // test SODA indexes
            String eplFirstLast = "@name('s0') select " +
                "last(intPrimitive), " +
                "last(intPrimitive,1), " +
                "first(intPrimitive), " +
                "first(intPrimitive,1) " +
                "from SupportBean#length(3)";
            env.eplToModelCompileDeploy(eplFirstLast);
            env.undeployAll();
        }
    }

    public static class ResultSetAggregateMixedNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2".split(",");
            String epl = "create window ABCWin.win:keepall() as SupportBean;\n" +
                "insert into ABCWin select * from SupportBean;\n" +
                "on SupportBean_S0 delete from ABCWin where intPrimitive = id;\n" +
                "@Name('s0') select theString as c0, sum(intPrimitive) as c1, window(intPrimitive) as c2 from ABCWin group by theString;\n";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, new int[]{10}});

            env.milestone(1);

            sendSupportBean(env, "E2", 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 100, new int[]{100}});

            env.milestone(2);

            sendSupportBean_S0(env, 100);    // delete E2 group
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", null, null});

            env.milestone(3);

            sendSupportBean(env, "E1", 11);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10 + 11, new int[]{10, 11}});

            env.milestone(4);

            sendSupportBean_S0(env, 10);    // delete from E1 group
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 11, new int[]{11}});

            env.milestone(5);

            env.milestone(6);  // no change

            sendSupportBean(env, "E2", 101);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 101, new int[]{101}});
            sendSupportBean(env, "E2", 102);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 101 + 102, new int[]{101, 102}});
            sendSupportBean(env, "E1", 12);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 11 + 12, new int[]{11, 12}});

            env.undeployAll();
        }
    }

    public static class ResultSetAggregateWindowAndSumWGroup implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0') select theString as c0, sum(intPrimitive) as c1," +
                "window(intPrimitive*longPrimitive) as c2 from SupportBean#length(3) group by theString order by theString asc";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1", 10, 5);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, new long[]{5 * 10}});

            env.milestone(1);

            sendSupportBean(env, "E2", 100, 20);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 100, new long[]{20 * 100}});

            env.milestone(2);

            sendSupportBean(env, "E1", 15, 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10 + 15, new long[]{5 * 10, 2 * 15}});

            env.milestone(3);

            sendSupportBean(env, "E1", 18, 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 15 + 18, new long[]{2 * 15, 3 * 18}});

            env.milestone(4);

            sendSupportBean(env, "E1", 19, 4); // pushed out E2
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(),
                fields, new Object[][]{{"E1", 15 + 18 + 19, new long[]{2 * 15, 3 * 18, 4 * 19}}, {"E2", null, null}});

            env.milestone(5);

            env.milestone(6);

            sendSupportBean(env, "E1", 17, -1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 18 + 19 + 17, new long[]{3 * 18, 4 * 19, -1 * 17}});
            sendSupportBean(env, "E2", 1, 1000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(),
                fields, new Object[][]{new Object[]{"E1", 19 + 17, new long[]{4 * 19, -1 * 17}}, {"E2", 1, new long[]{1 * 1000}}});

            env.undeployAll();
        }
    }

    private static void tryAssertionGrouped(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "theString,firststring,firstint,laststring,lastint,allint".split(",");

        env.sendEventBean(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10, "E1", 10, new int[]{10}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 11, "E2", 11, new int[]{11}});

        env.sendEventBean(new SupportBean("E1", 12));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10, "E1", 12, new int[]{10, 12}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E2", 13));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 11, "E2", 13, new int[]{11, 13}});

        env.sendEventBean(new SupportBean("E2", 14));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 11, "E2", 14, new int[]{11, 13, 14}});

        env.sendEventBean(new SupportBean("E1", 15));  // push out E1/10
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 12, "E1", 15, new int[]{12, 15}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E1", 16));  // push out E2/11 --> 2 events
        EventBean[] received = env.listener("s0").getAndResetLastNewData();
        EPAssertionUtil.assertPropsPerRow(received, fields,
            new Object[][]{
                new Object[]{"E1", "E1", 12, "E1", 16, new int[]{12, 15, 16}},
                new Object[]{"E2", "E2", 13, "E2", 14, new int[]{13, 14}}
            });
    }

    private static void tryAssertionFirstLastIndexed(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "f0,f1,f2,f3,l0,l1,l2,l3".split(",");
        env.sendEventBean(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, null, null, null, 10, null, null, null});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 11, null, null, 11, 10, null, null});

        env.sendEventBean(new SupportBean("E3", 12));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 11, 12, null, 12, 11, 10, null});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E4", 13));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11, 12, 13, null, 13, 12, 11, null});
    }

    private static void tryAssertionStar(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "firststar,firststarsb,laststar,laststarsb,windowstar,windowstarsb,firsteverstar,lasteverstar".split(",");

        Object beanE1 = new SupportBean("E1", 10);
        env.sendEventBean(beanE1);
        Object[] window = new Object[]{beanE1};
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{beanE1, beanE1, beanE1, beanE1, window, window, beanE1, beanE1});

        env.milestone(0);

        Object beanE2 = new SupportBean("E2", 20);
        env.sendEventBean(beanE2);
        window = new Object[]{beanE1, beanE2};
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{beanE1, beanE1, beanE2, beanE2, window, window, beanE1, beanE2});

        env.milestone(1);

        Object beanE3 = new SupportBean("E3", 30);
        env.sendEventBean(beanE3);
        window = new Object[]{beanE2, beanE3};
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{beanE2, beanE2, beanE3, beanE3, window, window, beanE1, beanE3});

        env.milestone(2);
    }

    private static void tryAssertionUngrouped(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "firststring,firstint,laststring,lastint,allint".split(",");

        env.sendEventBean(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "E1", 10, new int[]{10}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "E2", 11, new int[]{10, 11}});

        env.sendEventBean(new SupportBean("E3", 12));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 11, "E3", 12, new int[]{11, 12}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E4", 13));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 12, "E4", 13, new int[]{12, 13}});
    }

    private static Object split(String s) {
        if (s == null) {
            return new Object[0];
        }
        return s.split(",");
    }

    private static int[] intArray(int... value) {
        if (value == null) {
            return new int[0];
        }
        return value;
    }

    private static SupportBean sendEvent(RegressionEnvironment env, String theString, double doublePrimitive, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(bean);
        return bean;
    }

    private static void sendSupportBean(RegressionEnvironment env, String string, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(string, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String e1) {
        env.sendEventBean(new SupportBean(e1, 0));
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }

    private static void sendSupportBean_S0(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportBean_S0(id));
    }
}
