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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.ISupportAImpl;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.bean.SupportWebEvent;
import com.espertech.esper.regressionlib.support.context.SupportContextPropUtil;
import com.espertech.esper.regressionlib.support.context.SupportSelectorPartitioned;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.*;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class ContextKeySegmented {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextKeySegmentedPatternFilter());
        execs.add(new ContextKeySegmentedJoinRemoveStream());
        execs.add(new ContextKeySegmentedSelector());
        execs.add(new ContextKeySegmentedLargeNumberPartitions());
        execs.add(new ContextKeySegmentedAdditionalFilters());
        execs.add(new ContextKeySegmentedMultiStatementFilterCount());
        execs.add(new ContextKeySegmentedSubtype());
        execs.add(new ContextKeySegmentedJoinMultitypeMultifield());
        execs.add(new ContextKeySegmentedSubselectPrevPrior());
        execs.add(new ContextKeySegmentedPrior());
        execs.add(new ContextKeySegmentedSubqueryFiltered());
        execs.add(new ContextKeySegmentedJoin());
        execs.add(new ContextKeySegmentedPattern());
        execs.add(new ContextKeySegmentedPatternSceneTwo());
        execs.add(new ContextKeySegmentedViewSceneOne());
        execs.add(new ContextKeySegmentedViewSceneTwo());
        execs.add(new ContextKeySegmentedJoinWhereClauseOnPartitionKey());
        execs.add(new ContextKeySegmentedNullSingleKey());
        execs.add(new ContextKeySegmentedNullKeyMultiKey());
        execs.add(new ContextKeySegmentedInvalid());
        execs.add(new ContextKeySegmentedTermByFilter());
        execs.add(new ContextKeySegmentedMatchRecognize());
        execs.add(new ContextKeySegmentedMultikeyWArrayOfPrimitive());
        execs.add(new ContextKeySegmentedMultikeyWArrayTwoField());
        return execs;
    }

    private static class ContextKeySegmentedMultikeyWArrayTwoField implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context PartitionByArray partition by id, array from SupportEventWithIntArray;\n" +
                "@name('s0') context PartitionByArray select sum(value) as thesum from SupportEventWithIntArray;\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssertArray(env, "G1", new int[]{1, 2}, 1, 1);
            sendAssertArray(env, "G2", new int[]{1, 2}, 2, 2);
            sendAssertArray(env, "G1", new int[]{1}, 3, 3);

            env.milestone(0);

            sendAssertArray(env, "G2", new int[]{1, 2}, 10, 2 + 10);
            sendAssertArray(env, "G1", new int[]{1, 2}, 15, 1 + 15);
            sendAssertArray(env, "G1", new int[]{1}, 18, 3 + 18);

            SupportSelectorPartitioned selector = new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"G2", new int[]{1, 2}}));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(selector), "thesum".split(","), new Object[][]{{2 + 10}});

            ContextPartitionSelectorFiltered selectorWFilter = contextPartitionIdentifier -> {
                ContextPartitionIdentifierPartitioned partitioned = (ContextPartitionIdentifierPartitioned) contextPartitionIdentifier;
                return partitioned.getKeys()[0].equals("G2") && Arrays.equals((int[]) partitioned.getKeys()[1], new int[]{1, 2});
            };
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(selectorWFilter), "thesum".split(","), new Object[][]{{2 + 10}});

            env.undeployAll();
        }

        private void sendAssertArray(RegressionEnvironment env, String id, int[] array, int value, int expected) {
            env.sendEventBean(new SupportEventWithIntArray(id, array, value));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("thesum"));
        }
    }

    private static class ContextKeySegmentedMultikeyWArrayOfPrimitive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context PartitionByArray partition by array from SupportEventWithIntArray;\n" +
                "@name('s0') context PartitionByArray select sum(value) as thesum from SupportEventWithIntArray;\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssertArray(env, "E1", new int[]{1, 2}, 10, 10);
            sendAssertArray(env, "E2", new int[]{1, 2}, 11, 21);
            sendAssertArray(env, "E3", new int[]{1}, 12, 12);
            sendAssertArray(env, "E4", new int[]{}, 13, 13);
            sendAssertArray(env, "E5", null, 14, 14);

            env.milestone(0);

            sendAssertArray(env, "E10", null, 20, 14 + 20);
            sendAssertArray(env, "E11", new int[]{1, 2}, 21, 21 + 21);
            sendAssertArray(env, "E12", new int[]{1}, 22, 12 + 22);
            sendAssertArray(env, "E13", new int[]{}, 23, 13 + 23);

            SupportSelectorPartitioned selectorPartition = new SupportSelectorPartitioned(Collections.singletonList(new Object[]{new int[]{1, 2}}));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(selectorPartition), "thesum".split(","), new Object[][]{{21 + 21}});

            ContextPartitionSelectorFiltered selectorWFilter = contextPartitionIdentifier -> {
                ContextPartitionIdentifierPartitioned partitioned = (ContextPartitionIdentifierPartitioned) contextPartitionIdentifier;
                return Arrays.equals((int[]) partitioned.getKeys()[0], new int[]{1});
            };
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(selectorWFilter), "thesum".split(","), new Object[][]{{12 + 22}});

            env.undeployAll();
        }

        private void sendAssertArray(RegressionEnvironment env, String id, int[] array, int value, int expected) {
            env.sendEventBean(new SupportEventWithIntArray(id, array, value));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("thesum"));
        }
    }

    private static class ContextKeySegmentedPatternFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplContext = "create context IndividualBean partition by theString from SupportBean";
            env.compileDeploy(eplContext, path);

            String eplAnalysis = "@name('s0') context IndividualBean " +
                "select * from pattern [every (event1=SupportBean(stringContainsX(theString) = false) -> event2=SupportBean(stringContainsX(theString) = true))]";
            env.compileDeploy(eplAnalysis, path).addListener("s0");

            env.sendEventBean(new SupportBean("F1", 0));
            env.sendEventBean(new SupportBean("F1", 0));

            env.milestone(0);

            env.sendEventBean(new SupportBean("X1", 0));
            assertFalse(env.listener("s0").getIsInvokedAndReset());

            env.milestone(1);

            env.sendEventBean(new SupportBean("X1", 0));
            assertFalse(env.listener("s0").getIsInvokedAndReset());

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedMatchRecognize implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String eplContextOne = "create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplContextOne, path);

            String eplMatchRecog = "@name('s0') context SegmentedByString " +
                "select * from SupportBean\n" +
                "match_recognize ( \n" +
                "  measures A.longPrimitive as a, B.longPrimitive as b\n" +
                "  pattern (A B) \n" +
                "  define " +
                "    A as A.intPrimitive = 1," +
                "    B as B.intPrimitive = 2\n" +
                ")";
            env.compileDeploy(eplMatchRecog, path).addListener("s0");

            env.sendEventBean(makeEvent("A", 1, 10));

            env.milestone(0);

            env.sendEventBean(makeEvent("B", 1, 30));

            env.milestone(1);

            env.sendEventBean(makeEvent("A", 2, 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a,b".split(","), new Object[]{10L, 20L});

            env.milestone(2);

            env.sendEventBean(makeEvent("B", 2, 40));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a,b".split(","), new Object[]{30L, 40L});

            env.undeployAll();

            // try with "prev"
            path.clear();
            String eplContextTwo = "create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplContextTwo, path);

            String eplMatchRecogWithPrev = "@name('s0') context SegmentedByString select * from SupportBean " +
                "match_recognize ( " +
                "  measures A.longPrimitive as e1, B.longPrimitive as e2" +
                "  pattern (A B) " +
                "  define A as A.intPrimitive >= prev(A.intPrimitive),B as B.intPrimitive >= prev(B.intPrimitive) " +
                ")";
            env.compileDeploy(eplMatchRecogWithPrev, path).addListener("s0");

            env.sendEventBean(makeEvent("A", 1, 101));
            env.sendEventBean(makeEvent("B", 1, 201));

            env.milestone(1);

            env.sendEventBean(makeEvent("A", 2, 102));
            env.sendEventBean(makeEvent("B", 2, 202));

            env.milestone(2);

            env.sendEventBean(makeEvent("A", 3, 103));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "e1,e2".split(","), new Object[]{102L, 103L});

            env.milestone(3);

            env.sendEventBean(makeEvent("B", 3, 203));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "e1,e2".split(","), new Object[]{202L, 203L});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedJoinRemoveStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            RegressionPath path = new RegressionPath();

            String stmtContext = "create context SegmentedBySession partition by sessionId from SupportWebEvent";
            env.compileDeploy(stmtContext, path);

            String epl = "@name('s0') context SegmentedBySession " +
                " select rstream A.pageName as pageNameA , A.sessionId as sessionIdA, B.pageName as pageNameB, C.pageName as pageNameC from " +
                "SupportWebEvent(pageName='Start')#time(30) A " +
                "full outer join " +
                "SupportWebEvent(pageName='Middle')#time(30) B on A.sessionId = B.sessionId " +
                "full outer join " +
                "SupportWebEvent(pageName='End')#time(30) C on A.sessionId  = C.sessionId " +
                "where A.pageName is not null and (B.pageName is null or C.pageName is null) ";
            env.compileDeploy(epl, path);

            env.addListener("s0");

            // Set up statement for finding missing events
            sendWebEventsComplete(env, 0);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(20000);
            sendWebEventsComplete(env, 1);

            env.advanceTime(40000);
            sendWebEventsComplete(env, 2);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(60000);
            sendWebEventsIncomplete(env, 3);

            env.advanceTime(80000);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(100000);
            TestCase.assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedSelector implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context PartitionedByString partition by theString from SupportBean", path);
            String[] fields = "c0,c1".split(",");
            env.compileDeploy("@Name('s0') context PartitionedByString select context.key1 as c0, sum(intPrimitive) as c1 from SupportBean#length(5)", path);

            env.sendEventBean(new SupportBean("E1", 10));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean("E2", 21));

            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, new Object[][]{{"E1", 10}, {"E2", 41}});

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, new Object[][]{{"E1", 10}, {"E2", 41}});

            // test iterator targeted
            SupportSelectorPartitioned selector = new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"E2"}));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(selector), env.statement("s0").safeIterator(selector), fields, new Object[][]{{"E2", 41}});
            assertFalse(env.statement("s0").iterator(new SupportSelectorPartitioned((List) null)).hasNext());
            assertFalse(env.statement("s0").iterator(new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"EX"}))).hasNext());
            assertFalse(env.statement("s0").iterator(new SupportSelectorPartitioned(Collections.emptyList())).hasNext());

            // test iterator filtered
            MySelectorFilteredPartitioned filtered = new MySelectorFilteredPartitioned(new Object[]{"E2"});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(filtered), env.statement("s0").safeIterator(filtered), fields, new Object[][]{{"E2", 41}});

            // test always-false filter - compare context partition info
            MySelectorFilteredPartitioned filteredFalse = new MySelectorFilteredPartitioned(null);
            assertFalse(env.statement("s0").iterator(filteredFalse).hasNext());
            EPAssertionUtil.assertEqualsAnyOrder(new Object[]{new Object[]{"E1"}, new Object[]{"E2"}}, filteredFalse.getContexts().toArray());

            try {
                env.statement("s0").iterator(new ContextPartitionSelectorCategory() {
                    public Set<String> getLabels() {
                        return null;
                    }
                });
                fail();
            } catch (InvalidContextPartitionSelector ex) {
                TestCase.assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById, ContextPartitionSelectorSegmented] interfaces but received com."));
            }

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            // invalid filter spec
            epl = "create context SegmentedByAString partition by string from SupportBean(dummy = 1)";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [");

            // property not found
            epl = "create context SegmentedByAString partition by dummy from SupportBean";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "For context 'SegmentedByAString' property name 'dummy' not found on type SupportBean [");

            // mismatch number pf properties
            epl = "create context SegmentedByAString partition by theString from SupportBean, id, p00 from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "For context 'SegmentedByAString' expected the same number of property names for each event type, found 1 properties for event type 'SupportBean' and 2 properties for event type 'SupportBean_S0' [create context SegmentedByAString partition by theString from SupportBean, id, p00 from SupportBean_S0]");

            // incompatible property types
            epl = "create context SegmentedByAString partition by theString from SupportBean, id from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "For context 'SegmentedByAString' for context 'SegmentedByAString' found mismatch of property types, property 'theString' of type 'java.lang.String' compared to property 'id' of type 'java.lang.Integer' [");

            // duplicate type specification
            epl = "create context SegmentedByAString partition by theString from SupportBean, theString from SupportBean";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "For context 'SegmentedByAString' the event type 'SupportBean' is listed twice [");

            // duplicate type: subtype
            epl = "create context SegmentedByAString partition by baseAB from ISupportBaseAB, a from ISupportA";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "For context 'SegmentedByAString' the event type 'ISupportA' is listed twice: Event type 'ISupportA' is a subtype or supertype of event type 'ISupportBaseAB' [");

            // validate statement not applicable filters
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context SegmentedByAString partition by theString from SupportBean", path);
            epl = "context SegmentedByAString select * from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Segmented context 'SegmentedByAString' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type 'SupportBean_S0' is not one of the types listed [");

            // invalid attempt to partition a named window's streams
            env.compileDeploy("create window MyWindow#keepall as SupportBean", path);
            epl = "create context SegmentedByWhat partition by theString from MyWindow";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Partition criteria may not include named windows [create context SegmentedByWhat partition by theString from MyWindow]");

            // partitioned with named window
            env.compileDeploy("create schema SomeSchema(ipAddress string)", path);
            env.compileDeploy("create context TheSomeSchemaCtx Partition By ipAddress From SomeSchema", path);
            epl = "context TheSomeSchemaCtx create window MyEvent#time(30 sec) (ipAddress string)";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Segmented context 'TheSomeSchemaCtx' requires that named windows are associated to an existing event type and that the event type is listed among the partitions defined by the create-context statement");

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedLargeNumberPartitions implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByAString  partition by theString from SupportBean", path);

            String[] fields = "col1".split(",");
            env.compileDeploy("@name('s0') context SegmentedByAString " +
                "select sum(intPrimitive) as col1," +
                "prev(1, intPrimitive)," +
                "prior(1, intPrimitive)," +
                "(select id from SupportBean_S0#lastevent)" +
                "  from SupportBean#keepall", path);
            env.addListener("s0");

            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{i});
            }

            env.undeployAll();
        }
    }

    public static class ContextKeySegmentedAdditionalFilters implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByAString " +
                "partition by theString from SupportBean(intPrimitive>0), p00 from SupportBean_S0(id > 0)", path);

            // first send a view events
            env.sendEventBean(new SupportBean("B1", -1));
            env.sendEventBean(new SupportBean_S0(-2, "S0"));
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            String[] fields = "col1,col2".split(",");
            env.compileDeploy("@name('s0') context SegmentedByAString " +
                "select sum(sb.intPrimitive) as col1, sum(s0.id) as col2 " +
                "from pattern [every (s0=SupportBean_S0 or sb=SupportBean)]", path);
            env.addListener("s0");

            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(-3, "S0"));
            env.sendEventBean(new SupportBean("S0", -1));
            env.sendEventBean(new SupportBean("S1", -2));
            assertFalse(env.listener("s0").isInvoked());
            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(2, "S0"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, 2});

            env.milestone(2);

            env.sendEventBean(new SupportBean("S1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, null});

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(-2, "S0"));
            env.sendEventBean(new SupportBean("S1", -10));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(3, "S1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 3});

            env.milestone(5);

            env.sendEventBean(new SupportBean("S0", 9));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{9, 2});

            env.milestone(6);

            env.undeployAll();
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            env.milestone(7);

            // Test unnecessary filter
            String epl = "create context CtxSegmented partition by theString from SupportBean;" +
                "context CtxSegmented select * from pattern [every a=SupportBean -> c=SupportBean(c.theString=a.theString)];";
            env.compileDeploy(epl);
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E1", 2));

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedMultiStatementFilterCount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByAString " +
                "partition by theString from SupportBean, p00 from SupportBean_S0", path);
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            // first send a view events
            env.sendEventBean(new SupportBean("B1", 1));
            env.sendEventBean(new SupportBean_S0(10, "S0"));

            String[] fields = new String[]{"col1"};
            env.compileDeploy("@name('s0') context SegmentedByAString select sum(id) as col1 from SupportBean_S0", path);
            env.addListener("s0");

            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean_S0(10, "S0"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10});

            env.milestone(0);

            Assert.assertEquals(3, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean_S0(8, "S1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{8});

            env.milestone(1);

            Assert.assertEquals(4, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean_S0(4, "S0"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{14});

            env.milestone(2);

            Assert.assertEquals(4, SupportFilterHelper.getFilterCountApprox(env));

            env.compileDeploy("@name('s1') context SegmentedByAString select sum(intPrimitive) as col1 from SupportBean", path);
            env.addListener("s1");

            Assert.assertEquals(6, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean("S0", 5));
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fields, new Object[]{5});

            Assert.assertEquals(6, SupportFilterHelper.getFilterCountApprox(env));

            env.milestone(3);

            env.sendEventBean(new SupportBean("S2", 6));
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fields, new Object[]{6});

            Assert.assertEquals(8, SupportFilterHelper.getFilterCountApprox(env));

            env.undeployModuleContaining("s0");
            Assert.assertEquals(5, SupportFilterHelper.getFilterCountApprox(env));  // 5 = 3 from context instances and 2 from context itself

            env.milestone(4);

            env.undeployModuleContaining("s1");
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            env.undeployModuleContaining("context");
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedSubtype implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "col1".split(",");
            String epl = "@Name('context') create context SegmentedByString partition by baseAB from ISupportBaseAB;\n" +
                "@name('s0') context SegmentedByString select count(*) as col1 from ISupportA;\n";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.sendEventBean(new ISupportAImpl("A1", "AB1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});

            env.sendEventBean(new ISupportAImpl("A2", "AB1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L});

            env.milestone(1);

            env.sendEventBean(new ISupportAImpl("A3", "AB2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});

            env.sendEventBean(new ISupportAImpl("A4", "AB1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3L});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedJoinMultitypeMultifield implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedBy2Fields " +
                "partition by theString and intPrimitive from SupportBean, p00 and id from SupportBean_S0", path);

            String[] fields = "c1,c2,c3,c4,c5,c6".split(",");
            env.compileDeploy("@name('s0') context SegmentedBy2Fields " +
                "select theString as c1, intPrimitive as c2, id as c3, p00 as c4, context.key1 as c5, context.key2 as c6 " +
                "from SupportBean#lastevent, SupportBean_S0#lastevent", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(2, "G1"));

            env.milestone(1);

            env.sendEventBean(new SupportBean("G2", 2));

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(1, "G2"));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("G2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 1, 1, "G2", "G2", 1});

            env.sendEventBean(new SupportBean_S0(2, "G2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 2, 2, "G2", "G2", 2});

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(1, "G1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 1, 1, "G1", "G1", 1});

            env.milestone(4);

            env.sendEventBean(new SupportBean("G1", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, 2, "G1", "G1", 2});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedSubselectPrevPrior implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fieldsPrev = new String[]{"theString", "col1"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select theString, (select prev(0, id) from SupportBean_S0#keepall) as col1 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G1", null});

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G1", 1});

            env.sendEventBean(new SupportBean("G2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G2", null});

            env.sendEventBean(new SupportBean_S0(2, "E2"));
            env.sendEventBean(new SupportBean("G2", 21));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G2", 2});

            env.sendEventBean(new SupportBean("G1", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G1", null});  // since returning multiple rows

            env.undeployModuleContaining("s0");

            String[] fieldsPrior = new String[]{"theString", "col1"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select theString, (select prior(0, id) from SupportBean_S0#keepall) as col1 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G1", null});

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G1", 1});

            env.sendEventBean(new SupportBean("G2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G2", null});    // since category started as soon as statement added

            env.sendEventBean(new SupportBean_S0(2, "E2"));
            env.sendEventBean(new SupportBean("G2", 21));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G2", 2}); // since returning multiple rows

            env.sendEventBean(new SupportBean("G1", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G1", null});  // since returning multiple rows

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedPrior implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fields = new String[]{"val0", "val1"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select intPrimitive as val0, prior(1, intPrimitive) as val1 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, null});

            env.milestone(0);

            env.sendEventBean(new SupportBean("G2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, null});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11, 10});

            env.undeployModuleContaining("s0");
            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedSubqueryFiltered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fields = new String[]{"theString", "intPrimitive", "val0"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select theString, intPrimitive, (select p00 from SupportBean_S0#lastevent as s0 where sb.intPrimitive = s0.id) as val0 " +
                "from SupportBean as sb", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_S0(10, "s1"));
            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, null});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(10, "s2"));
            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "s2"});

            env.sendEventBean(new SupportBean("G2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 10, null});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(10, "s3"));
            env.sendEventBean(new SupportBean("G2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 10, "s3"});

            env.sendEventBean(new SupportBean("G3", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G3", 10, null});

            env.milestone(2);

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "s3"});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fields = new String[]{"sb.theString", "sb.intPrimitive", "s0.id"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select * from SupportBean#keepall as sb, SupportBean_S0#keepall as s0 " +
                "where intPrimitive = id", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G2", 20));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 20, 20});

            env.sendEventBean(new SupportBean_S0(30));
            env.sendEventBean(new SupportBean("G3", 30));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("G1", 30));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 30, 30});

            env.sendEventBean(new SupportBean("G2", 30));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 30, 30});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fields = new String[]{"a.theString", "a.intPrimitive", "b.theString", "b.intPrimitive"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select * from pattern [every a=SupportBean -> b=SupportBean(intPrimitive=a.intPrimitive+1)]", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G1", 20));
            env.sendEventBean(new SupportBean("G2", 10));
            env.sendEventBean(new SupportBean("G2", 20));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            env.sendEventBean(new SupportBean("G2", 21));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 20, "G2", 21});

            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "G1", 11});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G2", 22));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 21, "G2", 22});

            env.undeployModuleContaining("s0");

            // add another statement: contexts already exist, this one uses @Consume
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select * from pattern [every a=SupportBean -> b=SupportBean(intPrimitive=a.intPrimitive+1)@Consume]", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G1", 20));

            env.milestone(2);

            env.sendEventBean(new SupportBean("G2", 10));
            env.sendEventBean(new SupportBean("G2", 20));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("G2", 21));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 20, "G2", 21});

            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "G1", 11});

            env.milestone(3);

            env.sendEventBean(new SupportBean("G2", 22));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployModuleContaining("s0");

            // test truly segmented consume
            String[] fieldsThree = new String[]{"a.theString", "a.intPrimitive", "b.id", "b.p00"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select * from pattern [every a=SupportBean -> b=SupportBean_S0(id=a.intPrimitive)@Consume]", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G2", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(10, "E1"));   // should be 2 output rows
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getLastNewData(), fieldsThree, new Object[][]{{"G1", 10, 10, "E1"}, {"G2", 10, 10, "E1"}});

            env.undeployAll();
        }
    }

    public static class ContextKeySegmentedPatternSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@Name('CTX') create context SegmentedByString partition by theString from SupportBean, p00 from SupportBean_S0;\n" +
                "@Name('S1') context SegmentedByString " +
                "select a.theString as c0, a.intPrimitive as c1, b.id as c2, b.p00 as c3 from pattern [" +
                "every a=SupportBean -> b=SupportBean_S0(id=a.intPrimitive)];\n";
            env.compileDeploy(epl).addListener("S1");
            String[] fields = "c0,c1,c2,c3".split(",");

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G2", 20));
            env.sendEventBean(new SupportBean_S0(0, "G1"));
            env.sendEventBean(new SupportBean_S0(10, "G2"));
            assertFalse(env.listener("S1").isInvoked());

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(20, "G2"));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G2", 20, 20, "G2"});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(20, "G2"));
            env.sendEventBean(new SupportBean_S0(0, "G1"));
            assertFalse(env.listener("S1").isInvoked());

            env.sendEventBean(new SupportBean_S0(10, "G1"));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, 10, "G1"});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedViewSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String contextEPL = "@Name('context') create context SegmentedByString as partition by theString from SupportBean";
            env.compileDeploy(contextEPL, path);

            String[] fieldsIterate = "intPrimitive".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select irstream intPrimitive, prevwindow(items) as pw from SupportBean#length(2) as items", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            assertViewData(env, 10, new Object[][]{{"G1", 10}}, null);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fieldsIterate, new Object[][]{{10}});

            env.sendEventBean(new SupportBean("G2", 20));
            assertViewData(env, 20, new Object[][]{{"G2", 20}}, null);

            env.sendEventBean(new SupportBean("G1", 11));
            assertViewData(env, 11, new Object[][]{{"G1", 11}, {"G1", 10}}, null);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fieldsIterate, new Object[][]{{10}, {11}, {20}});

            env.sendEventBean(new SupportBean("G2", 21));
            assertViewData(env, 21, new Object[][]{{"G2", 21}, {"G2", 20}}, null);

            env.sendEventBean(new SupportBean("G1", 12));
            assertViewData(env, 12, new Object[][]{{"G1", 12}, {"G1", 11}}, 10);

            env.sendEventBean(new SupportBean("G2", 22));
            assertViewData(env, 22, new Object[][]{{"G2", 22}, {"G2", 21}}, 20);

            env.undeployModuleContaining("s0");

            // test SODA
            env.undeployAll();
            path.clear();

            env.eplToModelCompileDeploy(contextEPL, path);

            // test built-in properties
            String[] fields = "c1,c2,c3,c4".split(",");
            String ctx = "SegmentedByString";
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select context.name as c1, context.id as c2, context.key1 as c3, theString as c4 " +
                "from SupportBean#length(2) as items", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, 0, "G1", "G1"});
            SupportContextPropUtil.assertContextProps(env, "context", "SegmentedByString", new int[]{0}, "key1", new Object[][]{{"G1"}});

            env.undeployAll();

            // test grouped delivery
            path.clear();
            env.compileDeploy("@name('var') create variable boolean trigger = false", path);
            env.compileDeploy("create context MyCtx partition by theString from SupportBean", path);
            env.compileDeploy("@Name('s0') context MyCtx select * from SupportBean#expr(not trigger) for grouped_delivery(theString)", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.runtime().getVariableService().setVariableValue(env.deploymentId("var"), "trigger", true);
            env.advanceTime(100);

            Assert.assertEquals(2, env.listener("s0").getNewDataList().size());

            env.undeployAll();
        }
    }

    public static class ContextKeySegmentedViewSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplContext = "@Name('CTX') create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplContext, path);

            String[] fields = "theString,intPrimitive".split(",");
            String eplSelect = "@Name('S1') context SegmentedByString select irstream * from SupportBean#lastevent()";
            env.compileDeploy(eplSelect, path).addListener("S1");

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 1));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 1});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G2", 10));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G2", 10});

            env.milestone(2);

            env.sendEventBean(new SupportBean("G1", 2));
            EPAssertionUtil.assertProps(env.listener("S1").assertGetAndResetIRPair(), fields, new Object[]{"G1", 2}, new Object[]{"G1", 1});

            env.milestone(3);

            env.sendEventBean(new SupportBean("G2", 11));
            EPAssertionUtil.assertProps(env.listener("S1").assertGetAndResetIRPair(), fields, new Object[]{"G2", 11}, new Object[]{"G2", 10});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedJoinWhereClauseOnPartitionKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyCtx partition by theString from SupportBean;\n" +
                "@name('select') context MyCtx select * from SupportBean#lastevent as sb, SupportBean_S0#lastevent as s0 " +
                "where theString is 'Test'";
            env.compileDeploy(epl).addListener("select");

            env.sendEventBean(new SupportBean("Test", 10));
            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean_S0(1));
            assertTrue(env.listener("select").isInvoked());

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedNullSingleKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyContext partition by theString from SupportBean", path);
            env.compileDeploy("@name('s0') context MyContext select count(*) as cnt from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean(null, 10));
            Assert.assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            env.sendEventBean(new SupportBean(null, 20));
            Assert.assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            env.sendEventBean(new SupportBean("A", 30));
            Assert.assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedNullKeyMultiKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyContext partition by theString, intBoxed, intPrimitive from SupportBean", path);
            env.compileDeploy("@name('s0') context MyContext select count(*) as cnt from SupportBean", path);
            env.addListener("s0");

            sendSBEvent(env, "A", null, 1);
            Assert.assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            sendSBEvent(env, "A", null, 1);
            Assert.assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            sendSBEvent(env, "A", 10, 1);
            Assert.assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            env.undeployAll();
        }
    }

    private static void assertViewData(RegressionEnvironment env, int newIntExpected, Object[][] newArrayExpected, Integer oldIntExpected) {
        Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
        Assert.assertEquals(newIntExpected, env.listener("s0").getLastNewData()[0].get("intPrimitive"));
        SupportBean[] beans = (SupportBean[]) env.listener("s0").getLastNewData()[0].get("pw");
        assertEquals(newArrayExpected.length, beans.length);
        for (int i = 0; i < beans.length; i++) {
            Assert.assertEquals(newArrayExpected[i][0], beans[i].getTheString());
            Assert.assertEquals(newArrayExpected[i][1], beans[i].getIntPrimitive());
        }

        if (oldIntExpected != null) {
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals(oldIntExpected, env.listener("s0").getLastOldData()[0].get("intPrimitive"));
        } else {
            TestCase.assertNull(env.listener("s0").getLastOldData());
        }
        env.listener("s0").reset();
    }

    private static class ContextKeySegmentedTermByFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context ByP0 as partition by theString from SupportBean terminated by SupportBean(intPrimitive<0)", path);
            env.compileDeploy("@name('s0') context ByP0 select theString, count(*) as cnt from SupportBean(intPrimitive>= 0)", path);

            env.addListener("s0");

            sendAssertSB(1, env, "A", 0);

            sendAssertSB(2, env, "A", 0);
            sendAssertNone(env, new SupportBean("A", -1));
            sendAssertSB(1, env, "A", 0);

            sendAssertSB(1, env, "B", 0);
            sendAssertNone(env, new SupportBean("B", -1));
            sendAssertSB(1, env, "B", 0);
            sendAssertSB(2, env, "B", 0);
            sendAssertNone(env, new SupportBean("B", -1));
            sendAssertSB(1, env, "B", 0);

            sendAssertNone(env, new SupportBean("C", -1));

            env.undeployAll();
        }
    }

    private static class MySelectorFilteredPartitioned implements ContextPartitionSelectorFiltered {

        private Object[] match;

        private List<Object[]> contexts = new ArrayList<>();
        private LinkedHashSet<Integer> cpids = new LinkedHashSet<>();

        private MySelectorFilteredPartitioned(Object[] match) {
            this.match = match;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierPartitioned id = (ContextPartitionIdentifierPartitioned) contextPartitionIdentifier;
            if (match == null && cpids.contains(id.getContextPartitionId())) {
                throw new RuntimeException("Already exists context id: " + id.getContextPartitionId());
            }
            cpids.add(id.getContextPartitionId());
            contexts.add(id.getKeys());
            return Arrays.equals(id.getKeys(), match);
        }

        public List<Object[]> getContexts() {
            return contexts;
        }
    }

    private static void sendWebEventsIncomplete(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportWebEvent("Start", String.valueOf(id)));
        env.sendEventBean(new SupportWebEvent("End", String.valueOf(id)));
    }

    private static void sendWebEventsComplete(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportWebEvent("Start", String.valueOf(id)));
        env.sendEventBean(new SupportWebEvent("Middle", String.valueOf(id)));
        env.sendEventBean(new SupportWebEvent("End", String.valueOf(id)));
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    public static boolean stringContainsX(String theString) {
        return theString.contains("X");
    }

    private static void sendAssertSB(long expected, RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString,cnt".split(","), new Object[]{theString, expected});
    }

    private static void sendAssertNone(RegressionEnvironment env, Object event) {
        env.sendEventBean(event);
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendSBEvent(RegressionEnvironment env, String string, Integer intBoxed, int intPrimitive) {
        SupportBean bean = new SupportBean(string, intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
    }
}
