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
package com.espertech.esper.regression.context;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.context.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.filter.FilterSet;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.context.SupportContextPropUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class ExecContextPartitionedWInitTermPrioritized implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class, SupportBean_S2.class, ISupportA.class, ISupportB.class}) {
            configuration.addEventType(clazz);
        }
        configuration.getEngineDefaults().getExecution().setPrioritized(true);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionTermByFilter(epService);
        runAssertionTermByFilterWSubtype(epService);
        runAssertionTermByFilterWSecondType(epService);
        runAssertionTermByAfter(epService);
        runAssertionTermByCrontabOutputWhenTerminated(epService);
        runAssertionTermByPatternTwoFilters(epService);
        runAssertionTermByUnrelated(epService);
        runAssertionFilterExprTermByFilterWExpr(epService);
        runAssertionFilterExprTermByFilter(epService);
        runAssertionNestedCtxStartStop(epService);
        runAssertionNestedCtxFilter(epService);
        runAssertionTermByFilter2Keys(epService);
        runAssertionTermByPattern3Partition(epService);
        runAssertionInitTermNoPartitionFilter(epService);
        runAssertionInitTermWithPartitionFilter(epService);
        runAssertionInitTermWithTwoInit(epService);
        runAssertionInitNoTerm(epService);
        runAssertionInitWCorrelatedTermFilter(epService);
        runAssertionInitWCorrelatedTermPattern(epService);
        runAssertionPartitionWithCorrelatedTermFilter(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionPartitionWithCorrelatedTermFilter(EPServiceProvider epService) {
        runAssertionPartitionWithCorrelatedTermFilter(epService, false);
        runAssertionPartitionWithCorrelatedTermFilter(epService, true);
    }

    private void runAssertionPartitionWithCorrelatedTermFilter(EPServiceProvider epService, boolean soda) {
        String epl = "create context CtxPartitionWCorrTerm as " +
                "partition by theString from SupportBean as sb " +
                "terminated by SupportBean(intPrimitive=sb.intPrimitive)";
        EPStatement ctx = SupportModelHelper.createByCompileOrParse(epService, soda, epl);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context CtxPartitionWCorrTerm select theString, sum(intPrimitive) as theSum from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,theSum".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("A", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 99));
        epService.getEPRuntime().sendEvent(new SupportBean("C", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("C", 4));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 10+2});

        epService.getEPRuntime().sendEvent(new SupportBean("C", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"C", -1+4});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 12));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 99));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 99+3});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 11+12});

        assertFilterCount(1, ctx);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInitWCorrelatedTermFilter(EPServiceProvider epService) {
        String epl = "create context CtxPartitionInitWCorrTerm " +
                "partition by theString from SupportBean " +
                "initiated by SupportBean(boolPrimitive=true) as sb " +
                "terminated by SupportBean(boolPrimitive=false, intPrimitive=sb.intPrimitive)";
        EPStatement ctx = epService.getEPAdministrator().createEPL(epl);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context CtxPartitionInitWCorrTerm select theString, sum(longPrimitive) as theSum from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,theSum".split(",");

        SupportBean initA = sendBean(epService, "A", 100, 1, true);
        sendBean(epService, "B", 99, 2, false);
        SupportBean initB = sendBean(epService, "B", 200, 3, true);
        sendBean(epService, "A", 0, 4, false);
        sendBean(epService, "B", 0, 5, false);
        sendBean(epService, "A", 0, 6, true);
        assertFalse(listener.isInvoked());
        assertPartitionsInitWCorrelatedTermFilter(epService);
        SupportContextPropUtil.assertContextProps(epService, "CtxPartitionInitWCorrTerm", new int[] {0, 1}, "key1,sb", new Object[][] {{"A", initA}, {"B", initB}});

        sendBean(epService, "B", 200, 7, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 3+5L});

        sendBean(epService, "A", 100, 8, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 1+4+6L});

        assertFilterCount(1, ctx);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertPartitionsInitWCorrelatedTermFilter(EPServiceProvider epService) {
        ContextPartitionCollection partitions = epService.getEPAdministrator().getContextPartitionAdmin().getContextPartitions("CtxPartitionInitWCorrTerm", ContextPartitionSelectorAll.INSTANCE);
        assertEquals(2, partitions.getDescriptors().size());
        ContextPartitionIdentifierPartitioned first = (ContextPartitionIdentifierPartitioned) partitions.getDescriptors().get(0).getIdentifier();
        ContextPartitionIdentifierPartitioned second = (ContextPartitionIdentifierPartitioned) partitions.getDescriptors().get(1).getIdentifier();
        EPAssertionUtil.assertEqualsExactOrder(new Object[] {"A"}, first.getKeys());
        EPAssertionUtil.assertEqualsExactOrder(new Object[] {"B"}, second.getKeys());
    }

    private void runAssertionInitWCorrelatedTermPattern(EPServiceProvider epService) {
        String epl = "create context CtxPartitionInitWCorrTerm " +
                "partition by p20 from SupportBean_S2, p10 from SupportBean_S1, p00 from SupportBean_S0 " +
                "initiated by SupportBean_S0 as s0, SupportBean_S1 as s1 " +
                "terminated by pattern[SupportBean_S0(id=s0.id) or SupportBean_S1(id=s1.id)]";
        EPStatement ctx = epService.getEPAdministrator().createEPL(epl);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context CtxPartitionInitWCorrTerm select context.s0 as ctx0, context.s1 as ctx1, context.s0.id as ctx0id, context.s1.id as ctx1id, p20, sum(id) as theSum from SupportBean_S2 output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "ctx0id,ctx1id,p20,theSum".split(",");

        assertEquals(SupportBean_S0.class, stmt.getEventType().getPropertyType("ctx0"));
        assertEquals(SupportBean_S1.class, stmt.getEventType().getPropertyType("ctx1"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "B"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(10, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(11, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(12, "B"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {1, null, "A", 21});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "B"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {null, 2, "B", 12});

        assertFilterCount(2, ctx);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInitTermWithTwoInit(EPServiceProvider epService) {
        runAssertionInitTermWithTwoInit(epService, false);
        runAssertionInitTermWithTwoInit(epService, true);
    }

    private void runAssertionInitNoTerm(EPServiceProvider epService) {
        runAssertionInitNoTerm(epService, false);
        runAssertionInitNoTerm(epService, true);
    }

    private void runAssertionInitNoTerm(EPServiceProvider epService, boolean soda) {
        String epl = "create context CtxInitS0PositiveId as " +
                "partition by p00 and p01 from SupportBean_S0 " +
                "initiated by SupportBean_S0(id>0) as s0";
        SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        EPStatement stmt = epService.getEPAdministrator().createEPL("context CtxInitS0PositiveId select p00, p01, context.s0 as s0, sum(id) as theSum from SupportBean_S0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(SupportBean_S0.class, stmt.getEventType().getPropertyType("s0"));

        sendS0AssertNone(epService, listener, 0, "A", "G1");
        sendS0AssertNone(epService, listener, -1, "B", "G1");
        SupportBean_S0 s0BG1 = sendS0Assert(10, null, epService, listener, 10, "B", "G1");
        sendS0Assert(9, s0BG1, epService, listener, -1, "B", "G1");
        SupportBean_S0 s0AG1 = sendS0Assert(2, null, epService, listener, 2, "A", "G1");
        SupportBean_S0 s0AG2 = sendS0Assert(3, null, epService, listener, 3, "A", "G2");
        sendS0Assert(7, s0AG2, epService, listener, 4, "A", "G2");
        sendS0Assert(8, s0AG1, epService, listener, 6, "A", "G1");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInitTermWithTwoInit(EPServiceProvider epService, boolean soda) {
        String epl = "create context CtxTwoInitTerm as " +
                "partition by p01 from SupportBean_S0, p11 from SupportBean_S1, p21 from SupportBean_S2 " +
                "initiated by SupportBean_S0(p00=\"a\"), SupportBean_S1(p10=\"b\") " +
                "terminated by SupportBean_S2(p20=\"z\")";
        EPStatement ctx = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        EPStatement stmt = epService.getEPAdministrator().createEPL("context CtxTwoInitTerm select p21, count(*) as cnt from SupportBean_S2 output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "p21,cnt".split(",");

        sendS2(epService, "b", "A");
        sendS2(epService, "a", "A");
        sendS0(epService, "b", "A");
        sendS1(epService, "a", "A");
        sendS2(epService, "z", "A");
        sendS1(epService, "b", "B");
        sendS0(epService, "a", "C");
        sendS2(epService, "", "B");
        assertFalse(listener.isInvoked());

        sendS2(epService, "z", "B");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 1L});

        sendS2(epService, "z", "C");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"C", 0L});

        assertFilterCount(2, ctx);
        stmt.destroy();
        assertFilterCount(0, ctx);
        ctx.destroy();
    }

    private void runAssertionInitTermWithPartitionFilter(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context CtxStringZeroTo1k as " +
                "partition by theString from SupportBean(intPrimitive > 0) " +
                "initiated by SupportBean(intPrimitive=0)" +
                "terminated by SupportBean(intPrimitive=1000)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context CtxStringZeroTo1k select theString, sum(intPrimitive) as theSum from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,theSum".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("A", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 1000));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("B", -100));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 1000));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B", 1000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 30});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 40));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 50));
        epService.getEPRuntime().sendEvent(new SupportBean("A", -20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 90});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        // invalid initiated-by type
        epl = "create context InvalidCtx partition by theString from SupportBean initiated by SupportBean_S0";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Segmented context 'InvalidCtx' requires that all of the event types that are listed in the initialized-by also appear in the partition-by, type 'SupportBean_S0' is not one of the types listed in partition-by");

        // cannot assign name in different places
        epl = "create context InvalidCtx partition by p00 from SupportBean_S0 as n1 initiated by SupportBean_S0 as n2";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Segmented context 'InvalidCtx' requires that either partition-by or initialized-by assign stream names, but not both");

        // name assigned is already used
        String message = "Error starting statement: Name 'a' already used for type 'SupportBean_S0'";
        epl = "create context InvalidCtx partition by p00 from SupportBean_S0, p10 from SupportBean_S1 initiated by SupportBean_S0 as a, SupportBean_S1 as a";
        SupportMessageAssertUtil.tryInvalid(epService, epl, message);
        epl = "create context InvalidCtx partition by p00 from SupportBean_S0 as a, p10 from SupportBean_S1 as a";
        SupportMessageAssertUtil.tryInvalid(epService, epl, message);
    }

    private void runAssertionInitTermNoPartitionFilter(EPServiceProvider epService) {
        EPStatement ctx = epService.getEPAdministrator().createEPL("create context CtxStringZeroTo1k as " +
                "partition by theString from SupportBean " +
                "initiated by SupportBean(intPrimitive=0)" +
                "terminated by SupportBean(intPrimitive=1000)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context CtxStringZeroTo1k select theString, sum(intPrimitive) as theSum from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,theSum".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("A", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 1000));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 1000));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B", 1000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 30});

        epService.getEPRuntime().sendEvent(new SupportBean("C", 1000));
        epService.getEPRuntime().sendEvent(new SupportBean("C", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("C", 1000));
        epService.getEPRuntime().sendEvent(new SupportBean("C", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 40));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("C", 1000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"C", 0});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 40});

        assertFilterCount(1, ctx);
        stmt.destroy();
        assertFilterCount(0, ctx);
        ctx.destroy();
    }

    private void runAssertionTermByPattern3Partition(EPServiceProvider epService) {
        EPStatement ctx = epService.getEPAdministrator().createEPL("create context Ctx3Typed as " +
                "partition by p00 from SupportBean_S0, p10 from SupportBean_S1, p20 from SupportBean_S2 " +
                "terminated by pattern[SupportBean_S1 -> SupportBean_S2]");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context Ctx3Typed select p00, count(*) as cnt from SupportBean_S0 output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "p00,cnt".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "B"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "B"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(0, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "B"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(0, "B"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 2L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "A"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(0, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 3L});

        assertFilterCount(3, ctx);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTermByFilter2Keys(EPServiceProvider epService) {
        EPStatement ctx = epService.getEPAdministrator().createEPL("create context TwoKeyPartition " +
                "partition by theString, intPrimitive from SupportBean terminated by SupportBean(boolPrimitive = false)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context TwoKeyPartition select theString, intPrimitive, sum(longPrimitive) as thesum from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,intPrimitive,thesum".split(",");

        sendBean(epService, "A", 1, 10, true);
        sendBean(epService, "B", 1, 11, true);
        sendBean(epService, "A", 2, 12, true);
        sendBean(epService, "B", 2, 13, true);
        sendBean(epService, "B", 1, 20, true);
        sendBean(epService, "A", 1, 30, true);
        sendBean(epService, "A", 2, 40, true);
        sendBean(epService, "B", 2, 50, true);

        sendBean(epService, "A", 2, 0, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 2, 52L});

        sendBean(epService, "B", 2, 0, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 2, 63L});

        sendBean(epService, "A", 1, 0, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 1, 40L});

        sendBean(epService, "B", 1, 0, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 1, 31L});

        assertFilterCount(1, ctx);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNestedCtxFilter(EPServiceProvider epService) {
        EPStatement ctx = epService.getEPAdministrator().createEPL("create context NestedCtxWPartition " +
                "context ByString partition by theString from SupportBean, " +
                "context ByInt partition by intPrimitive from SupportBean terminated by SupportBean(boolPrimitive=false)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context NestedCtxWPartition select theString, intPrimitive, sum(longPrimitive) as thesum from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,intPrimitive,thesum".split(",");

        sendBean(epService, "A", 1, 10, true);
        sendBean(epService, "B", 1, 11, true);
        sendBean(epService, "A", 2, 12, true);
        sendBean(epService, "B", 2, 13, true);
        sendBean(epService, "B", 1, 20, true);
        sendBean(epService, "A", 1, 30, true);
        sendBean(epService, "A", 2, 40, true);
        sendBean(epService, "B", 2, 50, true);

        sendBean(epService, "A", 1, 0, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 1, 40L});

        sendBean(epService, "B", 2, 0, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 2, 63L});

        sendBean(epService, "A", 2, 0, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 2, 52L});

        sendBean(epService, "B", 1, 0, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 1, 31L});

        assertFilterCount(3, ctx);
        stmt.destroy();
        assertFilterCount(0, ctx);
        ctx.destroy();
    }

    private void runAssertionNestedCtxStartStop(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        EPStatement ctx = epService.getEPAdministrator().createEPL("create context NestedCtxWTime " +
                "context OuterCtx initiated @now and pattern[timer:interval(10000000)] terminated after 1 second, " +
                "context InnerCtx partition by theString from SupportBean(intPrimitive=0) terminated by SupportBean(intPrimitive=1)");
        epService.getEPAdministrator().createEPL("context NestedCtxWTime select theString, count(*) as cnt from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 0));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(100000));
        assertFilterCount(0, ctx);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterExprTermByFilter(EPServiceProvider epService) {
        EPStatement ctx = epService.getEPAdministrator().createEPL("@audit create context MyTermByUnrelated partition by theString from SupportBean(intPrimitive=0) terminated by SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyTermByUnrelated select theString, count(*) as cnt from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,cnt".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B", 99));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][] {{"B", 1L}});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][] {{"A", 1L}});

        stmt.destroy();
        assertFilterCount(0, ctx);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterExprTermByFilterWExpr(EPServiceProvider epService) {
        EPStatement ctx = epService.getEPAdministrator().createEPL("@audit create context MyTermByUnrelated partition by theString from SupportBean(intPrimitive=0) terminated by SupportBean(intPrimitive=1)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyTermByUnrelated select theString, count(*) as cnt from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,cnt".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][] {{"B", 1L}});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        stmt.destroy();
        assertFilterCount(0, ctx);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTermByUnrelated(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context MyTermByUnrelated partition by theString from SupportBean terminated by SupportBean_S0");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyTermByUnrelated select theString, count(*) as cnt from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,cnt".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][] {{"A", 2L}, {"B", 1L}});

        epService.getEPRuntime().sendEvent(new SupportBean("C", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][] {{"A", 1L}, {"C", 1L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTermByPatternTwoFilters(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context MyTermByTimeout partition by p00 from SupportBean_S0, p10 from SupportBean_S1 terminated by pattern [SupportBean_S0(id<0) or SupportBean_S1(id<0)]");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyTermByTimeout select coalesce(s0.p00, s1.p10) as key, count(*) as cnt from pattern [every (s0=SupportBean_S0 or s1=SupportBean_S1)] output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "key,cnt".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "B"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1, "B")); // stop B
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "B"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "B"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1, "A")); // stop A
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 3L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1, "A")); // stop A
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1, "B")); // stop B
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 2L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1, "B")); // stop B
        assertFalse(listener.isInvoked());
    }

    private void runAssertionTermByCrontabOutputWhenTerminated(EPServiceProvider epService) throws Exception {
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");

        epService.getEPAdministrator().createEPL("create context MyTermByTimeout partition by theString from SupportBean terminated (*, *, *, *, *)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyTermByTimeout select theString, count(*) as cnt from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));

        sendCurrentTime(epService, "2002-02-01T09:00:05.000");

        epService.getEPRuntime().sendEvent(new SupportBean("B", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));

        sendCurrentTime(epService, "2002-02-01T09:00:59.999");

        epService.getEPRuntime().sendEvent(new SupportBean("B", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-02-01T09:01:00.000");
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "theString,cnt".split(","), new Object[][] {{"A", 3L}, {"B", 2L}});

        epService.getEPRuntime().sendEvent(new SupportBean("C", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        sendCurrentTime(epService, "2002-02-01T09:01:30.000");
        epService.getEPRuntime().sendEvent(new SupportBean("D", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("C", 0));

        sendCurrentTime(epService, "2002-02-01T09:02:00.000");
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "theString,cnt".split(","), new Object[][] {{"A", 1L}, {"C", 2L}, {"D", 1L}});

        sendCurrentTime(epService, "2002-02-01T09:03:00.000");
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTermByAfter(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create context MyTermByTimeout partition by theString from SupportBean terminated after 10 seconds");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyTermByTimeout select theString, count(*) as cnt from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendAssertSB(1, epService, listener, "A");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        sendAssertSB(2, epService, listener, "A");
        sendAssertSB(1, epService, listener, "B");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(9999));

        sendAssertSB(2, epService, listener, "B");
        sendAssertSB(3, epService, listener, "A");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));

        sendAssertSB(3, epService, listener, "B");
        sendAssertSB(1, epService, listener, "A");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10999));

        sendAssertSB(4, epService, listener, "B");
        sendAssertSB(2, epService, listener, "A");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(11000));

        sendAssertSB(1, epService, listener, "B");
        sendAssertSB(3, epService, listener, "A");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(99999));

        sendAssertSB(1, epService, listener, "B");
        sendAssertSB(1, epService, listener, "A");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTermByFilterWSecondType(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema TypeOne(poa string)");
        epService.getEPAdministrator().createEPL("create map schema TypeTwo(pmap string)");
        epService.getEPAdministrator().createEPL("create context MyContextOAMap partition by poa from TypeOne, pmap from TypeTwo terminated by TypeTwo");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyContextOAMap select poa, count(*) as cnt from TypeOne");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendOAAssert(epService, listener, "A", 1L);
        sendOAAssert(epService, listener, "B", 1L);
        sendOAAssert(epService, listener, "A", 2L);
        sendOAAssert(epService, listener, "B", 2L);

        epService.getEPRuntime().sendEvent(CollectionUtil.populateNameValueMap("pmap", "B"), "TypeTwo");

        sendOAAssert(epService, listener, "A", 3L);
        sendOAAssert(epService, listener, "B", 1L);

        epService.getEPRuntime().sendEvent(CollectionUtil.populateNameValueMap("pmap", "A"), "TypeTwo");

        sendOAAssert(epService, listener, "A", 1L);
        sendOAAssert(epService, listener, "B", 2L);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("TypeOne", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("TypeTwo", true);
    }

    private void sendOAAssert(EPServiceProvider epService, SupportUpdateListener listener, String poa, long count) {
        epService.getEPRuntime().sendEvent(new Object[] {poa}, "TypeOne");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "poa,cnt".split(","), new Object[] {poa, count});
    }

    private void runAssertionTermByFilterWSubtype(EPServiceProvider epService) {
        EPStatement ctx = epService.getEPAdministrator().createEPL("create context ByP0 partition by a from ISupportA, b from ISupportB terminated by ISupportA(a='x')");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context ByP0 select coalesce(a.a, b.b) as p0, count(*) as cnt from pattern[every (a=ISupportA or b=ISupportB)]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new ISupportABCImpl("a", "a", null, null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "p0,cnt".split(","), new Object[] {"a", 1L});

        epService.getEPRuntime().sendEvent(new ISupportAImpl("a", null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "p0,cnt".split(","), new Object[] {"a", 2L});

        epService.getEPRuntime().sendEvent(new ISupportBImpl("a", null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "p0,cnt".split(","), new Object[] {"a", 3L});

        ctx.destroy();
        stmt.destroy();
    }

    private void runAssertionTermByFilter(EPServiceProvider epService) {
        runAssertionTermByFilter(epService, false);
        runAssertionTermByFilter(epService, true);
    }

    private void runAssertionTermByFilter(EPServiceProvider epService, boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create context ByP0 as partition by theString from SupportBean terminated by SupportBean(intPrimitive<0)");
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, "context ByP0 select theString, count(*) as cnt from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendAssertSB(1, epService, listener, "A", 0);
        sendAssertSB(2, epService, listener, "A", 0);
        sendAssertNone(epService, listener, new SupportBean("A", -1));
        sendAssertSB(1, epService, listener, "A", 0);

        sendAssertSB(1, epService, listener, "B", 0);
        sendAssertNone(epService, listener, new SupportBean("B", -1));
        sendAssertSB(1, epService, listener, "B", 0);
        sendAssertSB(2, epService, listener, "B", 0);
        sendAssertNone(epService, listener, new SupportBean("B", -1));
        sendAssertSB(1, epService, listener, "B", 0);

        sendAssertSB(1, epService, listener, "C", -1);
        sendAssertNone(epService, listener, new SupportBean("C", -1));
        sendAssertSB(1, epService, listener, "C", -1);
        sendAssertNone(epService, listener, new SupportBean("C", -1));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendAssertSB(long expected, EPServiceProvider epService, SupportUpdateListener listener, String theString) {
        sendAssertSB(expected, epService, listener, theString, 0);
    }

    private void sendAssertSB(long expected, EPServiceProvider epService, SupportUpdateListener listener, String theString, int intPrimitive) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString,cnt".split(","), new Object[] {theString, expected});
    }

    private void sendAssertNone(EPServiceProvider epService, SupportUpdateListener listener, Object event) {
        epService.getEPRuntime().sendEvent(event);
        assertFalse(listener.isInvoked());
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private SupportBean sendBean(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive, boolean boolPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setBoolPrimitive(boolPrimitive);
        sb.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(sb);
        return sb;
    }

    private void sendS0(EPServiceProvider epService, String p00, String p01) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00, p01));
    }

    private void sendS1(EPServiceProvider epService, String p10, String p11) {
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, p10, p11));
    }

    private void sendS2(EPServiceProvider epService, String p20, String p21) {
        epService.getEPRuntime().sendEvent(new SupportBean_S2(0, p20, p21));
    }

    private void sendS0AssertNone(EPServiceProvider epService, SupportUpdateListener listener, int id, String p00, String p01) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(id, p00, p01));
        assertFalse(listener.isInvoked());
    }

    private SupportBean_S0 sendS0Assert(int expected, SupportBean_S0 s0Init, EPServiceProvider epService, SupportUpdateListener listener, int id, String p00, String p01) {
        SupportBean_S0 s0 = new SupportBean_S0(id, p00, p01);
        epService.getEPRuntime().sendEvent(s0);
        String[] fields = "p00,p01,s0,theSum".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {p00, p01, s0Init == null ? s0 : s0Init, expected});
        return s0;
    }

    private void assertFilterCount(int count, EPStatement statement) {
        EPStatementSPI stmtSPI = (EPStatementSPI) statement;
        FilterServiceSPI filterSPI = (FilterServiceSPI) stmtSPI.getStatementContext().getFilterService();
        if (filterSPI.isSupportsTakeApply()) {
            FilterSet set = filterSPI.take(Collections.singleton(stmtSPI.getStatementId()));
            assertEquals(count, set.getFilters().size());
        }
    }
}
