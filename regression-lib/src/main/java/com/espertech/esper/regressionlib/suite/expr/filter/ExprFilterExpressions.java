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
package com.espertech.esper.regressionlib.suite.expr.filter;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.multistmtassert.EPLWithInvokedFlags;
import com.espertech.esper.regressionlib.support.multistmtassert.MultiStmtAssertUtil;
import com.espertech.esper.regressionlib.support.multistmtassert.SendAssertPair;
import org.junit.Assert;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ExprFilterExpressions {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterConstant());
        executions.add(new ExprFilterRelationalOpRange());
        executions.add(new ExprFilterMathExpression());
        executions.add(new ExprFilterBooleanExpr());
        executions.add(new ExprFilterIn3ValuesAndNull());
        executions.add(new ExprFilterNotEqualsNull());
        executions.add(new ExprFilterInSet());
        executions.add(new ExprFilterOverInClause());
        executions.add(new ExprFilterNotEqualsConsolidate());
        executions.add(new ExprFilterPromoteIndexToSetNotIn());
        executions.add(new ExprFilterShortCircuitEvalAndOverspecified());
        executions.add(new ExprFilterRelationalOpConstantFirst());
        executions.add(new ExprFilterNullBooleanExpr());
        executions.add(new ExprFilterEnumSyntaxOne());
        executions.add(new ExprFilterEnumSyntaxTwo());
        executions.add(new ExprFilterPatternFunc3Stream());
        executions.add(new ExprFilterPatternFunc());
        executions.add(new ExprFilterStaticFunc());
        executions.add(new ExprFilterWithEqualsSameCompare());
        executions.add(new ExprFilterEqualsSemanticFilter());
        executions.add(new ExprFilterPatternWithExpr());
        executions.add(new ExprFilterExprReversed());
        executions.add(new ExprFilterRewriteWhere());
        executions.add(new ExprFilterNotEqualsOp());
        executions.add(new ExprFilterCombinationEqualsOp());
        executions.add(new ExprFilterEqualsSemanticExpr());
        executions.add(new ExprFilterInvalid());
        executions.add(new ExprFilterInstanceMethodWWildcard());
        return executions;
    }

    private static class ExprFilterRelationalOpRange implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text;
            List<EPLWithInvokedFlags> assertions = new ArrayList<>();
            AtomicInteger milestone = new AtomicInteger();

            text = "select * from SupportBean(intBoxed in [2:3])";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, true, false}));

            text = "select * from SupportBean(intBoxed in [2:3] and intBoxed in [2:3])";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, true, false}));

            text = "select * from SupportBean(intBoxed in [2:3] and intBoxed in [2:2])";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, false, false}));

            text = "select * from SupportBean(intBoxed in [1:10] and intBoxed in [3:2])";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, true, false}));

            text = "select * from SupportBean(intBoxed in [3:3] and intBoxed in [1:3])";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, false, true, false}));

            text = "select * from SupportBean(intBoxed in [3:3] and intBoxed in [1:3] and intBoxed in [4:5])";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, false, false, false}));

            text = "select * from SupportBean(intBoxed not in [3:3] and intBoxed not in [1:3])";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, false, false, true}));

            text = "select * from SupportBean(intBoxed not in (2:4) and intBoxed not in (1:3))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, false, false, true}));

            text = "select * from SupportBean(intBoxed not in [2:4) and intBoxed not in [1:3))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, false, false, true}));

            text = "select * from SupportBean(intBoxed not in (2:4] and intBoxed not in (1:3])";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, false, false, false}));

            text = "select * from SupportBean where intBoxed not in (2:4)";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, true, false, true}));

            text = "select * from SupportBean where intBoxed not in [2:4]";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, false, false, false}));

            text = "select * from SupportBean where intBoxed not in [2:4)";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, false, false, true}));

            text = "select * from SupportBean where intBoxed not in (2:4]";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, true, false, false}));

            text = "select * from SupportBean where intBoxed in (2:4)";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, false, true, false}));

            text = "select * from SupportBean where intBoxed in [2:4]";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, true, true}));

            text = "select * from SupportBean where intBoxed in [2:4)";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, true, false}));

            text = "select * from SupportBean where intBoxed in (2:4]";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, false, true, true}));

            MultiStmtAssertUtil.runIsInvokedWTestdata(env, assertions, new Object[]{1, 2, 3, 4},
                data -> sendBeanIntDouble(env, (Integer) data, 0D), milestone);
        }
    }

    private static class ExprFilterMathExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            List<String> epl = new ArrayList<>();
            AtomicInteger milestone = new AtomicInteger();

            epl.add("select * from SupportBean(intBoxed*doubleBoxed > 20)");
            epl.add("select * from SupportBean(20 < intBoxed*doubleBoxed)");
            epl.add("select * from SupportBean(20/intBoxed < doubleBoxed)");
            epl.add("select * from SupportBean(20/intBoxed/doubleBoxed < 1)");

            MultiStmtAssertUtil.runSendAssertPairs(env, epl, new SendAssertPair[]{
                new SendAssertPair(
                    () -> sendBeanIntDouble(env, 5, 5d),
                    (eventIndex, statementName, failMessage) -> assertTrue(env.listener(statementName).getAndClearIsInvoked())),
                new SendAssertPair(
                    () -> sendBeanIntDouble(env, 5, 4d),
                    (eventIndex, statementName, failMessage) -> assertFalse(env.listener(statementName).getAndClearIsInvoked())),
                new SendAssertPair(
                    () -> sendBeanIntDouble(env, 5, 4.001d),
                    (eventIndex, statementName, failMessage) -> assertTrue(env.listener(statementName).getAndClearIsInvoked()))

            }, milestone);
        }
    }

    private static class ExprFilterBooleanExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportBean(2*intBoxed=doubleBoxed)";
            env.compileDeployAddListenerMile(text, "s0", 0);

            sendBeanIntDouble(env, 20, 50d);
            assertFalse(env.listener("s0").getAndClearIsInvoked());
            sendBeanIntDouble(env, 25, 50d);
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            text = "@name('s1') select * from SupportBean(2*intBoxed=doubleBoxed, theString='s')";
            env.compileDeployAddListenerMile(text, "s1", 1);

            sendBeanIntDoubleString(env, 25, 50d, "s");
            assertTrue(env.listener("s1").getAndClearIsInvoked());
            sendBeanIntDoubleString(env, 25, 50d, "x");
            assertFalse(env.listener("s1").getAndClearIsInvoked());

            env.undeployAll();

            // test priority of equals and boolean
            env.compileDeploy("@name('s0') select * from SupportBean(intPrimitive = 1 or intPrimitive = 2)").addListener("s0");
            env.compileDeploy("@name('s1') select * from SupportBean(intPrimitive = 3, SupportStaticMethodLib.alwaysTrue())").addListener("s1");

            SupportStaticMethodLib.getInvocations().clear();
            env.sendEventBean(new SupportBean("E1", 1));
            assertTrue(SupportStaticMethodLib.getInvocations().isEmpty());

            env.undeployAll();
        }
    }

    private static class ExprFilterIn3ValuesAndNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text;
            AtomicInteger milestone = new AtomicInteger();

            text = "select * from SupportBean(intPrimitive in (intBoxed, doubleBoxed))";
            try3Fields(env, milestone, text, new int[]{1, 1, 1}, new Integer[]{0, 1, 0}, new Double[]{2d, 2d, 1d}, new boolean[]{false, true, true});

            text = "select * from SupportBean(intPrimitive in (intBoxed, " +
                SupportStaticMethodLib.class.getName() + ".minusOne(doubleBoxed)))";
            try3Fields(env, milestone, text, new int[]{1, 1, 1}, new Integer[]{0, 1, 0}, new Double[]{2d, 2d, 1d}, new boolean[]{true, true, false});

            text = "select * from SupportBean(intPrimitive not in (intBoxed, doubleBoxed))";
            try3Fields(env, milestone, text, new int[]{1, 1, 1}, new Integer[]{0, 1, 0}, new Double[]{2d, 2d, 1d}, new boolean[]{true, false, false});

            text = "select * from SupportBean(intBoxed = doubleBoxed)";
            try3Fields(env, milestone, text, new int[]{1, 1, 1}, new Integer[]{null, 1, null}, new Double[]{null, null, 1d}, new boolean[]{false, false, false});

            text = "select * from SupportBean(intBoxed in (doubleBoxed))";
            try3Fields(env, milestone, text, new int[]{1, 1, 1}, new Integer[]{null, 1, null}, new Double[]{null, null, 1d}, new boolean[]{false, false, false});

            text = "select * from SupportBean(intBoxed not in (doubleBoxed))";
            try3Fields(env, milestone, text, new int[]{1, 1, 1}, new Integer[]{null, 1, null}, new Double[]{null, null, 1d}, new boolean[]{false, false, false});

            text = "select * from SupportBean(intBoxed in [doubleBoxed:10))";
            try3Fields(env, milestone, text, new int[]{1, 1, 1}, new Integer[]{null, 1, 2}, new Double[]{null, null, 1d}, new boolean[]{false, false, true});

            text = "select * from SupportBean(intBoxed not in [doubleBoxed:10))";
            try3Fields(env, milestone, text, new int[]{1, 1, 1}, new Integer[]{null, 1, 2}, new Double[]{null, null, 1d}, new boolean[]{false, true, false});
        }
    }

    private static class ExprFilterNotEqualsNull implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] stmts;
            String epl;

            // test equals&where-clause (can be optimized into filter)
            env.compileDeploy("@name('s0') select * from SupportBean where theString != 'A'").addListener("s0");
            env.compileDeploy("@name('s1') select * from SupportBean where theString != 'A' or intPrimitive != 0").addListener("s1");
            env.compileDeploy("@name('s2') select * from SupportBean where theString = 'A'").addListener("s2");
            env.compileDeploy("@name('s3') select * from SupportBean where theString = 'A' or intPrimitive != 0").addListener("s3");
            env.milestoneInc(milestone);
            stmts = "s0,s1,s2,s3".split(",");

            sendSupportBean(env, new SupportBean(null, 0));
            assertListeners(env, stmts, new boolean[]{false, false, false, false});

            sendSupportBean(env, new SupportBean(null, 1));
            assertListeners(env, stmts, new boolean[]{false, true, false, true});

            sendSupportBean(env, new SupportBean("A", 0));
            assertListeners(env, stmts, new boolean[]{false, false, true, true});

            sendSupportBean(env, new SupportBean("A", 1));
            assertListeners(env, stmts, new boolean[]{false, true, true, true});

            sendSupportBean(env, new SupportBean("B", 0));
            assertListeners(env, stmts, new boolean[]{true, true, false, false});

            sendSupportBean(env, new SupportBean("B", 1));
            assertListeners(env, stmts, new boolean[]{true, true, false, true});

            env.undeployAll();

            // test equals&selection
            String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
            epl = "@name('s0') select " +
                "theString != 'A' as val0, " +
                "theString != 'A' or intPrimitive != 0 as val1, " +
                "theString != 'A' and intPrimitive != 0 as val2, " +
                "theString = 'A' as val3," +
                "theString = 'A' or intPrimitive != 0 as val4, " +
                "theString = 'A' and intPrimitive != 0 as val5 from SupportBean";
            env.compileDeploy(epl).addListener("s0").milestoneInc(milestone);

            sendSupportBean(env, new SupportBean(null, 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, false, null, null, false});

            env.milestoneInc(milestone);

            sendSupportBean(env, new SupportBean(null, 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, true, null, null, true, null});

            sendSupportBean(env, new SupportBean("A", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, true, true, false});

            sendSupportBean(env, new SupportBean("A", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false, true, true, true});

            sendSupportBean(env, new SupportBean("B", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, false, false, false});

            sendSupportBean(env, new SupportBean("B", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, false, true, false});

            env.undeployAll().milestoneInc(milestone);

            // test is-and-isnot&where-clause
            env.compileDeploy("@name('s0') select * from SupportBean where theString is null").addListener("s0");
            env.compileDeploy("@name('s1') select * from SupportBean where theString is null or intPrimitive != 0").addListener("s1");
            env.compileDeploy("@name('s2') select * from SupportBean where theString is not null").addListener("s2");
            env.compileDeploy("@name('s3') select * from SupportBean where theString is not null or intPrimitive != 0").addListener("s3");
            env.milestoneInc(milestone);
            stmts = "s0,s1,s2,s3".split(",");

            sendSupportBean(env, new SupportBean(null, 0));
            assertListeners(env, stmts, new boolean[]{true, true, false, false});

            sendSupportBean(env, new SupportBean(null, 1));
            assertListeners(env, stmts, new boolean[]{true, true, false, true});

            sendSupportBean(env, new SupportBean("A", 0));
            assertListeners(env, stmts, new boolean[]{false, false, true, true});

            sendSupportBean(env, new SupportBean("A", 1));
            assertListeners(env, stmts, new boolean[]{false, true, true, true});

            env.undeployAll();

            // test is-and-isnot&selection
            epl = "@name('s0') select " +
                "theString is null as val0, " +
                "theString is null or intPrimitive != 0 as val1, " +
                "theString is null and intPrimitive != 0 as val2, " +
                "theString is not null as val3," +
                "theString is not null or intPrimitive != 0 as val4, " +
                "theString is not null and intPrimitive != 0 as val5 " +
                "from SupportBean";
            env.compileDeploy(epl).addListener("s0").milestoneInc(milestone);

            sendSupportBean(env, new SupportBean(null, 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, false, false, false});

            sendSupportBean(env, new SupportBean(null, 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, false, true, false});

            sendSupportBean(env, new SupportBean("A", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, true, true, false});

            sendSupportBean(env, new SupportBean("A", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false, true, true, true});

            env.undeployAll();

            // filter expression
            env.compileDeploy("@name('s0') select * from SupportBean(theString is null)").addListener("s0");
            env.compileDeploy("@name('s1') select * from SupportBean where theString = null").addListener("s1");
            env.compileDeploy("@name('s2') select * from SupportBean(theString = null)").addListener("s2");
            env.compileDeploy("@name('s3') select * from SupportBean(theString is not null)").addListener("s3");
            env.compileDeploy("@name('s4') select * from SupportBean where theString != null").addListener("s4");
            env.compileDeploy("@name('s5') select * from SupportBean(theString != null)").addListener("s5");
            env.milestoneInc(milestone);
            stmts = "s0,s1,s2,s3,s4,s5".split(",");

            sendSupportBean(env, new SupportBean(null, 0));
            assertListeners(env, stmts, new boolean[]{true, false, false, false, false, false});

            sendSupportBean(env, new SupportBean("A", 0));
            assertListeners(env, stmts, new boolean[]{false, false, false, true, false, false});

            env.undeployAll();

            // select constants
            fields = "val0,val1,val2,val3".split(",");
            env.compileDeploy("@name('s0') select " +
                "2 != null as val0," +
                "null = null as val1," +
                "2 != null or 1 = 2 as val2," +
                "2 != null and 2 = 2 as val3 " +
                "from SupportBean").addListener("s0");
            env.milestoneInc(milestone);

            sendSupportBean(env, new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

            env.undeployAll();

            // test SODA
            epl = "@name('s0') select intBoxed is null, intBoxed is not null, intBoxed=1, intBoxed!=1 from SupportBean";
            env.eplToModelCompileDeploy(epl);
            EPAssertionUtil.assertEqualsExactOrder(new String[]{"intBoxed is null", "intBoxed is not null",
                "intBoxed=1", "intBoxed!=1"}, env.statement("s0").getEventType().getPropertyNames());
            env.undeployAll();
        }
    }

    private static class ExprFilterInSet implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from \n" +
                "pattern [ \n" +
                " every start_load=SupportBeanArrayCollMap \n" +
                " -> \n" +
                " single_load=SupportBean(theString in (start_load.setOfString)) \n" +
                "]";
            env.compileDeployAddListenerMileZero(epl, "s0");

            HashSet<String> setOfString = new HashSet<>();
            setOfString.add("Version1");
            setOfString.add("Version2");
            env.sendEventBean(new SupportBeanArrayCollMap(setOfString));

            env.sendEventBean(new SupportBean("Version1", 0));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterOverInClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[every event1=SupportTradeEvent(userId in ('100','101'),amount>=1000)]";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportTradeEvent(1, "100", 1001));
            Assert.assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("event1.id"));

            String eplTwo = "@name('s1') select * from pattern [every event1=SupportTradeEvent(userId in ('100','101'))]";
            env.compileDeployAddListenerMileZero(eplTwo, "s1");

            env.sendEventBean(new SupportTradeEvent(2, "100", 1001));
            Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("event1.id"));
            Assert.assertEquals(2, env.listener("s1").assertOneGetNewAndReset().get("event1.id"));

            env.undeployAll();
        }
    }

    private static class ExprFilterNotEqualsConsolidate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String[] epl = new String[]{
                "select * from SupportBean(intPrimitive not in (1, 2))",
                "select * from SupportBean(intPrimitive != 1, intPrimitive != 2)",
                "select * from SupportBean(intPrimitive != 1 and intPrimitive != 2)"};
            MultiStmtAssertUtil.runEPL(env, Arrays.asList(epl), new Object[]{0, 1, 2, 3, 4},
                data -> sendSupportBean(env, new SupportBean("", (Integer) data)),
                (eventIndex, eventData, assertionDesc, statementName, failMessage) -> {
                    if (eventData.equals(1) || eventData.equals(2)) {
                        assertFalse(failMessage, env.listener(statementName).isInvoked());
                    } else {
                        assertTrue(failMessage, env.listener(statementName).isInvoked());
                    }
                    env.listener(statementName).reset();
                }, milestone);
        }
    }

    private static class ExprFilterPromoteIndexToSetNotIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplOne = "@name('s0') select * from SupportBean(theString != 'x' and theString != 'y' and doubleBoxed is not null)";
            String eplTwo = "@name('s1') select * from SupportBean(theString != 'x' and theString != 'y' and longBoxed is not null)";

            env.compileDeploy(eplOne).addListener("s0");
            env.compileDeploy(eplTwo).addListener("s1");
            env.milestone(0);

            SupportBean bean = new SupportBean("E1", 0);
            bean.setDoubleBoxed(1d);
            bean.setLongBoxed(1L);
            env.sendEventBean(bean);

            env.listener("s0").assertOneGetNewAndReset();
            env.listener("s1").assertOneGetNewAndReset();

            env.undeployAll();
        }
    }

    private static class ExprFilterShortCircuitEvalAndOverspecified implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportRuntimeExBean(SupportRuntimeExBean.property2 = '4' and SupportRuntimeExBean.property1 = '1')";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportRuntimeExBean());
            assertFalse("Subscriber should not have received result(s)", env.listener("s0").isInvoked());

            env.undeployAll();

            epl = "@name('s0') select * from SupportBean(theString='A' and theString='B')";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            sendSupportBean(env, new SupportBean("A", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterRelationalOpConstantFirst implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            List<EPLWithInvokedFlags> assertions = new ArrayList<>();
            AtomicInteger milestone = new AtomicInteger();

            assertions.add(new EPLWithInvokedFlags("select * from SupportInstanceMethodBean where 4 < x", new boolean[]{false, false, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportInstanceMethodBean where 4 <= x", new boolean[]{false, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportInstanceMethodBean where 4 > x", new boolean[]{true, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportInstanceMethodBean where 4 >= x", new boolean[]{true, true, false}));

            MultiStmtAssertUtil.runIsInvokedWTestdata(env, assertions, new Object[]{3, 4, 5},
                data -> env.sendEventBean(new SupportInstanceMethodBean((Integer) data)), milestone);
        }
    }

    private static class ExprFilterNullBooleanExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern [every event1=SupportTradeEvent(userId like '123%')]";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportTradeEvent(1, null, 1001));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportTradeEvent(2, "1234", 1001));
            Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("event1.id"));

            env.undeployAll();
        }
    }

    private static class ExprFilterConstant implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern [SupportBean(intPrimitive=" + ISupportA.class.getName() + ".VALUE_1)]";
            env.compileDeployAddListenerMileZero(epl, "s0");

            SupportBean theEvent = new SupportBean("e1", 2);
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            theEvent = new SupportBean("e1", 1);
            env.sendEventBean(theEvent);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterEnumSyntaxOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern [SupportBeanWithEnum(supportEnum=" + SupportEnum.class.getName() + ".valueOf('ENUM_VALUE_1'))]";
            env.compileDeployAddListenerMileZero(epl, "s0");

            SupportBeanWithEnum theEvent = new SupportBeanWithEnum("e1", SupportEnum.ENUM_VALUE_2);
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            theEvent = new SupportBeanWithEnum("e1", SupportEnum.ENUM_VALUE_1);
            env.sendEventBean(theEvent);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterEnumSyntaxTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern [SupportBeanWithEnum(supportEnum=" + SupportEnum.class.getName() + ".ENUM_VALUE_2)]";
            env.compileDeployAddListenerMileZero(epl, "s0");

            SupportBeanWithEnum theEvent = new SupportBeanWithEnum("e1", SupportEnum.ENUM_VALUE_2);
            env.sendEventBean(theEvent);
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            theEvent = new SupportBeanWithEnum("e2", SupportEnum.ENUM_VALUE_1);
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();

            // test where clause
            epl = "@name('s0') select * from SupportBeanWithEnum where supportEnum=" + SupportEnum.class.getName() + ".ENUM_VALUE_2";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            theEvent = new SupportBeanWithEnum("e1", SupportEnum.ENUM_VALUE_2);
            env.sendEventBean(theEvent);
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            theEvent = new SupportBeanWithEnum("e2", SupportEnum.ENUM_VALUE_1);
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterPatternFunc3Stream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text;
            AtomicInteger milestone = new AtomicInteger();

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed=a.intBoxed, intBoxed=b.intBoxed and intBoxed != null)]";
            tryPattern3Stream(env, text, milestone, new Integer[]{null, 2, 1, null, 8, 1, 2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 4, -2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 5, null}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{false, false, false, false, false, false, false});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed is a.intBoxed, intBoxed is b.intBoxed and intBoxed is not null)]";
            tryPattern3Stream(env, text, milestone, new Integer[]{null, 2, 1, null, 8, 1, 2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 4, -2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 5, null}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{false, false, true, false, false, false, false});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed=a.intBoxed or intBoxed=b.intBoxed)]";
            tryPattern3Stream(env, text, milestone, new Integer[]{null, 2, 1, null, 8, 1, 2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 4, -2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 5, null}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{false, true, true, true, false, false, false});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed=a.intBoxed, intBoxed=b.intBoxed)]";
            tryPattern3Stream(env, text, milestone, new Integer[]{null, 2, 1, null, 8, 1, 2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 4, -2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 5, null}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{false, false, true, false, false, false, false});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed!=a.intBoxed, intBoxed!=b.intBoxed)]";
            tryPattern3Stream(env, text, milestone, new Integer[]{null, 2, 1, null, 8, 1, 2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 4, -2}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, 3, 1, 8, null, 5, null}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{false, false, false, false, false, true, false});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed!=a.intBoxed)]";
            tryPattern3Stream(env, text, milestone, new Integer[]{2, 8, null, 2, 1, null, 1}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{-2, null, null, 3, 1, 8, 4}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, null, null, 3, 1, 8, 5}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{false, false, false, true, false, false, true});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed is not a.intBoxed)]";
            tryPattern3Stream(env, text, milestone, new Integer[]{2, 8, null, 2, 1, null, 1}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{-2, null, null, 3, 1, 8, 4}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{null, null, null, 3, 1, 8, 5}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{true, true, false, true, false, true, true});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed=a.intBoxed, doubleBoxed=b.doubleBoxed)]";
            tryPattern3Stream(env, text, milestone, new Integer[]{2, 2, 1, 2, 1, 7, 1}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{0, 0, 0, 0, 0, 0, 0}, new Double[]{1d, 2d, 0d, 2d, 0d, 1d, 0d},
                new Integer[]{2, 2, 3, 2, 1, 7, 5}, new Double[]{1d, 1d, 1d, 2d, 1d, 1d, 1d},
                new boolean[]{true, false, false, true, false, true, false});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed in (a.intBoxed, b.intBoxed))]";
            tryPattern3Stream(env, text, milestone, new Integer[]{2, 1, 1, null, 1, null, 1}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{1, 2, 1, null, null, 2, 0}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{2, 2, 3, null, 1, null, null}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{true, true, false, false, true, false, false});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed in [a.intBoxed:b.intBoxed])]";
            tryPattern3Stream(env, text, milestone, new Integer[]{2, 1, 1, null, 1, null, 1}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{1, 2, 1, null, null, 2, 0}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{2, 1, 3, null, 1, null, null}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{true, true, false, false, false, false, false});

            text = "select * from pattern [" +
                "a=SupportBean -> " +
                "b=SupportBean -> " +
                "c=SupportBean(intBoxed not in [a.intBoxed:b.intBoxed])]";
            tryPattern3Stream(env, text, milestone, new Integer[]{2, 1, 1, null, 1, null, 1}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{1, 2, 1, null, null, 2, 0}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new Integer[]{2, 1, 3, null, 1, null, null}, new Double[]{0d, 0d, 0d, 0d, 0d, 0d, 0d},
                new boolean[]{false, false, true, false, false, false, false});
        }
    }

    private static class ExprFilterPatternFunc implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text;
            AtomicInteger milestone = new AtomicInteger();

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(intBoxed = a.intBoxed and doubleBoxed = a.doubleBoxed)]";
            tryPattern(env, text, milestone, new Integer[]{null, 2, 1, null, 8, 1, 2}, new Double[]{2d, 2d, 2d, 1d, 5d, 6d, 7d},
                new Integer[]{null, 3, 1, 8, null, 1, 2}, new Double[]{2d, 3d, 2d, 1d, 5d, 6d, 8d},
                new boolean[]{false, false, true, false, false, true, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(intBoxed is a.intBoxed and doubleBoxed = a.doubleBoxed)]";
            tryPattern(env, text, milestone, new Integer[]{null, 2, 1, null, 8, 1, 2}, new Double[]{2d, 2d, 2d, 1d, 5d, 6d, 7d},
                new Integer[]{null, 3, 1, 8, null, 1, 2}, new Double[]{2d, 3d, 2d, 1d, 5d, 6d, 8d},
                new boolean[]{true, false, true, false, false, true, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(a.doubleBoxed = doubleBoxed)]";
            tryPattern(env, text, milestone, new Integer[]{0, 0}, new Double[]{2d, 2d},
                new Integer[]{0, 0}, new Double[]{2d, 3d},
                new boolean[]{true, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(a.doubleBoxed = b.doubleBoxed)]";
            tryPattern(env, text, milestone, new Integer[]{0, 0}, new Double[]{2d, 2d},
                new Integer[]{0, 0}, new Double[]{2d, 3d},
                new boolean[]{true, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(a.doubleBoxed != doubleBoxed)]";
            tryPattern(env, text, milestone, new Integer[]{0, 0}, new Double[]{2d, 2d},
                new Integer[]{0, 0}, new Double[]{2d, 3d},
                new boolean[]{false, true});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(a.doubleBoxed != b.doubleBoxed)]";
            tryPattern(env, text, milestone, new Integer[]{0, 0}, new Double[]{2d, 2d},
                new Integer[]{0, 0}, new Double[]{2d, 3d},
                new boolean[]{false, true});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed in [a.doubleBoxed:a.intBoxed])]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{false, true, true, true, true, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed in (a.doubleBoxed:a.intBoxed])]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{false, false, true, true, true, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(b.doubleBoxed in (a.doubleBoxed:a.intBoxed))]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{false, false, true, true, false, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed in [a.doubleBoxed:a.intBoxed))]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{false, true, true, true, false, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed not in [a.doubleBoxed:a.intBoxed])]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{true, false, false, false, false, true});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed not in (a.doubleBoxed:a.intBoxed])]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{true, true, false, false, false, true});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(b.doubleBoxed not in (a.doubleBoxed:a.intBoxed))]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{true, true, false, false, true, true});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed not in [a.doubleBoxed:a.intBoxed))]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{true, false, false, false, true, true});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed not in (a.doubleBoxed, a.intBoxed, 9))]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{true, false, true, false, false, true});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed in (a.doubleBoxed, a.intBoxed, 9))]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{false, true, false, true, true, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(b.doubleBoxed in (doubleBoxed, a.intBoxed, 9))]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{true, true, true, true, true, true});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed not in (doubleBoxed, a.intBoxed, 9))]";
            tryPattern(env, text, milestone, new Integer[]{1, 1, 1, 1, 1, 1}, new Double[]{10d, 10d, 10d, 10d, 10d, 10d},
                new Integer[]{0, 0, 0, 0, 0, 0}, new Double[]{0d, 1d, 2d, 9d, 10d, 11d},
                new boolean[]{false, false, false, false, false, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed = " + SupportStaticMethodLib.class.getName() + ".minusOne(a.doubleBoxed))]";
            tryPattern(env, text, milestone, new Integer[]{0, 0, 0}, new Double[]{10d, 10d, 10d},
                new Integer[]{0, 0, 0}, new Double[]{9d, 10d, 11d},
                new boolean[]{true, false, false});

            text = "select * from pattern [a=SupportBean -> b=" +
                SupportBean.class.getSimpleName() + "(doubleBoxed = " + SupportStaticMethodLib.class.getName() + ".minusOne(a.doubleBoxed) or " +
                "doubleBoxed = " + SupportStaticMethodLib.class.getName() + ".minusOne(a.intBoxed))]";
            tryPattern(env, text, milestone, new Integer[]{0, 0, 12}, new Double[]{10d, 10d, 10d},
                new Integer[]{0, 0, 0}, new Double[]{9d, 10d, 11d},
                new boolean[]{true, false, true});
        }
    }

    private static class ExprFilterStaticFunc implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text;
            List<EPLWithInvokedFlags> assertions = new ArrayList<>();
            AtomicInteger milestone = new AtomicInteger();

            text = "select * from SupportBean(" +
                SupportStaticMethodLib.class.getName() + ".isStringEquals('b', theString))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, false}));

            text = "select * from SupportBean(" +
                SupportStaticMethodLib.class.getName() + ".isStringEquals('bx', theString || 'x'))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, false}));

            text = "select * from SupportBean('b'=theString," +
                SupportStaticMethodLib.class.getName() + ".isStringEquals('bx', theString || 'x'))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, false}));

            text = "select * from SupportBean('b'=theString, theString='b', theString != 'a')";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, false}));

            text = "select * from SupportBean(theString != 'a', theString != 'c')";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, false}));

            text = "select * from SupportBean(theString = 'b', theString != 'c')";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, false}));

            text = "select * from SupportBean(theString != 'a' and theString != 'c')";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, false}));

            text = "select * from SupportBean(theString = 'a' and theString = 'c' and " +
                SupportStaticMethodLib.class.getName() + ".isStringEquals('bx', theString || 'x'))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, false, false}));

            MultiStmtAssertUtil.runIsInvokedWTestdata(env, assertions, new Object[]{"a", "b", "c"},
                data -> sendBeanString(env, (String) data), milestone);
        }
    }

    private static class ExprFilterWithEqualsSameCompare implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text;
            List<EPLWithInvokedFlags> assertions = new ArrayList<>();
            AtomicInteger milestone = new AtomicInteger();

            text = "select * from SupportBean(intBoxed=doubleBoxed)";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, false}));

            text = "select * from SupportBean(intBoxed=intBoxed and doubleBoxed=doubleBoxed)";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, true}));

            text = "select * from SupportBean(doubleBoxed=intBoxed)";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, false}));

            text = "select * from SupportBean(doubleBoxed in (intBoxed))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, false}));

            text = "select * from SupportBean(intBoxed in (doubleBoxed))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, false}));

            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 2, num -> sendBeanIntDouble(env, new int[]{1, 1}[num], new double[]{1, 10}[num]), milestone);

            assertions.clear();
            text = "select * from SupportBean(doubleBoxed not in (10, intBoxed))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{false, true, false}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 3, num -> sendBeanIntDouble(env, new int[]{1, 1, 1}[num], new double[]{1, 5, 10}[num]), milestone);

            assertions.clear();
            text = "select * from SupportBean(doubleBoxed in (intBoxed:20))";
            assertions.add(new EPLWithInvokedFlags(text, new boolean[]{true, false, false}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 3, num -> sendBeanIntDouble(env, new int[]{0, 1, 2}[num], new double[]{1, 1, 1}[num]), milestone);
        }
    }

    private static class ExprFilterEqualsSemanticFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBeanComplexProps(nested=nested)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            SupportBeanComplexProps eventOne = SupportBeanComplexProps.makeDefaultBean();
            eventOne.setSimpleProperty("1");

            env.sendEventBean(eventOne);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterPatternWithExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String text = "@name('s0') select * from pattern [every a=SupportBean -> " +
                "b=SupportMarketDataBean(a.longBoxed=volume*2)]";
            tryPatternWithExpr(env, text, milestone);

            text = "@name('s0') select * from pattern [every a=SupportBean -> " +
                "b=SupportMarketDataBean(volume*2=a.longBoxed)]";
            tryPatternWithExpr(env, text, milestone);
        }
    }

    private static class ExprFilterExprReversed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expr = "@name('s0') select * from SupportBean(5 = intBoxed)";
            env.compileDeployAddListenerMileZero(expr, "s0");

            sendBean(env, "intBoxed", 5);
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterRewriteWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryRewriteWhere(env, "", milestone);
            tryRewriteWhere(env, "@Hint('DISABLE_WHEREEXPR_MOVETO_FILTER')", milestone);
            tryRewriteWhereNamedWindow(env);
        }
    }

    private static class ExprFilterNotEqualsOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean(theString != 'a')";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "a");
            assertFalse(env.listener("s0").isInvoked());

            Object theEvent = sendEvent(env, "b");
            assertSame(theEvent, env.listener("s0").getAndResetLastNewData()[0].getUnderlying());

            sendEvent(env, "a");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            sendEvent(env, null);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterCombinationEqualsOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean(theString != 'a', intPrimitive=0)";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "b", 1);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            sendEvent(env, "a", 0);
            assertFalse(env.listener("s0").isInvoked());

            Object theEvent = sendEvent(env, "x", 0);
            assertSame(theEvent, env.listener("s0").getAndResetLastNewData()[0].getUnderlying());

            sendEvent(env, null, 0);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static Object sendEvent(RegressionEnvironment env, String stringValue) {
        return sendEvent(env, stringValue, -1);
    }

    private static Object sendEvent(RegressionEnvironment env, String stringValue, int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(stringValue);
        theEvent.setIntPrimitive(intPrimitive);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static class ExprFilterInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern [every a=SupportBean -> " +
                    "b=SupportMarketDataBean(sum(a.longBoxed) = 2)]",
                "Aggregation functions not allowed within filters [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern [every a=SupportBean(prior(1, a.longBoxed))]",
                "Failed to validate filter expression 'prior(1,a.longBoxed)': Prior function cannot be used in this context [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern [every a=SupportBean(prev(1, a.longBoxed))]",
                "Failed to validate filter expression 'prev(1,a.longBoxed)': Previous function cannot be used in this context [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean(5 - 10)",
                "Filter expression not returning a boolean value: '5-10' [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBeanWithEnum(theString=" + SupportEnum.class.getName() + ".ENUM_VALUE_1)",
                "Failed to validate filter expression 'theString=ENUM_VALUE_1': Implicit conversion from datatype 'SupportEnum' to 'String' is not allowed [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBeanWithEnum(supportEnum=A.b)",
                "Failed to validate filter expression 'supportEnum=A.b': Failed to resolve property 'A.b' to a stream or nested property in a stream [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern [a=SupportBean -> b=" +
                    SupportBean.class.getSimpleName() + "(doubleBoxed not in (doubleBoxed, x.intBoxed, 9))]",
                "Failed to validate filter expression 'doubleBoxed not in (doubleBoxed,x.i...(45 chars)': Failed to find a stream named 'x' (did you mean 'b'?) [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern [a=SupportBean"
                    + " -> b=SupportBean(cluedo.intPrimitive=a.intPrimitive)"
                    + " -> c=SupportBean"
                    + "]",
                "Failed to validate filter expression 'cluedo.intPrimitive=a.intPrimitive': Failed to resolve property 'cluedo.intPrimitive' to a stream or nested property in a stream [");
        }
    }

    private static void tryRewriteWhereNamedWindow(RegressionEnvironment env) {
        String epl = "create window NamedWindowA#length(1) as SupportBean;\n" +
            "select * from NamedWindowA mywindow WHERE (mywindow.theString.trim() is 'abc');\n";
        env.compileDeploy(epl).undeployAll();
    }

    private static class ExprFilterInstanceMethodWWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryFilterInstanceMethod(env, "select * from SupportInstanceMethodBean(s0.myInstanceMethodAlwaysTrue()) as s0", new boolean[]{true, true, true});
            tryFilterInstanceMethod(env, "select * from SupportInstanceMethodBean(s0.myInstanceMethodEventBean(s0, 'x', 1)) as s0", new boolean[]{false, true, false});
            tryFilterInstanceMethod(env, "select * from SupportInstanceMethodBean(s0.myInstanceMethodEventBean(*, 'x', 1)) as s0", new boolean[]{false, true, false});
        }

        private void tryFilterInstanceMethod(RegressionEnvironment env, String epl, boolean[] expected) {
            env.compileDeploy("@name('s0') " + epl).addListener("s0");
            for (int i = 0; i < 3; i++) {
                env.sendEventBean(new SupportInstanceMethodBean(i));
                assertEquals(expected[i], env.listener("s0").getAndClearIsInvoked());
            }
            env.undeployAll();
        }
    }

    private static class ExprFilterEqualsSemanticExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportBeanComplexProps(simpleProperty='1')#keepall as s0" +
                ", SupportBeanComplexProps(simpleProperty='2')#keepall as s1" +
                " where s0.nested = s1.nested";
            env.compileDeploy(text).addListener("s0");

            SupportBeanComplexProps eventOne = SupportBeanComplexProps.makeDefaultBean();
            eventOne.setSimpleProperty("1");

            SupportBeanComplexProps eventTwo = SupportBeanComplexProps.makeDefaultBean();
            eventTwo.setSimpleProperty("2");

            Assert.assertEquals(eventOne.getNested(), eventTwo.getNested());

            env.sendEventBean(eventOne);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(eventTwo);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static void sendBean(RegressionEnvironment env, String fieldName, Object value) {
        SupportBean theEvent = new SupportBean();
        if (fieldName.equals("theString")) {
            theEvent.setTheString((String) value);
        } else if (fieldName.equals("boolPrimitive")) {
            theEvent.setBoolPrimitive((Boolean) value);
        } else if (fieldName.equals("intBoxed")) {
            theEvent.setIntBoxed((Integer) value);
        } else if (fieldName.equals("longBoxed")) {
            theEvent.setLongBoxed((Long) value);
        } else {
            throw new IllegalArgumentException("field name not known");
        }
        env.sendEventBean(theEvent);
    }

    private static void sendBeanLong(RegressionEnvironment env, Long longBoxed) {
        SupportBean theEvent = new SupportBean();
        theEvent.setLongBoxed(longBoxed);
        env.sendEventBean(theEvent);
    }

    private static void sendBeanIntDoubleString(RegressionEnvironment env, Integer intBoxed, Double doubleBoxed, String theString) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntBoxed(intBoxed);
        theEvent.setDoubleBoxed(doubleBoxed);
        theEvent.setTheString(theString);
        env.sendEventBean(theEvent);
    }

    private static void sendBeanIntDouble(RegressionEnvironment env, Integer intBoxed, Double doubleBoxed) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntBoxed(intBoxed);
        theEvent.setDoubleBoxed(doubleBoxed);
        env.sendEventBean(theEvent);
    }

    private static void sendBeanIntIntDouble(RegressionEnvironment env, int intPrimitive, Integer intBoxed, Double doubleBoxed) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        theEvent.setIntBoxed(intBoxed);
        theEvent.setDoubleBoxed(doubleBoxed);
        env.sendEventBean(theEvent);
    }

    private static void sendSupportBean(RegressionEnvironment env, SupportBean sb) {
        env.sendEventBean(sb);
    }

    private static void assertListeners(RegressionEnvironment env, String[] statementNames, boolean[] invoked) {
        for (int i = 0; i < invoked.length; i++) {
            Assert.assertEquals("Failed for statement " + i + " name " + statementNames[i], invoked[i], env.listener(statementNames[i]).getAndClearIsInvoked());
        }
    }

    private static void sendBeanString(RegressionEnvironment env, String theString) {
        SupportBean num = new SupportBean(theString, -1);
        env.sendEventBean(num);
    }

    private static void tryPattern3Stream(RegressionEnvironment env,
                                          String text,
                                          AtomicInteger milestone,
                                          Integer[] intBoxedA,
                                          Double[] doubleBoxedA,
                                          Integer[] intBoxedB,
                                          Double[] doubleBoxedB,
                                          Integer[] intBoxedC,
                                          Double[] doubleBoxedC,
                                          boolean[] expected) {
        assertEquals(intBoxedA.length, doubleBoxedA.length);
        assertEquals(intBoxedB.length, doubleBoxedB.length);
        assertEquals(expected.length, doubleBoxedA.length);
        assertEquals(intBoxedA.length, doubleBoxedB.length);
        assertEquals(intBoxedC.length, doubleBoxedC.length);
        assertEquals(intBoxedB.length, doubleBoxedC.length);

        for (int i = 0; i < intBoxedA.length; i++) {
            env.compileDeployAddListenerMile("@name('s0')" + text, "s0", milestone.getAndIncrement());

            sendBeanIntDouble(env, intBoxedA[i], doubleBoxedA[i]);
            sendBeanIntDouble(env, intBoxedB[i], doubleBoxedB[i]);
            sendBeanIntDouble(env, intBoxedC[i], doubleBoxedC[i]);
            Assert.assertEquals("failed at index " + i, expected[i], env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static void try3Fields(RegressionEnvironment env, AtomicInteger milestone,
                                   String text,
                                   int[] intPrimitive,
                                   Integer[] intBoxed,
                                   Double[] doubleBoxed,
                                   boolean[] expected) {

        env.compileDeployAddListenerMile("@name('s0')" + text, "s0", milestone.incrementAndGet());

        assertEquals(intPrimitive.length, doubleBoxed.length);
        assertEquals(intBoxed.length, doubleBoxed.length);
        assertEquals(expected.length, doubleBoxed.length);
        for (int i = 0; i < intBoxed.length; i++) {
            sendBeanIntIntDouble(env, intPrimitive[i], intBoxed[i], doubleBoxed[i]);
            Assert.assertEquals("failed at index " + i, expected[i], env.listener("s0").getAndClearIsInvoked());
            if (i == 1) {
                env.milestone(milestone.incrementAndGet());
            }
        }

        env.undeployAll();
    }

    private static void tryPattern(RegressionEnvironment env, String text,
                                   AtomicInteger milestone, Integer[] intBoxedA,
                                   Double[] doubleBoxedA,
                                   Integer[] intBoxedB,
                                   Double[] doubleBoxedB,
                                   boolean[] expected) {

        assertEquals(intBoxedA.length, doubleBoxedA.length);
        assertEquals(intBoxedB.length, doubleBoxedB.length);
        assertEquals(expected.length, doubleBoxedA.length);
        assertEquals(intBoxedA.length, doubleBoxedB.length);

        for (int i = 0; i < intBoxedA.length; i++) {
            env.compileDeploy("@name('s0') " + text).addListener("s0");

            sendBeanIntDouble(env, intBoxedA[i], doubleBoxedA[i]);

            env.milestoneInc(milestone);

            sendBeanIntDouble(env, intBoxedB[i], doubleBoxedB[i]);
            Assert.assertEquals("failed at index " + i, expected[i], env.listener("s0").getAndClearIsInvoked());
            env.undeployAll();
        }
    }

    private static void tryPatternWithExpr(RegressionEnvironment env, String text, AtomicInteger milestone) {
        env.compileDeployAddListenerMile(text, "s0", milestone.getAndIncrement());

        sendBeanLong(env, 10L);
        env.sendEventBean(new SupportMarketDataBean("IBM", 0, 0L, ""));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportMarketDataBean("IBM", 0, 5L, ""));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        sendBeanLong(env, 0L);
        env.sendEventBean(new SupportMarketDataBean("IBM", 0, 0L, ""));
        assertTrue(env.listener("s0").getAndClearIsInvoked());
        env.sendEventBean(new SupportMarketDataBean("IBM", 0, 1L, ""));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendBeanLong(env, 20L);
        env.sendEventBean(new SupportMarketDataBean("IBM", 0, 10L, ""));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void tryRewriteWhere(RegressionEnvironment env, String prefix, AtomicInteger milestone) {
        String epl = prefix + " @name('s0') select * from SupportBean as A0 where A0.intPrimitive = 3";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        sendSupportBean(env, new SupportBean("E1", 3));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        sendSupportBean(env, new SupportBean("E2", 4));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

}
