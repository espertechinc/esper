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

import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.context.*;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import junit.framework.TestCase;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ContextCategory {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextCategorySceneOne());
        execs.add(new ContextCategorySceneTwo());
        execs.add(new ContextCategoryWContextProps());
        execs.add(new ContextCategoryBooleanExprFilter());
        execs.add(new ContextCategoryContextPartitionSelection());
        execs.add(new ContextCategorySingleCategorySODAPrior());
        execs.add(new ContextCategoryInvalid());
        execs.add(new ContextCategoryDeclaredExpr(true));
        execs.add(new ContextCategoryDeclaredExpr(false));
        return execs;
    }

    public static class ContextCategorySceneOne implements RegressionExecution {
        private final static String[] FIELDS = "c0,c1".split(",");

        public void run(RegressionEnvironment env) {
            String epl = "@name('context') create context CategoryContext\n" +
                "group theString = 'A' as cat1,\n" +
                "group theString = 'B' as cat2 \n" +
                "from SupportBean;\n" +
                "@name('s0') context CategoryContext select count(*) as c0, context.label as c1 from SupportBean;\n";
            env.compileDeployAddListenerMileZero(epl, "s0");

            String deploymentIdContext = env.deploymentId("context");
            String[] statementNames = env.runtime().getContextPartitionService().getContextStatementNames(deploymentIdContext, "CategoryContext");
            EPAssertionUtil.assertEqualsExactOrder(statementNames, "s0".split(","));
            assertEquals(1, env.runtime().getContextPartitionService().getContextNestingLevel(deploymentIdContext, "CategoryContext"));
            Set<Integer> ids = env.runtime().getContextPartitionService().getContextPartitionIds(deploymentIdContext, "CategoryContext", new ContextPartitionSelectorAll());
            assertEquals(2, env.runtime().getContextPartitionService().getContextPartitionCount(deploymentIdContext, "CategoryContext"));
            EPAssertionUtil.assertEqualsExactOrder(new Integer[]{0, 1}, ids.toArray());

            assertNull(env.statement("context").getProperty(StatementProperty.CONTEXTNAME));
            assertNull(env.statement("context").getProperty(StatementProperty.CONTEXTDEPLOYMENTID));
            assertEquals("CategoryContext", env.statement("s0").getProperty(StatementProperty.CONTEXTNAME));
            assertEquals(env.deploymentId("s0"), env.statement("s0").getProperty(StatementProperty.CONTEXTDEPLOYMENTID));

            sendAssert(env, "A", 1, "cat1", 1L);
            sendAssert(env, "C", 2, null, null);

            env.milestone(1);

            sendAssert(env, "B", 3, "cat2", 1L);
            sendAssert(env, "A", 4, "cat1", 2L);

            env.milestone(2);

            sendAssert(env, "A", 6, "cat1", 3L);
            sendAssert(env, "B", 5, "cat2", 2L);
            sendAssert(env, "C", 7, null, null);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String theString, int intPrimitive, String categoryName, Long expected) {
            env.sendEventBean(new SupportBean(theString, intPrimitive));
            if (expected == null) {
                assertFalse(env.listener("s0").isInvoked());
            } else {
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, new Object[]{expected, categoryName});
            }
        }
    }

    public static class ContextCategorySceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c1,c2,c3,c4,c5".split(",");
            String epl = "@Name('CTX') create context CtxCategory " +
                "group by intPrimitive > 0 as cat1," +
                "group by intPrimitive < 0 as cat2 from SupportBean;\n" +
                "@Name('s0') context CtxCategory select theString as c1, sum(intPrimitive) as c2, context.label as c3, context.name as c4, context.id as c5 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            assertPartitionInfo(env);

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 1, "cat1", "CtxCategory", 0});
            assertPartitionInfo(env);

            env.milestone(1);

            env.sendEventBean(new SupportBean("G2", -2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", -2, "cat2", "CtxCategory", 1});

            env.milestone(2);

            env.sendEventBean(new SupportBean("G3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G3", 4, "cat1", "CtxCategory", 0});

            env.milestone(3);

            env.sendEventBean(new SupportBean("G4", -4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G4", -6, "cat2", "CtxCategory", 1});

            env.milestone(4);

            env.sendEventBean(new SupportBean("G5", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G5", 9, "cat1", "CtxCategory", 0});

            env.undeployAll();
        }

        private void assertPartitionInfo(RegressionEnvironment env) {
            EPContextPartitionService partitionAdmin = env.runtime().getContextPartitionService();
            String depIdCtx = env.deploymentId("CTX");

            ContextPartitionCollection partitions = partitionAdmin.getContextPartitions(depIdCtx, "CtxCategory", ContextPartitionSelectorAll.INSTANCE);
            assertEquals(2, partitions.getIdentifiers().size());
            ContextPartitionIdentifier[] descs = partitions.getIdentifiers().values().toArray(new ContextPartitionIdentifier[2]);
            ContextPartitionIdentifierCategory first = (ContextPartitionIdentifierCategory) descs[0];
            ContextPartitionIdentifierCategory second = (ContextPartitionIdentifierCategory) descs[1];
            EPAssertionUtil.assertEqualsAnyOrder("cat1,cat2".split(","), new Object[]{first.getLabel(), second.getLabel()});

            ContextPartitionIdentifier desc = partitionAdmin.getIdentifier(depIdCtx, "CtxCategory", 0);
            assertEquals("cat1", ((ContextPartitionIdentifierCategory) desc).getLabel());

            SupportContextPropUtil.assertContextProps(env, "CTX", "CtxCategory", new int[]{0, 1}, "label", new Object[][]{{"cat1"}, {"cat2"}});
        }
    }

    private static class ContextCategoryBooleanExprFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCtx = "@name('ctx') create context Ctx600a group by theString like 'A%' as agroup, group by theString like 'B%' as bgroup, group by theString like 'C%' as cgroup from SupportBean";
            env.compileDeploy(eplCtx, path);
            String eplSum = "@name('s0') context Ctx600a select context.label as c0, count(*) as c1 from SupportBean";
            env.compileDeploy(eplSum, path).addListener("s0");

            assertEquals("Ctx600a", env.statement("s0").getProperty(StatementProperty.CONTEXTNAME));
            assertEquals(env.deploymentId("ctx"), env.statement("s0").getProperty(StatementProperty.CONTEXTDEPLOYMENTID));

            sendAssertBooleanExprFilter(env, "B1", "bgroup", 1);

            env.milestone(0);

            sendAssertBooleanExprFilter(env, "A1", "agroup", 1);

            env.milestone(1);

            sendAssertBooleanExprFilter(env, "B171771", "bgroup", 2);

            env.milestone(2);

            sendAssertBooleanExprFilter(env, "A  x", "agroup", 2);

            env.undeployAll();
        }
    }

    private static class ContextCategoryContextPartitionSelection implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            AtomicInteger milestone = new AtomicInteger();
            RegressionPath path = new RegressionPath();

            env.compileDeploy("@name('ctx') create context MyCtx as group by intPrimitive < -5 as grp1, group by intPrimitive between -5 and +5 as grp2, group by intPrimitive > 5 as grp3 from SupportBean", path);
            env.compileDeploy("@name('s0') context MyCtx select context.id as c0, context.label as c1, theString as c2, sum(intPrimitive) as c3 from SupportBean#keepall group by theString", path);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", -5));
            env.sendEventBean(new SupportBean("E1", 2));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E3", -100));
            env.sendEventBean(new SupportBean("E3", -8));
            env.sendEventBean(new SupportBean("E1", 60));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, new Object[][]{{0, "grp1", "E3", -108}, {1, "grp2", "E1", 3}, {1, "grp2", "E2", -5}, {2, "grp3", "E1", 60}});
            SupportContextPropUtil.assertContextProps(env, "ctx", "MyCtx", new int[]{0, 1, 2}, "label", new Object[][]{{"grp1"}, {"grp2"}, {"grp3"}});

            env.milestoneInc(milestone);

            // test iterator targeted by context partition id
            SupportSelectorById selectorById = new SupportSelectorById(Collections.singleton(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorById), env.statement("s0").safeIterator(selectorById), fields, new Object[][]{{1, "grp2", "E1", 3}, {1, "grp2", "E2", -5}});

            // test iterator targeted for a given category
            SupportSelectorCategory selector = new SupportSelectorCategory(new HashSet<>(Arrays.asList("grp1", "grp3")));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selector), env.statement("s0").safeIterator(selector), fields, new Object[][]{{0, "grp1", "E3", -108}, {2, "grp3", "E1", 60}});

            // test iterator targeted for a given filtered category
            MySelectorFilteredCategory filtered = new MySelectorFilteredCategory("grp1");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(filtered), env.statement("s0").safeIterator(filtered), fields, new Object[][]{{0, "grp1", "E3", -108}});
            TestCase.assertFalse(env.statement("s0").iterator(new SupportSelectorCategory((Set<String>) null)).hasNext());
            TestCase.assertFalse(env.statement("s0").iterator(new SupportSelectorCategory(Collections.emptySet())).hasNext());

            env.milestoneInc(milestone);

            // test always-false filter - compare context partition info
            filtered = new MySelectorFilteredCategory(null);
            TestCase.assertFalse(env.statement("s0").iterator(filtered).hasNext());
            EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"grp1", "grp2", "grp3"}, filtered.getCategories());

            try {
                env.statement("s0").iterator(new ContextPartitionSelectorSegmented() {
                    public List<Object[]> getPartitionKeys() {
                        return null;
                    }
                });
                fail();
            } catch (InvalidContextPartitionSelector ex) {
                TestCase.assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById, ContextPartitionSelectorCategory] interfaces but received com."));
            }

            env.undeployAll();
        }
    }

    private static class ContextCategoryInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            // invalid filter spec
            epl = "create context ACtx group theString is not null as cat1 from SupportBean(dummy = 1)";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [");

            // not a boolean expression
            epl = "create context ACtx group intPrimitive as grp1 from SupportBean";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Filter expression not returning a boolean value: 'intPrimitive' [");

            // validate statement not applicable filters
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context ACtx group intPrimitive < 10 as cat1 from SupportBean", path);
            epl = "context ACtx select * from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Category context 'ACtx' requires that any of the events types that are listed in the category context also appear in any of the filter expressions of the statement [");

            env.undeployAll();
        }
    }

    private static class ContextCategoryWContextProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String ctx = "CategorizedContext";
            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('context') create context " + ctx + " " +
                "group intPrimitive < 10 as cat1, " +
                "group intPrimitive between 10 and 20 as cat2, " +
                "group intPrimitive > 20 as cat3 " +
                "from SupportBean;\n";
            epl += "@name('s0') context CategorizedContext " +
                "select context.name as c0, context.label as c1, sum(intPrimitive) as c2 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(3, SupportFilterHelper.getFilterCountApprox(env));
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 3, null, null, null);

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E1", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", 5});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, new Object[][]{{ctx, "cat1", 5}, {ctx, "cat2", null}, {ctx, "cat3", null}});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", 9});

            env.sendEventBean(new SupportBean("E3", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat2", 11});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E4", 25));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat3", 25});

            env.sendEventBean(new SupportBean("E5", 25));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat3", 50});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E6", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", 12});

            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, new Object[][]{{ctx, "cat1", 12}, {ctx, "cat2", 11}, {ctx, "cat3", 50}});

            assertEquals(1, SupportContextMgmtHelper.getContextCount(env));
            assertEquals(3, SupportFilterHelper.getFilterCountApprox(env));

            env.undeployModuleContaining("s0");

            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
            assertEquals(0, SupportContextMgmtHelper.getContextCount(env));
        }
    }

    private static class ContextCategorySingleCategorySODAPrior implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            AtomicInteger milestone = new AtomicInteger();
            String ctx = "CategorizedContext";
            String eplCtx = "@Name('context') create context " + ctx + " as " +
                "group intPrimitive<10 as cat1 " +
                "from SupportBean";
            env.compileDeploy(eplCtx, path);

            String eplStmt = "@name('s0') context CategorizedContext select context.name as c0, context.label as c1, prior(1,intPrimitive) as c2 from SupportBean";
            env.compileDeploy(eplStmt, path).addListener("s0");

            runAssertion(env, ctx, milestone);

            // test SODA
            path.clear();
            env.eplToModelCompileDeploy(eplCtx, path);
            env.eplToModelCompileDeploy(eplStmt, path);
            env.addListener("s0");

            runAssertion(env, ctx, milestone);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String ctx, AtomicInteger milestone) {

        String[] fields = "c0,c1,c2".split(",");
        env.sendEventBean(new SupportBean("E1", 5));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", null});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E2", 20));
        TestCase.assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E1", 4));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", 5});

        assertEquals(1, SupportContextMgmtHelper.getContextCount(env));
        env.undeployAll();
        assertEquals(0, SupportContextMgmtHelper.getContextCount(env));
    }

    public static class ContextCategoryDeclaredExpr implements RegressionExecution {

        private final boolean isAlias;

        public ContextCategoryDeclaredExpr(boolean isAlias) {
            this.isAlias = isAlias;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context MyCtx as " +
                "group by intPrimitive < 0 as n, " +
                "group by intPrimitive > 0 as p " +
                "from SupportBean", path);
            env.compileDeploy("@name('expr-1') create expression getLabelOne { context.label }", path);
            env.compileDeploy("@name('expr-2') create expression getLabelTwo { 'x'||context.label||'x' }", path);

            env.milestone(0);

            if (!isAlias) {
                env.compileDeploy("@name('s0') expression getLabelThree { context.label } " +
                    "context MyCtx " +
                    "select getLabelOne() as c0, getLabelTwo() as c1, getLabelThree() as c2 from SupportBean", path);
            } else {
                env.compileDeploy("@name('s0') expression getLabelThree alias for { context.label } " +
                    "context MyCtx " +
                    "select getLabelOne as c0, getLabelTwo as c1, getLabelThree as c2 from SupportBean", path);
            }
            env.addListener("s0");

            env.milestone(1);

            String[] fields = "c0,c1,c2".split(",");
            env.sendEventBean(new SupportBean("E1", -2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"n", "xnx", "n"});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"p", "xpx", "p"});

            env.undeployAll();
        }
    }

    private static void sendAssertBooleanExprFilter(RegressionEnvironment env, String theString, String groupExpected, long countExpected) {
        String[] fields = "c0,c1".split(",");
        env.sendEventBean(new SupportBean(theString, 1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{groupExpected, countExpected});
    }

    private static class MySelectorFilteredCategory implements ContextPartitionSelectorFiltered {

        private final String matchCategory;

        private List<Object> categories = new ArrayList<>();
        private LinkedHashSet<Integer> cpids = new LinkedHashSet<>();

        private MySelectorFilteredCategory(String matchCategory) {
            this.matchCategory = matchCategory;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierCategory id = (ContextPartitionIdentifierCategory) contextPartitionIdentifier;
            if (matchCategory == null && cpids.contains(id.getContextPartitionId())) {
                throw new RuntimeException("Already exists context id: " + id.getContextPartitionId());
            }
            cpids.add(id.getContextPartitionId());
            categories.add(id.getLabel());
            return matchCategory != null && matchCategory.equals(id.getLabel());
        }

        Object[] getCategories() {
            return categories.toArray();
        }
    }
}
