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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;

public class ContextWDeclaredExpression {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextWDeclaredExpressionSimple());
        execs.add(new ContextWDeclaredExpressionAlias());
        execs.add(new ContextWDeclaredExpressionWFilter());
        return execs;
    }

    private static class ContextWDeclaredExpressionSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyCtx as " +
                "group by intPrimitive < 0 as n, " +
                "group by intPrimitive > 0 as p " +
                "from SupportBean", path);
            env.compileDeploy("create expression getLabelOne { context.label }", path);
            env.compileDeploy("create expression getLabelTwo { 'x'||context.label||'x' }", path);

            env.compileDeploy("@name('s0') expression getLabelThree { context.label } " +
                "context MyCtx " +
                "select getLabelOne() as c0, getLabelTwo() as c1, getLabelThree() as c2 from SupportBean", path).addListener("s0");

            tryAssertionExpression(env);

            env.undeployAll();
        }
    }

    private static class ContextWDeclaredExpressionAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyCtx as " +
                "group by intPrimitive < 0 as n, " +
                "group by intPrimitive > 0 as p " +
                "from SupportBean", path);
            env.compileDeploy("create expression getLabelOne alias for { context.label }", path);
            env.compileDeploy("create expression getLabelTwo alias for { 'x'||context.label||'x' }", path);

            env.compileDeploy("@name('s0') expression getLabelThree alias for { context.label } " +
                "context MyCtx " +
                "select getLabelOne as c0, getLabelTwo as c1, getLabelThree as c2 from SupportBean", path).addListener("s0");

            tryAssertionExpression(env);

            env.undeployAll();
        }
    }

    private static class ContextWDeclaredExpressionWFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String expr = "create expression THE_EXPRESSION alias for {theString='x'}";
            env.compileDeploy(expr, path);

            String context = "create context context2 initiated @now and pattern[every(SupportBean(THE_EXPRESSION))] terminated after 10 minutes";
            env.compileDeploy(context, path);

            String statement = "@name('s0') context context2 select * from pattern[e1=SupportBean(THE_EXPRESSION) -> e2=SupportBean(theString='y')]";
            env.compileDeploy(statement, path).addListener("s0");

            env.sendEventBean(new SupportBean("x", 1));
            env.sendEventBean(new SupportBean("y", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "e1.intPrimitive,e2.intPrimitive".split(","), new Object[]{1, 2});

            env.undeployAll();
        }
    }

    private static void tryAssertionExpression(RegressionEnvironment env) {
        String[] fields = "c0,c1,c2".split(",");
        env.sendEventBean(new SupportBean("E1", -2));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"n", "xnx", "n"});

        env.sendEventBean(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"p", "xpx", "p"});
    }
}
