/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.epl.join.plan.*;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescOnExpr;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescSubquery;
import com.espertech.esper.epl.lookup.SubordFullTableScanLookupStrategyFactory;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategyAllFiltered;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategyFactoryAllFiltered;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportExprNodeFactory;
import com.espertech.esper.support.epl.SupportQueryPlanBuilder;
import com.espertech.esper.support.epl.SupportQueryPlanIndexHelper;
import com.espertech.esper.support.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.support.util.IndexBackingTableInfo;
import junit.framework.TestCase;

public class TestExcludePlanHint extends TestCase implements IndexBackingTableInfo
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S1", SupportBean_S1.class);
    }

    public void testDocSample() {
        epService.getEPAdministrator().getConfiguration().addEventType(AEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType(BEvent.class);

        String[] hints = new String[] {
                "@hint('exclude_plan(true)')",
                "@hint('exclude_plan(opname=\"equals\")')",
                "@hint('exclude_plan(opname=\"equals\" and from_streamname=\"a\")')",
                "@hint('exclude_plan(opname=\"equals\" and from_streamname=\"b\")')",
                "@hint('exclude_plan(exprs[0]=\"aprop\")')"};
        for (String hint : hints) {
            epService.getEPAdministrator().createEPL("@Audit " + hint +
                    "select * from AEvent#keepall() as a, BEvent#keepall() as b where aprop = bprop");
        }

        // test subquery
        SupportQueryPlanIndexHook.reset();
        epService.getEPAdministrator().createEPL(INDEX_CALLBACK_HOOK + "@hint('exclude_plan(true)') select (select * from S0#unique(p00) as s0 where s1.p10 = p00) from S1 as s1");
        QueryPlanIndexDescSubquery subq = SupportQueryPlanIndexHook.getAndResetSubqueries().get(0);
        assertEquals(SubordFullTableScanLookupStrategyFactory.class.getSimpleName(), subq.getTableLookupStrategy());

        // test named window
        epService.getEPAdministrator().createEPL("create window S0Window#keepall() as S0");
        epService.getEPAdministrator().createEPL(INDEX_CALLBACK_HOOK + "@hint('exclude_plan(true)') on S1 as s1 select * from S0Window as s0 where s1.p10 = s0.p00");
        QueryPlanIndexDescOnExpr onExpr = SupportQueryPlanIndexHook.getAndResetOnExpr();
        assertEquals(SubordWMatchExprLookupStrategyFactoryAllFiltered.class.getSimpleName(), onExpr.getStrategyName());
    }

    public void testJoin() {
        String epl = "select * from S0#keepall() as s0, S1#keepall() as s1 ";
        QueryPlan planFullTableScan = SupportQueryPlanBuilder.start(2)
                .setIndexFullTableScan(0, "i0")
                .setIndexFullTableScan(1, "i1")
                .setLookupPlanInner(0, new FullTableScanLookupPlan(0, 1, getIndexKey("i1")))
                .setLookupPlanInner(1, new FullTableScanLookupPlan(1, 0, getIndexKey("i0"))).get();

        // test "any"
        String excludeAny = "@hint('exclude_plan(true)')";
        runAssertionJoin(epl, planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 = p10", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 = 'abc'", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 = (p10 || 'A')", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p10 = 'abc'", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 > p10", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 > 'A'", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p10 > 'A'", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p10 > 'A'", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 > (p10 || 'A')", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 between p10 and p11", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 between 'a' and p11", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 between 'a' and 'c'", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 between p10 and 'c'", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 in (p10, p11)", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 in ('a', p11)", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p00 in ('a', 'b')", planFullTableScan);
        runAssertionJoin(excludeAny + epl + " where p10 in (p00, p01)", planFullTableScan);

        // test EQUALS
        QueryPlan planEquals = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(0, "i1", "p00")
                .setIndexFullTableScan(1, "i2")
                .setLookupPlanInner(0, new FullTableScanLookupPlan(0, 1, getIndexKey("i2")))
                .setLookupPlanInner(1, new IndexedTableLookupPlanSingle(1, 0, getIndexKey("i1"), SupportExprNodeFactory.makeKeyed("p10"))).get();
        String eplWithWhereEquals = epl + " where p00 = p10";
        runAssertionJoin("@hint('exclude_plan(from_streamnum=0)')" + eplWithWhereEquals, planEquals);
        runAssertionJoin("@hint('exclude_plan(from_streamname=\"s0\")')" + eplWithWhereEquals, planEquals);
        runAssertionJoin("@hint('exclude_plan(from_streamname=\"s0\")') @hint('exclude_plan(from_streamname=\"s1\")')" + eplWithWhereEquals, planFullTableScan);
        runAssertionJoin("@hint('exclude_plan(from_streamname=\"s0\")') @hint('exclude_plan(from_streamname=\"s1\")')" + eplWithWhereEquals, planFullTableScan);
        runAssertionJoin("@hint('exclude_plan(to_streamname=\"s1\")')" + eplWithWhereEquals, planEquals);
        runAssertionJoin("@hint('exclude_plan(to_streamname=\"s0\")') @hint('exclude_plan(to_streamname=\"s1\")')" + eplWithWhereEquals, planFullTableScan);
        runAssertionJoin("@hint('exclude_plan(from_streamnum=0 and to_streamnum =  1)')" + eplWithWhereEquals, planEquals);
        runAssertionJoin("@hint('exclude_plan(to_streamnum=1)')" + eplWithWhereEquals, planEquals);
        runAssertionJoin("@hint('exclude_plan(to_streamnum = 1, from_streamnum = 0)')" + eplWithWhereEquals, planEquals);
        runAssertionJoin("@hint('exclude_plan(opname=\"equals\")')" + eplWithWhereEquals, planFullTableScan);
        runAssertionJoin("@hint('exclude_plan(exprs.anyOf(v=> v=\"p00\"))')" + eplWithWhereEquals, planFullTableScan);
        runAssertionJoin("@hint('exclude_plan(\"p10\" in (exprs))')" + eplWithWhereEquals, planFullTableScan);

        // test greater (relop)
        QueryPlan planGreater = SupportQueryPlanBuilder.start(2)
                .addIndexBtreeSingle(0, "i1", "p00")
                .setIndexFullTableScan(1, "i2")
                .setLookupPlanInner(0, new FullTableScanLookupPlan(0, 1, getIndexKey("i2")))
                .setLookupPlanInner(1, new SortedTableLookupPlan(1, 0, getIndexKey("i1"), SupportExprNodeFactory.makeRangeLess("p10"))).get();
        String eplWithWhereGreater = epl + " where p00 > p10";
        runAssertionJoin("@hint('exclude_plan(from_streamnum=0)')" + eplWithWhereGreater, planGreater);
        runAssertionJoin("@hint('exclude_plan(opname=\"relop\")')" + eplWithWhereGreater, planFullTableScan);

        // test range (relop)
        QueryPlan planRange = SupportQueryPlanBuilder.start(2)
                .addIndexBtreeSingle(0, "i1", "p00")
                .setIndexFullTableScan(1, "i2")
                .setLookupPlanInner(0, new FullTableScanLookupPlan(0, 1, getIndexKey("i2")))
                .setLookupPlanInner(1, new SortedTableLookupPlan(1, 0, getIndexKey("i1"), SupportExprNodeFactory.makeRangeIn("p10", "p11"))).get();
        String eplWithWhereRange = epl + " where p00 between p10 and p11";
        runAssertionJoin("@hint('exclude_plan(from_streamnum=0)')" + eplWithWhereRange, planRange);
        runAssertionJoin("@hint('exclude_plan(opname=\"relop\")')" + eplWithWhereRange, planFullTableScan);

        // test in (relop)
        QueryPlan planIn = SupportQueryPlanBuilder.start(2)
                .addIndexHashSingleNonUnique(0, "i1", "p00")
                .setIndexFullTableScan(1, "i2")
                .setLookupPlanInner(0, new FullTableScanLookupPlan(0, 1, getIndexKey("i2")))
                .setLookupPlanInner(1, new InKeywordTableLookupPlanSingleIdx(1, 0, getIndexKey("i1"), SupportExprNodeFactory.makeIdentExprNodes("p10", "p11"))).get();
        String eplWithIn = epl + " where p00 in (p10, p11)";
        runAssertionJoin("@hint('exclude_plan(from_streamnum=0)')" + eplWithIn, planIn);
        runAssertionJoin("@hint('exclude_plan(opname=\"inkw\")')" + eplWithIn, planFullTableScan);
    }

    public void testInvalid() {
        String epl = "select * from S0 unidirectional, S1#keepall()";
        // no params
        tryInvalid("@hint('exclude_plan') " + epl,
                "Failed to process statement annotations: Hint 'EXCLUDE_PLAN' requires additional parameters in parentheses [@hint('exclude_plan') select * from S0 unidirectional, S1#keepall()]");

        // empty parameter allowed, to be filled in
        epService.getEPAdministrator().createEPL("@hint('exclude_plan()') " + epl);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        // invalid return type
        tryInvalid("@hint('exclude_plan(1)') " + epl,
                "Error starting statement: Expression provided for hint EXCLUDE_PLAN must return a boolean value [@hint('exclude_plan(1)') select * from S0 unidirectional, S1#keepall()]");

        // invalid expression
        tryInvalid("@hint('exclude_plan(dummy = 1)') " + epl,
                "Error starting statement: Failed to validate hint expression 'dummy=1': Property named 'dummy' is not valid in any stream [@hint('exclude_plan(dummy = 1)') select * from S0 unidirectional, S1#keepall()]");
    }

    private void tryInvalid(String epl, String expected) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(expected, ex.getMessage());
        }
    }

    private void runAssertionJoin(String epl, QueryPlan expectedPlan)
    {
        SupportQueryPlanIndexHook.reset();
        epl = INDEX_CALLBACK_HOOK + epl;
        epService.getEPAdministrator().createEPL(epl);

        QueryPlan actualPlan = SupportQueryPlanIndexHook.assertJoinAndReset();
        SupportQueryPlanIndexHelper.compareQueryPlans(expectedPlan, actualPlan);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private static class AEvent {
        private final String aprop;

        private AEvent(String aprop) {
            this.aprop = aprop;
        }

        public String getAprop() {
            return aprop;
        }
    }

    private static class BEvent {
        private final String bprop;

        private BEvent(String bprop) {
            this.bprop = bprop;
        }

        public String getBprop() {
            return bprop;
        }
    }

    private static TableLookupIndexReqKey getIndexKey(String name) {
        return new TableLookupIndexReqKey(name);
    }
}
