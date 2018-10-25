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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.multistmtassert.EPLWithInvokedFlags;
import com.espertech.esper.regressionlib.support.multistmtassert.MultiStmtAssertUtil;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExprFilterInAndBetween {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterInDynamic());
        executions.add(new ExprFilterSimpleIntAndEnumWrite());
        executions.add(new ExprFilterInExpr());
        executions.add(new ExprFilterNotIn());
        executions.add(new ExprFilterInInvalid());
        executions.add(new ExprFilterReuse());
        executions.add(new ExprFilterReuseNot());
        executions.add(new ExprFilterInMultipleNonMatchingFirst());
        executions.add(new ExprFilterInMultipleWithBool());
        return executions;
    }

    public static class ExprFilterInMultipleWithBool implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplOne = "@name('s1') select * from SupportBean(intPrimitive in (0) and theString like 'X%')";
            env.compileDeploy(eplOne).addListener("s1");

            env.milestone(0);

            String eplTwo = "@name('s2') select * from SupportBean(intPrimitive in (0,1) and theString like 'A%')";
            env.compileDeploy(eplTwo).addListener("s2");

            env.milestone(1);

            env.sendEventBean(new SupportBean("A", 1));
            env.listener("s2").assertOneGetNewAndReset();
            assertFalse(env.listener("s1").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterInMultipleNonMatchingFirst implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplNotMatching = "@name('A') select * from SupportBean(intPrimitive in (0,0,1) and theString like 'X%')";
            env.compileDeploy(eplNotMatching).addListener("A");

            String eplMatching = "@name('B') select * from SupportBean(intPrimitive in (0,1) and theString like 'A%')";
            env.compileDeploy(eplMatching).addListener("B");

            env.milestone(0);

            env.sendEventBean(new SupportBean("A", 0));
            env.listener("B").assertOneGetNewAndReset();
            assertFalse(env.listener("A").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterInDynamic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern [a=SupportBeanNumeric -> every b=SupportBean(intPrimitive in (a.intOne, a.intTwo))]";
            env.compileDeployAddListenerMile(epl, "s0", 0);

            sendBeanNumeric(env, 10, 20);
            sendBeanInt(env, 10);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            sendBeanInt(env, 11);
            assertFalse(env.listener("s0").getAndClearIsInvoked());
            sendBeanInt(env, 20);
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();

            epl = "@name('s0') select * from pattern [a=SupportBean_S0 -> every b=SupportBean(theString in (a.p00, a.p01, a.p02))]";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            env.sendEventBean(new SupportBean_S0(1, "a", "b", "c", "d"));
            sendBeanString(env, "a");
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            sendBeanString(env, "x");
            assertFalse(env.listener("s0").getAndClearIsInvoked());
            sendBeanString(env, "b");
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            sendBeanString(env, "c");
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            sendBeanString(env, "d");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterSimpleIntAndEnumWrite implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean(intPrimitive in (1, 10))";
            env.compileDeployAddListenerMile(epl, "s0", 0);

            sendBeanInt(env, 10);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            sendBeanInt(env, 11);
            assertFalse(env.listener("s0").getAndClearIsInvoked());
            sendBeanInt(env, 1);
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();

            // try enum collection with substitution param
            Set<SupportEnum> types = new HashSet<>();
            types.add(SupportEnum.ENUM_VALUE_2);
            EPCompiled compiled = env.compile("@name('s0') select * from SupportBean ev " + "where ev.enumValue in (?::java.util.Collection)");
            env.deploy(compiled, new DeploymentOptions().setStatementSubstitutionParameter(prepared -> prepared.setObject(1, types)));
            env.addListener("s0");

            SupportBean theEvent = new SupportBean();
            theEvent.setEnumValue(SupportEnum.ENUM_VALUE_2);
            env.sendEventBean(theEvent);

            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprFilterInExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            List<EPLWithInvokedFlags> assertions = new ArrayList<>();
            AtomicInteger milestone = new AtomicInteger();

            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString > 'b')", new boolean[]{false, false, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString < 'b')", new boolean[]{true, false, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString >= 'b')", new boolean[]{false, true, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString <= 'b')", new boolean[]{true, true, false, false}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 4, num -> sendBean(env, "theString", new String[]{"a", "b", "c", "d"}[num]), milestone);

            assertions.clear();
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString in ['b':'d'])", new boolean[]{false, true, true, true, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString in ('b':'d'])", new boolean[]{false, false, true, true, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString in ['b':'d'))", new boolean[]{false, true, true, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString in ('b':'d'))", new boolean[]{false, false, true, false, false}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 5, num -> sendBean(env, "theString", new String[]{"a", "b", "c", "d", "e"}[num]), milestone);

            assertions.clear();
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(boolPrimitive in (false))", new boolean[]{false, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(boolPrimitive in (false, false, false))", new boolean[]{false, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(boolPrimitive in (false, true, false))", new boolean[]{true, true}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 2, num -> sendBean(env, "boolPrimitive", new Object[]{true, false}[num]), milestone);

            assertions.clear();
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed in (4, 6, 1))", new boolean[]{false, true, false, false, true, false, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed in (3))", new boolean[]{false, false, false, true, false, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed between 4 and 6)", new boolean[]{false, false, false, false, true, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed between 2 and 1)", new boolean[]{false, true, true, false, false, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed between 4 and -1)", new boolean[]{true, true, true, true, true, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed in [2:4])", new boolean[]{false, false, true, true, true, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed in (2:4])", new boolean[]{false, false, false, true, true, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed in [2:4))", new boolean[]{false, false, true, true, false, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed in (2:4))", new boolean[]{false, false, false, true, false, false, false}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 7, num -> sendBean(env, "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}[num]), milestone);

            assertions.clear();
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(longBoxed in (3))", new boolean[]{false, false, false, true, false, false, false}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 7, num -> sendBean(env, "longBoxed", new Object[]{0L, 1L, 2L, 3L, 4L, 5L, 6L}[num]), milestone);
        }
    }

    private static class ExprFilterNotIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            List<EPLWithInvokedFlags> assertions = new ArrayList<>();
            AtomicInteger milestone = new AtomicInteger();

            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed not between 4 and 6)", new boolean[]{true, true, true, true, false, false, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed not between 2 and 1)", new boolean[]{true, false, false, true, true, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed not between 4 and -1)", new boolean[]{false, false, false, false, false, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed not in [2:4])", new boolean[]{true, true, false, false, false, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed not in (2:4])", new boolean[]{true, true, true, false, false, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed not in [2:4))", new boolean[]{true, true, false, false, true, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed not in (2:4))", new boolean[]{true, true, true, false, true, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed not in (4, 6, 1))", new boolean[]{true, false, true, true, false, true, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(intBoxed not in (3))", new boolean[]{true, true, true, false, true, true, true}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 7, num -> sendBean(env, "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}[num]), milestone);

            assertions.clear();
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString not in ['b':'d'])", new boolean[]{true, false, false, false, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString not in ('b':'d'])", new boolean[]{true, true, false, false, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString not in ['b':'d'))", new boolean[]{true, false, false, true, true}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString not in ('b':'d'))", new boolean[]{true, true, false, true, true}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 5, num -> sendBean(env, "theString", new String[]{"a", "b", "c", "d", "e"}[num]), milestone);

            assertions.clear();
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(theString not in ('a', 'b'))", new boolean[]{false, true, false, true}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 4, num -> sendBean(env, "theString", new String[]{"a", "x", "b", "y"}[num]), milestone);

            assertions.clear();
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(boolPrimitive not in (false))", new boolean[]{true, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(boolPrimitive not in (false, false, false))", new boolean[]{true, false}));
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(boolPrimitive not in (false, true, false))", new boolean[]{false, false}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 2, num -> sendBean(env, "boolPrimitive", new Object[]{true, false}[num]), milestone);

            assertions.clear();
            assertions.add(new EPLWithInvokedFlags("select * from SupportBean(longBoxed not in (3))", new boolean[]{true, true, true, false, true, true, true}));
            MultiStmtAssertUtil.runIsInvokedWithEventSender(env, assertions, 7, num -> sendBean(env, "longBoxed", new Object[]{0L, 1L, 2L, 3L, 4L, 5L, 6L}[num]), milestone);
        }
    }

    private static class ExprFilterInInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // we do not coerce
            tryInvalidFilter(env, "select * from SupportBean(intPrimitive in (1L, 10L))");
            tryInvalidFilter(env, "select * from SupportBean(intPrimitive in (1, 10L))");
            tryInvalidFilter(env, "select * from SupportBean(intPrimitive in (1, 'x'))");

            String expr = "select * from pattern [a=SupportBean -> b=SupportBean(intPrimitive in (a.longPrimitive, a.longBoxed))]";
            tryInvalidFilter(env, expr);
        }
    }

    private static class ExprFilterReuse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expr = "select * from SupportBean(intBoxed in [2:4])";
            tryReuse(env, new String[]{expr, expr}, milestone);

            expr = "select * from SupportBean(intBoxed in (1, 2, 3))";
            tryReuse(env, new String[]{expr, expr}, milestone);

            String exprOne = "select * from SupportBean(intBoxed in (2:3])";
            String exprTwo = "select * from SupportBean(intBoxed in (1:3])";
            tryReuse(env, new String[]{exprOne, exprTwo}, milestone);

            exprOne = "select * from SupportBean(intBoxed in (2, 3, 4))";
            exprTwo = "select * from SupportBean(intBoxed in (1, 3))";
            tryReuse(env, new String[]{exprOne, exprTwo}, milestone);

            exprOne = "select * from SupportBean(intBoxed in (2, 3, 4))";
            exprTwo = "select * from SupportBean(intBoxed in (1, 3))";
            String exprThree = "select * from SupportBean(intBoxed in (8, 3))";
            tryReuse(env, new String[]{exprOne, exprTwo, exprThree}, milestone);

            exprOne = "select * from SupportBean(intBoxed in (3, 1, 3))";
            exprTwo = "select * from SupportBean(intBoxed in (3, 3))";
            exprThree = "select * from SupportBean(intBoxed in (1, 3))";
            tryReuse(env, new String[]{exprOne, exprTwo, exprThree}, milestone);

            exprOne = "select * from SupportBean(boolPrimitive=false, intBoxed in (1, 2, 3))";
            exprTwo = "select * from SupportBean(boolPrimitive=false, intBoxed in (3, 4))";
            exprThree = "select * from SupportBean(boolPrimitive=false, intBoxed in (3))";
            tryReuse(env, new String[]{exprOne, exprTwo, exprThree}, milestone);

            exprOne = "select * from SupportBean(intBoxed in (1, 2, 3), longPrimitive >= 0)";
            exprTwo = "select * from SupportBean(intBoxed in (3, 4), intPrimitive >= 0)";
            exprThree = "select * from SupportBean(intBoxed in (3), bytePrimitive < 1)";
            tryReuse(env, new String[]{exprOne, exprTwo, exprThree}, milestone);
        }
    }

    private static class ExprFilterReuseNot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String expr = "select * from SupportBean(intBoxed not in [1:2])";
            tryReuse(env, new String[]{expr, expr}, milestone);

            String exprOne = "select * from SupportBean(intBoxed in (3, 1, 3))";
            String exprTwo = "select * from SupportBean(intBoxed not in (2, 1))";
            String exprThree = "select * from SupportBean(intBoxed not between 0 and -3)";
            tryReuse(env, new String[]{exprOne, exprTwo, exprThree}, milestone);

            exprOne = "select * from SupportBean(intBoxed not in (1, 4, 5))";
            exprTwo = "select * from SupportBean(intBoxed not in (1, 4, 5))";
            exprThree = "select * from SupportBean(intBoxed not in (4, 5, 1))";
            tryReuse(env, new String[]{exprOne, exprTwo, exprThree}, milestone);

            exprOne = "select * from SupportBean(intBoxed not in (3:4))";
            exprTwo = "select * from SupportBean(intBoxed not in [1:3))";
            exprThree = "select * from SupportBean(intBoxed not in (1,1,1,33))";
            tryReuse(env, new String[]{exprOne, exprTwo, exprThree}, milestone);
        }
    }

    private static void tryReuse(RegressionEnvironment env, String[] statements, AtomicInteger milestone) {
        // create all statements
        for (int i = 0; i < statements.length; i++) {
            env.compileDeploy("@name('s" + i + "')" + statements[i]).addListener("s" + i);
        }
        env.milestone(milestone.getAndIncrement());

        SupportListener[] listeners = new SupportListener[statements.length];
        for (int i = 0; i < statements.length; i++) {
            listeners[i] = env.listener("s" + i);
        }

        // send event, all should receive the event
        sendBean(env, "intBoxed", 3);
        for (int i = 0; i < statements.length; i++) {
            assertTrue(listeners[i].getIsInvokedAndReset());
        }

        // stop first, then second, then third etc statement
        for (int toStop = 0; toStop < statements.length; toStop++) {
            env.undeployModuleContaining("s" + toStop);

            // send event, all remaining statement received it
            sendBean(env, "intBoxed", 3);
            for (int i = 0; i <= toStop; i++) {
                assertFalse(listeners[i].isInvoked());
            }
            for (int i = toStop + 1; i < statements.length; i++) {
                assertTrue(listeners[i].getIsInvokedAndReset());
            }
        }

        // now all statements are stopped, send event and verify no listener received
        sendBean(env, "intBoxed", 3);
        for (int i = 0; i < statements.length; i++) {
            assertFalse(listeners[i].isInvoked());
        }
    }

    private static void sendBeanInt(RegressionEnvironment env, int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        env.sendEventBean(theEvent);
    }

    private static void sendBeanString(RegressionEnvironment env, String value) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(value);
        env.sendEventBean(theEvent);
    }

    private static void sendBeanNumeric(RegressionEnvironment env, int intOne, int intTwo) {
        SupportBeanNumeric num = new SupportBeanNumeric(intOne, intTwo);
        env.sendEventBean(num);
    }

    private static void sendBean(RegressionEnvironment env, String fieldName, Object value) {
        SupportBean theEvent = new SupportBean();
        if (fieldName.equals("theString")) {
            theEvent.setTheString((String) value);
        }
        if (fieldName.equals("boolPrimitive")) {
            theEvent.setBoolPrimitive((Boolean) value);
        }
        if (fieldName.equals("intBoxed")) {
            theEvent.setIntBoxed((Integer) value);
        }
        if (fieldName.equals("longBoxed")) {
            theEvent.setLongBoxed((Long) value);
        }
        env.sendEventBean(theEvent);
    }

    private static void tryInvalidFilter(RegressionEnvironment env, String epl) {
        tryInvalidCompile(env, epl, "skip");
    }
}
