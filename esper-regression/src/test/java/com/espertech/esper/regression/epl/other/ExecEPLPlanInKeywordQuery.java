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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.epl.join.plan.*;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescOnExpr;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescSubquery;
import com.espertech.esper.epl.lookup.SubordFullTableScanLookupStrategyFactory;
import com.espertech.esper.epl.lookup.SubordInKeywordMultiTableLookupStrategyFactory;
import com.espertech.esper.epl.lookup.SubordInKeywordSingleTableLookupStrategyFactory;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.epl.SupportExprNodeFactory;
import com.espertech.esper.supportregression.epl.SupportQueryPlanBuilder;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHelper;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecEPLPlanInKeywordQuery implements RegressionExecution, IndexBackingTableInfo {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S1", SupportBean_S1.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S2", SupportBean_S2.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionNotIn(epService);
        runAssertionMultiIdxMultipleInAndMultirow(epService);
        runAssertionMultiIdxSubquery(epService);
        runAssertionSingleIdxMultipleInAndMultirow(epService);
        runAssertionSingleIdxSubquery(epService);
        runAssertionSingleIdxConstants(epService);
        runAssertionMultiIdxConstants(epService);
        runAssertionQueryPlan3Stream(epService);
        runAssertionQueryPlan2Stream(epService);
    }

    private void runAssertionNotIn(EPServiceProvider epService) {
        SupportQueryPlanIndexHook.reset();
        String epl = INDEX_CALLBACK_HOOK + "select * from S0 as s0 unidirectional, S1#keepall as s1 " +
                "where p00 not in (p10, p11)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<TableLookupIndexReqKey, QueryPlanIndexItem> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[1].getItems();
        assertEquals("null", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

        stmt.destroy();
    }

    private void runAssertionMultiIdxMultipleInAndMultirow(EPServiceProvider epService) {
        // assert join
        SupportQueryPlanIndexHook.reset();
        String epl = INDEX_CALLBACK_HOOK + "select * from S0 as s0 unidirectional, S1#keepall as s1 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<TableLookupIndexReqKey, QueryPlanIndexItem> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[1].getItems();
        assertEquals("[p10][p11]", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

        tryAssertionMultiIdx(epService, listener);
        epService.getEPAdministrator().destroyAllStatements();

        // assert named window
        epService.getEPAdministrator().createEPL("create window S1Window#keepall as S1");
        epService.getEPAdministrator().createEPL("insert into S1Window select * from S1");

        String eplNamedWindow = INDEX_CALLBACK_HOOK + "on S0 as s0 select * from S1Window as s1 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL(eplNamedWindow);
        stmtNamedWindow.addListener(listener);

        QueryPlanIndexDescOnExpr onExprNamedWindow = SupportQueryPlanIndexHook.assertOnExprAndReset();
        assertEquals(SubordInKeywordMultiTableLookupStrategyFactory.class.getSimpleName(), onExprNamedWindow.getTableLookupStrategy());

        tryAssertionMultiIdx(epService, listener);

        // assert table
        epService.getEPAdministrator().createEPL("create table S1Table(id int primary key, p10 string primary key, p11 string primary key, p12 string primary key, p13 string primary key)");
        epService.getEPAdministrator().createEPL("insert into S1Table select * from S1");
        epService.getEPAdministrator().createEPL("create index S1Idx1 on S1Table(p10)");
        epService.getEPAdministrator().createEPL("create index S1Idx2 on S1Table(p11)");
        epService.getEPAdministrator().createEPL("create index S1Idx3 on S1Table(p12)");
        epService.getEPAdministrator().createEPL("create index S1Idx4 on S1Table(p13)");

        String eplTable = INDEX_CALLBACK_HOOK + "on S0 as s0 select * from S1Table as s1 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
        EPStatement stmtTable = epService.getEPAdministrator().createEPL(eplTable);
        stmtTable.addListener(listener);

        QueryPlanIndexDescOnExpr onExprTable = SupportQueryPlanIndexHook.assertOnExprAndReset();
        assertEquals(SubordInKeywordMultiTableLookupStrategyFactory.class.getSimpleName(), onExprTable.getTableLookupStrategy());

        tryAssertionMultiIdx(epService, listener);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMultiIdxSubquery(EPServiceProvider epService) {

        String epl = INDEX_CALLBACK_HOOK + "select s0.id as c0," +
                "(select * from S1#keepall as s1 " +
                "  where s0.p00 in (s1.p10, s1.p11) and s0.p01 in (s1.p12, s1.p13))" +
                ".selectFrom(a=>S1.id) as c1 " +
                "from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        QueryPlanIndexDescSubquery subquery = SupportQueryPlanIndexHook.assertSubqueryAndReset();
        assertEquals(SubordInKeywordMultiTableLookupStrategyFactory.class.getSimpleName(), subquery.getTableLookupStrategy());

        // single row tests
        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "a", "b", "c", "d"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a", "x"));
        assertSubqueryC0C1(listener, 1, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "x", "c"));
        assertSubqueryC0C1(listener, 2, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "a", "c"));
        assertSubqueryC0C1(listener, 3, new Integer[]{101});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "b", "d"));
        assertSubqueryC0C1(listener, 4, new Integer[]{101});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "a", "d"));
        assertSubqueryC0C1(listener, 5, new Integer[]{101});

        // 2-row tests
        epService.getEPRuntime().sendEvent(new SupportBean_S1(102, "a1", "a", "d1", "d"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "a", "x"));
        assertSubqueryC0C1(listener, 10, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "x", "c"));
        assertSubqueryC0C1(listener, 11, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(12, "a", "c"));
        assertSubqueryC0C1(listener, 12, new Integer[]{101});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(13, "a", "d"));
        assertSubqueryC0C1(listener, 13, new Integer[]{101, 102});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(14, "a1", "d"));
        assertSubqueryC0C1(listener, 14, new Integer[]{102});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(15, "a", "d1"));
        assertSubqueryC0C1(listener, 15, new Integer[]{102});

        // 3-row tests
        epService.getEPRuntime().sendEvent(new SupportBean_S1(103, "a", "a2", "d", "d2"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "a", "c"));
        assertSubqueryC0C1(listener, 20, new Integer[]{101});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(21, "a", "d"));
        assertSubqueryC0C1(listener, 21, new Integer[]{101, 102, 103});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(22, "a2", "d"));
        assertSubqueryC0C1(listener, 22, new Integer[]{103});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(23, "a", "d2"));
        assertSubqueryC0C1(listener, 23, new Integer[]{103});

        stmt.destroy();

        // test coercion absence - types the same
        String eplCoercion = INDEX_CALLBACK_HOOK + "select *," +
                "(select * from S0#keepall as s0 where sb.longPrimitive in (id)) from SupportBean as sb";
        stmt = epService.getEPAdministrator().createEPL(eplCoercion);
        QueryPlanIndexDescSubquery subqueryCoercion = SupportQueryPlanIndexHook.assertSubqueryAndReset();
        assertEquals(SubordFullTableScanLookupStrategyFactory.class.getSimpleName(), subqueryCoercion.getTableLookupStrategy());
        stmt.destroy();
    }

    private void runAssertionSingleIdxMultipleInAndMultirow(EPServiceProvider epService) {
        // assert join
        SupportQueryPlanIndexHook.reset();
        String epl = INDEX_CALLBACK_HOOK + "select * from S0#keepall as s0, S1 as s1 unidirectional " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<TableLookupIndexReqKey, QueryPlanIndexItem> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[0].getItems();
        assertEquals("[p00]", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

        tryAssertionSingleIdx(epService, listener);
        epService.getEPAdministrator().destroyAllStatements();

        // assert named window
        epService.getEPAdministrator().createEPL("create window S0Window#keepall as S0");
        epService.getEPAdministrator().createEPL("insert into S0Window select * from S0");

        String eplNamedWindow = INDEX_CALLBACK_HOOK + "on S1 as s1 select * from S0Window as s0 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL(eplNamedWindow);
        stmtNamedWindow.addListener(listener);

        QueryPlanIndexDescOnExpr onExprNamedWindow = SupportQueryPlanIndexHook.assertOnExprAndReset();
        assertEquals(SubordInKeywordSingleTableLookupStrategyFactory.class.getSimpleName(), onExprNamedWindow.getTableLookupStrategy());

        tryAssertionSingleIdx(epService, listener);

        // assert table
        epService.getEPAdministrator().createEPL("create table S0Table(id int primary key, p00 string primary key, p01 string primary key, p02 string primary key, p03 string primary key)");
        epService.getEPAdministrator().createEPL("insert into S0Table select * from S0");
        epService.getEPAdministrator().createEPL("create index S0Idx1 on S0Table(p00)");
        epService.getEPAdministrator().createEPL("create index S0Idx2 on S0Table(p01)");

        String eplTable = INDEX_CALLBACK_HOOK + "on S1 as s1 select * from S0Table as s0 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
        EPStatement stmtTable = epService.getEPAdministrator().createEPL(eplTable);
        stmtTable.addListener(listener);

        QueryPlanIndexDescOnExpr onExprTable = SupportQueryPlanIndexHook.assertOnExprAndReset();
        assertEquals(SubordInKeywordSingleTableLookupStrategyFactory.class.getSimpleName(), onExprTable.getTableLookupStrategy());

        tryAssertionSingleIdx(epService, listener);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSingleIdxSubquery(EPServiceProvider epService) {
        SupportQueryPlanIndexHook.reset();
        String epl = INDEX_CALLBACK_HOOK + "select s1.id as c0," +
                "(select * from S0#keepall as s0 " +
                "  where s0.p00 in (s1.p10, s1.p11) and s0.p01 in (s1.p12, s1.p13))" +
                ".selectFrom(a=>S0.id) as c1 " +
                " from S1 as s1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        QueryPlanIndexDescSubquery subquery = SupportQueryPlanIndexHook.assertSubqueryAndReset();
        assertEquals(SubordInKeywordSingleTableLookupStrategyFactory.class.getSimpleName(), subquery.getTableLookupStrategy());

        // single row tests
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "a", "c"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "a1", "b", "c", "d"));
        assertSubqueryC0C1(listener, 1, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "a", "b", "x", "d"));
        assertSubqueryC0C1(listener, 2, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(3, "a", "b", "c", "d"));
        assertSubqueryC0C1(listener, 3, new Integer[]{100});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(4, "x", "a", "x", "c"));
        assertSubqueryC0C1(listener, 4, new Integer[]{100});

        // 2-rows available tests
        epService.getEPRuntime().sendEvent(new SupportBean_S0(101, "a", "d"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "a1", "b", "c", "d"));
        assertSubqueryC0C1(listener, 10, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "a", "b", "x", "c1"));
        assertSubqueryC0C1(listener, 11, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(12, "a", "b", "c", "d"));
        assertSubqueryC0C1(listener, 12, new Integer[]{100, 101});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(13, "x", "a", "x", "c"));
        assertSubqueryC0C1(listener, 13, new Integer[]{100});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(14, "x", "a", "d", "x"));
        assertSubqueryC0C1(listener, 14, new Integer[]{101});

        // 3-rows available tests
        epService.getEPRuntime().sendEvent(new SupportBean_S0(102, "b", "c"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "a1", "b", "c1", "d"));
        assertSubqueryC0C1(listener, 20, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(21, "a", "b", "x", "c1"));
        assertSubqueryC0C1(listener, 21, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(22, "a", "b", "c", "d"));
        assertSubqueryC0C1(listener, 22, new Integer[]{100, 101, 102});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(23, "b", "a", "x", "c"));
        assertSubqueryC0C1(listener, 23, new Integer[]{100, 102});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(24, "b", "a", "d", "c"));
        assertSubqueryC0C1(listener, 24, new Integer[]{100, 101, 102});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(25, "b", "x", "x", "c"));
        assertSubqueryC0C1(listener, 25, new Integer[]{102});

        stmt.destroy();

        // test coercion absence - types the same
        String eplCoercion = INDEX_CALLBACK_HOOK + "select *," +
                "(select * from SupportBean#keepall as sb where sb.longPrimitive in (s0.id)) from S0 as s0";
        stmt = epService.getEPAdministrator().createEPL(eplCoercion);
        QueryPlanIndexDescSubquery subqueryCoercion = SupportQueryPlanIndexHook.assertSubqueryAndReset();
        assertEquals(SubordFullTableScanLookupStrategyFactory.class.getSimpleName(), subqueryCoercion.getTableLookupStrategy());
        stmt.destroy();
    }

    private void tryAssertionSingleIdx(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "s0.id,s1.id".split(",");

        // single row tests
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "a", "c"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "a1", "b", "c", "d"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "a", "b", "x", "d"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "a", "b", "c", "d"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{100, 1}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "x", "a", "x", "c"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{100, 2}});

        // 2-rows available tests
        epService.getEPRuntime().sendEvent(new SupportBean_S0(101, "a", "d"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "a1", "b", "c", "d"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "a", "b", "x", "c1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "a", "b", "c", "d"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{100, 10}, {101, 10}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "x", "a", "x", "c"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{100, 11}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(12, "x", "a", "d", "x"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{101, 12}});

        // 3-rows available tests
        epService.getEPRuntime().sendEvent(new SupportBean_S0(102, "b", "c"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "a1", "b", "c1", "d"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "a", "b", "x", "c1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "a", "b", "c", "d"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{100, 20}, {101, 20}, {102, 20}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(21, "b", "a", "x", "c"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{100, 21}, {102, 21}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(22, "b", "a", "d", "c"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{100, 22}, {101, 22}, {102, 22}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(23, "b", "x", "x", "c"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{102, 23}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionMultiIdx(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "s0.id,s1.id".split(",");

        // single row tests
        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "a", "b", "c", "d"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "a", "x"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "x", "c"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a", "c"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 101});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "b", "d"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, 101});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "a", "d"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, 101});

        // 2-row tests
        epService.getEPRuntime().sendEvent(new SupportBean_S1(102, "a1", "a", "d1", "d"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "a", "x"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "x", "c"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "a", "c"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 101});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "a", "d"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{11, 101}, {11, 102}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(12, "a1", "d"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{12, 102}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(13, "a", "d1"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{13, 102}});

        // 3-row tests
        epService.getEPRuntime().sendEvent(new SupportBean_S1(103, "a", "a2", "d", "d2"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "a", "c"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 101});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(21, "a", "d"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{21, 101}, {21, 102}, {21, 103}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(22, "a2", "d"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{22, 103}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(23, "a", "d2"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{23, 103}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSingleIdxConstants(EPServiceProvider epService) {
        SupportQueryPlanIndexHook.reset();
        String epl = INDEX_CALLBACK_HOOK + "select * from S0 as s0 unidirectional, S1#keepall as s1 " +
                "where p10 in ('a', 'b')";
        String[] fields = "s0.id,s1.id".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<TableLookupIndexReqKey, QueryPlanIndexItem> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[1].getItems();
        assertEquals("[p10]", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(100, "x"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "a"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{1, 101}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(102, "b"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{2, 101}, {2, 102}});

        stmt.destroy();
    }

    private void runAssertionMultiIdxConstants(EPServiceProvider epService) {
        SupportQueryPlanIndexHook.reset();
        String epl = INDEX_CALLBACK_HOOK + "select * from S0 as s0 unidirectional, S1#keepall as s1 " +
                "where 'a' in (p10, p11)";
        String[] fields = "s0.id,s1.id".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<TableLookupIndexReqKey, QueryPlanIndexItem> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[1].getItems();
        assertEquals("[p10][p11]", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(100, "x", "y"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "x", "a"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{1, 101}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(102, "b", "a"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{2, 101}, {2, 102}});

        stmt.destroy();
    }

    private void runAssertionQueryPlan3Stream(EPServiceProvider epService) {
        String epl = "select * from S0 as s0 unidirectional, S1#keepall, S2#keepall ";

        // 3-stream join with in-multiindex directional
        InKeywordTableLookupPlanMultiIdx planInMidx = new InKeywordTableLookupPlanMultiIdx(0, 1, getIndexKeys("i1a", "i1b"), SupportExprNodeFactory.makeIdentExprNode("p00"));
        tryAssertion(epService, epl + " where p00 in (p10, p11)",
                SupportQueryPlanBuilder.start(3)
                        .addIndexHashSingleNonUnique(1, "i1a", "p10")
                        .addIndexHashSingleNonUnique(1, "i1b", "p11")
                        .setIndexFullTableScan(2, "i2")
                        .setLookupPlanInstruction(0, "s0", new LookupInstructionPlan[]{
                            new LookupInstructionPlan(0, "s0", new int[]{1},
                                    new TableLookupPlan[]{planInMidx}, null, new boolean[3]),
                            new LookupInstructionPlan(0, "s0", new int[]{2},
                                    new TableLookupPlan[]{new FullTableScanLookupPlan(1, 2, getIndexKey("i2"))}, null, new boolean[3])
                        })
                        .get());

        InKeywordTableLookupPlanMultiIdx planInMidxMulitiSrc = new InKeywordTableLookupPlanMultiIdx(0, 1, getIndexKeys("i1", "i2"), SupportExprNodeFactory.makeIdentExprNode("p00"));
        tryAssertion(epService, epl + " where p00 in (p10, p20)",
                SupportQueryPlanBuilder.start(3)
                        .setIndexFullTableScan(1, "i1")
                        .setIndexFullTableScan(2, "i2")
                        .setLookupPlanInstruction(0, "s0", new LookupInstructionPlan[]{
                            new LookupInstructionPlan(0, "s0", new int[]{1},
                                    new TableLookupPlan[]{new FullTableScanLookupPlan(0, 1, getIndexKey("i1"))}, null, new boolean[3]),
                            new LookupInstructionPlan(0, "s0", new int[]{2},
                                    new TableLookupPlan[]{new FullTableScanLookupPlan(1, 2, getIndexKey("i2"))}, null, new boolean[3])
                        })
                        .get());

        // 3-stream join with in-singleindex directional
        InKeywordTableLookupPlanSingleIdx planInSidx = new InKeywordTableLookupPlanSingleIdx(0, 1, getIndexKey("i1"), SupportExprNodeFactory.makeIdentExprNodes("p00", "p01"));
        tryAssertion(epService, epl + " where p10 in (p00, p01)", getSingleIndexPlan(planInSidx));

        // 3-stream join with in-singleindex multi-sourced
        InKeywordTableLookupPlanSingleIdx planInSingleMultiSrc = new InKeywordTableLookupPlanSingleIdx(0, 1, getIndexKey("i1"), SupportExprNodeFactory.makeIdentExprNodes("p00"));
        tryAssertion(epService, epl + " where p10 in (p00, p20)", getSingleIndexPlan(planInSingleMultiSrc));
    }

    private void runAssertionQueryPlan2Stream(EPServiceProvider epService) {
        String epl = "select * from S0 as s0 unidirectional, S1#keepall ";
        QueryPlan fullTableScan = SupportQueryPlanBuilder.start(2)
                .setIndexFullTableScan(1, "a")
                .setLookupPlanInner(0, new FullTableScanLookupPlan(0, 1, getIndexKey("a"))).get();

        // 2-stream unidirectional joins
        tryAssertion(epService, epl, fullTableScan);

        QueryPlan planEquals = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p10")
                .setLookupPlanInner(0, new IndexedTableLookupPlanSingle(0, 1, getIndexKey("a"), SupportExprNodeFactory.makeKeyed("p00"))).get();
        tryAssertion(epService, epl + "where p00 = p10", planEquals);
        tryAssertion(epService, epl + "where p00 = p10 and p00 in (p11, p12, p13)", planEquals);

        QueryPlan planInMultiInner = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p11")
                .addIndexHashSingleNonUnique(1, "b", "p12")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanMultiIdx(0, 1, getIndexKeys("a", "b"), SupportExprNodeFactory.makeIdentExprNode("p00"))).get();
        tryAssertion(epService, epl + "where p00 in (p11, p12)", planInMultiInner);
        tryAssertion(epService, epl + "where p00 = p11 or p00 = p12", planInMultiInner);

        QueryPlan planInMultiOuter = SupportQueryPlanBuilder.start(planInMultiInner)
                .setLookupPlanOuter(0, new InKeywordTableLookupPlanMultiIdx(0, 1, getIndexKeys("a", "b"), SupportExprNodeFactory.makeIdentExprNode("p00"))).get();
        String eplOuterJoin = "select * from S0 as s0 unidirectional full outer join S1#keepall ";
        tryAssertion(epService, eplOuterJoin + "where p00 in (p11, p12)", planInMultiOuter);

        QueryPlan planInMultiWConst = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p11")
                .addIndexHashSingleNonUnique(1, "b", "p12")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanMultiIdx(0, 1, getIndexKeys("a", "b"), SupportExprNodeFactory.makeConstExprNode("A"))).get();
        tryAssertion(epService, epl + "where 'A' in (p11, p12)", planInMultiWConst);
        tryAssertion(epService, epl + "where 'A' = p11 or 'A' = p12", planInMultiWConst);

        QueryPlan planInMultiWAddConst = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p12")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanMultiIdx(0, 1, getIndexKeys("a"), SupportExprNodeFactory.makeConstExprNode("A"))).get();
        tryAssertion(epService, epl + "where 'A' in ('B', p12)", planInMultiWAddConst);
        tryAssertion(epService, epl + "where 'A' in ('B', 'C')", fullTableScan);

        QueryPlan planInSingle = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p10")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanSingleIdx(0, 1, getIndexKey("a"), SupportExprNodeFactory.makeIdentExprNodes("p00", "p01"))).get();
        tryAssertion(epService, epl + "where p10 in (p00, p01)", planInSingle);

        QueryPlan planInSingleWConst = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p10")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanSingleIdx(0, 1, getIndexKey("a"), SupportExprNodeFactory.makeConstAndIdentNode("A", "p01"))).get();
        tryAssertion(epService, epl + "where p10 in ('A', p01)", planInSingleWConst);

        QueryPlan planInSingleJustConst = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p10")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanSingleIdx(0, 1, getIndexKey("a"), SupportExprNodeFactory.makeConstAndConstNode("A", "B"))).get();
        tryAssertion(epService, epl + "where p10 in ('A', 'B')", planInSingleJustConst);
    }

    private void tryAssertion(EPServiceProvider epService, String epl, QueryPlan expectedPlan) {
        SupportQueryPlanIndexHook.reset();
        epl = INDEX_CALLBACK_HOOK + epl;
        epService.getEPAdministrator().createEPL(epl);

        QueryPlan actualPlan = SupportQueryPlanIndexHook.assertJoinAndReset();
        SupportQueryPlanIndexHelper.compareQueryPlans(expectedPlan, actualPlan);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertSubqueryC0C1(SupportUpdateListener listener, int c0, Integer[] c1) {
        EventBean event = listener.assertOneGetNewAndReset();
        assertEquals(c0, event.get("c0"));
        Collection<Object> c1Coll = (Collection<Object>) event.get("c1");
        EPAssertionUtil.assertEqualsAnyOrder(c1, c1Coll == null ? null : c1Coll.toArray());
    }

    private QueryPlan getSingleIndexPlan(InKeywordTableLookupPlanSingleIdx plan) {
        return SupportQueryPlanBuilder.start(3)
                .addIndexHashSingleNonUnique(1, "i1", "p10")
                .setIndexFullTableScan(2, "i2")
                .setLookupPlanInstruction(0, "s0", new LookupInstructionPlan[]{
                    new LookupInstructionPlan(0, "s0", new int[]{1},
                            new TableLookupPlan[]{plan}, null, new boolean[3]),
                    new LookupInstructionPlan(0, "s0", new int[]{2},
                            new TableLookupPlan[]{new FullTableScanLookupPlan(1, 2, getIndexKey("i2"))}, null, new boolean[3])
                })
                .get();
    }

    private static TableLookupIndexReqKey[] getIndexKeys(String... names) {
        TableLookupIndexReqKey[] keys = new TableLookupIndexReqKey[names.length];
        for (int i = 0; i < names.length; i++) {
            keys[i] = new TableLookupIndexReqKey(names[i]);
        }
        return keys;
    }

    private static TableLookupIndexReqKey getIndexKey(String name) {
        return new TableLookupIndexReqKey(name);
    }
}
