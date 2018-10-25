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

import com.espertech.esper.common.client.context.ContextPartitionCollection;
import com.espertech.esper.common.client.context.ContextPartitionIdentifierPartitioned;
import com.espertech.esper.common.client.context.ContextPartitionSelectorAll;
import com.espertech.esper.common.client.context.EPContextPartitionService;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ContextKeySegmentedAggregate {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextKeySegmentedRowForAll());
        execs.add(new ContextKeySegmentedAccessOnly());
        execs.add(new ContextKeySegmentedSubqueryWithAggregation());
        execs.add(new ContextKeySegmentedRowPerGroupStream());
        execs.add(new ContextKeySegmentedRowPerGroupBatchContextProp());
        execs.add(new ContextKeySegmentedRowPerGroupWithAccess());
        execs.add(new ContextKeySegmentedRowPerGroupUnidirectionalJoin());
        execs.add(new ContextKeySegmentedRowPerEvent());
        execs.add(new ContextKeySegmentedRowPerGroup3Stmts());
        return execs;
    }

    private static class ContextKeySegmentedAccessOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplContext = "@Name('CTX') create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplContext, path);

            String[] fieldsGrouped = "theString,intPrimitive,col1".split(",");
            String eplGroupedAccess = "@Name('s0') context SegmentedByString select theString,intPrimitive,window(longPrimitive) as col1 from SupportBean#keepall sb group by intPrimitive";
            env.compileDeploy(eplGroupedAccess, path);

            env.addListener("s0");

            env.sendEventBean(makeEvent("G1", 1, 10L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsGrouped, new Object[]{"G1", 1, new Object[]{10L}});

            env.milestone(0);

            env.sendEventBean(makeEvent("G1", 2, 100L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsGrouped, new Object[]{"G1", 2, new Object[]{100L}});

            env.sendEventBean(makeEvent("G2", 1, 200L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsGrouped, new Object[]{"G2", 1, new Object[]{200L}});

            env.milestone(1);

            env.sendEventBean(makeEvent("G1", 1, 11L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsGrouped, new Object[]{"G1", 1, new Object[]{10L, 11L}});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedSubqueryWithAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fields = new String[]{"theString", "intPrimitive", "val0"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select theString, intPrimitive, (select count(*) from SupportBean_S0#keepall as s0 where sb.intPrimitive = s0.id) as val0 " +
                "from SupportBean as sb", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_S0(10, "s1"));

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, 0L});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedRowPerGroupStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fieldsOne = "intPrimitive,count(*)".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString select intPrimitive, count(*) from SupportBean group by intPrimitive", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 1L});

            env.sendEventBean(new SupportBean("G2", 200));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{200, 1L});

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 2L});

            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{11, 1L});

            env.sendEventBean(new SupportBean("G2", 200));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{200, 2L});

            env.sendEventBean(new SupportBean("G2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 1L});

            env.undeployModuleContaining("s0");

            // add "string" : a context property
            String[] fieldsTwo = "theString,intPrimitive,count(*)".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString select theString, intPrimitive, count(*) from SupportBean group by intPrimitive", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G1", 10, 1L});

            env.sendEventBean(new SupportBean("G2", 200));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G2", 200, 1L});

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G1", 10, 2L});

            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G1", 11, 1L});

            env.sendEventBean(new SupportBean("G2", 200));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G2", 200, 2L});

            env.sendEventBean(new SupportBean("G2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G2", 10, 1L});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedRowPerGroupBatchContextProp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fieldsOne = "intPrimitive,count(*)".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString select intPrimitive, count(*) from SupportBean#length_batch(2) group by intPrimitive order by intPrimitive asc", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G2", 200));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fieldsOne, new Object[]{10, 1L});
            EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[1], fieldsOne, new Object[]{11, 1L});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G1", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            env.sendEventBean(new SupportBean("G2", 200));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{200, 2L});

            env.milestone(3);

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fieldsOne, new Object[]{10, 2L});
            EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[1], fieldsOne, new Object[]{11, 0L});

            env.milestone(4);

            env.sendEventBean(new SupportBean("G2", 10));
            env.sendEventBean(new SupportBean("G2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fieldsOne, new Object[]{10, 2L});
            EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[1], fieldsOne, new Object[]{200, 0L});

            env.undeployModuleContaining("s0");

            // add "string" : add context property
            String[] fieldsTwo = "theString,intPrimitive,count(*)".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString select theString, intPrimitive, count(*) from SupportBean#length_batch(2) group by intPrimitive order by theString, intPrimitive asc", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G2", 200));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(5);

            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fieldsTwo, new Object[]{"G1", 10, 1L});
            EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[1], fieldsTwo, new Object[]{"G1", 11, 1L});

            env.sendEventBean(new SupportBean("G1", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);

            env.sendEventBean(new SupportBean("G2", 200));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G2", 200, 2L});

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fieldsTwo, new Object[]{"G1", 10, 2L});
            EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[1], fieldsTwo, new Object[]{"G1", 11, 0L});

            env.milestone(7);

            env.sendEventBean(new SupportBean("G2", 10));
            env.sendEventBean(new SupportBean("G2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fieldsTwo, new Object[]{"G2", 10, 2L});
            EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[1], fieldsTwo, new Object[]{"G2", 200, 0L});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedRowPerGroupWithAccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fieldsOne = "intPrimitive,col1,col2,col3".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select intPrimitive, count(*) as col1, toArray(window(*).selectFrom(v=>v.longPrimitive)) as col2, first().longPrimitive as col3 " +
                "from SupportBean#keepall as sb " +
                "group by intPrimitive order by intPrimitive asc", path);
            env.addListener("s0");

            env.sendEventBean(makeEvent("G1", 10, 200L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 1L, new Object[]{200L}, 200L});

            env.sendEventBean(makeEvent("G1", 10, 300L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 2L, new Object[]{200L, 300L}, 200L});

            env.milestone(0);

            env.sendEventBean(makeEvent("G2", 10, 1000L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 1L, new Object[]{1000L}, 1000L});

            env.milestone(1);

            env.sendEventBean(makeEvent("G2", 10, 1010L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 2L, new Object[]{1000L, 1010L}, 1000L});

            env.undeployModuleContaining("s0");
            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedRowForAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] fieldsOne = "col1".split(",");
            RegressionPath path = new RegressionPath();

            String eplCtx = "@Name('context') create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplCtx, path);

            String epl = "@Name('s0') context SegmentedByString select sum(intPrimitive) as col1 from SupportBean;\n";
            env.compileDeploy(epl, path).addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("G1", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{3});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("G2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{2});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("G1", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{7});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("G2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{3});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("G3", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{-1});

            env.milestoneInc(milestone);

            env.undeployModuleContaining("s0");

            // test mixed with access
            String[] fieldsTwo = "col1,col2".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select sum(intPrimitive) as col1, toArray(window(*).selectFrom(v=>v.intPrimitive)) as col2 " +
                "from SupportBean#keepall", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 8));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{8, new Object[]{8}});

            env.sendEventBean(new SupportBean("G2", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{5, new Object[]{5}});

            env.sendEventBean(new SupportBean("G1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{9, new Object[]{8, 1}});

            env.sendEventBean(new SupportBean("G2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{7, new Object[]{5, 2}});

            env.undeployModuleContaining("s0");

            // test only access
            String[] fieldsThree = "col1".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select toArray(window(*).selectFrom(v=>v.intPrimitive)) as col1 " +
                "from SupportBean#keepall", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 8));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsThree, new Object[]{new Object[]{8}});

            env.sendEventBean(new SupportBean("G2", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsThree, new Object[]{new Object[]{5}});

            env.sendEventBean(new SupportBean("G1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsThree, new Object[]{new Object[]{8, 1}});

            env.sendEventBean(new SupportBean("G2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsThree, new Object[]{new Object[]{5, 2}});

            env.undeployModuleContaining("s0");

            // test subscriber
            EPStatement stmtFour = env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select count(*) as col1 " +
                "from SupportBean", path).statement("s0");
            SupportSubscriber subs = new SupportSubscriber();
            stmtFour.setSubscriber(subs);

            env.sendEventBean(new SupportBean("G1", 1));
            assertEquals(1L, subs.assertOneGetNewAndReset());

            env.sendEventBean(new SupportBean("G1", 1));
            assertEquals(2L, subs.assertOneGetNewAndReset());

            env.sendEventBean(new SupportBean("G2", 2));
            assertEquals(1L, subs.assertOneGetNewAndReset());

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedRowPerGroupUnidirectionalJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            String[] fieldsOne = "intPrimitive,col1".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select intPrimitive, count(*) as col1 " +
                "from SupportBean unidirectional, SupportBean_S0#keepall " +
                "group by intPrimitive order by intPrimitive asc", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S0(2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 2L});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(3));

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 3L});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G2", 20));
            env.sendEventBean(new SupportBean_S0(4));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("G2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{20, 1L});

            env.sendEventBean(new SupportBean_S0(5));

            env.milestone(2);

            env.sendEventBean(new SupportBean("G2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{20, 2L});

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 5L});

            env.undeployModuleContaining("s0");
            env.undeployAll();
        }
    }

    public static class ContextKeySegmentedRowPerEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplContext = "@Name('CTX') create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplContext, path);

            String[] fields = "theString,col1".split(",");
            String eplUngrouped = "@Name('S1') context SegmentedByString select theString,sum(intPrimitive) as col1 from SupportBean";
            env.compileDeploy(eplUngrouped, path).addListener("S1");

            String eplGroupedAccess = "@Name('S2') context SegmentedByString select theString,window(intPrimitive) as col1 from SupportBean#keepall() sb";
            env.compileDeploy(eplGroupedAccess, path).addListener("S2");

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 2));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G1", new Object[]{2}});

            env.sendEventBean(new SupportBean("G1", 3));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 5});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G1", new Object[]{2, 3}});
            assertPartitionInfo(env);

            env.milestone(1);

            assertPartitionInfo(env);
            env.sendEventBean(new SupportBean("G2", 10));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G2", 10});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G2", new Object[]{10}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("G2", 11));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G2", 21});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G2", new Object[]{10, 11}});

            env.milestone(3);

            env.sendEventBean(new SupportBean("G1", 4));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 9});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G1", new Object[]{2, 3, 4}});

            env.milestone(4);

            env.sendEventBean(new SupportBean("G3", 100));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G3", 100});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G3", new Object[]{100}});

            env.milestone(5);

            env.sendEventBean(new SupportBean("G3", 101));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G3", 201});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G3", new Object[]{100, 101}});

            env.undeployModuleContaining("S1");
            env.undeployModuleContaining("S2");
            env.undeployModuleContaining("CTX");
        }

        private void assertPartitionInfo(RegressionEnvironment env) {
            EPContextPartitionService partitionAdmin = env.runtime().getContextPartitionService();
            ContextPartitionCollection partitions = partitionAdmin.getContextPartitions(env.deploymentId("CTX"), "SegmentedByString", ContextPartitionSelectorAll.INSTANCE);
            assertEquals(1, partitions.getIdentifiers().size());
            ContextPartitionIdentifierPartitioned ident = (ContextPartitionIdentifierPartitioned) partitions.getIdentifiers().values().iterator().next();
            EPAssertionUtil.assertEqualsExactOrder(new String[]{"G1"}, ident.getKeys());
        }
    }

    public static class ContextKeySegmentedRowPerGroup3Stmts implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplContext = "@Name('CTX') create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplContext, path);

            String[] fields = "theString,intPrimitive,col1".split(",");
            String eplGrouped = "@Name('S1') context SegmentedByString select theString,intPrimitive,sum(longPrimitive) as col1 from SupportBean group by intPrimitive";
            env.compileDeploy(eplGrouped, path).addListener("S1");

            String eplGroupedAccess = "@Name('S2') context SegmentedByString select theString,intPrimitive,window(longPrimitive) as col1 from SupportBean.win:keepall() sb group by intPrimitive";
            env.compileDeploy(eplGroupedAccess, path).addListener("S2");

            String eplGroupedDistinct = "@Name('S3') context SegmentedByString select theString,intPrimitive,sum(distinct longPrimitive) as col1 from SupportBean.win:keepall() sb group by intPrimitive";
            env.compileDeploy(eplGroupedDistinct, path).addListener("S3");

            env.sendEventBean(makeEvent("G1", 1, 10L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 1, 10L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G1", 1, new Object[]{10L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G1", 1, 10L});

            env.milestone(0);

            env.sendEventBean(makeEvent("G2", 1, 25L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G2", 1, 25L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G2", 1, new Object[]{25L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G2", 1, 25L});

            env.milestone(1);

            env.sendEventBean(makeEvent("G1", 2, 2L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, 2L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, new Object[]{2L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, 2L});

            env.milestone(2);

            env.sendEventBean(makeEvent("G2", 2, 100L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G2", 2, 100L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G2", 2, new Object[]{100L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G2", 2, 100L});

            env.milestone(3);

            env.sendEventBean(makeEvent("G1", 1, 10L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 1, 20L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G1", 1, new Object[]{10L, 10L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G1", 1, 10L});

            env.milestone(4);

            env.sendEventBean(makeEvent("G1", 2, 3L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, 5L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, new Object[]{2L, 3L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, 5L});

            env.milestone(5);

            env.sendEventBean(makeEvent("G2", 2, 101L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G2", 2, 201L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G2", 2, new Object[]{100L, 101L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G2", 2, 201L});

            env.milestone(6);

            env.sendEventBean(makeEvent("G3", 1, -1L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G3", 1, -1L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G3", 1, new Object[]{-1L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G3", 1, -1L});

            env.milestone(7);

            env.sendEventBean(makeEvent("G3", 2, -2L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G3", 2, -2L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G3", 2, new Object[]{-2L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G3", 2, -2L});

            env.milestone(8);

            env.sendEventBean(makeEvent("G3", 1, -3L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G3", 1, -4L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G3", 1, new Object[]{-1L, -3L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G3", 1, -4L});

            env.milestone(9);

            env.sendEventBean(makeEvent("G1", 2, 3L));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, 8L});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, new Object[]{2L, 3L, 3L}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, 5L});

            env.undeployAll();
        }
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    public static Object toArray(Collection input) {
        return input.toArray();
    }
}
