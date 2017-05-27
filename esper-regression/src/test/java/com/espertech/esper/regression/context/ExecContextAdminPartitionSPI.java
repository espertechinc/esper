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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.context.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.ScopeTestHelper;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.context.mgr.AgentInstance;
import com.espertech.esper.core.context.mgr.AgentInstanceSelector;
import com.espertech.esper.core.context.mgr.ContextStatePathKey;
import com.espertech.esper.core.service.EPContextPartitionAdminSPI;
import com.espertech.esper.core.service.EPContextPartitionExtract;
import com.espertech.esper.core.service.EPContextPartitionImportResult;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.context.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.ContextState;
import com.espertech.esper.supportregression.util.ContextStateCacheHook;
import com.espertech.esper.supportregression.util.SupportContextStateCacheImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecContextAdminPartitionSPI implements RegressionExecution, ContextStateCacheHook {

    private final static SupportHashCodeFuncGranularCRC32 CODE_FUNC_MOD64 = new SupportHashCodeFuncGranularCRC32(64);
    private final static String[] FIELDS = "c0,c1".split(",");
    private final static String[] FIELDSCP = "c0,c1,c2".split(",");
    private final static int HASH_MOD_E1_STRING_BY_64 = 5;

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionDestroyCtxPartitions(epService);
        runAssertionInvalid(epService);
        runAssertionStopStartNestedCtxPartitions(epService);
        runAssertionGetContextStatementNames(epService);
        runAssertionAcrossURIExtractImport(epService);
        runAssertionSameURIExtractStopImportStart(epService);
    }

    private void runAssertionDestroyCtxPartitions(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecContextAdminPartitionSPI.class)) {
            return;
        }

        assertExtractDestroyPartitionedById(epService);
        assertDestroyCategory(epService);
        assertDestroyHashSegmented(epService);
        assertDestroyPartitioned(epService);
        assertDestroyInitTerm(epService);
        assertDestroyNested(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecContextAdminPartitionSPI.class)) {
            return;
        }

        // context not found
        try {
            getSpi(epService).getContextNestingLevel("undefined");
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Context by name 'undefined' could not be found", ex.getMessage());
        }

        // invalid selector for context
        epService.getEPAdministrator().createEPL("create context SomeContext partition by theString from SupportBean");
        try {
            getSpi(epService).destroyContextPartitions("SomeContext", new SupportSelectorCategory("abc"));
            fail();
        } catch (InvalidContextPartitionSelector ex) {
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStopStartNestedCtxPartitions(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecContextAdminPartitionSPI.class)) {
            return;
        }

        String contextName = "CategoryContext";
        String createCtx = CONTEXT_CACHE_HOOK + "create context CategoryContext as " +
                "group by intPrimitive < 0 as negative, group by intPrimitive > 0 as positive from SupportBean";
        epService.getEPAdministrator().createEPL(createCtx);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context CategoryContext " +
                "select theString as c0, sum(intPrimitive) as c1, context.id as c2 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -5));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 20));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E1", 10, 1}, {"E2", -5, 0}, {"E3", 30, 1}});

        // stop category "negative"
        SupportContextStateCacheImpl.reset();
        ContextPartitionCollection collStop = getSpi(epService).stopContextPartitions("CategoryContext", new SupportSelectorCategory("negative"));
        assertPathInfo(collStop.getDescriptors(), new Object[][]{{0, makeIdentCat("negative"), "+"}});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{{0, makeIdentCat("negative"), "-"}, {1, makeIdentCat("positive"), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, 0, false));

        epService.getEPRuntime().sendEvent(new SupportBean("E4", -6));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 30));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E5", 60, 1}});

        // start category "negative"
        ContextPartitionCollection collStart = getSpi(epService).startContextPartitions("CategoryContext", new SupportSelectorCategory("negative"));
        assertPathInfo(collStart.getDescriptors(), new Object[][]{{0, makeIdentCat("negative"), "+"}});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{{0, makeIdentCat("negative"), "+"}, {1, makeIdentCat("positive"), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, 0, true));

        epService.getEPRuntime().sendEvent(new SupportBean("E6", -7));
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 40));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E6", -7, 0}, {"E7", 100, 1}});

        // stop category "positive"
        SupportContextStateCacheImpl.reset();
        getSpi(epService).stopContextPartition("CategoryContext", 1);
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{{0, makeIdentCat("negative"), "+"}, {1, makeIdentCat("positive"), "-"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 2, 1, 1, false));

        epService.getEPRuntime().sendEvent(new SupportBean("E8", -8));
        epService.getEPRuntime().sendEvent(new SupportBean("E9", 50));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E8", -15, 0}});

        // start category "positive"
        getSpi(epService).startContextPartition("CategoryContext", 1);
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{{0, makeIdentCat("negative"), "+"}, {1, makeIdentCat("positive"), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 2, 1, 1, true));

        epService.getEPRuntime().sendEvent(new SupportBean("E10", -9));
        epService.getEPRuntime().sendEvent(new SupportBean("E11", 60));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E10", -24, 0}, {"E11", 60, 1}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionGetContextStatementNames(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecContextAdminPartitionSPI.class)) {
            return;
        }

        epService.getEPAdministrator().createEPL("create context CtxA partition by theString from SupportBean");
        EPStatement stmtA = epService.getEPAdministrator().createEPL("@Name('A') context CtxA select count(*) from SupportBean");
        EPStatement stmtB = epService.getEPAdministrator().createEPL("@Name('B') context CtxA select sum(intPrimitive) from SupportBean");

        EPAssertionUtil.assertEqualsAnyOrder(getSpi(epService).getContextStatementNames("CtxA"), "A,B".split(","));

        stmtA.destroy();
        EPAssertionUtil.assertEqualsAnyOrder(getSpi(epService).getContextStatementNames("CtxA"), "B".split(","));

        stmtB.destroy();
        EPAssertionUtil.assertEqualsAnyOrder(getSpi(epService).getContextStatementNames("CtxA"), new String[0]);

        assertEquals(null, getSpi(epService).getContextStatementNames("undefined"));
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAcrossURIExtractImport(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecContextAdminPartitionSPI.class)) {
            return;
        }

        assertHashSegmentedImport(epService);
        assertPartitionedImport(epService);
        assertCategoryImport(epService);
        assertInitTermImport(epService);
        assertNestedContextImport(epService);
    }

    private void runAssertionSameURIExtractStopImportStart(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecContextAdminPartitionSPI.class)) {
            return;
        }

        assertHashSegmentedIndividualSelector(epService, new MySelectorHashById(Collections.singleton(HASH_MOD_E1_STRING_BY_64)));
        assertHashSegmentedIndividualSelector(epService, new MySelectorHashFiltered(HASH_MOD_E1_STRING_BY_64));
        assertHashSegmentedIndividualSelector(epService, new SupportSelectorById(Collections.singleton(0)));
        assertHashSegmentedAllSelector(epService);

        assertCategoryIndividualSelector(epService, new SupportSelectorCategory(Collections.singleton("G2")));
        assertCategoryIndividualSelector(epService, new MySelectorCategoryFiltered("G2"));
        assertCategoryIndividualSelector(epService, new SupportSelectorById(Collections.singleton(1)));
        assertCategoryAllSelector(epService);

        assertPartitionedIndividualSelector(epService, new SupportSelectorById(Collections.singleton(0)));
        assertPartitionedIndividualSelector(epService, new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"E1"})));
        assertPartitionedIndividualSelector(epService, new MySelectorPartitionFiltered(new Object[]{"E1"}));
        assertPartitionedAllSelector(epService);

        assertInitTermIndividualSelector(epService, new MySelectorInitTermFiltered("E1"));
        assertInitTermIndividualSelector(epService, new SupportSelectorById(Collections.singleton(0)));
        assertInitTermAllSelector(epService);

        assertNestedContextIndividualSelector(epService, new SupportSelectorNested(
                new MySelectorPartitionFiltered(new Object[]{"E2"}), new MySelectorCategoryFiltered("positive")));
    }

    private void assertDestroyNested(EPServiceProvider epService) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextNested(epService, listener);
        assertEquals(2, epService.getEPAdministrator().getContextPartitionAdmin().getContextNestingLevel(contextName));
        String[] fieldsnested = "c0,c1,c2,c3".split(",");

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 10));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 11));
        epService.getEPRuntime().sendEvent(makeEvent("E1", -1, 12));
        epService.getEPRuntime().sendEvent(makeEvent("E2", -1, 13));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), fieldsnested,
                new Object[][]{{"E1", 1, 10L, 1}, {"E2", 1, 11L, 3}, {"E1", -1, 12L, 0}, {"E2", -1, 13L, 2}});

        // destroy hash for "S0_2"
        ContextPartitionCollection collDestroy = getSpi(epService).destroyContextPartitions(contextName, new SupportSelectorNested(new SupportSelectorPartitioned("E2"), new SupportSelectorCategory("negative")));
        assertPathInfo(collDestroy.getDescriptors(), new Object[][]{{2, makeIdentNested(makeIdentPart("E2"), makeIdentCat("negative")), "+"}});
        assertPathInfo(getAllCPDescriptors(epService, contextName, true), new Object[][]{
                {0, makeIdentNested(makeIdentPart("E1"), makeIdentCat("negative")), "+"},
                {1, makeIdentNested(makeIdentPart("E1"), makeIdentCat("positive")), "+"},
                {3, makeIdentNested(makeIdentPart("E2"), makeIdentCat("positive")), "+"}});
        SupportContextStateCacheImpl.assertRemovedState(new ContextStatePathKey(2, 2, 1));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 20));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 21));
        epService.getEPRuntime().sendEvent(makeEvent("E1", -1, 22));
        epService.getEPRuntime().sendEvent(makeEvent("E2", -1, 23));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), fieldsnested,
                new Object[][]{{"E1", 1, 30L, 1}, {"E2", 1, 32L, 3}, {"E1", -1, 34L, 0}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertDestroyInitTerm(EPServiceProvider epService) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextInitTerm(epService, listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "S0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "S0_3"));
        epService.getEPRuntime().sendEvent(new SupportBean("S0_1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("S0_2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("S0_3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"S0_1", 1, 0}, {"S0_2", 2, 1}, {"S0_3", 3, 2}});

        // destroy hash for "S0_2"
        ContextPartitionCollection collDestroy = getSpi(epService).destroyContextPartitions(contextName, new SupportSelectorById(1));
        assertPathInfo(collDestroy.getDescriptors(), new Object[][]{{1, null, "+"}});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{{0, null, "+"}, {2, null, "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, null, true), new ContextState(1, 0, 3, 2, null, true));
        SupportContextStateCacheImpl.assertRemovedState(new ContextStatePathKey(1, 0, 2));

        epService.getEPRuntime().sendEvent(new SupportBean("S0_1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("S0_2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("S0_3", 30));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"S0_1", 11, 0}, {"S0_3", 33, 2}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertDestroyHashSegmented(EPServiceProvider epService) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextHashSegmented(epService, listener);
        int hashCodeE1 = CODE_FUNC_MOD64.codeFor("E1");
        int hashCodeE2 = CODE_FUNC_MOD64.codeFor("E2");
        int hashCodeE3 = CODE_FUNC_MOD64.codeFor("E3");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E1", 1, 0}, {"E2", 2, 1}, {"E3", 3, 2}});

        // destroy hash for "E2"
        ContextPartitionCollection collDestroy = getSpi(epService).destroyContextPartitions(contextName, new SupportSelectorByHashCode(hashCodeE2));
        assertPathInfo(collDestroy.getDescriptors(), new Object[][]{{1, makeIdentHash(hashCodeE2), "+"}});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{{0, makeIdentHash(hashCodeE1), "+"}, {2, makeIdentHash(hashCodeE3), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, hashCodeE1, true), new ContextState(1, 0, 3, 2, hashCodeE3, true));
        SupportContextStateCacheImpl.assertRemovedState(new ContextStatePathKey(1, 0, 2));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E1", 11, 0}, {"E2", 20, 3}, {"E3", 33, 2}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertDestroyPartitioned(EPServiceProvider epService) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextPartitioned(epService, listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E1", 1, 0}, {"E2", 2, 1}, {"E3", 3, 2}});

        // destroy hash for "E2"
        ContextPartitionCollection collDestroy = getSpi(epService).destroyContextPartitions(contextName, new SupportSelectorPartitioned("E2"));
        assertPathInfo(collDestroy.getDescriptors(), new Object[][]{{1, makeIdentPart("E2"), "+"}});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{{0, makeIdentPart("E1"), "+"}, {2, makeIdentPart("E3"), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, new Object[]{"E1"}, true), new ContextState(1, 0, 3, 2, new Object[]{"E3"}, true));
        SupportContextStateCacheImpl.assertRemovedState(new ContextStatePathKey(1, 0, 2));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E1", 11, 0}, {"E2", 20, 3}, {"E3", 33, 2}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertExtractDestroyPartitionedById(EPServiceProvider epService) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextPartitioned(epService, listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E1", 1, 0}, {"E2", 2, 1}, {"E3", 3, 2}});

        // destroy hash for "E2"
        ContextPartitionDescriptor destroyedOne = getSpi(epService).destroyContextPartition(contextName, 1);
        assertPathInfo("destroyed", destroyedOne, new Object[]{1, makeIdentPart("E2"), "+"});

        // destroy hash for "E3"
        EPContextPartitionExtract collDestroy = getSpi(epService).extractDestroyPaths(contextName, new SupportSelectorById(0));
        assertPathInfo(collDestroy.getCollection().getDescriptors(), new Object[][]{{0, makeIdentPart("E1"), "+"}});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{{2, makeIdentPart("E3"), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 3, 2, new Object[]{"E3"}, true));
        SupportContextStateCacheImpl.assertRemovedState(new ContextStatePathKey(1, 0, 1), new ContextStatePathKey(1, 0, 2));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E1", 10, 3}, {"E2", 20, 4}, {"E3", 33, 2}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertDestroyCategory(EPServiceProvider epService) {
        SupportContextStateCacheImpl.reset();
        String contextName = "CategoryContext";
        String createCtx = CONTEXT_CACHE_HOOK + "create context CategoryContext as " +
                "group by intPrimitive < 0 as negative, " +
                "group by intPrimitive = 0 as zero," +
                "group by intPrimitive > 0 as positive from SupportBean";
        epService.getEPAdministrator().createEPL(createCtx);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context CategoryContext " +
                "select theString as c0, count(*) as c1, context.id as c2 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E1", 1L, 0}, {"E2", 1L, 1}, {"E3", 1L, 2}});

        // destroy category "negative"
        ContextPartitionCollection collDestroy = getSpi(epService).destroyContextPartitions(contextName, new SupportSelectorCategory("zero"));
        assertPathInfo(collDestroy.getDescriptors(), new Object[][]{{1, makeIdentCat("zero"), "+"}});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{{0, makeIdentCat("negative"), "+"}, {2, makeIdentCat("positive"), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, 0, true), new ContextState(1, 0, 3, 2, 2, true));
        SupportContextStateCacheImpl.assertRemovedState(new ContextStatePathKey(1, 0, 2));

        epService.getEPRuntime().sendEvent(new SupportBean("E4", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP, new Object[][]{{"E4", 2L, 0}, {"E6", 2L, 2}});

        // destroy again, should return empty
        ContextPartitionCollection collDestroyTwo = getSpi(epService).destroyContextPartitions(contextName, new SupportSelectorCategory("zero"));
        assertTrue(collDestroyTwo.getDescriptors().isEmpty());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertCategoryAllSelector(EPServiceProvider epService) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextCategory(epService, listener);
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, 0, true), new ContextState(1, 0, 2, 1, 1, true), new ContextState(1, 0, 3, 2, 2, true));

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 100));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDS, new Object[][]{{"G1", 1}, {"G2", 10}, {"G3", 100}});

        // deactivate all categories
        EPContextPartitionExtract extract = getSpi(epService).extractStopPaths(contextName, ContextPartitionSelectorAll.INSTANCE);
        assertPathInfo(extract.getCollection().getDescriptors(), new Object[][]
            {{0, makeIdentCat("G1"), "+"}, {1, makeIdentCat("G2"), "+"}, {2, makeIdentCat("G3"), "+"}});
        assertEquals(1, extract.getNumNestingLevels());
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, 0, false), new ContextState(1, 0, 2, 1, 1, false), new ContextState(1, 0, 3, 2, 2, false));

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 12));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 102));
        assertFalse(listener.isInvoked());
        assertCreateStmtNotActive(epService, "context CategoryContext select * from SupportBean", new SupportBean("G1", -1));
        assertCreateStmtNotActive(epService, "context CategoryContext select * from SupportBean", new SupportBean("G2", -1));
        assertCreateStmtNotActive(epService, "context CategoryContext select * from SupportBean", new SupportBean("G3", -1));

        // activate categories
        getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, 0, true), new ContextState(1, 0, 2, 1, 1, true), new ContextState(1, 0, 3, 2, 2, true));

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 13));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 103));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDS, new Object[][]{{"G1", 3}, {"G2", 13}, {"G3", 103}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertCategoryIndividualSelector(EPServiceProvider epService, ContextPartitionSelector selectorCategoryG2) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextCategory(epService, listener);
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, 0, true), new ContextState(1, 0, 2, 1, 1, true), new ContextState(1, 0, 3, 2, 2, true));

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 100));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDS, new Object[][]{{"G1", 1}, {"G2", 10}, {"G3", 100}});

        // deactivate category G2
        EPContextPartitionExtract extract = getSpi(epService).extractStopPaths(contextName, selectorCategoryG2);
        assertPathInfo(extract.getCollection().getDescriptors(), new Object[][]{{1, makeIdentCat("G2"), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, 0, true), new ContextState(1, 0, 2, 1, 1, false), new ContextState(1, 0, 3, 2, 2, true));

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 12));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 102));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDS, new Object[][]{{"G1", 3}, {"G3", 202}});
        assertCreateStmtNotActive(epService, "context CategoryContext select * from SupportBean", new SupportBean("G2", -1));

        // activate category G2
        getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, 0, true), new ContextState(1, 0, 2, 1, 1, true), new ContextState(1, 0, 3, 2, 2, true));

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 13));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 103));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDS, new Object[][]{{"G1", 6}, {"G2", 13}, {"G3", 305}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertHashSegmentedImport(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextHashSegmented(epService, listener);

        // context partition 0 = code for E2
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"E2", 20, 0});

        // context partition 1 = code for E1
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"E1", 10, 1});

        EPContextPartitionExtract extract = getSpi(epService).extractPaths(contextName, new ContextPartitionSelectorAll());

        epService.getEPAdministrator().destroyAllStatements();
        setUpContextHashSegmented(epService, listener);

        // context partition 0 = code for E3
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"E3", 30, 0});

        // context partition 1 = code for E4
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 40));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"E4", 40, 1});

        // context partition 2 = code for E1
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"E1", 11, 2});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{
                {0, makeIdentHash(CODE_FUNC_MOD64.codeFor("E3")), "+"},
                {1, makeIdentHash(CODE_FUNC_MOD64.codeFor("E4")), "+"},
                {2, makeIdentHash(CODE_FUNC_MOD64.codeFor("E1")), "+"}});

        EPContextPartitionImportResult importResult = getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        assertImportsCPids(importResult.getExistingToImported(), new int[][]{{2, 1}}); // mapping 1 --> 2  (agent instance id 1 to 2)
        assertImportsCPids(importResult.getAllocatedToImported(), new int[][]{{3, 0}}); // mapping 0 --> 3 (agent instance id 0 to 3)
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{
                {0, makeIdentHash(CODE_FUNC_MOD64.codeFor("E3")), "+"},
                {1, makeIdentHash(CODE_FUNC_MOD64.codeFor("E4")), "+"},
                {2, makeIdentHash(CODE_FUNC_MOD64.codeFor("E1")), "+"},
                {3, makeIdentHash(CODE_FUNC_MOD64.codeFor("E2")), "+"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 31));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 41));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));  // was reset
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 22));  // was created
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP,
                new Object[][]{{"E3", 61, 0}, {"E4", 81, 1}, {"E1", 12, 2}, {"E2", 22, 3}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertPartitionedImport(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextPartitioned(epService, listener);
        assertEquals(1, epService.getEPAdministrator().getContextPartitionAdmin().getContextNestingLevel(contextName));

        // context partition 0 = E2
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"E2", 20, 0});

        // context partition 1 = E1
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"E1", 10, 1});

        EPContextPartitionExtract extract = getSpi(epService).extractPaths(contextName, new ContextPartitionSelectorAll());
        assertPathInfo(extract.getCollection().getDescriptors(), new Object[][]{{0, makeIdentPart("E2"), "+"}, {1, makeIdentPart("E1"), "+"}});

        epService.getEPAdministrator().destroyAllStatements();
        setUpContextPartitioned(epService, listener);

        // context partition 0 = E1
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"E1", 11, 0});

        // context partition 1 = E3
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"E3", 30, 1});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{
                {0, makeIdentPart("E1"), "+"}, {1, makeIdentPart("E3"), "+"}});

        EPContextPartitionImportResult importResult = getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        assertImportsCPids(importResult.getExistingToImported(), new int[][]{{0, 1}}); // mapping 1 --> 0  (agent instance id 1 to 2)
        assertImportsCPids(importResult.getAllocatedToImported(), new int[][]{{2, 0}}); // mapping 0 --> 2 (agent instance id 0 to 3)
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{
                {0, makeIdentPart("E1"), "+"}, {1, makeIdentPart("E3"), "+"}, {2, makeIdentPart("E2"), "+"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));  // was reset
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 31));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 22));  // was created
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP,
                new Object[][]{{"E1", 12, 0}, {"E3", 61, 1}, {"E2", 22, 2}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertCategoryImport(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextCategory(epService, listener);
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{
                {0, makeIdentCat("G1"), "+"}, {1, makeIdentCat("G2"), "+"}, {2, makeIdentCat("G3"), "+"}});

        // context partition 0 = G1
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"G1", 10, 0});

        EPContextPartitionExtract extract = getSpi(epService).extractPaths(contextName, new ContextPartitionSelectorAll());

        epService.getEPAdministrator().destroyAllStatements();
        setUpContextCategory(epService, listener);

        // context partition 1 = G2
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"G2", 20, 1});
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{
                {0, makeIdentCat("G1"), "+"}, {1, makeIdentCat("G2"), "+"}, {2, makeIdentCat("G3"), "+"}});

        EPContextPartitionImportResult importResult = getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        assertImportsCPids(importResult.getExistingToImported(), new int[][]{{0, 0}, {1, 1}, {2, 2}}); // mapping 1 --> 0  (agent instance id 1 to 2)
        assertImportsCPids(importResult.getAllocatedToImported(), new int[0][]); // no new ones allocated
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{
                {0, makeIdentCat("G1"), "+"}, {1, makeIdentCat("G2"), "+"}, {2, makeIdentCat("G3"), "+"}});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));  // was reset
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 21));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 31));  // was created
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), FIELDSCP,
                new Object[][]{{"G1", 11, 0}, {"G2", 21, 1}, {"G3", 31, 2}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertInitTermImport(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextInitTerm(epService, listener);

        // context partition 0
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("S0_1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"S0_1", 10, 0});

        EPContextPartitionExtract extract = getSpi(epService).extractPaths(contextName, new ContextPartitionSelectorAll());

        epService.getEPAdministrator().destroyAllStatements();
        setUpContextInitTerm(epService, listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "S0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean("S0_2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"S0_2", 20, 0});

        EPContextPartitionImportResult importResult = getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        assertImportsCPids(importResult.getExistingToImported(), new int[0][]); // no existing found
        assertImportsCPids(importResult.getAllocatedToImported(), new int[][]{{1, 0}}); // new one created is 1

        epService.getEPRuntime().sendEvent(new SupportBean("S0_2", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"S0_2", 41, 0});

        epService.getEPRuntime().sendEvent(new SupportBean("S0_1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDSCP, new Object[]{"S0_1", 11, 1});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertNestedContextImport(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextNested(epService, listener);
        String[] fieldsnested = "c0,c1,c2,c3".split(",");

        // context partition subpath 0=G1+negative, 1=G1+positive
        epService.getEPRuntime().sendEvent(makeEvent("G1", 10, 1000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsnested, new Object[]{"G1", 10, 1000L, 1});

        // context partition subpath 2=G2+negative, 2=G2+positive
        epService.getEPRuntime().sendEvent(makeEvent("G2", -20, 2000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsnested, new Object[]{"G2", -20, 2000L, 2});

        assertPathInfo(getAllCPDescriptors(epService, contextName, true), new Object[][]{
                {0, makeIdentNested(makeIdentPart("G1"), makeIdentCat("negative")), "+"},
                {1, makeIdentNested(makeIdentPart("G1"), makeIdentCat("positive")), "+"},
                {2, makeIdentNested(makeIdentPart("G2"), makeIdentCat("negative")), "+"},
                {3, makeIdentNested(makeIdentPart("G2"), makeIdentCat("positive")), "+"}});

        SupportSelectorNested nestedSelector = new SupportSelectorNested(new ContextPartitionSelectorAll(), new ContextPartitionSelectorAll());
        EPContextPartitionExtract extract = getSpi(epService).extractPaths(contextName, nestedSelector);
        assertEquals(2, extract.getNumNestingLevels());

        epService.getEPAdministrator().destroyAllStatements();
        setUpContextNested(epService, listener);

        // context partition subpath 0=G3+negative, 1=G3+positive
        epService.getEPRuntime().sendEvent(makeEvent("G3", 30, 3000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsnested, new Object[]{"G3", 30, 3000L, 1});

        // context partition subpath 2=G1+negative, 3=G1+positive
        epService.getEPRuntime().sendEvent(makeEvent("G1", 11, 1001));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsnested, new Object[]{"G1", 11, 1001L, 3});

        EPContextPartitionImportResult importResult = getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        assertImportsCPids(importResult.getExistingToImported(), new int[][]{{2, 0}, {3, 1}}); // mapping 0 --> 2, 1 --> 3  (agent instance id 1 to 2)
        assertImportsCPids(importResult.getAllocatedToImported(), new int[][]{{4, 2}, {5, 3}});  // allocated ones

        assertPathInfo(getAllCPDescriptors(epService, contextName, true), new Object[][]{
                {0, makeIdentNested(makeIdentPart("G3"), makeIdentCat("negative")), "+"},
                {1, makeIdentNested(makeIdentPart("G3"), makeIdentCat("positive")), "+"},
                {2, makeIdentNested(makeIdentPart("G1"), makeIdentCat("negative")), "+"},
                {3, makeIdentNested(makeIdentPart("G1"), makeIdentCat("positive")), "+"},
                {4, makeIdentNested(makeIdentPart("G2"), makeIdentCat("negative")), "+"},
                {5, makeIdentNested(makeIdentPart("G2"), makeIdentCat("positive")), "+"}});

        epService.getEPRuntime().sendEvent(makeEvent("G3", 31, 3001));
        epService.getEPRuntime().sendEvent(makeEvent("G1", 12, 1002));  // reset
        epService.getEPRuntime().sendEvent(makeEvent("G2", 21, 2001));  // new
        epService.getEPRuntime().sendEvent(makeEvent("G2", -22, 2002));  // new
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), fieldsnested,
                new Object[][]{{"G3", 31, 6001L, 1}, {"G1", 12, 1002L, 3}, {"G2", 21, 2001L, 5}, {"G2", -22, 2002L, 4}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertHashSegmentedAllSelector(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextHashSegmented(epService, listener);
        int hashCodeE1 = CODE_FUNC_MOD64.codeFor("E1");
        int hashCodeE2 = CODE_FUNC_MOD64.codeFor("E2");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 10});
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0}, getAllCPIds(epService, "HashSegByString", false));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 20});
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0, 1}, getAllCPIds(epService, "HashSegByString", false));

        // deactivate all partitions
        EPContextPartitionExtract extract = getSpi(epService).extractStopPaths(contextName, ContextPartitionSelectorAll.INSTANCE);
        assertPathInfo(getAllCPDescriptors(epService, contextName, false), new Object[][]{
                {0, makeIdentHash(hashCodeE1), "-"}, {1, makeIdentHash(hashCodeE2), "-"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, hashCodeE1, false), new ContextState(1, 0, 2, 1, hashCodeE2, false));

        // assert E1 and E2 inactive
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));
        assertFalse(listener.isInvoked());
        assertCreateStmtNotActive(epService, "context HashSegByString select * from SupportBean", new SupportBean("E1", -1));
        assertCreateStmtNotActive(epService, "context HashSegByString select * from SupportBean", new SupportBean("E2", -1));

        // activate context partitions
        getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, hashCodeE1, true), new ContextState(1, 0, 2, 1, hashCodeE2, true));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 12});
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 22));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 22});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertHashSegmentedIndividualSelector(EPServiceProvider epService, ContextPartitionSelector selector) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextHashSegmented(epService, listener);
        int hashCodeE1 = CODE_FUNC_MOD64.codeFor("E1");
        int hashCodeE2 = CODE_FUNC_MOD64.codeFor("E2");
        assertTrue(hashCodeE1 != hashCodeE2);
        assertEquals(HASH_MOD_E1_STRING_BY_64, hashCodeE1);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 10});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, hashCodeE1, true));
        assertPathInfo("failed at code E1", getSpi(epService).getDescriptor(contextName, 0), new Object[]{0, makeIdentHash(hashCodeE1), "+"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 20});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, hashCodeE1, true), new ContextState(1, 0, 2, 1, hashCodeE2, true));

        // deactive partition for "E1" code
        EPContextPartitionExtract extract = getSpi(epService).extractStopPaths(contextName, selector);
        assertPathInfo(extract.getCollection().getDescriptors(), new Object[][]{{0, makeIdentHash(hashCodeE1), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, hashCodeE1, false), new ContextState(1, 0, 2, 1, hashCodeE2, true));

        // assert E1 inactive
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        assertFalse(listener.isInvoked());
        assertCreateStmtNotActive(epService, "context HashSegByString select * from SupportBean", new SupportBean("E1", -1));

        // assert E2 still active
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 41});

        // activate context partition for "E1"
        getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, hashCodeE1, true), new ContextState(1, 0, 2, 1, hashCodeE2, true));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 12});
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 22));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 63});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertCreateStmtNotActive(EPServiceProvider epService, String epl, SupportBean testevent) {
        SupportUpdateListener local = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(local);

        epService.getEPRuntime().sendEvent(testevent);
        assertFalse(local.isInvoked());

        stmt.destroy();
    }

    private void assertPartitionedIndividualSelector(EPServiceProvider epService, ContextPartitionSelector selector) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextPartitioned(epService, listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 10});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, new Object[]{"E1"}, true));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 20});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, new Object[]{"E1"}, true), new ContextState(1, 0, 2, 1, new Object[]{"E2"}, true));

        // deactive partition for "E1" code
        EPContextPartitionExtract extract = getSpi(epService).extractStopPaths(contextName, selector);
        assertPathInfo(extract.getCollection().getDescriptors(), new Object[][]{{0, makeIdentPart("E1"), "+"}});
        assertCreateStmtNotActive(epService, "context PartitionByString select * from SupportBean", new SupportBean("E1", -1));
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, new Object[]{"E1"}, false), new ContextState(1, 0, 2, 1, new Object[]{"E2"}, true));

        // assert E1 inactive
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 41});

        // activate context partition for "E1"
        getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, new Object[]{"E1"}, true), new ContextState(1, 0, 2, 1, new Object[]{"E2"}, true));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 12});
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 22));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 63});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertPartitionedAllSelector(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextPartitioned(epService, listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 20});

        // deactive partition for all
        EPContextPartitionExtract extract = getSpi(epService).extractStopPaths(contextName, ContextPartitionSelectorAll.INSTANCE);
        assertPathInfo(extract.getCollection().getDescriptors(), new Object[][]{{0, makeIdentPart("E1"), "+"}, {1, makeIdentPart("E2"), "+"}});
        assertCreateStmtNotActive(epService, "context PartitionByString select * from SupportBean", new SupportBean("E1", -1));
        assertCreateStmtNotActive(epService, "context PartitionByString select * from SupportBean", new SupportBean("E2", -1));

        // assert E1 inactive
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));
        assertFalse(listener.isInvoked());

        // activate context partition for all
        getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 12});
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 22));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 22});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertInitTermIndividualSelector(EPServiceProvider epService, ContextPartitionSelector selector) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextInitTerm(epService, listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, null, true));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E2"));
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, null, true), new ContextState(1, 0, 2, 1, null, true));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 20});

        // deactive partition for "E1" code
        EPContextPartitionExtract extract = getSpi(epService).extractStopPaths(contextName, selector);
        assertPathInfo(extract.getCollection().getDescriptors(), new Object[][]{{0, null, "+"}});
        assertCreateStmtNotActive(epService, "context InitAndTermCtx select * from SupportBean(theString = context.sbs0.p00)", new SupportBean("E1", -1));
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, null, false), new ContextState(1, 0, 2, 1, null, true));

        // assert E1 inactive
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 41});

        // activate context partition for "E1"
        getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 0, null, true), new ContextState(1, 0, 2, 1, null, true));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 12});
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 22));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 63});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertInitTermAllSelector(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextInitTerm(epService, listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E2"));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 20});

        // deactive partitions (all)
        EPContextPartitionExtract extract = getSpi(epService).extractStopPaths(contextName, ContextPartitionSelectorAll.INSTANCE);
        assertPathInfo(extract.getCollection().getDescriptors(), new Object[][]{{0, null, "+"}, {0, null, "+"}});
        assertCreateStmtNotActive(epService, "context InitAndTermCtx select * from SupportBean(theString = context.sbs0.p00)", new SupportBean("E1", -1));
        assertCreateStmtNotActive(epService, "context InitAndTermCtx select * from SupportBean(theString = context.sbs0.p00)", new SupportBean("E2", -1));

        // assert all inactive
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));
        assertFalse(listener.isInvoked());

        // activate context partition (all)
        getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 12});
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 22));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E2", 22});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertNestedContextIndividualSelector(EPServiceProvider epService, ContextPartitionSelector selector) {
        SupportContextStateCacheImpl.reset();
        SupportUpdateListener listener = new SupportUpdateListener();
        String contextName = setUpContextNested(epService, listener);
        String[] fields = "c0,c1,c2".split(",");

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 10));
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 1, null, true),
                new ContextState(2, 1, 1, 0, null, true), new ContextState(2, 1, 2, 1, null, true));
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0, 1}, getAllCPIds(epService, "NestedContext", false));

        epService.getEPRuntime().sendEvent(makeEvent("E1", -1, 11));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 12));
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 1, null, true),
                new ContextState(2, 1, 1, 0, null, true), new ContextState(2, 1, 2, 1, null, true),
                new ContextState(1, 0, 2, 2, null, true),
                new ContextState(2, 2, 1, 2, null, true), new ContextState(2, 2, 2, 3, null, true));

        epService.getEPRuntime().sendEvent(makeEvent("E2", -1, 13));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), fields,
                new Object[][]{{"E1", 1, 10L}, {"E1", -1, 11L}, {"E2", 1, 12L}, {"E2", -1, 13L}});

        // deactive partition for E2/positive code
        EPContextPartitionExtract extract = getSpi(epService).extractStopPaths(contextName, selector);
        assertPathInfo(extract.getCollection().getDescriptors(), new Object[][]{{
                3, makeIdentNested(makeIdentPart("E2"), makeIdentCat("positive")), "+"}});
        SupportContextStateCacheImpl.assertState(new ContextState(1, 0, 1, 1, null, true),
                new ContextState(2, 1, 1, 0, null, true), new ContextState(2, 1, 2, 1, null, true),
                new ContextState(1, 0, 2, 2, null, true),
                new ContextState(2, 2, 1, 2, null, true), new ContextState(2, 2, 2, 3, null, false));

        // assert E2/G2(1) inactive
        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 20));
        epService.getEPRuntime().sendEvent(makeEvent("E1", -1, 21));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 22)); // not used
        epService.getEPRuntime().sendEvent(makeEvent("E2", -1, 23));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), fields,
                new Object[][]{{"E1", 1, 30L}, {"E1", -1, 32L}, {"E2", -1, 36L}});
        assertCreateStmtNotActive(epService, "context NestedContext select * from SupportBean", new SupportBean("E2", 10000));

        // activate context partition for E2/positive
        getSpi(epService).importStartPaths(contextName, extract.getImportable(), new AgentInstanceSelectorAll());

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 30));
        epService.getEPRuntime().sendEvent(makeEvent("E1", -1, 31));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 32));
        epService.getEPRuntime().sendEvent(makeEvent("E2", -1, 33));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), fields,
                new Object[][]{{"E1", 1, 60L}, {"E1", -1, 63L}, {"E2", 1, 32L}, {"E2", -1, 33L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private String setUpContextNested(EPServiceProvider epService, SupportUpdateListener listener) {

        String createCtx = CONTEXT_CACHE_HOOK + "create context NestedContext as " +
                "context ACtx partition by theString from SupportBean, " +
                "context BCtx " +
                "  group by intPrimitive < 0 as negative," +
                "  group by intPrimitive > 0 as positive from SupportBean";
        epService.getEPAdministrator().createEPL(createCtx);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context NestedContext " +
                "select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2, context.id as c3 from SupportBean");
        stmt.addListener(listener);

        return "NestedContext";
    }

    private String setUpContextHashSegmented(EPServiceProvider epService, SupportUpdateListener listener) {

        String createCtx = CONTEXT_CACHE_HOOK + "create context HashSegByString as coalesce by consistent_hash_crc32(theString) from SupportBean granularity 64";
        epService.getEPAdministrator().createEPL(createCtx);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context HashSegByString " +
                "select theString as c0, sum(intPrimitive) as c1, context.id as c2 " +
                "from SupportBean group by theString");
        stmt.addListener(listener);

        return "HashSegByString";
    }

    private String setUpContextPartitioned(EPServiceProvider epService, SupportUpdateListener listener) {

        String createCtx = CONTEXT_CACHE_HOOK + "create context PartitionByString as partition by theString from SupportBean";
        epService.getEPAdministrator().createEPL(createCtx);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context PartitionByString " +
                "select theString as c0, sum(intPrimitive) as c1, context.id as c2 " +
                "from SupportBean");
        stmt.addListener(listener);

        return "PartitionByString";
    }

    private String setUpContextCategory(EPServiceProvider epService, SupportUpdateListener listener) {

        String createCtx = CONTEXT_CACHE_HOOK + "create context CategoryContext as " +
                "group by theString = 'G1' as G1," +
                "group by theString = 'G2' as G2," +
                "group by theString = 'G3' as G3 from SupportBean";
        epService.getEPAdministrator().createEPL(createCtx);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context CategoryContext " +
                "select theString as c0, sum(intPrimitive) as c1, context.id as c2 " +
                "from SupportBean");
        stmt.addListener(listener);

        return "CategoryContext";
    }

    private Object makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    private String setUpContextInitTerm(EPServiceProvider epService, SupportUpdateListener listener) {

        String createCtx = CONTEXT_CACHE_HOOK + "create context InitAndTermCtx as " +
                "initiated by SupportBean_S0 sbs0 " +
                "terminated after 24 hours";
        epService.getEPAdministrator().createEPL(createCtx);

        EPStatement stmt = epService.getEPAdministrator().createEPL("context InitAndTermCtx " +
                "select theString as c0, sum(intPrimitive) as c1, context.id as c2 " +
                "from SupportBean(theString = context.sbs0.p00)");
        stmt.addListener(listener);

        return "InitAndTermCtx";
    }

    public static class MySelectorHashById implements ContextPartitionSelectorHash {

        private final Set<Integer> hashes;

        public MySelectorHashById(Set<Integer> hashes) {
            this.hashes = hashes;
        }

        public Set<Integer> getHashes() {
            return hashes;
        }
    }

    public static class MySelectorHashFiltered implements ContextPartitionSelectorFiltered {
        private final int hashCode;

        public MySelectorHashFiltered(int hashCode) {
            this.hashCode = hashCode;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierHash hash = (ContextPartitionIdentifierHash) contextPartitionIdentifier;
            return hash.getHash() == hashCode;
        }
    }

    public static class MySelectorCategoryFiltered implements ContextPartitionSelectorFiltered {
        private final String label;

        public MySelectorCategoryFiltered(String label) {
            this.label = label;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierCategory cat = (ContextPartitionIdentifierCategory) contextPartitionIdentifier;
            return cat.getLabel().equals(label);
        }
    }

    public static class MySelectorPartitionFiltered implements ContextPartitionSelectorFiltered {
        private final Object[] keys;

        public MySelectorPartitionFiltered(Object[] keys) {
            this.keys = keys;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierPartitioned part = (ContextPartitionIdentifierPartitioned) contextPartitionIdentifier;
            return Arrays.equals(part.getKeys(), keys);
        }
    }

    public static class MySelectorInitTermFiltered implements ContextPartitionSelectorFiltered {
        private final String p00PropertyValue;

        public MySelectorInitTermFiltered(String p00PropertyValue) {
            this.p00PropertyValue = p00PropertyValue;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierInitiatedTerminated id = (ContextPartitionIdentifierInitiatedTerminated) contextPartitionIdentifier;
            EventBean event = (EventBean) id.getProperties().get("sbs0");
            return p00PropertyValue.equals(event.get("p00"));
        }
    }

    public static void assertImportsCPids(Map<Integer, Integer> received, int[][] expected) {
        if (expected == null) {
            if (received == null) {
                return;
            }
        } else {
            ScopeTestHelper.assertNotNull(received);
        }

        if (expected != null) {
            for (int j = 0; j < expected.length; j++) {
                int key = expected[j][0];
                int value = expected[j][1];
                Integer receivevalue = received.get(key);
                ScopeTestHelper.assertEquals("Error asserting key '" + key + "'", value, receivevalue);
            }
        }
    }

    private static void assertPathInfo(Map<Integer, ContextPartitionDescriptor> cpinfo,
                                       Object[][] expected) {

        assertEquals(expected.length, cpinfo.size());

        for (int i = 0; i < expected.length; i++) {
            Object[] expectedRow = expected[i];
            String message = "failed assertion for item " + i;
            int expectedId = (Integer) expectedRow[0];
            ContextPartitionDescriptor desc = cpinfo.get(expectedId);
            assertPathInfo(message, desc, expectedRow);
        }
    }

    private static void assertPathInfo(String message,
                                       ContextPartitionDescriptor desc,
                                       Object[] expectedRow) {
        int expectedId = (Integer) expectedRow[0];
        ContextPartitionIdentifier expectedIdent = (ContextPartitionIdentifier) expectedRow[1];
        String expectedState = (String) expectedRow[2];

        assertEquals(message, desc.getAgentInstanceId(), expectedId);
        if (expectedIdent != null) {
            assertTrue(message, expectedIdent.compareTo(desc.getIdentifier()));
        } else {
            assertTrue(message, desc.getIdentifier() instanceof ContextPartitionIdentifierInitiatedTerminated);
        }

        ContextPartitionState stateEnum;
        if (expectedState.equals("+")) {
            stateEnum = ContextPartitionState.STARTED;
        } else if (expectedState.equals("-")) {
            stateEnum = ContextPartitionState.STOPPED;
        } else {
            throw new IllegalStateException("Failed to parse expected state '" + expectedState + "' as {+,-}");
        }
        assertEquals(message, stateEnum, desc.getState());
    }

    private static Map<Integer, ContextPartitionDescriptor> getAllCPDescriptors(EPServiceProvider epService, String contextName, boolean nested) {
        ContextPartitionSelector selector = ContextPartitionSelectorAll.INSTANCE;
        if (nested) {
            selector = new SupportSelectorNested(ContextPartitionSelectorAll.INSTANCE, ContextPartitionSelectorAll.INSTANCE);
        }
        return getSpi(epService).getContextPartitions(contextName, selector).getDescriptors();
    }

    private static Set<Integer> getAllCPIds(EPServiceProvider epService, String contextName, boolean nested) {
        ContextPartitionSelector selector = ContextPartitionSelectorAll.INSTANCE;
        if (nested) {
            selector = new SupportSelectorNested(ContextPartitionSelectorAll.INSTANCE, ContextPartitionSelectorAll.INSTANCE);
        }
        return getSpi(epService).getContextPartitionIds(contextName, selector);
    }

    private static EPContextPartitionAdminSPI getSpi(EPServiceProvider epService) {
        return (EPContextPartitionAdminSPI) epService.getEPAdministrator().getContextPartitionAdmin();
    }

    private static ContextPartitionIdentifier makeIdentCat(String label) {
        return new ContextPartitionIdentifierCategory(label);
    }

    private static ContextPartitionIdentifier makeIdentHash(int code) {
        return new ContextPartitionIdentifierHash(code);
    }

    private static ContextPartitionIdentifier makeIdentPart(Object singleKey) {
        return new ContextPartitionIdentifierPartitioned(new Object[]{singleKey});
    }

    private static ContextPartitionIdentifier makeIdentNested(ContextPartitionIdentifier... identifiers) {
        return new ContextPartitionIdentifierNested(identifiers);
    }

    private static class AgentInstanceSelectorAll implements AgentInstanceSelector {
        public boolean select(AgentInstance agentInstance) {
            return true;
        }
    }
}
