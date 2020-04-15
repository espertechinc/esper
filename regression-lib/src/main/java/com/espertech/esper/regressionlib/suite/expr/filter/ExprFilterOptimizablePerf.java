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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeploymentService;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExprFilterOptimizablePerf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterOptimizablePerfOr());
        executions.add(new ExprFilterOptimizablePerfEqualsWithFunc());
        executions.add(new ExprFilterOptimizablePerfTrueWithFunc());
        executions.add(new ExprFilterOptimizablePerfEqualsDeclaredExpr());
        executions.add(new ExprFilterOptimizablePerfTrueDeclaredExpr());
        return executions;
    }

    private static class ExprFilterOptimizablePerfEqualsWithFunc implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            // func(...) = value
            tryOptimizableEquals(env, new RegressionPath(), "select * from SupportBean(libSplit(theString) = !NUM!)", 10);
        }
    }

    private static class ExprFilterOptimizablePerfTrueWithFunc implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            // func(...) implied true
            tryOptimizableBoolean(env, new RegressionPath(), "select * from SupportBean(libE1True(theString))");
        }
    }

    private static class ExprFilterOptimizablePerfEqualsDeclaredExpr implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            // declared expression (...) = value
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create-expr') create expression thesplit {theString => libSplit(theString)}", path).addListener("create-expr");
            tryOptimizableEquals(env, path, "select * from SupportBean(thesplit(*) = !NUM!)", 10);
        }
    }

    private static class ExprFilterOptimizablePerfTrueDeclaredExpr implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            // declared expression (...) implied true
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create-expr') create expression theE1Test {theString => libE1True(theString)}", path).addListener("create-expr");
            tryOptimizableBoolean(env, path, "select * from SupportBean(theE1Test(*))");
        }
    }

    private static class ExprFilterOptimizablePerfOr implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            SupportUpdateListener listener = new SupportUpdateListener();
            for (int i = 0; i < 100; i++) {
                String epl = "@name('s" + i + "') select * from SupportBean(theString = '" + i + "' or intPrimitive=" + i + ")";
                EPCompiled compiled = env.compile(epl);
                env.deploy(compiled).statement("s" + i).addListener(listener);
            }

            long start = System.nanoTime();
            // System.out.println("Starting " + DateTime.print(new Date()));
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean("100", 1));
                assertTrue(listener.isInvoked());
                listener.reset();
            }
            // System.out.println("Ending " + DateTime.print(new Date()));
            double delta = (System.nanoTime() - start) / 1000d / 1000d;
            // System.out.println("Delta=" + (delta + " msec"));
            assertTrue(delta < 500);

            env.undeployAll();
        }
    }

    private static void tryOptimizableEquals(RegressionEnvironment env, RegressionPath path, String epl, int numStatements) {
        // test function returns lookup value and "equals"
        for (int i = 0; i < numStatements; i++) {
            String text = "@name('s" + i + "') " + epl.replace("!NUM!", Integer.toString(i));
            env.compileDeploy(text, path).addListener("s" + i);
        }
        env.milestone(0);

        long startTime = System.currentTimeMillis();
        SupportStaticMethodLib.resetCountInvoked();
        int loops = 1000;
        for (int i = 0; i < loops; i++) {
            env.sendEventBean(new SupportBean("E_" + i % numStatements, 0));
            SupportListener listener = env.listener("s" + i % numStatements);
            assertTrue(listener.getAndClearIsInvoked());
        }
        long delta = System.currentTimeMillis() - startTime;
        assertEquals(loops, SupportStaticMethodLib.getCountInvoked());

        assertTrue("Delta is " + delta, delta < 1000);
        env.undeployAll();
    }

    private static void tryOptimizableBoolean(RegressionEnvironment env, RegressionPath path, String epl) {

        // test function returns lookup value and "equals"
        int count = 10;
        for (int i = 0; i < count; i++) {
            EPCompiled compiled = env.compile("@name('s" + i + "')" + epl, path);
            EPDeploymentService admin = env.runtime().getDeploymentService();
            try {
                admin.deploy(compiled);
            } catch (EPDeployException ex) {
                ex.printStackTrace();
                fail();
            }
        }

        env.milestone(0);

        SupportUpdateListener listener = new SupportUpdateListener();
        for (int i = 0; i < 10; i++) {
            env.statement("s" + i).addListener(listener);
        }

        long startTime = System.currentTimeMillis();
        SupportStaticMethodLib.resetCountInvoked();
        int loops = 10000;
        for (int i = 0; i < loops; i++) {
            String key = "E_" + i % 100;
            env.sendEventBean(new SupportBean(key, 0));
            if (key.equals("E_1")) {
                assertEquals(count, listener.getNewDataList().size());
                listener.reset();
            } else {
                assertFalse(listener.isInvoked());
            }
        }
        long delta = System.currentTimeMillis() - startTime;
        assertEquals(loops, SupportStaticMethodLib.getCountInvoked());

        assertTrue("Delta is " + delta, delta < 1000);
        env.undeployAll();
    }
}
