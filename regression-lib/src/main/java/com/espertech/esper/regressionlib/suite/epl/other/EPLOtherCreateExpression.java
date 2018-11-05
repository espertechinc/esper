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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EPLOtherCreateExpression {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherInvalid());
        execs.add(new EPLOtherParseSpecialAndMixedExprAndScript());
        execs.add(new EPLOtherExprAndScriptLifecycleAndFilter());
        execs.add(new EPLOtherScriptUse());
        execs.add(new EPLOtherExpressionUse());
        return execs;
    }

    private static class EPLOtherInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('s0') create expression E1 {''}", path);
            assertEquals(StatementType.CREATE_EXPRESSION, env.statement("s0").getProperty(StatementProperty.STATEMENTTYPE));
            assertEquals("E1", env.statement("s0").getProperty(StatementProperty.CREATEOBJECTNAME));

            SupportMessageAssertUtil.tryInvalidCompile(env, path, "create expression E1 {''}",
                "Expression 'E1' has already been declared [create expression E1 {''}]");

            env.compileDeploy("create expression int js:abc(p1, p2) [p1*p2]", path);
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "create expression int js:abc(a, a) [p1*p2]",
                "Script 'abc' that takes the same number of parameters has already been declared [create expression int js:abc(a, a) [p1*p2]]");

            env.undeployAll();
        }
    }

    private static class EPLOtherParseSpecialAndMixedExprAndScript implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create expression string js:myscript(p1) [\"--\"+p1+\"--\"]", path);
            env.compileDeploy("create expression myexpr {sb => '--'||theString||'--'}", path);

            // test mapped property syntax
            String eplMapped = "@name('s0') select myscript('x') as c0, myexpr(sb) as c1 from SupportBean as sb";
            env.compileDeploy(eplMapped, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{"--x--", "--E1--"});
            env.undeployModuleContaining("s0");

            // test expression chained syntax
            String eplExpr = "" +
                "create expression scalarfilter {s => " +
                "   strvals.where(y => y != 'E1') " +
                "}";
            env.compileDeploy(eplExpr, path);
            String eplSelect = "@name('s0') select scalarfilter(t).where(x => x != 'E2') as val1 from SupportCollection as t";
            env.compileDeploy(eplSelect, path).addListener("s0");
            assertStatelessStmt(env, "s0", true);
            env.sendEventBean(SupportCollection.makeString("E1,E2,E3,E4"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", "E3", "E4");
            env.undeployAll();

            // test script chained synax
            String eplScript = "create expression " + SupportBean.class.getName() + " js:callIt() [ new " + SupportBean.class.getName() + "('E1', 10); ]";
            env.compileDeploy(eplScript, path);
            env.compileDeploy("@name('s0') select callIt() as val0, callIt().getTheString() as val1 from SupportBean as sb", path).addListener("s0");
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0.theString,val0.intPrimitive,val1".split(","), new Object[]{"E1", 10, "E1"});

            env.undeployAll();
        }
    }

    private static class EPLOtherScriptUse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create expression int js:abc(p1, p2) [p1*p2*10]", path);
            env.compileDeploy("create expression int js:abc(p1) [p1*10]", path);

            String epl = "@name('s0') select abc(intPrimitive, doublePrimitive) as c0, abc(intPrimitive) as c1 from SupportBean";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(makeBean("E1", 10, 3.5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{350, 100});

            env.undeployAll();

            // test SODA
            String eplExpr = "@name('expr') create expression somescript(i1) ['a']";
            EPStatementObjectModel modelExpr = env.eplToModel(eplExpr);
            Assert.assertEquals(eplExpr, modelExpr.toEPL());
            env.compileDeploy(modelExpr, path);
            Assert.assertEquals(eplExpr, env.statement("expr").getProperty(StatementProperty.EPL));

            String eplSelect = "@name('select') select somescript(1) from SupportBean";
            EPStatementObjectModel modelSelect = env.eplToModel(eplSelect);
            Assert.assertEquals(eplSelect, modelSelect.toEPL());
            env.compileDeploy(modelSelect, path);
            Assert.assertEquals(eplSelect, env.statement("select").getProperty(StatementProperty.EPL));

            env.undeployAll();
        }
    }

    private static class EPLOtherExpressionUse implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create expression TwoPi {Math.PI * 2}", path);
            env.compileDeploy("create expression factorPi {sb => Math.PI * intPrimitive}", path);

            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "TwoPi() as c0," +
                "(select TwoPi() from SupportBean_S0#lastevent) as c1," +
                "factorPi(sb) as c2 " +
                "from SupportBean sb";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(10));
            env.sendEventBean(new SupportBean("E1", 3));   // factor is 3
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{Math.PI * 2, Math.PI * 2, Math.PI * 3});

            env.undeployModuleContaining("s0");

            // test local expression override
            env.compileDeploy("@name('s0') expression TwoPi {Math.PI * 10} select TwoPi() as c0 from SupportBean", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{Math.PI * 10});

            // test SODA
            String eplExpr = "@name('expr') create expression JoinMultiplication {(s1,s2) => s1.intPrimitive*s2.id}";
            EPStatementObjectModel modelExpr = env.eplToModel(eplExpr);
            Assert.assertEquals(eplExpr, modelExpr.toEPL());
            env.compileDeploy(modelExpr, path);
            Assert.assertEquals(eplExpr, env.statement("expr").getProperty(StatementProperty.EPL));

            // test SODA and join and 2-stream parameter
            String eplJoin = "@name('join') select JoinMultiplication(sb,s0) from SupportBean#lastevent as sb, SupportBean_S0#lastevent as s0";
            EPStatementObjectModel modelJoin = env.eplToModel(eplJoin);
            Assert.assertEquals(eplJoin, modelJoin.toEPL());
            env.compileDeploy(modelJoin, path);
            Assert.assertEquals(eplJoin, env.statement("join").getProperty(StatementProperty.EPL));
            env.undeployAll();

            // test subquery against named window and table defined in declared expression
            tryAssertionTestExpressionUse(env, true);
            tryAssertionTestExpressionUse(env, false);

            env.undeployAll();
        }

        private static void tryAssertionTestExpressionUse(RegressionEnvironment env, boolean namedWindow) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create expression myexpr {(select intPrimitive from MyInfra)}", path);
            String eplCreate = namedWindow ?
                "create window MyInfra#keepall as SupportBean" :
                "create table MyInfra(theString string, intPrimitive int)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("insert into MyInfra select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("@name('s0') select myexpr() as c0 from SupportBean_S0", path).addListener("s0");
            assertStatelessStmt(env, "s0", false);

            env.sendEventBean(new SupportBean("E1", 100));
            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{100});

            env.undeployAll();
        }
    }

    private static class EPLOtherExprAndScriptLifecycleAndFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // expression assertion
            tryAssertionLifecycleAndFilter(env, "create expression MyFilter {sb => intPrimitive = 1}",
                "select * from SupportBean(MyFilter(sb)) as sb",
                "create expression MyFilter {sb => intPrimitive = 2}");

            // script assertion
            tryAssertionLifecycleAndFilter(env, "create expression boolean js:MyFilter(intPrimitive) [intPrimitive==1]",
                "select * from SupportBean(MyFilter(intPrimitive)) as sb",
                "create expression boolean js:MyFilter(intPrimitive) [intPrimitive==2]");
        }
    }

    private static void tryAssertionLifecycleAndFilter(RegressionEnvironment env, String expressionBefore,
                                                       String selector,
                                                       String expressionAfter) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('expr-one') " + expressionBefore, path);
        env.compileDeploy("@name('s1') " + selector, path).addListener("s1");

        env.sendEventBean(new SupportBean("E1", 0));
        assertFalse(env.listener("s1").getAndClearIsInvoked());
        env.sendEventBean(new SupportBean("E2", 1));
        assertTrue(env.listener("s1").getAndClearIsInvoked());

        SupportListener listenerS1 = env.listener("s1");
        path.clear();
        env.undeployAll();

        env.compileDeploy("@name('expr-two') " + expressionAfter, path);
        env.compileDeploy("@name('s2') " + selector, path).addListener("s2");

        env.sendEventBean(new SupportBean("E3", 0));
        assertFalse(listenerS1.getAndClearIsInvoked() || env.listener("s2").getAndClearIsInvoked());

        env.milestone(0);

        env.sendEventBean(new SupportBean("E4", 1));
        assertFalse(listenerS1.getAndClearIsInvoked());
        assertFalse(env.listener("s2").getAndClearIsInvoked());
        env.sendEventBean(new SupportBean("E4", 2));
        assertFalse(listenerS1.getAndClearIsInvoked());
        assertTrue(env.listener("s2").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static SupportBean makeBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean();
        sb.setIntPrimitive(intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }
}
