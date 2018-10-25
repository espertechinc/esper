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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.context.ContextPartitionSelectorAll;
import com.espertech.esper.common.client.context.ContextPartitionSelectorCategory;
import com.espertech.esper.common.client.context.InvalidContextPartitionSelector;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.context.SupportSelectorById;
import com.espertech.esper.regressionlib.support.context.SupportSelectorPartitioned;

import java.util.*;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class ContextSelectionAndFireAndForget {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextSelectionAndFireAndForgetInvalid());
        execs.add(new ContextSelectionIterateStatement());
        execs.add(new ContextSelectionAndFireAndForgetNamedWindowQuery());
        execs.add(new ContextSelectionFAFNestedNamedWindowQuery());
        return execs;
    }

    private static class ContextSelectionAndFireAndForgetInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context SegmentedSB as partition by theString from SupportBean", path);
            env.compileDeploy("create context SegmentedS0 as partition by p00 from SupportBean_S0", path);
            env.compileDeploy("context SegmentedSB create window WinSB#keepall as SupportBean", path);
            env.compileDeploy("context SegmentedS0 create window WinS0#keepall as SupportBean_S0", path);
            env.compileDeploy("create window WinS1#keepall as SupportBean_S1", path);

            // when a context is declared, it must be the same context that applies to all named windows
            tryInvalidCompileQuery(env, path, "context SegmentedSB select * from WinSB, WinS0",
                "Joins in runtime queries for context partitions are not supported [context SegmentedSB select * from WinSB, WinS0]");

            // test join
            env.compileDeploy("create context PartitionedByString partition by theString from SupportBean", path);
            env.compileDeploy("context PartitionedByString create window MyWindowOne#keepall as SupportBean", path);

            env.compileDeploy("create context PartitionedByP00 partition by p00 from SupportBean_S0", path);
            env.compileDeploy("context PartitionedByP00 create window MyWindowTwo#keepall as SupportBean_S0", path);

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G2", 11));
            env.sendEventBean(new SupportBean_S0(1, "G2"));
            env.sendEventBean(new SupportBean_S0(2, "G1"));

            tryInvalidCompileQuery(env, path, "select mw1.intPrimitive as c1, mw2.id as c2 from MyWindowOne mw1, MyWindowTwo mw2 where mw1.theString = mw2.p00",
                "Joins against named windows that are under context are not supported");

            env.undeployAll();
        }
    }

    private static class ContextSelectionAndFireAndForgetNamedWindowQuery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context PartitionedByString partition by theString from SupportBean", path);
            env.compileDeploy("context PartitionedByString create window MyWindow#keepall as SupportBean", path);
            env.compileDeploy("insert into MyWindow select * from SupportBean", path);

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 21));

            // test no context
            runQueryAll(env, path, "select sum(intPrimitive) as c1 from MyWindow", "c1", new Object[][]{{51}}, 1);
            runQueryAll(env, path, "select sum(intPrimitive) as c1 from MyWindow where intPrimitive > 15", "c1", new Object[][]{{41}}, 1);
            runQuery(env, path, "select sum(intPrimitive) as c1 from MyWindow", "c1", new Object[][]{{41}}, new ContextPartitionSelector[]{new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"E2"}))});
            runQuery(env, path, "select sum(intPrimitive) as c1 from MyWindow", "c1", new Object[][]{{41}}, new ContextPartitionSelector[]{new SupportSelectorById(Collections.<Integer>singleton(1))});

            // test with context props
            runQueryAll(env, path, "context PartitionedByString select context.key1 as c0, intPrimitive as c1 from MyWindow",
                "c0,c1", new Object[][]{{"E1", 10}, {"E2", 20}, {"E2", 21}}, 1);
            runQueryAll(env, path, "context PartitionedByString select context.key1 as c0, intPrimitive as c1 from MyWindow where intPrimitive > 15",
                "c0,c1", new Object[][]{{"E2", 20}, {"E2", 21}}, 1);

            // test targeted context partition
            runQuery(env, path, "context PartitionedByString select context.key1 as c0, intPrimitive as c1 from MyWindow where intPrimitive > 15",
                "c0,c1", new Object[][]{{"E2", 20}, {"E2", 21}}, new SupportSelectorPartitioned[]{new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"E2"}))});

            EPCompiled compiled = env.compileFAF("context PartitionedByString select * from MyWindow", path);
            try {
                env.runtime().getFireAndForgetService().executeQuery(compiled, new ContextPartitionSelector[]{
                    new ContextPartitionSelectorCategory() {
                        public Set<String> getLabels() {
                            return null;
                        }
                    } });
            } catch (InvalidContextPartitionSelector ex) {
                assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById, ContextPartitionSelectorSegmented] interfaces but received com"));
            }

            env.undeployAll();
        }
    }

    private static class ContextSelectionFAFNestedNamedWindowQuery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NestedContext " +
                "context ACtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id=s0.id), " +
                "context BCtx group by intPrimitive < 0 as grp1, group by intPrimitive = 0 as grp2, group by intPrimitive > 0 as grp3 from SupportBean", path);
            env.compileDeploy("context NestedContext create window MyWindow#keepall as SupportBean", path);
            env.compileDeploy("insert into MyWindow select * from SupportBean", path);

            env.sendEventBean(new SupportBean_S0(1, "S0_1"));
            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(2, "S0_2"));
            env.sendEventBean(new SupportBean("E2", -1));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E3", 5));
            env.sendEventBean(new SupportBean("E1", 2));

            runQueryAll(env, path, "select theString as c1, sum(intPrimitive) as c2 from MyWindow group by theString", "c1,c2", new Object[][]{{"E1", 5}, {"E2", -2}, {"E3", 10}}, 1);
            runQuery(env, path, "select theString as c1, sum(intPrimitive) as c2 from MyWindow group by theString", "c1,c2", new Object[][]{{"E1", 3}, {"E3", 5}},
                new ContextPartitionSelector[]{new SupportSelectorById(Collections.singleton(2))});

            runQuery(env, path, "context NestedContext select context.ACtx.s0.p00 as c1, context.BCtx.label as c2, theString as c3, sum(intPrimitive) as c4 from MyWindow group by theString", "c1,c2,c3,c4", new Object[][]{{"S0_1", "grp3", "E1", 3}, {"S0_1", "grp3", "E3", 5}},
                new ContextPartitionSelector[]{new SupportSelectorById(Collections.singleton(2))});

            env.undeployAll();
        }
    }

    private static class ContextSelectionIterateStatement implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "create context PartitionedByString partition by theString from SupportBean;\n" +
                "@Name('s0') context PartitionedByString select context.key1 as c0, sum(intPrimitive) as c1 from SupportBean#length(5);\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean("E2", 21));

            env.milestone(0);

            Object[][] expectedAll = new Object[][]{{"E1", 10}, {"E2", 41}};
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, expectedAll);

            // test iterator ALL
            ContextPartitionSelector selector = ContextPartitionSelectorAll.INSTANCE;
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(selector), env.statement("s0").safeIterator(selector), fields, expectedAll);

            // test iterator by context partition id
            selector = new SupportSelectorById(new HashSet<Integer>(Arrays.asList(0, 1, 2)));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(selector), env.statement("s0").safeIterator(selector), fields, expectedAll);

            selector = new SupportSelectorById(new HashSet<Integer>(Arrays.asList(1)));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(selector), env.statement("s0").safeIterator(selector), fields, new Object[][]{{"E2", 41}});

            assertFalse(env.statement("s0").iterator(new SupportSelectorById(Collections.<Integer>emptySet())).hasNext());
            assertFalse(env.statement("s0").iterator(new SupportSelectorById(null)).hasNext());

            try {
                env.statement("s0").iterator(null);
                fail();
            } catch (IllegalArgumentException ex) {
                assertEquals(ex.getMessage(), "No selector provided");
            }

            try {
                env.statement("s0").safeIterator(null);
                fail();
            } catch (IllegalArgumentException ex) {
                assertEquals(ex.getMessage(), "No selector provided");
            }

            env.compileDeploy("@name('s2') select * from SupportBean");
            try {
                env.statement("s2").iterator(null);
                fail();
            } catch (UnsupportedOperationException ex) {
                assertEquals(ex.getMessage(), "Iterator with context selector is only supported for statements under context");
            }

            try {
                env.statement("s2").safeIterator(null);
                fail();
            } catch (UnsupportedOperationException ex) {
                assertEquals(ex.getMessage(), "Iterator with context selector is only supported for statements under context");
            }

            env.undeployAll();
        }
    }

    private static void runQueryAll(RegressionEnvironment env, RegressionPath path, String epl, String fields, Object[][] expected, int numStreams) {
        ContextPartitionSelector[] selectors = new ContextPartitionSelector[numStreams];
        for (int i = 0; i < numStreams; i++) {
            selectors[i] = ContextPartitionSelectorAll.INSTANCE;
        }

        runQuery(env, path, epl, fields, expected, selectors);

        // run same query without selector
        EPCompiled compiled = env.compileFAF(epl, path);
        EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(compiled);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields.split(","), expected);
    }

    private static void runQuery(RegressionEnvironment env, RegressionPath path, String epl, String fields, Object[][] expected, ContextPartitionSelector[] selectors) {
        // try FAF without prepare
        EPCompiled compiled = env.compileFAF(epl, path);
        EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(compiled, selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields.split(","), expected);

        // test unparameterized prepare and execute
        EPFireAndForgetPreparedQuery preparedQuery = env.runtime().getFireAndForgetService().prepareQuery(compiled);
        EPFireAndForgetQueryResult resultPrepared = preparedQuery.execute(selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(resultPrepared.getArray(), fields.split(","), expected);

        // test unparameterized prepare and execute
        EPFireAndForgetPreparedQueryParameterized preparedParameterizedQuery = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
        EPFireAndForgetQueryResult resultPreparedParameterized = env.runtime().getFireAndForgetService().executeQuery(preparedParameterizedQuery, selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(resultPreparedParameterized.getArray(), fields.split(","), expected);

        // test SODA prepare and execute
        EPStatementObjectModel model = env.eplToModel(epl);
        EPCompiled compiledFromModel = env.compileFAF(model, path);
        EPFireAndForgetPreparedQuery preparedQueryModel = env.runtime().getFireAndForgetService().prepareQuery(compiledFromModel);
        EPFireAndForgetQueryResult resultPreparedModel = preparedQueryModel.execute(selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(resultPreparedModel.getArray(), fields.split(","), expected);

        // test model query
        result = env.runtime().getFireAndForgetService().executeQuery(compiledFromModel, selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields.split(","), expected);
    }

    private static void tryInvalidRuntimeQuery(RegressionEnvironment env, RegressionPath path, ContextPartitionSelector[] selectors, String epl, String expected) {
        EPCompiled faf = env.compileFAF(epl, path);
        try {
            env.runtime().getFireAndForgetService().executeQuery(faf, selectors);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), expected);
        }
    }

    private static void tryInvalidCompileQuery(RegressionEnvironment env, RegressionPath path, String epl, String expected) {
        SupportMessageAssertUtil.tryInvalidFAFCompile(env, path, epl, expected);
    }
}
