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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.FullTableScanLookupPlanForge;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.InKeywordTableLookupPlanMultiIdxForge;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.InKeywordTableLookupPlanSingleIdxForge;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.IndexedTableLookupPlanHashedOnlyForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItemForge;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplanouter.LookupInstructionPlanForge;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescOnExpr;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescSubquery;
import com.espertech.esper.common.internal.epl.lookup.SubordFullTableScanLookupStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.lookup.SubordInKeywordMultiTableLookupStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.lookup.SubordInKeywordSingleTableLookupStrategyFactoryForge;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.epl.SupportExprNodeFactory;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanBuilder;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHelper;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;

public class EPLOtherPlanInKeywordQuery implements IndexBackingTableInfo {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherNotIn());
        execs.add(new EPLOtherMultiIdxMultipleInAndMultirow());
        execs.add(new EPLOtherMultiIdxSubquery());
        execs.add(new EPLOtherSingleIdxMultipleInAndMultirow());
        execs.add(new EPLOtherSingleIdxSubquery());
        execs.add(new EPLOtherSingleIdxConstants());
        execs.add(new EPLOtherMultiIdxConstants());
        execs.add(new EPLOtherQueryPlan3Stream());
        execs.add(new EPLOtherQueryPlan2Stream());
        return execs;
    }

    private static class EPLOtherNotIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportQueryPlanIndexHook.reset();
            String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select * from SupportBean_S0 as s0 unidirectional, SupportBean_S1#keepall as s1 " +
                "where p00 not in (p10, p11)";
            env.compileDeploy(epl);


            Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[1].getItems();
            Assert.assertEquals("[]", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

            env.undeployAll();
        }
    }

    private static class EPLOtherMultiIdxMultipleInAndMultirow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // assert join
            SupportQueryPlanIndexHook.reset();
            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select * from SupportBean_S0 as s0 unidirectional, SupportBean_S1#keepall as s1 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
            env.compileDeploy(epl).addListener("s0");

            Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[1].getItems();
            Assert.assertEquals("[p10][p11]", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

            tryAssertionMultiIdx(env);
            env.undeployAll();

            // assert named window
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window S1Window#keepall as SupportBean_S1", path);
            env.compileDeploy("insert into S1Window select * from SupportBean_S1", path);

            String eplNamedWindow = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportBean_S0 as s0 select * from S1Window as s1 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
            env.compileDeploy(eplNamedWindow, path).addListener("s0");

            QueryPlanIndexDescOnExpr onExprNamedWindow = SupportQueryPlanIndexHook.assertOnExprAndReset();
            Assert.assertEquals(SubordInKeywordMultiTableLookupStrategyFactoryForge.class.getSimpleName(), onExprNamedWindow.getTableLookupStrategy());

            tryAssertionMultiIdx(env);

            // assert table
            path.clear();
            env.compileDeploy("create table S1Table(id int primary key, p10 string primary key, p11 string primary key, p12 string primary key, p13 string primary key)", path);
            env.compileDeploy("insert into S1Table select * from SupportBean_S1", path);
            env.compileDeploy("create index S1Idx1 on S1Table(p10)", path);
            env.compileDeploy("create index S1Idx2 on S1Table(p11)", path);
            env.compileDeploy("create index S1Idx3 on S1Table(p12)", path);
            env.compileDeploy("create index S1Idx4 on S1Table(p13)", path);

            String eplTable = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportBean_S0 as s0 select * from S1Table as s1 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
            env.compileDeploy(eplTable, path).addListener("s0");

            QueryPlanIndexDescOnExpr onExprTable = SupportQueryPlanIndexHook.assertOnExprAndReset();
            Assert.assertEquals(SubordInKeywordMultiTableLookupStrategyFactoryForge.class.getSimpleName(), onExprTable.getTableLookupStrategy());

            tryAssertionMultiIdx(env);

            env.undeployAll();
        }
    }

    private static class EPLOtherMultiIdxSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select s0.id as c0," +
                "(select * from SupportBean_S1#keepall as s1 " +
                "  where s0.p00 in (s1.p10, SupportBean_S1.p11) and s0.p01 in (s1.p12, SupportBean_S1.p13))" +
                ".selectFrom(a=>SupportBean_S1.id) as c1 " +
                "from SupportBean_S0 as s0";
            env.compileDeploy(epl).addListener("s0");

            QueryPlanIndexDescSubquery subquery = SupportQueryPlanIndexHook.assertSubqueryAndReset();
            Assert.assertEquals(SubordInKeywordMultiTableLookupStrategyFactoryForge.class.getSimpleName(), subquery.getTableLookupStrategy());

            // single row tests
            env.sendEventBean(new SupportBean_S1(101, "a", "b", "c", "d"));

            env.sendEventBean(new SupportBean_S0(1, "a", "x"));
            assertSubqueryC0C1(env, 1, null);

            env.sendEventBean(new SupportBean_S0(2, "x", "c"));
            assertSubqueryC0C1(env, 2, null);

            env.sendEventBean(new SupportBean_S0(3, "a", "c"));
            assertSubqueryC0C1(env, 3, new Integer[]{101});

            env.sendEventBean(new SupportBean_S0(4, "b", "d"));
            assertSubqueryC0C1(env, 4, new Integer[]{101});

            env.sendEventBean(new SupportBean_S0(5, "a", "d"));
            assertSubqueryC0C1(env, 5, new Integer[]{101});

            // 2-row tests
            env.sendEventBean(new SupportBean_S1(102, "a1", "a", "d1", "d"));

            env.sendEventBean(new SupportBean_S0(10, "a", "x"));
            assertSubqueryC0C1(env, 10, null);

            env.sendEventBean(new SupportBean_S0(11, "x", "c"));
            assertSubqueryC0C1(env, 11, null);

            env.sendEventBean(new SupportBean_S0(12, "a", "c"));
            assertSubqueryC0C1(env, 12, new Integer[]{101});

            env.sendEventBean(new SupportBean_S0(13, "a", "d"));
            assertSubqueryC0C1(env, 13, new Integer[]{101, 102});

            env.sendEventBean(new SupportBean_S0(14, "a1", "d"));
            assertSubqueryC0C1(env, 14, new Integer[]{102});

            env.sendEventBean(new SupportBean_S0(15, "a", "d1"));
            assertSubqueryC0C1(env, 15, new Integer[]{102});

            // 3-row tests
            env.sendEventBean(new SupportBean_S1(103, "a", "a2", "d", "d2"));

            env.sendEventBean(new SupportBean_S0(20, "a", "c"));
            assertSubqueryC0C1(env, 20, new Integer[]{101});

            env.sendEventBean(new SupportBean_S0(21, "a", "d"));
            assertSubqueryC0C1(env, 21, new Integer[]{101, 102, 103});

            env.sendEventBean(new SupportBean_S0(22, "a2", "d"));
            assertSubqueryC0C1(env, 22, new Integer[]{103});

            env.sendEventBean(new SupportBean_S0(23, "a", "d2"));
            assertSubqueryC0C1(env, 23, new Integer[]{103});

            env.undeployAll();

            // test coercion absence - types the same
            String eplCoercion = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select *," +
                "(select * from SupportBean_S0#keepall as s0 where sb.longPrimitive in (id)) from SupportBean as sb";
            env.compileDeploy(eplCoercion);
            QueryPlanIndexDescSubquery subqueryCoercion = SupportQueryPlanIndexHook.assertSubqueryAndReset();
            Assert.assertEquals(SubordFullTableScanLookupStrategyFactoryForge.class.getSimpleName(), subqueryCoercion.getTableLookupStrategy());
            env.undeployAll();
        }
    }

    private static class EPLOtherSingleIdxMultipleInAndMultirow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // assert join
            SupportQueryPlanIndexHook.reset();
            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select * from SupportBean_S0#keepall as s0, SupportBean_S1 as s1 unidirectional " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
            env.compileDeploy(epl).addListener("s0");

            Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[0].getItems();
            Assert.assertEquals("[p00]", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

            tryAssertionSingleIdx(env);
            env.undeployAll();

            // assert named window
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window S0Window#keepall as SupportBean_S0", path);
            env.compileDeploy("insert into S0Window select * from SupportBean_S0", path);

            String eplNamedWindow = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportBean_S1 as s1 select * from S0Window as s0 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
            env.compileDeploy(eplNamedWindow, path).addListener("s0");

            QueryPlanIndexDescOnExpr onExprNamedWindow = SupportQueryPlanIndexHook.assertOnExprAndReset();
            Assert.assertEquals(SubordInKeywordSingleTableLookupStrategyFactoryForge.class.getSimpleName(), onExprNamedWindow.getTableLookupStrategy());

            tryAssertionSingleIdx(env);

            // assert table
            path.clear();
            env.compileDeploy("create table S0Table(id int primary key, p00 string primary key, p01 string primary key, p02 string primary key, p03 string primary key)", path);
            env.compileDeploy("insert into S0Table select * from SupportBean_S0", path);
            env.compileDeploy("create index S0Idx1 on S0Table(p00)", path);
            env.compileDeploy("create index S0Idx2 on S0Table(p01)", path);

            String eplTable = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportBean_S1 as s1 select * from S0Table as s0 " +
                "where p00 in (p10, p11) and p01 in (p12, p13)";
            env.compileDeploy(eplTable, path).addListener("s0");

            QueryPlanIndexDescOnExpr onExprTable = SupportQueryPlanIndexHook.assertOnExprAndReset();
            Assert.assertEquals(SubordInKeywordSingleTableLookupStrategyFactoryForge.class.getSimpleName(), onExprTable.getTableLookupStrategy());

            tryAssertionSingleIdx(env);

            env.undeployAll();
        }
    }

    private static class EPLOtherSingleIdxSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportQueryPlanIndexHook.reset();
            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select s1.id as c0," +
                "(select * from SupportBean_S0#keepall as s0 " +
                "  where s0.p00 in (s1.p10, SupportBean_S1.p11) and s0.p01 in (s1.p12, SupportBean_S1.p13))" +
                ".selectFrom(a=>SupportBean_S0.id) as c1 " +
                " from SupportBean_S1 as s1";
            env.compileDeploy(epl).addListener("s0");

            QueryPlanIndexDescSubquery subquery = SupportQueryPlanIndexHook.assertSubqueryAndReset();
            Assert.assertEquals(SubordInKeywordSingleTableLookupStrategyFactoryForge.class.getSimpleName(), subquery.getTableLookupStrategy());

            // single row tests
            env.sendEventBean(new SupportBean_S0(100, "a", "c"));

            env.sendEventBean(new SupportBean_S1(1, "a1", "b", "c", "d"));
            assertSubqueryC0C1(env, 1, null);

            env.sendEventBean(new SupportBean_S1(2, "a", "b", "x", "d"));
            assertSubqueryC0C1(env, 2, null);

            env.sendEventBean(new SupportBean_S1(3, "a", "b", "c", "d"));
            assertSubqueryC0C1(env, 3, new Integer[]{100});

            env.sendEventBean(new SupportBean_S1(4, "x", "a", "x", "c"));
            assertSubqueryC0C1(env, 4, new Integer[]{100});

            // 2-rows available tests
            env.sendEventBean(new SupportBean_S0(101, "a", "d"));

            env.sendEventBean(new SupportBean_S1(10, "a1", "b", "c", "d"));
            assertSubqueryC0C1(env, 10, null);

            env.sendEventBean(new SupportBean_S1(11, "a", "b", "x", "c1"));
            assertSubqueryC0C1(env, 11, null);

            env.sendEventBean(new SupportBean_S1(12, "a", "b", "c", "d"));
            assertSubqueryC0C1(env, 12, new Integer[]{100, 101});

            env.sendEventBean(new SupportBean_S1(13, "x", "a", "x", "c"));
            assertSubqueryC0C1(env, 13, new Integer[]{100});

            env.sendEventBean(new SupportBean_S1(14, "x", "a", "d", "x"));
            assertSubqueryC0C1(env, 14, new Integer[]{101});

            // 3-rows available tests
            env.sendEventBean(new SupportBean_S0(102, "b", "c"));

            env.sendEventBean(new SupportBean_S1(20, "a1", "b", "c1", "d"));
            assertSubqueryC0C1(env, 20, null);

            env.sendEventBean(new SupportBean_S1(21, "a", "b", "x", "c1"));
            assertSubqueryC0C1(env, 21, null);

            env.sendEventBean(new SupportBean_S1(22, "a", "b", "c", "d"));
            assertSubqueryC0C1(env, 22, new Integer[]{100, 101, 102});

            env.sendEventBean(new SupportBean_S1(23, "b", "a", "x", "c"));
            assertSubqueryC0C1(env, 23, new Integer[]{100, 102});

            env.sendEventBean(new SupportBean_S1(24, "b", "a", "d", "c"));
            assertSubqueryC0C1(env, 24, new Integer[]{100, 101, 102});

            env.sendEventBean(new SupportBean_S1(25, "b", "x", "x", "c"));
            assertSubqueryC0C1(env, 25, new Integer[]{102});

            env.undeployAll();

            // test coercion absence - types the same
            String eplCoercion = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select *," +
                "(select * from SupportBean#keepall as sb where sb.longPrimitive in (s0.id)) from SupportBean_S0 as s0";
            env.compileDeploy(eplCoercion);
            QueryPlanIndexDescSubquery subqueryCoercion = SupportQueryPlanIndexHook.assertSubqueryAndReset();
            Assert.assertEquals(SubordFullTableScanLookupStrategyFactoryForge.class.getSimpleName(), subqueryCoercion.getTableLookupStrategy());
            env.undeployAll();
        }
    }

    private static class EPLOtherSingleIdxConstants implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportQueryPlanIndexHook.reset();
            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select * from SupportBean_S0 as s0 unidirectional, SupportBean_S1#keepall as s1 " +
                "where p10 in ('a', 'b')";
            String[] fields = "s0.id,s1.id".split(",");
            env.compileDeploy(epl).addListener("s0");

            Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[1].getItems();
            Assert.assertEquals("[p10]", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

            env.sendEventBean(new SupportBean_S1(100, "x"));
            env.sendEventBean(new SupportBean_S1(101, "a"));

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{1, 101}});

            env.sendEventBean(new SupportBean_S1(102, "b"));
            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{2, 101}, {2, 102}});

            env.undeployAll();
        }
    }

    private static class EPLOtherMultiIdxConstants implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportQueryPlanIndexHook.reset();
            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select * from SupportBean_S0 as s0 unidirectional, SupportBean_S1#keepall as s1 " +
                "where 'a' in (p10, p11)";
            String[] fields = "s0.id,s1.id".split(",");
            env.compileDeploy(epl).addListener("s0");

            Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> items = SupportQueryPlanIndexHook.assertJoinAndReset().getIndexSpecs()[1].getItems();
            Assert.assertEquals("[p10][p11]", SupportQueryPlanIndexHelper.getIndexedExpressions(items));

            env.sendEventBean(new SupportBean_S1(100, "x", "y"));
            env.sendEventBean(new SupportBean_S1(101, "x", "a"));

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{1, 101}});

            env.sendEventBean(new SupportBean_S1(102, "b", "a"));
            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{2, 101}, {2, 102}});

            env.undeployAll();
        }
    }

    private static class EPLOtherQueryPlan3Stream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventType[] types = new EventType[3];
            String epl = "@name('s0') select * from SupportBean_S0 as s0 unidirectional, SupportBean_S1#keepall, SupportBean_S2#keepall ";

            // 3-stream join with in-multiindex directional
            InKeywordTableLookupPlanMultiIdxForge planInMidx = new InKeywordTableLookupPlanMultiIdxForge(0, 1, false, types, getIndexKeys("i1a", "i1b"), SupportExprNodeFactory.makeIdentExprNode("p00"));
            tryAssertion(env, epl + " where p00 in (p10, p11)",
                SupportQueryPlanBuilder.start(3)
                    .addIndexHashSingleNonUnique(1, "i1a", "p10")
                    .addIndexHashSingleNonUnique(1, "i1b", "p11")
                    .setIndexFullTableScan(2, "i2")
                    .setLookupPlanInstruction(0, "s0", new LookupInstructionPlanForge[]{
                        new LookupInstructionPlanForge(0, "s0", new int[]{1},
                            new TableLookupPlanForge[]{planInMidx}, null, new boolean[3]),
                        new LookupInstructionPlanForge(0, "s0", new int[]{2},
                            new TableLookupPlanForge[]{new FullTableScanLookupPlanForge(1, 2, false, types, getIndexKey("i2"))}, null, new boolean[3])
                    })
                    .get());

            InKeywordTableLookupPlanMultiIdxForge planInMidxMulitiSrc = new InKeywordTableLookupPlanMultiIdxForge(0, 1, false, types, getIndexKeys("i1", "i2"), SupportExprNodeFactory.makeIdentExprNode("p00"));
            tryAssertion(env, epl + " where p00 in (p10, p20)",
                SupportQueryPlanBuilder.start(3)
                    .setIndexFullTableScan(1, "i1")
                    .setIndexFullTableScan(2, "i2")
                    .setLookupPlanInstruction(0, "s0", new LookupInstructionPlanForge[]{
                        new LookupInstructionPlanForge(0, "s0", new int[]{1},
                            new TableLookupPlanForge[]{new FullTableScanLookupPlanForge(0, 1, false, types, getIndexKey("i1"))}, null, new boolean[3]),
                        new LookupInstructionPlanForge(0, "s0", new int[]{2},
                            new TableLookupPlanForge[]{new FullTableScanLookupPlanForge(1, 2, false, types, getIndexKey("i2"))}, null, new boolean[3])
                    })
                    .get());

            // 3-stream join with in-singleindex directional
            InKeywordTableLookupPlanSingleIdxForge planInSidx = new InKeywordTableLookupPlanSingleIdxForge(0, 1, false, types, getIndexKey("i1"), SupportExprNodeFactory.makeIdentExprNodes("p00", "p01"));
            tryAssertion(env, epl + " where p10 in (p00, p01)", getSingleIndexPlan(types, planInSidx));

            // 3-stream join with in-singleindex multi-sourced
            InKeywordTableLookupPlanSingleIdxForge planInSingleMultiSrc = new InKeywordTableLookupPlanSingleIdxForge(0, 1, false, types, getIndexKey("i1"), SupportExprNodeFactory.makeIdentExprNodes("p00"));
            tryAssertion(env, epl + " where p10 in (p00, p20)", getSingleIndexPlan(types, planInSingleMultiSrc));
        }
    }

    private static class EPLOtherQueryPlan2Stream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventType[] types = new EventType[2];
            String epl = "select * from SupportBean_S0 as s0 unidirectional, SupportBean_S1#keepall ";
            QueryPlanForge fullTableScan = SupportQueryPlanBuilder.start(2)
                .setIndexFullTableScan(1, "a")
                .setLookupPlanInner(0, new FullTableScanLookupPlanForge(0, 1, false, types, getIndexKey("a"))).get();

            // 2-stream unidirectional joins
            tryAssertion(env, epl, fullTableScan);

            QueryPlanForge planEquals = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p10")
                .setLookupPlanInner(0, new IndexedTableLookupPlanHashedOnlyForge(0, 1, false, types, getIndexKey("a"), new QueryGraphValueEntryHashKeyedForge[]{SupportExprNodeFactory.makeKeyed("p00")}, null, null, null)).get();
            tryAssertion(env, epl + "where p00 = p10", planEquals);
            tryAssertion(env, epl + "where p00 = p10 and p00 in (p11, p12, p13)", planEquals);

            QueryPlanForge planInMultiInner = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p11")
                .addIndexHashSingleNonUnique(1, "b", "p12")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanMultiIdxForge(0, 1, false, types, getIndexKeys("a", "b"), SupportExprNodeFactory.makeIdentExprNode("p00"))).get();
            tryAssertion(env, epl + "where p00 in (p11, p12)", planInMultiInner);
            tryAssertion(env, epl + "where p00 = p11 or p00 = p12", planInMultiInner);

            QueryPlanForge planInMultiOuter = SupportQueryPlanBuilder.start(planInMultiInner)
                .setLookupPlanOuter(0, new InKeywordTableLookupPlanMultiIdxForge(0, 1, false, types, getIndexKeys("a", "b"), SupportExprNodeFactory.makeIdentExprNode("p00"))).get();
            String eplOuterJoin = "select * from SupportBean_S0 as s0 unidirectional full outer join SupportBean_S1#keepall ";
            tryAssertion(env, eplOuterJoin + "where p00 in (p11, p12)", planInMultiOuter);

            QueryPlanForge planInMultiWConst = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p11")
                .addIndexHashSingleNonUnique(1, "b", "p12")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanMultiIdxForge(0, 1, false, types, getIndexKeys("a", "b"), SupportExprNodeFactory.makeConstExprNode("A"))).get();
            tryAssertion(env, epl + "where 'A' in (p11, p12)", planInMultiWConst);
            tryAssertion(env, epl + "where 'A' = p11 or 'A' = p12", planInMultiWConst);

            QueryPlanForge planInMultiWAddConst = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p12")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanMultiIdxForge(0, 1, false, types, getIndexKeys("a"), SupportExprNodeFactory.makeConstExprNode("A"))).get();
            tryAssertion(env, epl + "where 'A' in ('B', p12)", planInMultiWAddConst);
            tryAssertion(env, epl + "where 'A' in ('B', 'C')", fullTableScan);

            QueryPlanForge planInSingle = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p10")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanSingleIdxForge(0, 1, false, types, getIndexKey("a"), SupportExprNodeFactory.makeIdentExprNodes("p00", "p01"))).get();
            tryAssertion(env, epl + "where p10 in (p00, p01)", planInSingle);

            QueryPlanForge planInSingleWConst = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p10")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanSingleIdxForge(0, 1, false, types, getIndexKey("a"), SupportExprNodeFactory.makeConstAndIdentNode("A", "p01"))).get();
            tryAssertion(env, epl + "where p10 in ('A', p01)", planInSingleWConst);

            QueryPlanForge planInSingleJustConst = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(1, "a", "p10")
                .setLookupPlanInner(0, new InKeywordTableLookupPlanSingleIdxForge(0, 1, false, types, getIndexKey("a"), SupportExprNodeFactory.makeConstAndConstNode("A", "B"))).get();
            tryAssertion(env, epl + "where p10 in ('A', 'B')", planInSingleJustConst);
        }
    }

    private static void tryAssertion(RegressionEnvironment env, String epl, QueryPlanForge expectedPlan) {
        SupportQueryPlanIndexHook.reset();
        epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + epl;
        env.compileDeploy(epl);

        QueryPlanForge actualPlan = SupportQueryPlanIndexHook.assertJoinAndReset();
        SupportQueryPlanIndexHelper.compareQueryPlans(expectedPlan, actualPlan);

        env.undeployAll();
    }

    private static void assertSubqueryC0C1(RegressionEnvironment env, int c0, Integer[] c1) {
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        Assert.assertEquals(c0, event.get("c0"));
        Collection<Object> c1Coll = (Collection<Object>) event.get("c1");
        EPAssertionUtil.assertEqualsAnyOrder(c1, c1Coll == null ? null : c1Coll.toArray());
    }

    private static QueryPlanForge getSingleIndexPlan(EventType[] types, InKeywordTableLookupPlanSingleIdxForge plan) {
        return SupportQueryPlanBuilder.start(3)
            .addIndexHashSingleNonUnique(1, "i1", "p10")
            .setIndexFullTableScan(2, "i2")
            .setLookupPlanInstruction(0, "s0", new LookupInstructionPlanForge[]{
                new LookupInstructionPlanForge(0, "s0", new int[]{1},
                    new TableLookupPlanForge[]{plan}, null, new boolean[3]),
                new LookupInstructionPlanForge(0, "s0", new int[]{2},
                    new TableLookupPlanForge[]{new FullTableScanLookupPlanForge(1, 2, false, types, getIndexKey("i2"))}, null, new boolean[3])
            })
            .get();
    }

    private static TableLookupIndexReqKey[] getIndexKeys(String... names) {
        TableLookupIndexReqKey[] keys = new TableLookupIndexReqKey[names.length];
        for (int i = 0; i < names.length; i++) {
            keys[i] = new TableLookupIndexReqKey(names[i], null);
        }
        return keys;
    }

    private static void tryAssertionMultiIdx(RegressionEnvironment env) {
        String[] fields = "s0.id,s1.id".split(",");

        // single row tests
        env.sendEventBean(new SupportBean_S1(101, "a", "b", "c", "d"));

        env.sendEventBean(new SupportBean_S0(0, "a", "x"));
        env.sendEventBean(new SupportBean_S0(0, "x", "c"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(1, "a", "c"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 101});

        env.sendEventBean(new SupportBean_S0(2, "b", "d"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, 101});

        env.sendEventBean(new SupportBean_S0(3, "a", "d"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 101});

        // 2-row tests
        env.sendEventBean(new SupportBean_S1(102, "a1", "a", "d1", "d"));

        env.sendEventBean(new SupportBean_S0(0, "a", "x"));
        env.sendEventBean(new SupportBean_S0(0, "x", "c"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(10, "a", "c"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 101});

        env.sendEventBean(new SupportBean_S0(11, "a", "d"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{11, 101}, {11, 102}});

        env.sendEventBean(new SupportBean_S0(12, "a1", "d"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{12, 102}});

        env.sendEventBean(new SupportBean_S0(13, "a", "d1"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{13, 102}});

        // 3-row tests
        env.sendEventBean(new SupportBean_S1(103, "a", "a2", "d", "d2"));

        env.sendEventBean(new SupportBean_S0(20, "a", "c"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, 101});

        env.sendEventBean(new SupportBean_S0(21, "a", "d"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{21, 101}, {21, 102}, {21, 103}});

        env.sendEventBean(new SupportBean_S0(22, "a2", "d"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{22, 103}});

        env.sendEventBean(new SupportBean_S0(23, "a", "d2"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{23, 103}});

        env.undeployAll();
    }

    private static void tryAssertionSingleIdx(RegressionEnvironment env) {
        String[] fields = "s0.id,s1.id".split(",");

        // single row tests
        env.sendEventBean(new SupportBean_S0(100, "a", "c"));

        env.sendEventBean(new SupportBean_S1(0, "a1", "b", "c", "d"));
        env.sendEventBean(new SupportBean_S1(0, "a", "b", "x", "d"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(1, "a", "b", "c", "d"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{100, 1}});

        env.sendEventBean(new SupportBean_S1(2, "x", "a", "x", "c"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{100, 2}});

        // 2-rows available tests
        env.sendEventBean(new SupportBean_S0(101, "a", "d"));

        env.sendEventBean(new SupportBean_S1(0, "a1", "b", "c", "d"));
        env.sendEventBean(new SupportBean_S1(0, "a", "b", "x", "c1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(10, "a", "b", "c", "d"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{100, 10}, {101, 10}});

        env.sendEventBean(new SupportBean_S1(11, "x", "a", "x", "c"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{100, 11}});

        env.sendEventBean(new SupportBean_S1(12, "x", "a", "d", "x"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{101, 12}});

        // 3-rows available tests
        env.sendEventBean(new SupportBean_S0(102, "b", "c"));

        env.sendEventBean(new SupportBean_S1(0, "a1", "b", "c1", "d"));
        env.sendEventBean(new SupportBean_S1(0, "a", "b", "x", "c1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(20, "a", "b", "c", "d"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{100, 20}, {101, 20}, {102, 20}});

        env.sendEventBean(new SupportBean_S1(21, "b", "a", "x", "c"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{100, 21}, {102, 21}});

        env.sendEventBean(new SupportBean_S1(22, "b", "a", "d", "c"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{100, 22}, {101, 22}, {102, 22}});

        env.sendEventBean(new SupportBean_S1(23, "b", "x", "x", "c"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{102, 23}});

        env.undeployAll();
    }

    private static TableLookupIndexReqKey getIndexKey(String name) {
        return new TableLookupIndexReqKey(name, null);
    }
}
