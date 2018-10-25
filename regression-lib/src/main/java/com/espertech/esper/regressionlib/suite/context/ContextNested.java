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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.support.context.*;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import com.espertech.esper.regressionlib.support.util.SupportScheduleHelper;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriber;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class ContextNested {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextNestedWithFilterUDF());
        execs.add(new ContextNestedIterateTargetedCP());
        execs.add(new ContextNestedInvalid());
        execs.add(new ContextNestedIterator());
        execs.add(new ContextNestedPartitionedWithFilterOverlap());
        execs.add(new ContextNestedPartitionedWithFilterNonOverlap());
        execs.add(new ContextNestedNestingFilterCorrectness());
        execs.add(new ContextNestedCategoryOverPatternInitiated());
        execs.add(new ContextNestedSingleEventTriggerNested());
        execs.add(new ContextNestedFourContextsNested());
        execs.add(new ContextNestedTemporalOverCategoryOverPartition());
        execs.add(new ContextNestedTemporalFixedOverHash());
        execs.add(new ContextNestedCategoryOverTemporalOverlapping());
        execs.add(new ContextNestedFixedTemporalOverPartitioned());
        execs.add(new ContextNestedPartitionedOverFixedTemporal());
        execs.add(new ContextNestedContextProps());
        execs.add(new ContextNestedLateComingStatement());
        execs.add(new ContextNestedPartitionWithMultiPropsAndTerm());
        execs.add(new ContextNestedOverlappingAndPattern());
        execs.add(new ContextNestedNonOverlapping());
        execs.add(new ContextNestedPartitionedOverPatternInitiated());
        execs.add(new ContextNestedInitWStartNow());
        execs.add(new ContextNestedInitWStartNowSceneTwo());
        execs.add(new ContextNestedKeyedStartStop());
        execs.add(new ContextNestedKeyedFilter());
        execs.add(new ContextNestedNonOverlapOverNonOverlapNoEndCondition(false));
        execs.add(new ContextNestedNonOverlapOverNonOverlapNoEndCondition(true));
        execs.add(new ContextNestedInitTermWCategoryWHash());
        execs.add(new ContextNestedInitTermOverHashIterate(true));
        execs.add(new ContextNestedInitTermOverHashIterate(false));
        execs.add(new ContextNestedInitTermOverPartitionedIterate());
        execs.add(new ContextNestedInitTermOverCategoryIterate());
        execs.add(new ContextNestedInitTermOverInitTermIterate());
        execs.add(new ContextNestedCategoryOverInitTermDistinct());
        return execs;
    }

    private static class ContextNestedInitWStartNow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.advanceTime(0);
            env.compileDeploy("create context Ctx "
                + "context C0 initiated by SupportBean as criteria terminated by SupportBean(theString='x'), "
                + "context C1 start @now end (*,*,*,*,*,*/5)", path);
            env.compileDeploy("@name('s0') context Ctx select context.C0.criteria as c0, event, count(*) as cnt from SupportBean_S0(p00=context.C0.criteria.theString) as event", path);
            env.addListener("s0");

            SupportBean criteriaA = new SupportBean("A", 0);
            env.sendEventBean(criteriaA);
            env.sendEventBean(new SupportBean_S0(1, "B"));
            env.sendEventBean(new SupportBean("B", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            SupportBean_S0 s0 = new SupportBean_S0(2, "A");
            env.sendEventBean(s0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,event,cnt".split(","), new Object[]{criteriaA, s0, 1L});

            env.sendEventBean(new SupportBean_S0(3, "A"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,cnt".split(","), new Object[]{criteriaA, 2L});

            env.milestone(1);

            env.advanceTime(5000000);

            env.sendEventBean(new SupportBean_S0(4, "A"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,cnt".split(","), new Object[]{criteriaA, 1L});

            env.undeployAll();
        }
    }

    public static class ContextNestedInitWStartNowSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.advanceTime(0);
            RegressionPath path = new RegressionPath();

            env.compileDeploy("@name('ctx') create context MyContext \n" +
                "context C0 initiated by SupportBean(intPrimitive=0) AS criteria terminated by SupportBean(intPrimitive=1), \n" +
                "context C1 start @now end (*,*,*,*,*,*/5)", path);
            env.compileDeploy("@name('s0') context MyContext select count(*) as cnt from SupportBean(theString = context.C0.criteria.theString)", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("A", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "cnt".split(","), new Object[]{1L});

            env.milestone(0);

            ContextPartitionCollection cpc = env.runtime().getContextPartitionService().getContextPartitions(env.deploymentId("ctx"), "MyContext", ContextPartitionSelectorAll.INSTANCE);
            ContextPartitionIdentifierNested nested = (ContextPartitionIdentifierNested) cpc.getIdentifiers().get(0);
            ContextPartitionIdentifierInitiatedTerminated first = (ContextPartitionIdentifierInitiatedTerminated) nested.getIdentifiers()[0];
            assertFalse(first.getProperties().isEmpty());
            ContextPartitionIdentifierInitiatedTerminated second = (ContextPartitionIdentifierInitiatedTerminated) nested.getIdentifiers()[1];

            env.sendEventBean(new SupportBean("A", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "cnt".split(","), new Object[]{2L});

            env.milestone(1);

            env.advanceTime(100000);

            env.sendEventBean(new SupportBean("A", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "cnt".split(","), new Object[]{1L});

            env.undeployAll();
        }
    }

    private static class ContextNestedPartitionedOverPatternInitiated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context TheContext " +
                "context C0 partition by theString from SupportBean," +
                "context C1 initiated by SupportBean(intPrimitive=1) terminated by SupportBean(intPrimitive=2)", path);
            env.compileDeploy("@name('s0') context TheContext select theString, sum(longPrimitive) as theSum from SupportBean output last when terminated", path);
            env.addListener("s0");

            sendSupportBean(env, "A", 0, 1);
            sendSupportBean(env, "B", 0, 2);

            env.milestone(0);

            sendSupportBean(env, "C", 1, 3);
            sendSupportBean(env, "D", 1, 4);

            env.milestone(1);

            sendSupportBean(env, "A", 0, 5);
            sendSupportBean(env, "C", 0, 6);
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBean(env, "C", 2, -10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString,theSum".split(","), new Object[]{"C", -1L});

            env.milestone(2);

            sendSupportBean(env, "D", 2, 5);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString,theSum".split(","), new Object[]{"D", 9L});

            env.undeployAll();
        }
    }

    private static class ContextNestedWithFilterUDF implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NestedContext " +
                "context ACtx initiated by SupportBean_S0 as s0 terminated after 24 hours, " +
                "context BCtx initiated by SupportBean_S1 as s1 terminated after 1 hour", path);
            env.compileDeploy("@Name('s0') context NestedContext select * " +
                "from SupportBean(" +
                "customEnabled(theString, context.ACtx.s0.p00, intPrimitive, context.BCtx.s1.id)" +
                " and " +
                "customDisabled(theString, context.ACtx.s0.p00, intPrimitive, context.BCtx.s1.id))", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "S0"));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(2, "S1"));

            env.milestone(1);

            env.sendEventBean(new SupportBean("X", -1));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();

            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
        }
    }

    private static class ContextNestedIterateTargetedCP implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NestedContext " +
                "context ACtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id=s0.id), " +
                "context BCtx group by intPrimitive < 0 as grp1, group by intPrimitive = 0 as grp2, group by intPrimitive > 0 as grp3 from SupportBean", path);

            String[] fields = "c0,c1,c2,c3".split(",");
            env.compileDeploy("@Name('s0') context NestedContext " +
                "select context.ACtx.s0.p00 as c0, context.BCtx.label as c1, theString as c2, sum(intPrimitive) as c3 from SupportBean#length(5) group by theString", path);

            env.sendEventBean(new SupportBean_S0(1, "S0_1"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", -1));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E3", 5));
            env.sendEventBean(new SupportBean_S0(2, "S0_2"));
            env.sendEventBean(new SupportBean("E1", 2));

            env.milestoneInc(milestone);

            Object[][] expectedAll = new Object[][]{{"S0_1", "grp1", "E2", -1}, {"S0_1", "grp3", "E3", 5}, {"S0_1", "grp3", "E1", 3}, {"S0_2", "grp3", "E1", 2}};
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, expectedAll);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(ContextPartitionSelectorAll.INSTANCE), env.statement("s0").safeIterator(ContextPartitionSelectorAll.INSTANCE), fields, expectedAll);
            SupportSelectorById allIds = new SupportSelectorById(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5)));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(allIds), env.statement("s0").safeIterator(allIds), fields, expectedAll);

            // test iterator targeted
            ContextPartitionSelector firstOne = new SupportSelectorFilteredInitTerm("S0_2");
            ContextPartitionSelector secondOne = new SupportSelectorCategory(Collections.singleton("grp3"));
            SupportSelectorNested nestedSelector = new SupportSelectorNested(Collections.singletonList(new ContextPartitionSelector[]{firstOne, secondOne}));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(nestedSelector), env.statement("s0").safeIterator(nestedSelector), fields, new Object[][]{{"S0_2", "grp3", "E1", 2}});

            ContextPartitionSelector firstTwo = new SupportSelectorFilteredInitTerm("S0_1");
            ContextPartitionSelector secondTwo = new SupportSelectorCategory(Collections.singleton("grp1"));
            SupportSelectorNested nestedSelectorTwo = new SupportSelectorNested(Arrays.asList(new ContextPartitionSelector[]{firstOne, secondOne}, new ContextPartitionSelector[]{firstTwo, secondTwo}));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(nestedSelectorTwo), env.statement("s0").safeIterator(nestedSelectorTwo), fields, new Object[][]{{"S0_2", "grp3", "E1", 2}, {"S0_1", "grp1", "E2", -1}});

            // test iterator filtered : not supported for nested
            try {
                MySelectorFilteredNested filtered = new MySelectorFilteredNested(new Object[]{"S0_2", "grp3"});
                env.statement("s0").iterator(filtered);
                fail();
            } catch (InvalidContextPartitionSelector ex) {
                TestCase.assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorById, ContextPartitionSelectorNested] interfaces but received com."));
            }

            env.undeployAll();
            path.clear();

            // test 3 nesting levels and targeted
            env.compileDeploy("create context NestedContext " +
                "context ACtx group by intPrimitive < 0 as i1, group by intPrimitive = 0 as i2, group by intPrimitive > 0 as i3 from SupportBean," +
                "context BCtx group by longPrimitive < 0 as l1, group by longPrimitive = 0 as l2, group by longPrimitive > 0 as l3 from SupportBean," +
                "context CCtx group by boolPrimitive = true as b1, group by boolPrimitive = false as b2 from SupportBean", path);

            String[] fieldsSelect = "c0,c1,c2,c3".split(",");
            env.compileDeploy("@Name('StmtOne') context NestedContext " +
                "select context.ACtx.label as c0, context.BCtx.label as c1, context.CCtx.label as c2, count(*) as c3 from SupportBean#length(5) having count(*) > 0", path);

            env.sendEventBean(makeEvent("E1", -1, 10L, true));
            env.sendEventBean(makeEvent("E2", 2, -10L, false));

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E3", 1, 11L, false));
            env.sendEventBean(makeEvent("E4", 0, 0L, true));

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E5", -1, 10L, false));
            env.sendEventBean(makeEvent("E6", -1, 10L, true));

            env.milestoneInc(milestone);

            Object[][] expectedRows = new Object[][]{
                {"i1", "l3", "b1", 2L},
                {"i3", "l1", "b2", 1L},
                {"i1", "l3", "b2", 1L},
                {"i2", "l2", "b1", 1L},
                {"i3", "l3", "b2", 1L},
            };
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("StmtOne").iterator(), env.statement("StmtOne").safeIterator(), fieldsSelect, expectedRows);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("StmtOne").iterator(ContextPartitionSelectorAll.INSTANCE), env.statement("StmtOne").safeIterator(ContextPartitionSelectorAll.INSTANCE), fields, expectedRows);

            // test iterator targeted
            ContextPartitionSelector[] selectors = new ContextPartitionSelector[]{
                new SupportSelectorCategory(Collections.singleton("i3")),
                new SupportSelectorCategory(Collections.singleton("l1")),
                new SupportSelectorCategory(Collections.singleton("b2"))
            };
            SupportSelectorNested nestedSelectorSelect = new SupportSelectorNested(Collections.singletonList(selectors));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("StmtOne").iterator(nestedSelectorSelect), env.statement("StmtOne").safeIterator(nestedSelectorSelect), fieldsSelect, new Object[][]{{"i3", "l1", "b2", 1L}});

            env.undeployAll();
        }
    }

    private static class ContextNestedInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            // invalid same sub-context name twice
            epl = "create context ABC context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *)";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Context by name 'EightToNine' has already been declared within nested context 'ABC' [");

            // validate statement added to nested context
            RegressionPath path = new RegressionPath();
            epl = "create context ABC context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), context PartCtx as partition by theString from SupportBean";
            env.compileDeploy(epl, path);
            epl = "context ABC select * from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Segmented context 'ABC' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type 'SupportBean_S0' is not one of the types listed [");

            env.undeployAll();
        }
    }

    private static class ContextNestedIterator implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();

            env.compileDeploy("create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context SegByString partition by theString from SupportBean", path);

            String[] fields = "c0,c1,c2".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.EightToNine.startTime as c0, context.SegByString.key1 as c1, intPrimitive as c2 from SupportBean#keepall", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            Object[][] expected = new Object[][]{{DateTime.parseDefaultMSec("2002-05-1T08:00:00.000"), "E1", 1}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, expected);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, expected);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").safeIterator(), fields, expected);

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 2));
            expected = new Object[][]{{DateTime.parseDefaultMSec("2002-05-1T08:00:00.000"), "E1", 1}, {DateTime.parseDefaultMSec("2002-05-1T08:00:00.000"), "E1", 2}};
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, expected);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").safeIterator(), fields, expected);

            env.undeployAll();
        }
    }

    private static class ContextNestedNestingFilterCorrectness implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            RegressionPath path = new RegressionPath();
            String eplContext;
            String eplSelect = "@name('s0') context TheContext select count(*) from SupportBean";
            SupportBean bean;

            // category over partition
            eplContext = "@name('ctx') create context TheContext " +
                "context CtxCategory as group intPrimitive < 0 as negative, group intPrimitive > 0 as positive from SupportBean, " +
                "context CtxPartition as partition by theString from SupportBean";
            env.compileDeploy(eplContext, path);
            env.compileDeploy(eplSelect, path);

            assertFilters(env, "[SupportBean(intPrimitive<0), SupportBean(intPrimitive>0)]", "ctx");
            env.sendEventBean(new SupportBean("E1", -1));

            env.milestoneInc(milestone);

            assertFilters(env, "[SupportBean(intPrimitive<0,theStringisE1)]", "s0");
            env.undeployAll();
            path.clear();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            // category over partition over category
            eplContext = "@name('ctx') create context TheContext " +
                "context CtxCategoryOne as group intPrimitive < 0 as negative, group intPrimitive > 0 as positive from SupportBean, " +
                "context CtxPartition as partition by theString from SupportBean," +
                "context CtxCategoryTwo as group longPrimitive < 0 as negative, group longPrimitive > 0 as positive from SupportBean";
            env.compileDeploy(eplContext, path);
            env.compileDeploy(eplSelect, path);

            assertFilters(env, "[SupportBean(intPrimitive<0), SupportBean(intPrimitive>0)]", "ctx");
            bean = new SupportBean("E1", -1);
            bean.setLongPrimitive(1);
            env.sendEventBean(bean);

            env.milestoneInc(milestone);

            assertFilters(env, "[SupportBean(intPrimitive<0,theStringisE1,longPrimitive<0), SupportBean(intPrimitive<0,theStringisE1,longPrimitive>0)]", "s0");
            assertFilters(env, "[SupportBean(intPrimitive<0), SupportBean(intPrimitive>0)]", "ctx");
            env.undeployAll();
            path.clear();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            // partition over partition over partition
            eplContext = "@name('ctx') create context TheContext " +
                "context CtxOne as partition by theString from SupportBean, " +
                "context CtxTwo as partition by intPrimitive from SupportBean," +
                "context CtxThree as partition by longPrimitive from SupportBean";
            env.compileDeploy(eplContext, path);
            env.compileDeploy(eplSelect, path);

            assertFilters(env, "[SupportBean()]", "ctx");
            bean = new SupportBean("E1", 2);
            bean.setLongPrimitive(3);
            env.sendEventBean(bean);
            env.milestoneInc(milestone);

            assertFilters(env, "[SupportBean(theStringisE1,intPrimitiveis2,longPrimitiveis3)]", "s0");
            assertFilters(env, "[SupportBean(), SupportBean(theStringisE1), SupportBean(theStringisE1,intPrimitiveis2)]", "ctx");

            env.undeployAll();
            path.clear();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            // category over hash
            eplContext = "@name('ctx') create context TheContext " +
                "context CtxCategoryOne as group intPrimitive < 0 as negative, group intPrimitive > 0 as positive from SupportBean, " +
                "context CtxTwo as coalesce by consistent_hash_crc32(theString) from SupportBean granularity 100";
            env.compileDeploy(eplContext, path);
            env.compileDeploy(eplSelect, path);

            assertFilters(env, "[SupportBean(intPrimitive<0), SupportBean(intPrimitive>0)]", "ctx");
            bean = new SupportBean("E1", 2);
            bean.setLongPrimitive(3);
            env.sendEventBean(bean);

            env.milestoneInc(milestone);

            assertFilters(env, "[SupportBean(intPrimitive>0,consistent_hash_crc32(theString)=33)]", "s0");
            assertFilters(env, "[SupportBean(intPrimitive<0), SupportBean(intPrimitive>0)]", "ctx");
            env.undeployAll();
            path.clear();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            eplContext = "@name('ctx') create context TheContext " +
                "context CtxOne as partition by theString from SupportBean, " +
                "context CtxTwo as start pattern [SupportBean_S0] end pattern[SupportBean_S1]";
            env.compileDeploy(eplContext, path);
            env.compileDeploy(eplSelect, path);

            assertFilters(env, "[SupportBean()]", "ctx");
            env.sendEventBean(new SupportBean("E1", 2));

            env.milestoneInc(milestone);

            assertFilters(env, "[]", "s0");
            assertFilters(env, "[SupportBean(), SupportBean_S0()]", "ctx");
            env.undeployAll();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
        }

        private static void assertFilters(RegressionEnvironment env, String expected, String name) {
            assertEquals(expected, SupportFilterHelper.getFilterToString(env, name));
        }
    }

    private static class ContextNestedPartitionedWithFilterOverlap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Audit('pattern-instances') create context TheContext"
                + " context CtxSession partition by id from SupportBean_S0, "
                + " context CtxStartEnd start SupportBean_S0 as te end SupportBean_S1(id=te.id)", path);
            env.compileDeploy("@name('s0') context TheContext select firstEvent from SupportBean_S0#firstevent() as firstEvent"
                + " inner join SupportBean_S0#lastevent as lastEvent", path);
            SupportSubscriber supportSubscriber = new SupportSubscriber();
            env.statement("s0").setSubscriber(supportSubscriber);
            AtomicInteger milestone = new AtomicInteger();

            for (int i = 0; i < 2; i++) {
                env.sendEventBean(new SupportBean_S0(1, "A"));

                env.milestoneInc(milestone);
                env.statement("s0").setSubscriber(supportSubscriber);

                env.sendEventBean(new SupportBean_S0(2, "B"));
                env.sendEventBean(new SupportBean_S1(1));

                env.milestoneInc(milestone);
                env.statement("s0").setSubscriber(supportSubscriber);

                supportSubscriber.reset();
                env.sendEventBean(new SupportBean_S0(2, "C"));
                assertEquals("B", ((SupportBean_S0) supportSubscriber.assertOneGetNewAndReset()).getP00());

                env.sendEventBean(new SupportBean_S1(1));

                env.milestoneInc(milestone);
                env.statement("s0").setSubscriber(supportSubscriber);

                env.sendEventBean(new SupportBean_S1(2));
            }

            env.undeployAll();
        }
    }

    private static class ContextNestedCategoryOverPatternInitiated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();

            String eplCtx = "@name('ctx') create context NestedContext as " +
                "context ByCat as group intPrimitive < 0 as g1, group intPrimitive > 0 as g2, group intPrimitive = 0 as g3 from SupportBean, " +
                "context InitCtx as initiated by pattern [every a=SupportBean_S0 -> b=SupportBean_S1(id = a.id)] terminated after 10 sec";
            env.compileDeploy(eplCtx, path);

            String[] fields = "c0,c1,c2,c3".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.ByCat.label as c0, context.InitCtx.a.p00 as c1, context.InitCtx.b.p10 as c2, sum(intPrimitive) as c3 from SupportBean group by theString", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(100, "S0_1"));

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(101, "S1_1"));

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 2));

            env.milestone(3);

            env.sendEventBean(new SupportBean_S1(100, "S1_2"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_1", "S1_2", 3}});

            env.milestone(5);

            env.sendEventBean(new SupportBean("E4", -2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"g1", "S0_1", "S1_2", -2}});

            env.sendEventBean(new SupportBean("E5", 0));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"g3", "S0_1", "S1_2", 0}});

            env.sendEventBean(new SupportBean("E3", 5));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_1", "S1_2", 8}});

            env.sendEventBean(new SupportBean("E6", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_1", "S1_2", 6}});

            env.milestone(6);

            env.sendEventBean(new SupportBean_S0(102, "S0_3"));

            env.milestone(7);

            env.sendEventBean(new SupportBean_S1(102, "S1_3"));

            env.sendEventBean(new SupportBean("E3", 7));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_1", "S1_2", 15}, {"g2", "S0_3", "S1_3", 7}});

            env.milestone(8);

            sendTimeEvent(env, "2002-05-1T08:00:10.000");

            env.sendEventBean(new SupportBean("E3", 8));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(104, "S0_4"));
            env.sendEventBean(new SupportBean_S1(104, "S1_4"));

            env.milestone(9);

            env.sendEventBean(new SupportBean("E3", 9));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_4", "S1_4", 9}});

            env.undeployAll();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
        }
    }

    private static class ContextNestedPartitionedWithFilterNonOverlap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();

            String eplCtx = "@name('ctx') create context NestedContext as " +
                "context SegByString as partition by theString from SupportBean(intPrimitive > 0), " +
                "context InitCtx initiated by SupportBean_S0 as s0 terminated after 60 seconds";
            env.compileDeploy(eplCtx, path);

            String[] fields = "c0,c1,c2".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.InitCtx.s0.p00 as c0, theString as c1, sum(intPrimitive) as c2 from SupportBean group by theString", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            SupportBean_S0 s0Bean1 = new SupportBean_S0(1, "S0_1");
            env.sendEventBean(s0Bean1);

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", -5));
            assertFalse(env.listener("s0").isInvoked());
            SupportContextPropUtil.assertContextPropsNested(env, "ctx", "NestedContext", new int[]{0}, "SegByString,InitCtx".split(","), new String[]{"key1", "s0"},
                new Object[][][]{{{"E1"}, {s0Bean1}}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E1", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_1", "E1", 2}});

            env.milestone(3);

            env.sendEventBean(new SupportBean("E2", 3));
            assertFalse(env.listener("s0").isInvoked());

            SupportBean_S0 s0Bean2 = new SupportBean_S0(2, "S0_2");
            env.sendEventBean(s0Bean2);

            env.milestone(4);

            SupportContextPropUtil.assertContextPropsNested(env, "ctx", "NestedContext", new int[]{0, 1, 2}, "SegByString,InitCtx".split(","), new String[]{"key1", "s0"},
                new Object[][][]{{{"E1"}, {s0Bean1}}, {{"E1"}, {s0Bean2}}, {{"E2"}, {s0Bean2}}});

            env.sendEventBean(new SupportBean("E2", 4));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "E2", 4}});

            env.milestone(5);

            env.sendEventBean(new SupportBean("E1", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_1", "E1", 8}, {"S0_2", "E1", 6}});

            env.undeployAll();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
        }
    }

    private static class ContextNestedSingleEventTriggerNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test partitioned context
            //
            RegressionPath path = new RegressionPath();
            AtomicInteger milestone = new AtomicInteger();

            String eplCtxOne = "create context NestedContext as " +
                "context SegByString as partition by theString from SupportBean, " +
                "context SegByInt as partition by intPrimitive from SupportBean, " +
                "context SegByLong as partition by longPrimitive from SupportBean ";
            env.compileDeploy(eplCtxOne, path);

            String[] fieldsOne = "c0,c1,c2,c3".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.SegByString.key1 as c0, context.SegByInt.key1 as c1, context.SegByLong.key1 as c2, count(*) as c3 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(makeEvent("E1", 10, 100));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsOne, new Object[][]{{"E1", 10, 100L, 1L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E2", 10, 100));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsOne, new Object[][]{{"E2", 10, 100L, 1L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E1", 11, 100));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsOne, new Object[][]{{"E1", 11, 100L, 1L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E1", 10, 101));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsOne, new Object[][]{{"E1", 10, 101L, 1L}});

            env.sendEventBean(makeEvent("E1", 10, 100));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsOne, new Object[][]{{"E1", 10, 100L, 2L}});

            env.undeployAll();
            path.clear();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            // Test partitioned context
            //
            String eplCtxTwo = "@name('ctx') create context NestedContext as " +
                "context HashOne coalesce by hash_code(theString) from SupportBean granularity 10, " +
                "context HashTwo coalesce by hash_code(intPrimitive) from SupportBean granularity 10";
            env.compileDeploy(eplCtxTwo, path);

            String[] fieldsTwo = "c1,c2".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "theString as c1, count(*) as c2 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsTwo, new Object[][]{{"E1", 1L}});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsTwo, new Object[][]{{"E2", 1L}});

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsTwo, new Object[][]{{"E1", 2L}});

            env.undeployModuleContaining("s0");
            env.undeployModuleContaining("ctx");
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
            path.clear();

            // Test partitioned context
            //
            String eplCtxThree = "create context NestedContext as " +
                "context InitOne initiated by SupportBean(theString like 'I%') as sb0 terminated after 10 sec, " +
                "context InitTwo initiated by SupportBean(intPrimitive > 0) as sb1 terminated after 10 sec";
            env.compileDeploy(eplCtxThree, path);

            String[] fieldsThree = "c1,c2".split(",");
            env.compileDeploy("@name('s0') context NestedContext select theString as c1, count(*) as c2 from SupportBean", path);
            env.addListener("s0");

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("I1", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsThree, new Object[][]{{"I1", 1L}});

            env.undeployModuleContaining("s0");
            env.undeployAll();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
        }
    }

    private static class ContextNestedFourContextsNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T07:00:00.000");
            RegressionPath path = new RegressionPath();
            AtomicInteger milestone = new AtomicInteger();

            String eplCtx = "@name('ctx') create context NestedContext as " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context InitCtx0 initiated by SupportBean_S0 as s0 terminated after 60 seconds, " +
                "context InitCtx1 initiated by SupportBean_S1 as s1 terminated after 30 seconds, " +
                "context InitCtx2 initiated by SupportBean_S2 as s2 terminated after 10 seconds";
            env.compileDeploy(eplCtx, path);

            String[] fields = "c1,c2,c3,c4".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.InitCtx0.s0.p00 as c1, context.InitCtx1.s1.p10 as c2, context.InitCtx2.s2.p20 as c3, sum(intPrimitive) as c4 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "S0_1"));
            env.sendEventBean(new SupportBean_S1(100, "S1_1"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S2(200, "S2_1"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E1", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimeEvent(env, "2002-05-1T08:00:00.000");

            env.sendEventBean(new SupportBean_S0(1, "S0_2"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S1(100, "S1_2"));
            env.sendEventBean(new SupportBean_S2(200, "S2_2"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_2", 2}});

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_2", 5}});

            sendTimeEvent(env, "2002-05-1T08:00:05.000");

            env.sendEventBean(new SupportBean_S1(101, "S1_3"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_2", 9}});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S2(201, "S2_3"));
            env.sendEventBean(new SupportBean("E5", 5));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_2", 14}, {"S0_2", "S1_2", "S2_3", 5}, {"S0_2", "S1_3", "S2_3", 5}});

            sendTimeEvent(env, "2002-05-1T08:00:10.000"); // terminate S2_2 leaf

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E6", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_3", 11}, {"S0_2", "S1_3", "S2_3", 11}});

            sendTimeEvent(env, "2002-05-1T08:00:15.000"); // terminate S0_2/S1_2/S2_3 and S0_2/S1_3/S2_3 leafs

            env.sendEventBean(new SupportBean("E7", 7));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S2(201, "S2_4"));
            env.sendEventBean(new SupportBean("E8", 8));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_4", 8}, {"S0_2", "S1_3", "S2_4", 8}});

            env.milestoneInc(milestone);

            sendTimeEvent(env, "2002-05-1T08:00:30.000"); // terminate S1_2 branch

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E9", 9));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(105, "S1_5"));
            env.sendEventBean(new SupportBean_S2(205, "S2_5"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E10", 10));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_3", "S2_5", 10}, {"S0_2", "S1_5", "S2_5", 10}});

            sendTimeEvent(env, "2002-05-1T08:00:60.000"); // terminate S0_2 branch, only the "8to9" is left

            env.sendEventBean(new SupportBean("E11", 11));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(6, "S0_6"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S1(106, "S1_6"));
            env.sendEventBean(new SupportBean_S2(206, "S2_6"));
            env.sendEventBean(new SupportBean("E2", 12));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_6", "S1_6", "S2_6", 12}});

            env.sendEventBean(new SupportBean_S0(7, "S0_7"));
            env.sendEventBean(new SupportBean_S1(107, "S1_7"));
            env.sendEventBean(new SupportBean_S2(207, "S2_7"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E3", 13));
            Assert.assertEquals(4, env.listener("s0").getAndResetLastNewData().length);

            env.milestoneInc(milestone);

            sendTimeEvent(env, "2002-05-1T10:00:00.000"); // terminate all

            env.sendEventBean(new SupportBean("E14", 14));
            assertFalse(env.listener("s0").isInvoked());

            sendTimeEvent(env, "2002-05-2T08:00:00.000"); // start next day

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(8, "S0_8"));
            env.sendEventBean(new SupportBean_S1(108, "S1_8"));
            env.sendEventBean(new SupportBean_S2(208, "S2_8"));
            env.sendEventBean(new SupportBean("E15", 15));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_8", "S1_8", "S2_8", 15}});

            SupportListener listener = env.listener("s0");
            env.undeployModuleContaining("s0");

            env.sendEventBean(new SupportBean("E16", 16));
            assertFalse(listener.isInvoked());
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            env.undeployAll();
        }
    }

    private static class ContextNestedTemporalOverlapOverPartition implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            String[] fields = "c1,c2,c3".split(",");

            String epl = "@name('ctx') create context NestedContext as " +
                "context InitCtx initiated by SupportBean_S0(id > 0) as s0 terminated after 10 seconds, " +
                "context SegmCtx as partition by theString from SupportBean(intPrimitive > 0);\n" +
                "@name('s0') context NestedContext select " +
                "context.InitCtx.s0.p00 as c1, context.SegmCtx.key1 as c2, sum(intPrimitive) as c3 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", -1));
            env.sendEventBean(new SupportBean_S0(-1, "S0_1"));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 1));
            env.sendEventBean(new SupportBean_S0(1, "S0_2"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_2", "E3", 3});

            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_2", "E4", 4});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E3", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_2", "E3", 8});

            sendTimeEvent(env, "2002-05-1T08:00:05.000");

            env.sendEventBean(new SupportBean_S0(-2, "S0_3"));

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(1, "S0_4"));

            env.sendEventBean(new SupportBean("E3", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "E3", 14}, {"S0_4", "E3", 6}});

            env.milestone(4);

            env.sendEventBean(new SupportBean("E4", 7));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "E4", 11}, {"S0_4", "E4", 7}});

            sendTimeEvent(env, "2002-05-1T08:00:10.000"); // expires first context

            env.sendEventBean(new SupportBean("E3", 8));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_4", "E3", 14});

            env.sendEventBean(new SupportBean("E4", 9));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_4", "E4", 16});

            env.milestone(5);

            sendTimeEvent(env, "2002-05-1T08:00:15.000"); // expires second context

            env.sendEventBean(new SupportBean("Ex", 1));
            env.sendEventBean(new SupportBean_S0(1, "S0_5"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);

            env.sendEventBean(new SupportBean("E4", 10));
            env.sendEventBean(new SupportBean("E4", -10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_5", "E4", 10});

            sendTimeEvent(env, "2002-05-1T08:00:25.000"); // expires second context

            env.sendEventBean(new SupportBean("E4", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ContextNestedTemporalOverCategoryOverPartition implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();
            AtomicInteger milestone = new AtomicInteger();

            String eplCtx = "@name('ctx') create context NestedContext as " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context ByCat as group intPrimitive<0 as g1, group intPrimitive=0 as g2, group intPrimitive>0 as g3 from SupportBean, " +
                "context SegmentedByString as partition by theString from SupportBean";
            env.compileDeploy(eplCtx, path);

            String[] fields = "c1,c2,c3".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.ByCat.label as c1, context.SegmentedByString.key1 as c2, sum(longPrimitive) as c3 from SupportBean", path);
            env.addListener("s0");

            tryAssertion3Contexts(env, milestone, fields, "2002-05-1T08:00:00.000", "2002-05-1T09:00:00.000");

            env.undeployModuleContaining("s0");
            env.undeployModuleContaining("ctx");
            path.clear();

            sendTimeEvent(env, "2002-05-2T08:00:00.000");

            // test SODA
            env.eplToModelCompileDeploy(eplCtx, path);
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.ByCat.label as c1, context.SegmentedByString.key1 as c2, sum(longPrimitive) as c3 from SupportBean", path);
            env.addListener("s0");

            tryAssertion3Contexts(env, milestone, fields, "2002-05-2T08:00:00.000", "2002-05-2T09:00:00.000");

            env.undeployAll();
        }
    }

    /**
     * Root: Temporal
     * Sub: Hash
     * }
     */
    private static class ContextNestedTemporalFixedOverHash implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            sendTimeEvent(env, "2002-05-1T07:00:00.000");

            env.compileDeploy("create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context HashedCtx coalesce hash_code(intPrimitive) from SupportBean granularity 10 preallocate", path);
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            String[] fields = "c1,c2".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "theString as c1, count(*) as c2 from SupportBean group by theString", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            sendTimeEvent(env, "2002-05-1T08:00:00.000"); // start context

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});

            sendTimeEvent(env, "2002-05-1T09:00:00.000"); // terminate

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            sendTimeEvent(env, "2002-05-2T08:00:00.000"); // start context

            env.milestone(4);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});

            env.undeployAll();
        }
    }

    /**
     * Root: Category
     * Sub: Initiated
     * }
     */
    private static class ContextNestedCategoryOverTemporalOverlapping implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            sendTimeEvent(env, "2002-05-1T08:00:00.000");

            env.compileDeploy("create context NestedContext " +
                "context ByCat " +
                "  group intPrimitive < 0 and intPrimitive != -9999 as g1, " +
                "  group intPrimitive = 0 as g2, " +
                "  group intPrimitive > 0 as g3 from SupportBean, " +
                "context InitGrd initiated by SupportBean(theString like 'init%') as sb terminated after 10 seconds", path);
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            String[] fields = "c1,c2,c3".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.ByCat.label as c1, context.InitGrd.sb.theString as c2, count(*) as c3 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 5));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            env.sendEventBean(new SupportBean("init_1", -9999));
            env.sendEventBean(new SupportBean("X100", 0));

            env.milestone(1);

            env.sendEventBean(new SupportBean("X101", 10));
            env.sendEventBean(new SupportBean("X102", -10));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("init_2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g2", "init_2", 1L});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E3", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g2", "init_2", 2L});

            env.sendEventBean(new SupportBean("E4", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            env.sendEventBean(new SupportBean("init_3", -2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g1", "init_3", 1L});

            env.sendEventBean(new SupportBean("E5", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g1", "init_3", 2L});

            env.milestone(4);

            env.sendEventBean(new SupportBean("E6", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g1", "init_3", 3L});

            sendTimeEvent(env, "2002-05-1T08:11:00.000"); // terminates all

            env.milestone(5);

            env.sendEventBean(new SupportBean("E7", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    /**
     * Root: Fixed temporal
     * Sub: Partition by string
     * <p>
     * - Root starts deactivated.
     * - With context destroy before statement destroy
     * }
     */
    private static class ContextNestedFixedTemporalOverPartitioned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            sendTimeEvent(env, "2002-05-1T07:00:00.000");

            env.compileDeploy("create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context SegmentedByAString partition by theString from SupportBean", path);
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            String[] fields = "c1".split(",");
            env.compileDeploy("@name('s0') context NestedContext select count(*) as c1 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean());
            assertFalse(env.listener("s0").isInvoked());
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));

            env.milestone(0);

            // starts EightToNine context
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            Assert.assertEquals(1, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});
            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});
            Assert.assertEquals(3, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L});
            Assert.assertEquals(3, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));

            env.milestone(2);

            // ends EightToNine context
            sendTimeEvent(env, "2002-05-1T09:00:00.000");
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            // starts EightToNine context
            sendTimeEvent(env, "2002-05-2T08:00:00.000");
            Assert.assertEquals(1, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L});

            env.milestone(4);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 2, null, null, null);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L});

            SupportListener listener = env.listener("s0");
            env.undeployAll();

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 0));
            assertFalse(listener.isInvoked());

            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));
        }
    }

    /**
     * Root: Partition by string
     * Sub: Fixed temporal
     * <p>
     * - Sub starts deactivated.
     * - With statement destroy before context destroy
     * }
     */
    private static class ContextNestedPartitionedOverFixedTemporal implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            sendTimeEvent(env, "2002-05-1T07:00:00.000");

            env.compileDeploy("create context NestedContext " +
                "context SegmentedByAString partition by theString from SupportBean, " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *)", path);
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            String[] fields = "c1".split(",");
            env.compileDeploy("@name('s0') context NestedContext select count(*) as c1 from SupportBean", path);
            env.addListener("s0");
            Assert.assertEquals(1, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            env.sendEventBean(new SupportBean("E1", 0));
            assertFalse(env.listener("s0").isInvoked());
            Assert.assertEquals(1, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));

            // starts EightToNine context
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});
            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});
            Assert.assertEquals(3, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(2, SupportScheduleHelper.scheduleCountOverall(env));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L});
            Assert.assertEquals(3, SupportFilterHelper.getFilterCountApprox(env));

            // ends EightToNine context
            sendTimeEvent(env, "2002-05-1T09:00:00.000");
            Assert.assertEquals(1, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 0));
            assertFalse(env.listener("s0").isInvoked());
            Assert.assertEquals(2, SupportScheduleHelper.scheduleCountOverall(env));

            env.milestone(2);

            // starts EightToNine context
            sendTimeEvent(env, "2002-05-2T08:00:00.000");
            Assert.assertEquals(3, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});

            env.milestone(3);

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L});

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 2, null, null, null);
            Assert.assertEquals(2, SupportScheduleHelper.scheduleCountOverall(env));

            SupportListener listener = env.listener("s0");
            env.undeployAll();

            env.sendEventBean(new SupportBean("E1", 0));
            assertFalse(listener.isInvoked());
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));
        }
    }

    /**
     * Test nested context properties.
     * <p>
     * Root: Fixed temporal
     * Sub: Partition by string
     * <p>
     * - fixed temportal starts active
     * - starting and stopping statement
     * }
     */
    private static class ContextNestedContextProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            sendTimeEvent(env, "2002-05-1T08:30:00.000");

            env.compileDeploy("@name('ctx') create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context SegmentedByAString partition by theString from SupportBean", path);

            String[] fields = "c0,c1,c2,c3,c4,c5,c6".split(",");
            String epl = "@name('s0') context NestedContext select " +
                "context.EightToNine.name as c0, " +
                "context.EightToNine.startTime as c1, " +
                "context.SegmentedByAString.name as c2, " +
                "context.SegmentedByAString.key1 as c3, " +
                "context.name as c4, " +
                "intPrimitive as c5," +
                "count(*) as c6 " +
                "from SupportBean";
            env.compileDeploy(epl, path).addListener("s0");
            Assert.assertEquals(1, SupportFilterHelper.getFilterCountApprox(env));

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"EightToNine", DateTime.parseDefaultMSec("2002-05-1T08:30:00.000"),
                "SegmentedByAString", "E1",
                "NestedContext",
                10, 1L});
            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"EightToNine", DateTime.parseDefaultMSec("2002-05-1T08:30:00.000"),
                "SegmentedByAString", "E2",
                "NestedContext",
                20, 1L});
            Assert.assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));
            Assert.assertEquals(3, SupportFilterHelper.getFilterCountApprox(env));
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 2);

            env.milestone(1);

            env.undeployModuleContaining("s0");
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            env.milestone(2);

            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean("E2", 30));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"EightToNine", DateTime.parseDefaultMSec("2002-05-1T08:30:00.000"),
                "SegmentedByAString", "E2",
                "NestedContext",
                30, 1L});
            Assert.assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));
            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 1);

            env.milestone(3);

            SupportListener listener = env.listener("s0");
            env.undeployModuleContaining("s0");
            env.undeployModuleContaining("ctx");

            env.milestone(4);

            env.sendEventBean(new SupportBean("E2", 30));
            assertFalse(listener.isInvoked());
            Assert.assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
        }
    }

    /**
     * Test late-coming env.statement("s0").
     * <p>
     * Root: Fixed temporal
     * Sub: Partition by string
     * }
     */
    private static class ContextNestedLateComingStatement implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            sendTimeEvent(env, "2002-05-1T08:30:00.000");

            env.compileDeploy("create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context SegmentedByAString partition by theString from SupportBean", path);

            String[] fields = "c0,c1".split(",");
            env.compileDeploy("@name('s0') context NestedContext select theString as c0, count(*) as c1 from SupportBean", path);
            env.addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

            env.compileDeploy("@name('s2') context NestedContext select theString as c0, sum(intPrimitive) as c1 from SupportBean", path);
            env.addListener("s2");

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 2L});
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fields, new Object[]{"E1", 20});

            env.sendEventBean(new SupportBean("E2", 30));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fields, new Object[]{"E2", 30});

            env.milestone(2);

            env.compileDeploy("@name('s3') context NestedContext select theString as c0, min(intPrimitive) as c1 from SupportBean", path);
            env.addListener("s3");

            env.milestone(3);

            env.sendEventBean(new SupportBean("E1", 40));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 3L});
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fields, new Object[]{"E1", 60});
            EPAssertionUtil.assertProps(env.listener("s3").assertOneGetNewAndReset(), fields, new Object[]{"E1", 40});

            env.milestone(4);

            SupportListener s2Listener = env.listener("s2");
            env.undeployModuleContaining("s2");

            env.sendEventBean(new SupportBean("E1", 50));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 4L});
            assertFalse(s2Listener.isInvoked());
            EPAssertionUtil.assertProps(env.listener("s3").assertOneGetNewAndReset(), fields, new Object[]{"E1", 40});

            SupportListener s0Listener = env.listener("s0");
            env.undeployModuleContaining("s0");

            env.sendEventBean(new SupportBean("E1", -60));
            assertFalse(s0Listener.isInvoked());
            assertFalse(s2Listener.isInvoked());
            EPAssertionUtil.assertProps(env.listener("s3").assertOneGetNewAndReset(), fields, new Object[]{"E1", -60});

            env.undeployModuleContaining("s3");

            env.undeployAll();
        }
    }

    private static class ContextNestedPartitionWithMultiPropsAndTerm implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NestedContext " +
                "context PartitionedByKeys partition by theString, intPrimitive from SupportBean, " +
                "context InitiateAndTerm start SupportBean as e1 " +
                "end SupportBean_S0(id=e1.intPrimitive and p00=e1.theString)", path);

            String[] fields = "c0,c1,c2".split(",");
            env.compileDeploy("@name('s0') context NestedContext " +
                "select theString as c0, intPrimitive as c1, count(longPrimitive) as c2 from SupportBean \n" +
                "output last when terminated", path);
            env.addListener("s0");

            env.sendEventBean(makeEvent("E1", 0, 10));
            env.sendEventBean(makeEvent("E1", 0, 10));

            env.milestone(0);

            env.sendEventBean(makeEvent("E2", 1, 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(0, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 0, 2L});

            env.undeployAll();
        }
    }

    private static class ContextNestedOverlappingAndPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NestedContext " +
                "context PartitionedByKeys partition by theString from SupportBean, " +
                "context TimedImmediate initiated @now and pattern[every timer:interval(10)] terminated after 10 seconds", path);
            tryAssertion(env, path);
        }
    }

    private static class ContextNestedNonOverlapping implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NestedContext " +
                "context PartitionedByKeys partition by theString from SupportBean, " +
                "context TimedImmediate start @now end after 10 seconds", path);
            tryAssertion(env, path);
        }
    }

    private static class ContextNestedKeyedStartStop implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.advanceTime(0);
            env.compileDeploy("@name('ctx') create context NestedCtxWTime " +
                "context OuterCtx initiated @now and pattern[timer:interval(10000000)] terminated after 1 second, " +
                "context InnerCtx partition by theString from SupportBean(intPrimitive=0) terminated by SupportBean(intPrimitive=1)", path);
            env.compileDeploy("context NestedCtxWTime select theString, count(*) as cnt from SupportBean", path);

            env.sendEventBean(new SupportBean("A", 0));
            env.sendEventBean(new SupportBean("B", 0));

            env.milestone(0);

            env.advanceTime(100000);
            assertFilterCount(env, 0, "ctx");

            env.undeployAll();
        }
    }

    private static class ContextNestedKeyedFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context NestedCtxWPartition " +
                "context ByString partition by theString from SupportBean, " +
                "context ByInt partition by intPrimitive from SupportBean terminated by SupportBean(boolPrimitive=false)", path);
            env.compileDeploy("@name('s0') context NestedCtxWPartition select theString, intPrimitive, sum(longPrimitive) as thesum from SupportBean output last when terminated", path);
            env.addListener("s0");
            String[] fields = "theString,intPrimitive,thesum".split(",");

            sendBean(env, "A", 1, 10, true);
            sendBean(env, "B", 1, 11, true);
            sendBean(env, "A", 2, 12, true);

            env.milestone(0);

            sendBean(env, "B", 2, 13, true);
            sendBean(env, "B", 1, 20, true);
            sendBean(env, "A", 1, 30, true);

            env.milestone(1);

            sendBean(env, "A", 2, 40, true);
            sendBean(env, "B", 2, 50, true);

            sendBean(env, "A", 1, 0, false);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 1, 40L});

            env.milestone(2);

            sendBean(env, "B", 2, 0, false);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 2, 63L});

            sendBean(env, "A", 2, 0, false);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 2, 52L});

            env.milestone(3);

            sendBean(env, "B", 1, 0, false);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 1, 31L});

            assertFilterCount(env, 3, "ctx");
            env.undeployModuleContaining("s0");
            assertFilterCount(env, 0, "ctx");
            env.undeployAll();
        }
    }

    public static class ContextNestedNonOverlapOverNonOverlapNoEndCondition implements RegressionExecution {
        private final boolean soda;

        public ContextNestedNonOverlapOverNonOverlapNoEndCondition(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(soda, "create context MyCtx as " +
                "context Lvl1Ctx as start SupportBean_S0 as s0, " +
                "context Lvl2Ctx as start SupportBean_S1 as s1", path);
            env.compileDeploy("@name('s0') context MyCtx " +
                "select theString, context.Lvl1Ctx.s0.p00 as p00, context.Lvl2Ctx.s1.p10 as p10 from SupportBean", path);
            env.addListener("s0");
            String[] fields = "theString,p00,p10".split(",");

            env.sendEventBean(new SupportBean("P1", 100));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1, "A"));
            env.sendEventBean(new SupportBean("P1", 100));

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(2, "B"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "A", "B"});

            env.undeployAll();
        }
    }

    public static class ContextNestedInitTermWCategoryWHash implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T8:00:00.000");
            RegressionPath path = new RegressionPath();

            String eplCtx = "@Name('ctx') create context NestedContext as " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context ByCat as group intPrimitive < 0 as g1, group intPrimitive = 0 as g2, group intPrimitive > 0 as g3 from SupportBean, " +
                "context SegmentedByString as partition by theString from SupportBean";
            env.compileDeploy(eplCtx, path);

            String[] fields = "c1,c2,c3".split(",");
            env.compileDeploy("@Name('s0') context NestedContext select " +
                "context.ByCat.label as c1, context.SegmentedByString.key1 as c2, sum(longPrimitive) as c3 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(makeEvent("E1", 0, 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g2", "E1", 10L});
            assertPartitionInfo(env);

            env.milestone(0);

            assertPartitionInfo(env);
            env.sendEventBean(makeEvent("E2", 0, 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g2", "E2", 11L});

            env.milestone(1);

            env.sendEventBean(makeEvent("E1", 0, 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g2", "E1", 22L});
            assertPartitionInfoMulti(env, 2);

            env.milestone(2);

            assertPartitionInfoMulti(env, 2);
            env.sendEventBean(makeEvent("E1", 1, 13));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g3", "E1", 13L});
            assertPartitionInfoMulti(env, 3);

            env.milestone(3);

            assertPartitionInfoMulti(env, 3);
            env.sendEventBean(makeEvent("E1", -1, 14));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g1", "E1", 14L});

            env.milestone(4);

            env.sendEventBean(makeEvent("E2", -1, 15));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g1", "E2", 15L});

            env.milestone(5);

            sendTimeEvent(env, "2002-05-1T9:01:00.000");

            env.milestone(6);

            env.sendEventBean(makeEvent("E2", -1, 15));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }

        private static Object makeEvent(String theString, int intPrimitive, long longPrimitive) {
            SupportBean bean = new SupportBean(theString, intPrimitive);
            bean.setLongPrimitive(longPrimitive);
            return bean;
        }

        private void assertPartitionInfoMulti(RegressionEnvironment env, int size) {
            EPContextPartitionService partitionAdmin = env.runtime().getContextPartitionService();
            ContextPartitionCollection partitions = partitionAdmin.getContextPartitions(env.deploymentId("ctx"), "NestedContext", ContextPartitionSelectorAll.INSTANCE);
            assertEquals(size, partitions.getIdentifiers().size());
        }

        private void assertPartitionInfo(RegressionEnvironment env) {
            EPContextPartitionService partitionAdmin = env.runtime().getContextPartitionService();
            ContextPartitionCollection partitions = partitionAdmin.getContextPartitions(env.deploymentId("ctx"), "NestedContext", ContextPartitionSelectorAll.INSTANCE);
            assertEquals(1, partitions.getIdentifiers().size());
            ContextPartitionIdentifierNested nested = (ContextPartitionIdentifierNested) partitions.getIdentifiers().values().iterator().next();
            assertNested(nested);
        }

        private void assertNested(ContextPartitionIdentifierNested nested) {
            assertTrue(((ContextPartitionIdentifierInitiatedTerminated) nested.getIdentifiers()[0]).getStartTime() >= 0);
            assertEquals("g2", ((ContextPartitionIdentifierCategory) nested.getIdentifiers()[1]).getLabel());
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, ((ContextPartitionIdentifierPartitioned) nested.getIdentifiers()[2]).getKeys());
        }

        private String getLblLvl1(ContextPartitionIdentifierNested ident) {
            return ((ContextPartitionIdentifierCategory) ident.getIdentifiers()[1]).getLabel();
        }
    }

    public static class ContextNestedInitTermOverHashIterate implements RegressionExecution {

        private final boolean preallocate;

        public ContextNestedInitTermOverHashIterate(boolean preallocate) {
            this.preallocate = preallocate;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context NestedContext " +
                "context FirstCtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id = s0.id), " +
                "context SecondCtx coalesce by consistent_hash_crc32(theString) from SupportBean granularity 4 " + (preallocate ? "preallocate" : ""), path);

            String[] fields = "c0,c1".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.FirstCtx.s0.id as c0, theString as c1 from SupportBean#keepall", path);
            env.addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S0(2));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields,
                new Object[][]{{1, "E1"}, {2, "E1"}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 1));
            Object[][] expectedAll = new Object[][]{{1, "E1"}, {2, "E1"}, {1, "E2"}, {2, "E2"}};
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, expectedAll);

            // all-selector
            SupportSelectorNested selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new ContextPartitionSelectorAll());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);
            // hash-specific-selector
            selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), SupportSelectorByHashCode.fromSetOfAll(4));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);
            // filter-specific-selector
            selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new SupportSelectorFilteredPassAll());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);
            // id-specific-selector
            selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), SupportSelectorById.fromSetOfAll(100));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);

            env.milestone(3);

            env.sendEventBean(new SupportBean_S1(2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields,
                new Object[][]{{1, "E1"}, {1, "E2"}});

            env.milestone(4);

            env.sendEventBean(new SupportBean_S1(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields,
                new Object[0][]);

            env.undeployAll();
        }
    }

    public static class ContextNestedInitTermOverPartitionedIterate implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context NestedContext " +
                "context FirstCtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id = s0.id), " +
                "context SecondCtx partition by theString from SupportBean", path);

            String[] fields = "c0,c1".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.FirstCtx.s0.id as c0, theString as c1 from SupportBean#keepall", path);
            env.addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S0(2));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{{1, "E1"}, {2, "E1"}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 1));
            Object[][] expectedAll = new Object[][]{{1, "E1"}, {2, "E1"}, {1, "E2"}, {2, "E2"}};
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, expectedAll);

            // all-selector
            SupportSelectorNested selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new ContextPartitionSelectorAll());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);
            // segmented-specific-selector
            selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new SupportSelectorPartitioned(Arrays.asList(new Object[]{"E1"}, new Object[]{"E2"})));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);
            // filter-specific-selector
            selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new SupportSelectorFilteredPassAll());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);

            env.milestone(3);

            env.sendEventBean(new SupportBean_S1(2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{{1, "E1"}, {1, "E2"}});

            env.milestone(4);

            env.sendEventBean(new SupportBean_S1(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[0][]);

            env.undeployAll();
        }
    }

    public static class ContextNestedInitTermOverCategoryIterate implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context NestedContext " +
                "context FirstCtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id = s0.id), " +
                "context SecondCtx group by theString = 'E1' as cat1, group by theString = 'E2' as cat2 from SupportBean", path);

            String[] fields = "c0,c1".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.FirstCtx.s0.id as c0, theString as c1 from SupportBean#keepall", path);
            env.addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S0(2));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{{1, "E1"}, {2, "E1"}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 1));
            Object[][] expectedAll = new Object[][]{{1, "E1"}, {2, "E1"}, {1, "E2"}, {2, "E2"}};
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, expectedAll);

            // all-selector
            SupportSelectorNested selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new ContextPartitionSelectorAll());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);
            // category-specific-selector
            selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new SupportSelectorCategory(new HashSet(Arrays.asList("cat1,cat2".split(",")))));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);
            // filter-specific-selector
            selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new SupportSelectorFilteredPassAll());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);

            env.milestone(3);

            env.sendEventBean(new SupportBean_S1(2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{{1, "E1"}, {1, "E2"}});

            env.milestone(4);

            env.sendEventBean(new SupportBean_S1(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[0][]);

            env.undeployAll();
        }
    }

    public static class ContextNestedInitTermOverInitTermIterate implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context NestedContext " +
                "context FirstCtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id = s0.id), " +
                "context SecondCtx initiated by SupportBean_S2 as s2 terminated after 24 hours", path);

            String[] fields = "c0,c1,c2".split(",");
            env.compileDeploy("@name('s0') context NestedContext select " +
                "context.FirstCtx.s0.id as c0, context.SecondCtx.s2.id as c1, theString as c2 from SupportBean#keepall", path);
            env.addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S0(2));
            env.sendEventBean(new SupportBean_S2(10));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{{1, 10, "E1"}, {2, 10, "E1"}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 1));
            Object[][] expectedAll = new Object[][]{{1, 10, "E1"}, {2, 10, "E1"}, {1, 10, "E2"}, {2, 10, "E2"}};
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, expectedAll);

            // all-selector
            SupportSelectorNested selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new ContextPartitionSelectorAll());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);
            // filter-specific-selector
            selectorNested = new SupportSelectorNested(new ContextPartitionSelectorAll(), new SupportSelectorFilteredPassAll());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorNested), fields, expectedAll);

            env.milestone(3);

            env.sendEventBean(new SupportBean_S1(2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{{1, 10, "E1"}, {1, 10, "E2"}});

            env.milestone(4);

            env.sendEventBean(new SupportBean_S1(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[0][]);

            env.undeployAll();
        }
    }

    public static class ContextNestedCategoryOverInitTermDistinct implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NestedContext " +
                "context ACtx group by intPrimitive < 0 as grp1, group by intPrimitive = 0 as grp2, group by intPrimitive > 0 as grp3 from SupportBean, " +
                "context BCtx initiated by distinct(a.intPrimitive) SupportBean(theString='A') as a terminated by SupportBean(theString='B') ", path);
            env.compileDeploy("@name('s0') context NestedContext select count(*) as cnt from SupportBean(intPrimitive = context.BCtx.a.intPrimitive and theString != 'B')", path).addListener("s0");

            sendBeanAssertCount(env, "A", 10, 1);
            sendBeanAssertCount(env, "A", 10, 2);

            env.milestone(0);

            sendBeanAssertCount(env, "A", -5, 1);
            sendBeanAssertCount(env, "A", -4, 1);
            env.sendEventBean(new SupportBean("B", 10));

            env.milestone(1);

            sendBeanAssertCount(env, "A", 10, 1);
            sendBeanAssertCount(env, "A", -5, 2);

            env.milestone(2);

            env.sendEventBean(new SupportBean("B", -5));
            sendBeanAssertCount(env, "A", -5, 1);

            env.undeployAll();
        }

        private void sendBeanAssertCount(RegressionEnvironment env, String theString, int intPrimitive, long count) {
            env.sendEventBean(new SupportBean(theString, intPrimitive));
            assertEquals(count, env.listener("s0").assertOneGetNewAndReset().get("cnt"));
        }
    }

    private static void tryAssertion(RegressionEnvironment env, RegressionPath path) {

        env.advanceTime(0);
        String[] fields = "c0,c1".split(",");
        env.compileDeploy("@name('s0') context NestedContext " +
            "select theString as c0, sum(intPrimitive) as c1 from SupportBean \n" +
            "output last when terminated", path);
        env.addListener("s0");

        env.sendEventBean(new SupportBean("E1", 1));
        env.sendEventBean(new SupportBean("E2", 2));

        env.milestone(0);

        env.advanceTime(10000);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getDataListsFlattened(), fields,
            new Object[][]{{"E1", 1}, {"E2", 2}}, null);
        env.listener("s0").reset();

        env.sendEventBean(new SupportBean("E1", 3));
        env.sendEventBean(new SupportBean("E3", 4));

        env.milestone(1);

        env.advanceTime(20000);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getDataListsFlattened(), fields,
            new Object[][]{{"E1", 3}, {"E3", 4}}, null);

        env.undeployAll();
    }

    private static void tryAssertion3Contexts(RegressionEnvironment env, AtomicInteger milestone, String[] fields, String startTime, String subsequentTime) {
        env.sendEventBean(makeEvent("E1", 0, 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g2", "E1", 10L});

        assertPartitionInfo(env, startTime);

        env.sendEventBean(makeEvent("E2", 0, 11));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g2", "E2", 11L});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E1", 0, 12));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g2", "E1", 22L});

        env.sendEventBean(makeEvent("E1", 1, 13));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g3", "E1", 13L});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E1", -1, 14));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g1", "E1", 14L});

        env.sendEventBean(makeEvent("E2", -1, 15));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"g1", "E2", 15L});

        sendTimeEvent(env, subsequentTime);

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E2", -1, 15));
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }

    public static boolean customMatch(String theString, String p00, int intPrimitive, int s1id) {
        assertEquals("X", theString);
        assertEquals("S0", p00);
        assertEquals(-1, intPrimitive);
        assertEquals(2, s1id);
        return true;
    }

    private static Object makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    private static Object makeEvent(String theString, int intPrimitive, long longPrimitive, boolean boolPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        bean.setBoolPrimitive(boolPrimitive);
        return bean;
    }

    private static void sendTimeEvent(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void assertPartitionInfo(RegressionEnvironment env, String startTime) {
        EPContextPartitionService partitionAdmin = env.runtime().getContextPartitionService();
        String deploymentId = env.statement("ctx").getDeploymentId();
        ContextPartitionCollection partitions = partitionAdmin.getContextPartitions(deploymentId, "NestedContext", ContextPartitionSelectorAll.INSTANCE);
        Assert.assertEquals(1, partitions.getIdentifiers().size());
        ContextPartitionIdentifierNested nested = (ContextPartitionIdentifierNested) partitions.getIdentifiers().values().iterator().next();
        assertNested(nested, startTime);
    }

    private static void assertNested(ContextPartitionIdentifierNested nested, String startTime) {
        Assert.assertEquals(DateTime.parseDefaultMSec(startTime), ((ContextPartitionIdentifierInitiatedTerminated) nested.getIdentifiers()[0]).getStartTime());
        Assert.assertEquals("g2", ((ContextPartitionIdentifierCategory) nested.getIdentifiers()[1]).getLabel());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, ((ContextPartitionIdentifierPartitioned) nested.getIdentifiers()[2]).getKeys());
    }

    private static SupportBean sendBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive, boolean boolPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setBoolPrimitive(boolPrimitive);
        sb.setLongPrimitive(longPrimitive);
        env.sendEventBean(sb);
        return sb;
    }

    private static void assertFilterCount(RegressionEnvironment env, int count, String stmtName) {
        EPStatement statement = env.statement(stmtName);
        assertEquals(count, SupportFilterHelper.getFilterCount(statement, "SupportBean"));
    }

    public static class MySelectorFilteredNested implements ContextPartitionSelectorFiltered {

        private final Object[] pathMatch;

        private List<Object[]> paths = new ArrayList<Object[]>();
        private LinkedHashSet<Integer> cpids = new LinkedHashSet<Integer>();

        public MySelectorFilteredNested(Object[] pathMatch) {
            this.pathMatch = pathMatch;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierNested nested = (ContextPartitionIdentifierNested) contextPartitionIdentifier;
            if (pathMatch == null && cpids.contains(nested.getContextPartitionId())) {
                throw new RuntimeException("Already exists context id: " + nested.getContextPartitionId());
            }
            cpids.add(nested.getContextPartitionId());

            ContextPartitionIdentifierInitiatedTerminated first = (ContextPartitionIdentifierInitiatedTerminated) nested.getIdentifiers()[0];
            ContextPartitionIdentifierCategory second = (ContextPartitionIdentifierCategory) nested.getIdentifiers()[1];

            Object[] extract = new Object[2];
            extract[0] = ((EventBean) first.getProperties().get("s0")).get("p00");
            extract[1] = second.getLabel();
            paths.add(extract);

            return paths != null && Arrays.equals(pathMatch, extract);
        }
    }
}
