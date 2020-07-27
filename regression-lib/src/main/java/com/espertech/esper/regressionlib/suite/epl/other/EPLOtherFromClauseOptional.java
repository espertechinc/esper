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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.context.SupportSelectorById;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertPropsPerRow;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidFAFCompile;
import static org.junit.Assert.*;

public class EPLOtherFromClauseOptional {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherFromOptionalContext(false));
        execs.add(new EPLOtherFromOptionalContext(true));
        execs.add(new EPLOtherFromOptionalNoContext());
        execs.add(new EPLOtherFromOptionalFAFNoContext());
        execs.add(new EPLOtherFromOptionalFAFContext());
        execs.add(new EPLOtherFromOptionalInvalid());
        return execs;
    }

    private static class EPLOtherFromOptionalContext implements RegressionExecution {
        private final boolean soda;

        public EPLOtherFromOptionalContext(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyContext initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id=s0.id)", path);

            String eplOnInit = "@name('s0') context MyContext select context.s0 as ctxs0";
            env.compileDeploy(soda, eplOnInit, path).addListener("s0");

            String eplOnTerm = "@name('s1') context MyContext select context.s0 as ctxs0 output when terminated";
            env.compileDeploy(soda, eplOnTerm, path).addListener("s1");

            SupportBean_S0 s0A = new SupportBean_S0(10, "A");
            env.sendEventBean(s0A);
            assertSame(s0A, env.listener("s0").assertOneGetNewAndReset().get("ctxs0"));
            assertEquals(s0A, env.iterator("s0").next().get("ctxs0"));

            env.milestone(0);

            SupportBean_S0 s0B = new SupportBean_S0(20, "B");
            env.sendEventBean(s0B);
            assertSame(s0B, env.listener("s0").assertOneGetNewAndReset().get("ctxs0"));
            assertIterator(env, "s0", s0A, s0B);
            assertIterator(env, "s1", s0A, s0B);

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(10, "A"));
            assertSame(s0A, env.listener("s1").assertOneGetNewAndReset().get("ctxs0"));
            assertIterator(env, "s0", s0B);
            assertIterator(env, "s1", s0B);

            env.milestone(2);

            env.sendEventBean(new SupportBean_S1(20, "A"));
            assertSame(s0B, env.listener("s1").assertOneGetNewAndReset().get("ctxs0"));
            assertIterator(env, "s0");
            assertIterator(env, "s1");

            env.undeployAll();
        }
    }

    private static class EPLOtherFromOptionalFAFNoContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.advanceTime(1000);

            String eplObjects = "@public create variable string MYVAR = 'abc';\n" +
                    "@public create window MyWindow#keepall as SupportBean;\n" +
                    "on SupportBean merge MyWindow insert select *;\n" +
                    "@public create table MyTable(field int);\n" +
                    "on SupportBean merge MyTable insert select intPrimitive as field;\n";
            env.compileDeploy(eplObjects, path);
            env.sendEventBean(new SupportBean("E1", 1));

            runSelectFAFSimpleCol(env, path, 1, "1");
            runSelectFAFSimpleCol(env, path, 1000L, "current_timestamp()");
            runSelectFAFSimpleCol(env, path, "abc", "MYVAR");
            runSelectFAFSimpleCol(env, path, 1, "sum(1)");
            runSelectFAFSimpleCol(env, path, 1L, "(select count(*) from MyWindow)");
            runSelectFAFSimpleCol(env, path, 1L, "(select count(*) from MyTable)");
            runSelectFAFSimpleCol(env, path, 1, "MyTable.field");

            runSelectFAF(env, path, null, "select 1 as value where 'a'='b'");
            runSelectFAF(env, path, 1, "select 1 as value where 1-0=1");
            runSelectFAF(env, path, null, "select 1 as value having 'a'='b'");

            String eplScript = "expression string one() ['x']\n select one() as value";
            runSelectFAF(env, path, "x", eplScript);

            String eplInlinedClass = "inlined_class \"\"\"\n" +
                    "  public class Helper {\n" +
                    "    public static String doit() { return \"y\";}\n" +
                    "  }\n" +
                    "\"\"\"\n select Helper.doit() as value";
            runSelectFAF(env, path, "y", eplInlinedClass);

            env.undeployAll();
        }
    }

    private static class EPLOtherFromOptionalNoContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select 1 as value");
            assertEquals(1, env.iterator("s0").next().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLOtherFromOptionalInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String context = "create context MyContext initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id=s0.id);";
            env.compileDeploy(context, path);

            // subselect needs from clause
            tryInvalidCompile(env, "select (select 1)", "Incorrect syntax near ')'");

            // wildcard not allowed
            tryInvalidCompile(env, "select *", "Wildcard cannot be used when the from-clause is not provided");
            tryInvalidFAFCompile(env, path, "select *", "Wildcard cannot be used when the from-clause is not provided");

            // context requires a single selector
            EPCompiled compiled = env.compileFAF("context MyContext select context.s0.p00 as id", path);
            try {
                env.runtime().getFireAndForgetService().executeQuery(compiled, new ContextPartitionSelector[2]);
                fail();
            } catch (IllegalArgumentException ex) {
                assertEquals("Fire-and-forget queries without a from-clause allow only a single context partition selector", ex.getMessage());
            }

            // context + order-by not allowed
            tryInvalidFAFCompile(env, path, "context MyContext select context.s0.p00 as p00 order by p00 desc",
                    "Fire-and-forget queries without a from-clause and with context do not allow order-by");

            env.undeployAll();
        }
    }

    private static class EPLOtherFromOptionalFAFContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create context MyContext initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id=s0.id);\n" +
                    "context MyContext select count(*) from SupportBean;\n";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean_S0(10, "A", "x"));
            env.sendEventBean(new SupportBean_S0(20, "B", "x"));
            String eplFAF = "context MyContext select context.s0.p00 as id";
            EPCompiled compiled = env.compileFAF(eplFAF, path);
            assertPropsPerRow(env.runtime().getFireAndForgetService().executeQuery(compiled).getArray(), "id".split(","), new Object[][]{{"A"}, {"B"}});

            // context partition selector
            ContextPartitionSelector selector = new SupportSelectorById(1);
            assertPropsPerRow(env.runtime().getFireAndForgetService().executeQuery(compiled, new ContextPartitionSelector[]{selector}).getArray(), "id".split(","), new Object[][]{{"B"}});

            // SODA
            EPStatementObjectModel model = env.eplToModel(eplFAF);
            assertEquals(eplFAF, model.toEPL());
            compiled = env.compileFAF(model, path);
            assertPropsPerRow(env.runtime().getFireAndForgetService().executeQuery(compiled).getArray(), "id".split(","), new Object[][]{{"A"}, {"B"}});

            // distinct
            String eplFAFDistint = "context MyContext select distinct context.s0.p01 as p01";
            EPFireAndForgetQueryResult result = env.compileExecuteFAF(eplFAFDistint, path);
            assertPropsPerRow(result.getArray(), "p01".split(","), new Object[][]{{"x"}});

            // where-clause and having-clause
            runSelectFAF(env, path, null, "context MyContext select 1 as value where 'a'='b'");
            runSelectFAF(env, path, "A", "context MyContext select context.s0.p00 as value where context.s0.id=10");
            runSelectFAF(env, path, "A", "context MyContext select context.s0.p00 as value having context.s0.id=10");

            env.undeployAll();
        }
    }

    private static void runSelectFAFSimpleCol(RegressionEnvironment env, RegressionPath path, Object expected, String col) {
        runSelectFAF(env, path, expected, "select " + col + " as value");
    }

    private static void runSelectFAF(RegressionEnvironment env, RegressionPath path, Object expected, String epl) {
        EventBean[] result = env.compileExecuteFAF(epl, path).getArray();
        if (expected == null) {
            assertEquals(0, result == null ? 0 : result.length);
        } else {
            assertEquals(expected, result[0].get("value"));
        }
    }

    private static void assertIterator(RegressionEnvironment env, String name, SupportBean_S0... s0) {
        Iterator<EventBean> it = env.iterator(name);
        for (int i = 0; i < s0.length; i++) {
            assertTrue(it.hasNext());
            assertEquals(s0[i], it.next().get("ctxs0"));
        }
        assertFalse(it.hasNext());
    }
}
