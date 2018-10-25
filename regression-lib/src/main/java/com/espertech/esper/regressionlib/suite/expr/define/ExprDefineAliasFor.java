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
package com.espertech.esper.regressionlib.suite.expr.define;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class ExprDefineAliasFor {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprDefineContextPartition());
        execs.add(new ExprDefineDocSamples());
        execs.add(new ExprDefineNestedAlias());
        execs.add(new ExprDefineAliasAggregation());
        execs.add(new ExprDefineGlobalAliasAndSODA());
        execs.add(new ExprDefineInvalid());
        return execs;
    }

    private static class ExprDefineContextPartition implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create expression the_expr alias for {theString='a' and intPrimitive=1};\n" +
                "create context the_context start @now end after 10 minutes;\n" +
                "@name('s0') context the_context select * from SupportBean(the_expr)\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("a", 1));
            assertTrue(env.listener("s0").getIsInvokedAndReset());

            env.sendEventBean(new SupportBean("b", 1));
            assertFalse(env.listener("s0").getIsInvokedAndReset());

            env.undeployAll();
        }
    }

    private static class ExprDefineDocSamples implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SampleEvent()", path);
            env.compileDeploy("expression twoPI alias for {Math.PI * 2}\n" +
                "select twoPI from SampleEvent", path);

            env.compileDeploy("create schema EnterRoomEvent()", path);
            env.compileDeploy("expression countPeople alias for {count(*)} \n" +
                "select countPeople from EnterRoomEvent#time(10 seconds) having countPeople > 10", path);

            env.undeployAll();
        }
    }

    private static class ExprDefineNestedAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create expression F1 alias for {10}", path);
            env.compileDeploy("create expression F2 alias for {20}", path);
            env.compileDeploy("create expression F3 alias for {F1+F2}", path);
            env.compileDeploy("@name('s0') select F3 as c0 from SupportBean", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{30});

            env.undeployAll();
        }
    }

    private static class ExprDefineAliasAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') @Audit expression total alias for {sum(intPrimitive)} " +
                "select total, total+1 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "total,total+1".split(",");
            for (String field : fields) {
                assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(field));
            }

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 11});

            env.undeployAll();
        }
    }

    private static class ExprDefineGlobalAliasAndSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplDeclare = "create expression myaliastwo alias for {2}";
            env.compileDeploy(eplDeclare, path);

            env.compileDeploy("create expression myalias alias for {1}", path);
            env.compileDeploy("@name('s0') select myaliastwo from SupportBean(intPrimitive = myalias)", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("myaliastwo"));

            env.undeployAll();
        }
    }

    private static class ExprDefineInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "expression total alias for {sum(xxx)} select total+1 from SupportBean",
                "Failed to validate select-clause expression 'total+1': Error validating expression alias 'total': Failed to validate alias expression body expression 'sum(xxx)': Property named 'xxx' is not valid in any stream [expression total alias for {sum(xxx)} select total+1 from SupportBean]");
            tryInvalidCompile(env, "expression total xxx for {1} select total+1 from SupportBean",
                "For expression alias 'total' expecting 'alias' keyword but received 'xxx' [expression total xxx for {1} select total+1 from SupportBean]");
            tryInvalidCompile(env, "expression total(a) alias for {1} select total+1 from SupportBean",
                "For expression alias 'total' expecting no parameters but received 'a' [expression total(a) alias for {1} select total+1 from SupportBean]");
            tryInvalidCompile(env, "expression total alias for {a -> 1} select total+1 from SupportBean",
                "For expression alias 'total' expecting an expression without parameters but received 'a ->' [expression total alias for {a -> 1} select total+1 from SupportBean]");
            tryInvalidCompile(env, "expression total alias for ['some text'] select total+1 from SupportBean",
                "For expression alias 'total' expecting an expression but received a script [expression total alias for ['some text'] select total+1 from SupportBean]");
        }
    }
}
