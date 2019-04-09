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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.FullTableScanLookupPlanForge;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.InKeywordTableLookupPlanSingleIdxForge;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.IndexedTableLookupPlanHashedOnlyForge;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.SortedTableLookupPlanForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescOnExpr;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescSubquery;
import com.espertech.esper.common.internal.epl.lookup.SubordFullTableScanLookupStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.lookupsubord.SubordWMatchExprLookupStrategyAllFilteredForge;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.epl.SupportExprNodeFactory;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanBuilder;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHelper;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class EPLOtherPlanExcludeHint implements IndexBackingTableInfo {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherDocSample());
        execs.add(new EPLOtherJoin());
        execs.add(new EPLOtherInvalid());
        return execs;
    }

    private static class EPLOtherDocSample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schema =
                "create schema AEvent as " + AEvent.class.getName() + ";\n" +
                    "create schema BEvent as " + BEvent.class.getName() + ";\n";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(schema, path);

            String[] hints = new String[]{
                "@hint('exclude_plan(true)')",
                "@hint('exclude_plan(opname=\"equals\")')",
                "@hint('exclude_plan(opname=\"equals\" and from_streamname=\"a\")')",
                "@hint('exclude_plan(opname=\"equals\" and from_streamname=\"b\")')",
                "@hint('exclude_plan(exprs[0]=\"aprop\")')"};
            for (String hint : hints) {
                env.compileDeploy("@Audit " + hint +
                    "select * from AEvent#keepall as a, BEvent#keepall as b where aprop = bprop", path);
            }

            // test subquery
            SupportQueryPlanIndexHook.reset();
            env.compileDeploy(IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@hint('exclude_plan(true)') select (select * from SupportBean_S0#unique(p00) as s0 where s1.p10 = p00) from SupportBean_S1 as s1", path);
            QueryPlanIndexDescSubquery subq = SupportQueryPlanIndexHook.getAndResetSubqueries().get(0);
            Assert.assertEquals(SubordFullTableScanLookupStrategyFactoryForge.class.getSimpleName(), subq.getTableLookupStrategy());

            // test named window
            env.compileDeploy("create window S0Window#keepall as SupportBean_S0", path);
            env.compileDeploy(IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@hint('exclude_plan(true)') on SupportBean_S1 as s1 select * from S0Window as s0 where s1.p10 = s0.p00", path);
            QueryPlanIndexDescOnExpr onExpr = SupportQueryPlanIndexHook.getAndResetOnExpr();
            Assert.assertEquals(SubordWMatchExprLookupStrategyAllFilteredForge.class.getSimpleName(), onExpr.getStrategyName());

            env.undeployAll();
        }
    }

    private static class EPLOtherJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventType[] types = new EventType[]{env.runtime().getEventTypeService().getEventTypePreconfigured("SupportBean_S0"), env.runtime().getEventTypeService().getEventTypePreconfigured("SupportBean_S1")};
            String epl = "select * from SupportBean_S0#keepall as s0, SupportBean_S1#keepall as s1 ";
            QueryPlanForge planFullTableScan = SupportQueryPlanBuilder.start(2)
                .setIndexFullTableScan(0, "i0")
                .setIndexFullTableScan(1, "i1")
                .setLookupPlanInner(0, new FullTableScanLookupPlanForge(0, 1, false, types, getIndexKey("i1")))
                .setLookupPlanInner(1, new FullTableScanLookupPlanForge(1, 0, false, types, getIndexKey("i0"))).get();

            // test "any"
            String excludeAny = "@hint('exclude_plan(true)')";
            tryAssertionJoin(env, epl, planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 = p10", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 = 'abc'", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 = (p10 || 'A')", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p10 = 'abc'", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 > p10", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 > 'A'", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p10 > 'A'", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p10 > 'A'", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 > (p10 || 'A')", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 between p10 and p11", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 between 'a' and p11", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 between 'a' and 'c'", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 between p10 and 'c'", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 in (p10, p11)", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 in ('a', p11)", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p00 in ('a', 'b')", planFullTableScan);
            tryAssertionJoin(env, excludeAny + epl + " where p10 in (p00, p01)", planFullTableScan);

            // test EQUALS
            QueryPlanForge planEquals = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(0, "i1", "p00")
                .setIndexFullTableScan(1, "i2")
                .setLookupPlanInner(0, new FullTableScanLookupPlanForge(0, 1, false, types, getIndexKey("i2")))
                .setLookupPlanInner(1, new IndexedTableLookupPlanHashedOnlyForge(1, 0, false, types, getIndexKey("i1"), new QueryGraphValueEntryHashKeyedForge[]{SupportExprNodeFactory.makeKeyed("p10")}, null, null, null)).get();
            String eplWithWhereEquals = epl + " where p00 = p10";
            tryAssertionJoin(env, "@hint('exclude_plan(from_streamnum=0)')" + eplWithWhereEquals, planEquals);
            tryAssertionJoin(env, "@hint('exclude_plan(from_streamname=\"s0\")')" + eplWithWhereEquals, planEquals);
            tryAssertionJoin(env, "@hint('exclude_plan(from_streamname=\"s0\")') @hint('exclude_plan(from_streamname=\"s1\")')" + eplWithWhereEquals, planFullTableScan);
            tryAssertionJoin(env, "@hint('exclude_plan(from_streamname=\"s0\")') @hint('exclude_plan(from_streamname=\"s1\")')" + eplWithWhereEquals, planFullTableScan);
            tryAssertionJoin(env, "@hint('exclude_plan(to_streamname=\"s1\")')" + eplWithWhereEquals, planEquals);
            tryAssertionJoin(env, "@hint('exclude_plan(to_streamname=\"s0\")') @hint('exclude_plan(to_streamname=\"s1\")')" + eplWithWhereEquals, planFullTableScan);
            tryAssertionJoin(env, "@hint('exclude_plan(from_streamnum=0 and to_streamnum =  1)')" + eplWithWhereEquals, planEquals);
            tryAssertionJoin(env, "@hint('exclude_plan(to_streamnum=1)')" + eplWithWhereEquals, planEquals);
            tryAssertionJoin(env, "@hint('exclude_plan(to_streamnum = 1, from_streamnum = 0)')" + eplWithWhereEquals, planEquals);
            tryAssertionJoin(env, "@hint('exclude_plan(opname=\"equals\")')" + eplWithWhereEquals, planFullTableScan);
            tryAssertionJoin(env, "@hint('exclude_plan(exprs.anyOf(v=> v=\"p00\"))')" + eplWithWhereEquals, planFullTableScan);
            tryAssertionJoin(env, "@hint('exclude_plan(\"p10\" in (exprs))')" + eplWithWhereEquals, planFullTableScan);

            // test greater (relop)
            QueryPlanForge planGreater = SupportQueryPlanBuilder.start(2)
                .addIndexBtreeSingle(0, "i1", "p00")
                .setIndexFullTableScan(1, "i2")
                .setLookupPlanInner(0, new FullTableScanLookupPlanForge(0, 1, false, types, getIndexKey("i2")))
                .setLookupPlanInner(1, new SortedTableLookupPlanForge(1, 0, false, types, getIndexKey("i1"), SupportExprNodeFactory.makeRangeLess("p10"), null)).get();
            String eplWithWhereGreater = epl + " where p00 > p10";
            tryAssertionJoin(env, "@hint('exclude_plan(from_streamnum=0)')" + eplWithWhereGreater, planGreater);
            tryAssertionJoin(env, "@hint('exclude_plan(opname=\"relop\")')" + eplWithWhereGreater, planFullTableScan);

            // test range (relop)
            QueryPlanForge planRange = SupportQueryPlanBuilder.start(2)
                .addIndexBtreeSingle(0, "i1", "p00")
                .setIndexFullTableScan(1, "i2")
                .setLookupPlanInner(0, new FullTableScanLookupPlanForge(0, 1, false, types, getIndexKey("i2")))
                .setLookupPlanInner(1, new SortedTableLookupPlanForge(1, 0, false, types, getIndexKey("i1"), SupportExprNodeFactory.makeRangeIn("p10", "p11"), null)).get();
            String eplWithWhereRange = epl + " where p00 between p10 and p11";
            tryAssertionJoin(env, "@hint('exclude_plan(from_streamnum=0)')" + eplWithWhereRange, planRange);
            tryAssertionJoin(env, "@hint('exclude_plan(opname=\"relop\")')" + eplWithWhereRange, planFullTableScan);

            // test in (relop)
            QueryPlanForge planIn = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(0, "i1", "p00")
                .setIndexFullTableScan(1, "i2")
                .setLookupPlanInner(0, new FullTableScanLookupPlanForge(0, 1, false, types, getIndexKey("i2")))
                .setLookupPlanInner(1, new InKeywordTableLookupPlanSingleIdxForge(1, 0, false, types, getIndexKey("i1"), SupportExprNodeFactory.makeIdentExprNodes("p10", "p11"))).get();
            String eplWithIn = epl + " where p00 in (p10, p11)";
            tryAssertionJoin(env, "@hint('exclude_plan(from_streamnum=0)')" + eplWithIn, planIn);
            tryAssertionJoin(env, "@hint('exclude_plan(opname=\"inkw\")')" + eplWithIn, planFullTableScan);

            env.undeployAll();
        }
    }

    private static class EPLOtherInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select * from SupportBean_S0 unidirectional, SupportBean_S1#keepall";
            // no params
            SupportMessageAssertUtil.tryInvalidCompile(env, "@hint('exclude_plan') " + epl,
                "Failed to process statement annotations: Hint 'EXCLUDE_PLAN' requires additional parameters in parentheses");

            // empty parameter allowed, to be filled in
            env.compileDeploy("@hint('exclude_plan()') " + epl);
            env.sendEventBean(new SupportBean_S0(1));

            // invalid return type
            SupportMessageAssertUtil.tryInvalidCompile(env, "@hint('exclude_plan(1)') " + epl,
                "Expression provided for hint EXCLUDE_PLAN must return a boolean value");

            // invalid expression
            SupportMessageAssertUtil.tryInvalidCompile(env, "@hint('exclude_plan(dummy = 1)') " + epl,
                "Failed to validate hint expression 'dummy=1': Property named 'dummy' is not valid in any stream");

            env.undeployAll();
        }
    }

    private static void tryAssertionJoin(RegressionEnvironment env, String epl, QueryPlanForge expectedPlan) {
        SupportQueryPlanIndexHook.reset();
        epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + epl;
        env.compileDeploy(epl);

        QueryPlanForge actualPlan = SupportQueryPlanIndexHook.assertJoinAndReset();
        SupportQueryPlanIndexHelper.compareQueryPlans(expectedPlan, actualPlan);

        env.undeployAll();
    }

    public static class AEvent {
        private final String aprop;

        private AEvent(String aprop) {
            this.aprop = aprop;
        }

        public String getAprop() {
            return aprop;
        }
    }

    public static class BEvent {
        private final String bprop;

        private BEvent(String bprop) {
            this.bprop = bprop;
        }

        public String getBprop() {
            return bprop;
        }
    }

    private static TableLookupIndexReqKey getIndexKey(String name) {
        return new TableLookupIndexReqKey(name, null);
    }
}
