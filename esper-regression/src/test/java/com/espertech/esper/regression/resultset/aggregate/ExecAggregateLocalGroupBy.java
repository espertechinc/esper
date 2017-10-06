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
package com.espertech.esper.regression.resultset.aggregate;

import com.espertech.esper.client.ConfigurationPlugInAggregationMultiFunction;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.HookType;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.agg.util.AggregationGroupByLocalGroupDesc;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlanForge;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.MyConcatAggregationFunctionFactory;
import com.espertech.esper.supportregression.client.SupportAggMFFactory;
import com.espertech.esper.supportregression.client.SupportAggMFFunc;
import com.espertech.esper.supportregression.epl.SupportAggLevelPlanHook;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.util.Collection;

import static org.junit.Assert.*;

public class ExecAggregateLocalGroupBy implements RegressionExecution {
    public final static String PLAN_CALLBACK_HOOK = "@Hook(type=" + HookType.class.getName() + ".INTERNAL_AGGLOCALLEVEL,hook='" + SupportAggLevelPlanHook.class.getName() + "')";

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        SupportAggLevelPlanHook.getAndReset();

        runAssertionInvalid(epService);
        runAssertionUngroupedAndLocalSyntax(epService);
        runAssertionGrouped(epService);
        runAssertionPlanning(epService);
        runAssertionFullyVersusNotFullyAgg(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {

        // not valid with count-min-sketch
        SupportMessageAssertUtil.tryInvalid(epService, "create table MyTable(approx countMinSketch(group_by:theString) @type(SupportBean))",
                "Error starting statement: Failed to validate table-column expression 'countMinSketch(group_by:theString)': Count-min-sketch aggregation function 'countMinSketch'  expects either no parameter or a single json parameter object");

        // not allowed with tables
        SupportMessageAssertUtil.tryInvalid(epService, "create table MyTable(col sum(int, group_by:theString) @type(SupportBean))",
                "Error starting statement: Failed to validate table-column expression 'sum(int,group_by:theString)': The 'group_by' and 'filter' parameter is not allowed in create-table statements");

        // invalid named parameter
        SupportMessageAssertUtil.tryInvalid(epService, "select sum(intPrimitive, xxx:theString) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'sum(intPrimitive,xxx:theString)': Invalid named parameter 'xxx' (did you mean 'group_by' or 'filter'?) [");

        // invalid group-by expression
        SupportMessageAssertUtil.tryInvalid(epService, "select sum(intPrimitive, group_by:sum(intPrimitive)) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'sum(intPrimitive,group_by:sum(intPr...(44 chars)': Group-by expressions cannot contain aggregate functions");

        // other functions don't accept this named parameter
        SupportMessageAssertUtil.tryInvalid(epService, "select coalesce(0, 1, group_by:theString) from SupportBean",
                "Incorrect syntax near ':' at line 1 column 30");
        SupportMessageAssertUtil.tryInvalid(epService, "select " + SupportStaticMethodLib.class.getName() + ".staticMethod(group_by:intPrimitive) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'com.espertech.esper.supportregressi...(100 chars)': Named parameters are not allowed");

        // not allowed in combination with roll-up
        SupportMessageAssertUtil.tryInvalid(epService, "select sum(intPrimitive, group_by:theString) from SupportBean group by rollup(theString)",
                "Error starting statement: Roll-up and group-by parameters cannot be combined ");

        // not allowed in combination with into-table
        epService.getEPAdministrator().createEPL("create table mytable (thesum sum(int))");
        SupportMessageAssertUtil.tryInvalid(epService, "into table mytable select sum(intPrimitive, group_by:theString) as thesum from SupportBean",
                "Error starting statement: Into-table and group-by parameters cannot be combined");

        // not allowed for match-rezognize measure clauses
        String eplMatchRecog = "select * from SupportBean match_recognize (" +
                "  measures count(B.intPrimitive, group_by:B.theString) pattern (A B* C))";
        SupportMessageAssertUtil.tryInvalid(epService, eplMatchRecog,
                "Error starting statement: Match-recognize does not allow aggregation functions to specify a group-by");

        // disallow subqueries to specify their own local group-by
        String eplSubq = "select (select sum(intPrimitive, group_by:theString) from SupportBean#keepall) from SupportBean_S0";
        SupportMessageAssertUtil.tryInvalid(epService, eplSubq,
                "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subselect aggregations functions cannot specify a group-by");
    }

    private void runAssertionUngroupedAndLocalSyntax(EPServiceProvider epService) throws Exception {
        tryAssertionUngroupedAggSQLStandard(epService);
        tryAssertionUngroupedAggEvent(epService);
        tryAssertionUngroupedAggAdditionalAndPlugin(epService);
        tryAssertionUngroupedAggIterator(epService);
        tryAssertionUngroupedParenSODA(epService, false);
        tryAssertionUngroupedParenSODA(epService, true);
        tryAssertionColNameRendering(epService);
        tryAssertionUngroupedSameKey(epService);
        tryAssertionUngroupedRowRemove(epService);
        tryAssertionUngroupedHaving(epService);
        tryAssertionUngroupedOrderBy(epService);
        tryAssertionUngroupedUnidirectionalJoin(epService);
        tryAssertionEnumMethods(epService, true);
    }

    private void runAssertionGrouped(EPServiceProvider epService) throws Exception {
        tryAssertionGroupedSolutionPattern(epService);
        tryAssertionGroupedMultiLevelMethod(epService);
        tryAssertionGroupedMultiLevelAccess(epService);
        tryAssertionGroupedMultiLevelNoDefaultLvl(epService);
        tryAssertionGroupedSameKey(epService);
        tryAssertionGroupedRowRemove(epService);
        tryAssertionGroupedOnSelect(epService);
        tryAssertionEnumMethods(epService, false);
    }

    private void runAssertionPlanning(EPServiceProvider epService) {
        assertNoPlan(epService, "select sum(group_by:(),intPrimitive) as c0 from SupportBean");
        assertNoPlan(epService, "select sum(group_by:(theString),intPrimitive) as c0 from SupportBean group by theString");
        assertNoPlan(epService, "select sum(group_by:(theString, intPrimitive),longPrimitive) as c0 from SupportBean group by theString, intPrimitive");
        assertNoPlan(epService, "select sum(group_by:(intPrimitive, theString),longPrimitive) as c0 from SupportBean group by theString, intPrimitive");

        // provide column count stays at 1
        assertCountColsAndLevels(epService, "select sum(group_by:(theString),intPrimitive) as c0, sum(group_by:(theString),intPrimitive) as c1 from SupportBean",
                1, 1);

        // prove order of group-by expressions does not matter
        assertCountColsAndLevels(epService, "select sum(group_by:(intPrimitive, theString),longPrimitive) as c0, sum(longPrimitive, group_by:(theString, intPrimitive)) as c1 from SupportBean",
                1, 1);

        // prove the number of levels stays the same even when group-by expressions vary
        assertCountColsAndLevels(epService, "select sum(group_by:(intPrimitive, theString),longPrimitive) as c0, count(*, group_by:(theString, intPrimitive)) as c1 from SupportBean",
                2, 1);

        // prove there is one shared state factory
        String theEpl = PLAN_CALLBACK_HOOK + "select window(*, group_by:theString), last(*, group_by:theString) from SupportBean#length(2)";
        epService.getEPAdministrator().createEPL(theEpl);
        Pair<AggregationGroupByLocalGroupDesc, AggregationLocalGroupByPlanForge> plan = SupportAggLevelPlanHook.getAndReset();
        assertEquals(1, plan.getSecond().getAllLevelsForges().length);
        assertEquals(1, plan.getSecond().getAllLevelsForges()[0].getAccessStateForges().length);
    }

    private void runAssertionFullyVersusNotFullyAgg(EPServiceProvider epService) throws Exception {
        final String[] colsC0 = "c0".split(",");

        // full-aggregated and un-grouped (row for all)
        tryAssertionAggAndFullyAgg(epService, "select sum(group_by:(),intPrimitive) as c0 from SupportBean",
                new MyAssertion() {
                    public void doAssert(SupportUpdateListener listener) {
                        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), colsC0, new Object[]{60});
                    }
                });

        // aggregated and un-grouped (row for event)
        tryAssertionAggAndFullyAgg(epService, "select sum(group_by:theString, intPrimitive) as c0 from SupportBean#keepall",
                new MyAssertion() {
                    public void doAssert(SupportUpdateListener listener) {
                        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), colsC0, new Object[][]{{10}, {50}, {50}});
                    }
                });

        // fully aggregated and grouped (row for group)
        tryAssertionAggAndFullyAgg(epService, "select sum(intPrimitive, group_by:()) as c0, sum(group_by:theString, intPrimitive) as c1, theString " +
                        "from SupportBean group by theString",
                new MyAssertion() {
                    public void doAssert(SupportUpdateListener listener) {
                        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "theString,c0,c1".split(","), new Object[][]{{"E1", 60, 10}, {"E2", 60, 50}});
                    }
                });

        // aggregated and grouped (row for event)
        tryAssertionAggAndFullyAgg(epService, "select sum(longPrimitive, group_by:()) as c0," +
                        " sum(longPrimitive, group_by:theString) as c1, " +
                        " sum(longPrimitive, group_by:intPrimitive) as c2, " +
                        " theString " +
                        "from SupportBean#keepall group by theString",
                new MyAssertion() {
                    public void doAssert(SupportUpdateListener listener) {
                        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(),
                                "theString,c0,c1,c2".split(","), new Object[][]{{"E1", 600L, 100L, 100L}, {"E2", 600L, 500L, 200L}, {"E2", 600L, 500L, 300L}});
                    }
                });
    }

    private void tryAssertionUngroupedRowRemove(EPServiceProvider epService) throws Exception {
        String[] cols = "theString,intPrimitive,c0,c1".split(",");
        String epl = "create window MyWindow#keepall as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "on SupportBean_S0 delete from MyWindow where p00 = theString and id = intPrimitive;\n" +
                "on SupportBean_S1 delete from MyWindow;\n" +
                "@name('out') select theString, intPrimitive, sum(longPrimitive) as c0, " +
                "  sum(longPrimitive, group_by:theString) as c1 from MyWindow;\n";
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        makeSendEvent(epService, "E1", 10, 101);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 10, 101L, 101L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E1")); // delete event {"E1", 10}
        assertFalse(listener.isInvoked());

        makeSendEvent(epService, "E1", 20, 102);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 20, 102L, 102L});

        makeSendEvent(epService, "E2", 30, 103);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E2", 30, 102 + 103L, 103L});

        makeSendEvent(epService, "E1", 40, 104);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 40, 102 + 103 + 104L, 102 + 104L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(40, "E1")); // delete event {"E1", 40}
        assertFalse(listener.isInvoked());

        makeSendEvent(epService, "E1", 50, 105);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 50, 102 + 103 + 105L, 102 + 105L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1)); // delete all
        assertFalse(listener.isInvoked());

        makeSendEvent(epService, "E1", 60, 106);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 60, 106L, 106L});

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(result.getDeploymentId());
    }

    private void tryAssertionGroupedRowRemove(EPServiceProvider epService) throws Exception {
        String[] cols = "theString,intPrimitive,c0,c1".split(",");
        String epl = "create window MyWindow#keepall as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "on SupportBean_S0 delete from MyWindow where p00 = theString and id = intPrimitive;\n" +
                "on SupportBean_S1 delete from MyWindow;\n" +
                "@name('out') select theString, intPrimitive, sum(longPrimitive) as c0, " +
                "  sum(longPrimitive, group_by:theString) as c1 " +
                "  from MyWindow group by theString, intPrimitive;\n";
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        makeSendEvent(epService, "E1", 10, 101);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 10, 101L, 101L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E1")); // delete event {"E1", 10}
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 10, null, null});

        makeSendEvent(epService, "E1", 20, 102);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 20, 102L, 102L});

        makeSendEvent(epService, "E2", 30, 103);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E2", 30, 103L, 103L});

        makeSendEvent(epService, "E1", 40, 104);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 40, 104L, 102 + 104L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(40, "E1")); // delete event {"E1", 40}
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 40, null, 102L});

        makeSendEvent(epService, "E1", 50, 105);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 50, 105L, 102 + 105L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1)); // delete all
        listener.reset();

        makeSendEvent(epService, "E1", 60, 106);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{"E1", 60, 106L, 106L});

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(result.getDeploymentId());
    }

    private void tryAssertionGroupedMultiLevelMethod(EPServiceProvider epService) {
        sendTime(epService, 0);
        String[] fields = "theString,intPrimitive,c0,c1,c2,c3,c4".split(",");
        String epl = "select" +
                "   theString, intPrimitive," +
                "   sum(longPrimitive, group_by:(intPrimitive, theString)) as c0," +
                "   sum(longPrimitive) as c1," +
                "   sum(longPrimitive, group_by:(theString)) as c2," +
                "   sum(longPrimitive, group_by:(intPrimitive)) as c3," +
                "   sum(longPrimitive, group_by:()) as c4" +
                " from SupportBean" +
                " group by theString, intPrimitive" +
                " output snapshot every 10 seconds";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        makeSendEvent(epService, "E1", 10, 100);
        makeSendEvent(epService, "E1", 20, 202);
        makeSendEvent(epService, "E2", 10, 303);
        makeSendEvent(epService, "E1", 10, 404);
        makeSendEvent(epService, "E2", 10, 505);
        sendTime(epService, 10000);

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, 504L, 504L, 706L, 1312L, 1514L}, {"E1", 20, 202L, 202L, 706L, 202L, 1514L}, {"E2", 10, 808L, 808L, 808L, 1312L, 1514L}});

        makeSendEvent(epService, "E1", 10, 1);
        sendTime(epService, 20000);

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, 505L, 505L, 707L, 1313L, 1515L}, {"E1", 20, 202L, 202L, 707L, 202L, 1515L}, {"E2", 10, 808L, 808L, 808L, 1313L, 1515L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionGroupedMultiLevelAccess(EPServiceProvider epService) {
        sendTime(epService, 0);
        String[] fields = "theString,intPrimitive,c0,c1,c2,c3,c4".split(",");
        String epl = "select" +
                "   theString, intPrimitive," +
                "   window(*, group_by:(intPrimitive, theString)) as c0," +
                "   window(*) as c1," +
                "   window(*, group_by:theString) as c2," +
                "   window(*, group_by:intPrimitive) as c3," +
                "   window(*, group_by:()) as c4" +
                " from SupportBean#keepall" +
                " group by theString, intPrimitive" +
                " output snapshot every 10 seconds" +
                " order by theString, intPrimitive";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        SupportBean b1 = makeSendEvent(epService, "E1", 10, 100);
        SupportBean b2 = makeSendEvent(epService, "E1", 20, 202);
        SupportBean b3 = makeSendEvent(epService, "E2", 10, 303);
        SupportBean b4 = makeSendEvent(epService, "E1", 10, 404);
        SupportBean b5 = makeSendEvent(epService, "E2", 10, 505);
        sendTime(epService, 10000);

        Object[] all = new Object[]{b1, b2, b3, b4, b5};
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields,
                new Object[]{"E1", 10, new Object[]{b1, b4}, new Object[]{b1, b4}, new Object[]{b1, b2, b4},
                    new Object[]{b1, b3, b4, b5}, all});
        EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields,
                new Object[]{"E1", 20, new Object[]{b2}, new Object[]{b2}, new Object[]{b1, b2, b4},
                    new Object[]{b2}, all});
        EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields,
                new Object[]{"E2", 10, new Object[]{b3, b5}, new Object[]{b3, b5}, new Object[]{b3, b5},
                    new Object[]{b1, b3, b4, b5}, all});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionGroupedMultiLevelNoDefaultLvl(EPServiceProvider epService) {
        sendTime(epService, 0);
        String[] fields = "theString,intPrimitive,c0,c1,c2".split(",");
        String epl = "select" +
                "   theString, intPrimitive," +
                "   sum(longPrimitive, group_by:(theString)) as c0," +
                "   sum(longPrimitive, group_by:(intPrimitive)) as c1," +
                "   sum(longPrimitive, group_by:()) as c2" +
                " from SupportBean" +
                " group by theString, intPrimitive" +
                " output snapshot every 10 seconds";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        makeSendEvent(epService, "E1", 10, 100);
        makeSendEvent(epService, "E1", 20, 202);
        makeSendEvent(epService, "E2", 10, 303);
        makeSendEvent(epService, "E1", 10, 404);
        makeSendEvent(epService, "E2", 10, 505);
        sendTime(epService, 10000);

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, 706L, 1312L, 1514L}, {"E1", 20, 706L, 202L, 1514L}, {"E2", 10, 808L, 1312L, 1514L}});

        makeSendEvent(epService, "E1", 10, 1);
        sendTime(epService, 20000);

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, 707L, 1313L, 1515L}, {"E1", 20, 707L, 202L, 1515L}, {"E2", 10, 808L, 1313L, 1515L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionGroupedSolutionPattern(EPServiceProvider epService) {
        sendTime(epService, 0);
        String[] fields = "theString,pct".split(",");
        String epl = "select theString, count(*) / count(*, group_by:()) as pct" +
                " from SupportBean#time(30 sec)" +
                " group by theString" +
                " output snapshot every 10 seconds";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        sendEventMany(epService, "A", "B", "C", "B", "B", "C");
        sendTime(epService, 10000);

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{
                {"A", 1 / 6d}, {"B", 3 / 6d}, {"C", 2 / 6d}});

        sendEventMany(epService, "A", "B", "B", "B", "B", "A");
        sendTime(epService, 20000);

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{
                {"A", 3 / 12d}, {"B", 7 / 12d}, {"C", 2 / 12d}});

        sendEventMany(epService, "C", "A", "A", "A", "B", "A");
        sendTime(epService, 30000);

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{
                {"A", 6 / 12d}, {"B", 5 / 12d}, {"C", 1 / 12d}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionAggAndFullyAgg(EPServiceProvider epService, String selected, MyAssertion assertion) throws Exception {
        String epl = "create context StartS0EndS1 start SupportBean_S0 end SupportBean_S1;" +
                "@name('out') context StartS0EndS1 " +
                selected +
                " output snapshot when terminated;";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        makeSendEvent(epService, "E1", 10, 100);
        makeSendEvent(epService, "E2", 20, 200);
        makeSendEvent(epService, "E2", 30, 300);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));

        assertion.doAssert(listener);

        // try an empty batch
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
    }

    private void tryAssertionUngroupedParenSODA(EPServiceProvider epService, boolean soda) {
        String[] cols = "c0,c1,c2,c3,c4".split(",");
        String epl = "select longPrimitive, " +
                "sum(longPrimitive) as c0, " +
                "sum(group_by:(),longPrimitive) as c1, " +
                "sum(longPrimitive,group_by:()) as c2, " +
                "sum(longPrimitive,group_by:theString) as c3, " +
                "sum(longPrimitive,group_by:(theString,intPrimitive)) as c4" +
                " from SupportBean";
        SupportUpdateListener listener = new SupportUpdateListener();
        SupportModelHelper.createByCompileOrParse(epService, soda, epl).addListener(listener);

        makeSendEvent(epService, "E1", 1, 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{10L, 10L, 10L, 10L, 10L});

        makeSendEvent(epService, "E1", 2, 11);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{21L, 21L, 21L, 21L, 11L});

        makeSendEvent(epService, "E2", 1, 12);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{33L, 33L, 33L, 12L, 12L});

        makeSendEvent(epService, "E2", 2, 13);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{46L, 46L, 46L, 25L, 13L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionUngroupedAggAdditionalAndPlugin(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("concatstring", MyConcatAggregationFunctionFactory.class.getName());
        ConfigurationPlugInAggregationMultiFunction mfAggConfig = new ConfigurationPlugInAggregationMultiFunction(SupportAggMFFunc.getFunctionNames(), SupportAggMFFactory.class.getName());
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationMultiFunction(mfAggConfig);

        String[] cols = "c0,c1,c2,c3,c4,c5,c8,c9,c10,c11,c12,c13".split(",");
        String epl = "select intPrimitive, " +
                " countever(*, intPrimitive>0, group_by:(theString)) as c0," +
                " countever(*, intPrimitive>0, group_by:()) as c1," +
                " countever(*, group_by:(theString)) as c2," +
                " countever(*, group_by:()) as c3," +
                " concatstring(Integer.toString(intPrimitive), group_by:(theString)) as c4," +
                " concatstring(Integer.toString(intPrimitive), group_by:()) as c5," +
                " sc(intPrimitive, group_by:(theString)) as c6," +
                " sc(intPrimitive, group_by:()) as c7," +
                " leaving(group_by:(theString)) as c8," +
                " leaving(group_by:()) as c9," +
                " rate(3, group_by:(theString)) as c10," +
                " rate(3, group_by:()) as c11," +
                " nth(intPrimitive, 1, group_by:(theString)) as c12," +
                " nth(intPrimitive, 1, group_by:()) as c13" +
                " from SupportBean as sb";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        makeSendEvent(epService, "E1", 10);
        assertScalarColl(listener.getLastNewData()[0], new Integer[]{10}, new Integer[]{10});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{1L, 1L, 1L, 1L, "10", "10", false, false,
            null, null, null, null});

        makeSendEvent(epService, "E2", 20);
        assertScalarColl(listener.getLastNewData()[0], new Integer[]{20}, new Integer[]{10, 20});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{1L, 2L, 1L, 2L, "20", "10 20", false, false,
            null, null, null, 10});

        makeSendEvent(epService, "E1", -1);
        assertScalarColl(listener.getLastNewData()[0], new Integer[]{10, -1}, new Integer[]{10, 20, -1});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{1L, 2L, 2L, 3L, "10 -1", "10 20 -1", false, false,
            null, null, 10, 20});

        makeSendEvent(epService, "E2", 30);
        assertScalarColl(listener.getLastNewData()[0], new Integer[]{20, 30}, new Integer[]{10, 20, -1, 30});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{2L, 3L, 2L, 4L, "20 30", "10 20 -1 30", false, false,
            null, null, 20, -1});

        // plug-in aggregation function can also take other parameters
        epService.getEPAdministrator().createEPL("select sc(intPrimitive, dummy:1)," +
                "concatstring(Integer.toString(intPrimitive), dummy2:(1,2,3)) from SupportBean");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionUngroupedAggEvent(EPServiceProvider epService) {
        String[] cols = "first0,first1,last0,last1,window0,window1,maxby0,maxby1,minby0,minby1,sorted0,sorted1,maxbyever0,maxbyever1,minbyever0,minbyever1,firstever0,firstever1,lastever0,lastever1".split(",");
        String epl = "select intPrimitive as c0, " +
                " first(sb, group_by:(theString)) as first0," +
                " first(sb, group_by:()) as first1," +
                " last(sb, group_by:(theString)) as last0," +
                " last(sb, group_by:()) as last1," +
                " window(sb, group_by:(theString)) as window0," +
                " window(sb, group_by:()) as window1," +
                " maxby(intPrimitive, group_by:(theString)) as maxby0," +
                " maxby(intPrimitive, group_by:()) as maxby1," +
                " minby(intPrimitive, group_by:(theString)) as minby0," +
                " minby(intPrimitive, group_by:()) as minby1," +
                " sorted(intPrimitive, group_by:(theString)) as sorted0," +
                " sorted(intPrimitive, group_by:()) as sorted1," +
                " maxbyever(intPrimitive, group_by:(theString)) as maxbyever0," +
                " maxbyever(intPrimitive, group_by:()) as maxbyever1," +
                " minbyever(intPrimitive, group_by:(theString)) as minbyever0," +
                " minbyever(intPrimitive, group_by:()) as minbyever1," +
                " firstever(sb, group_by:(theString)) as firstever0," +
                " firstever(sb, group_by:()) as firstever1," +
                " lastever(sb, group_by:(theString)) as lastever0," +
                " lastever(sb, group_by:()) as lastever1" +
                " from SupportBean#length(3) as sb";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        SupportBean b1 = makeSendEvent(epService, "E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{b1, b1, b1, b1, new Object[]{b1}, new Object[]{b1},
            b1, b1, b1, b1, new Object[]{b1}, new Object[]{b1}, b1, b1, b1, b1,
            b1, b1, b1, b1});

        SupportBean b2 = makeSendEvent(epService, "E2", 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{b2, b1, b2, b2, new Object[]{b2}, new Object[]{b1, b2},
            b2, b2, b2, b1, new Object[]{b2}, new Object[]{b1, b2}, b2, b2, b2, b1,
            b2, b1, b2, b2});

        SupportBean b3 = makeSendEvent(epService, "E1", 15);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{b1, b1, b3, b3, new Object[]{b1, b3}, new Object[]{b1, b2, b3},
            b3, b2, b1, b1, new Object[]{b1, b3}, new Object[]{b1, b3, b2}, b3, b2, b1, b1,
            b1, b1, b3, b3});

        SupportBean b4 = makeSendEvent(epService, "E3", 16);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{b4, b2, b4, b4, new Object[]{b4}, new Object[]{b2, b3, b4},
            b4, b2, b4, b3, new Object[]{b4}, new Object[]{b3, b4, b2}, b4, b2, b4, b1,
            b4, b1, b4, b4});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionUngroupedAggSQLStandard(EPServiceProvider epService) {
        String[] fields = "c0,sum0,sum1,avedev0,avg0,max0,fmax0,min0,fmin0,maxever0,fmaxever0,minever0,fminever0,median0,stddev0".split(",");
        String epl = "select intPrimitive as c0, " +
                "sum(intPrimitive, group_by:()) as sum0, " +
                "sum(intPrimitive, group_by:(theString)) as sum1," +
                "avedev(intPrimitive, group_by:(theString)) as avedev0," +
                "avg(intPrimitive, group_by:(theString)) as avg0," +
                "max(intPrimitive, group_by:(theString)) as max0," +
                "fmax(intPrimitive, intPrimitive>0, group_by:(theString)) as fmax0," +
                "min(intPrimitive, group_by:(theString)) as min0," +
                "fmin(intPrimitive, intPrimitive>0, group_by:(theString)) as fmin0," +
                "maxever(intPrimitive, group_by:(theString)) as maxever0," +
                "fmaxever(intPrimitive, intPrimitive>0, group_by:(theString)) as fmaxever0," +
                "minever(intPrimitive, group_by:(theString)) as minever0," +
                "fminever(intPrimitive, intPrimitive>0, group_by:(theString)) as fminever0," +
                "median(intPrimitive, group_by:(theString)) as median0," +
                "Math.round(coalesce(stddev(intPrimitive, group_by:(theString)), 0)) as stddev0" +
                " from SupportBean#keepall";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 10, 10,
            0.0d, 10d, 10, 10, 10, 10, 10, 10, 10, 10, 10.0, 0L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 10 + 20, 20,
            0.0d, 20d, 20, 20, 20, 20, 20, 20, 20, 20, 20.0, 0L});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 30));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{30, 10 + 20 + 30, 10 + 30,
            10.0d, 20d, 30, 30, 10, 10, 30, 30, 10, 10, 20.0, 14L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 40));
        Object[] expected = new Object[]{40, 10 + 20 + 30 + 40, 20 + 40,
            10.0d, 30d, 40, 40, 20, 20, 40, 40, 20, 20, 30.0, 14L};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionUngroupedSameKey(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventOne (d1 String, d2 String, val int)");
        String epl = "select sum(val, group_by: d1) as c0, sum(val, group_by: d2) as c1 from MyEventOne";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);
        String[] cols = "c0,c1".split(",");

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "E1", 10}, "MyEventOne");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{10, 10});

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "E2", 11}, "MyEventOne");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{21, 11});

        epService.getEPRuntime().sendEvent(new Object[]{"E2", "E1", 12}, "MyEventOne");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{12, 22});

        epService.getEPRuntime().sendEvent(new Object[]{"E3", "E1", 13}, "MyEventOne");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{13, 35});

        epService.getEPRuntime().sendEvent(new Object[]{"E3", "E3", 14}, "MyEventOne");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{27, 14});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionGroupedSameKey(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventTwo (g1 String, d1 String, d2 String, val int)");
        String epl = "select sum(val) as c0, sum(val, group_by: d1) as c1, sum(val, group_by: d2) as c2 from MyEventTwo group by g1";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);
        String[] cols = "c0,c1,c2".split(",");

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "E1", "E1", 10}, "MyEventTwo");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{10, 10, 10});

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "E1", "E2", 11}, "MyEventTwo");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{21, 21, 11});

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "E2", "E1", 12}, "MyEventTwo");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{33, 12, 22});

        epService.getEPRuntime().sendEvent(new Object[]{"X", "E1", "E1", 13}, "MyEventTwo");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{13, 10 + 11 + 13, 10 + 12 + 13});

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "E2", "E3", 14}, "MyEventTwo");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), cols, new Object[]{10 + 11 + 12 + 14, 12 + 14, 14});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionUngroupedAggIterator(EPServiceProvider epService) {
        String[] fields = "c0,sum0,sum1".split(",");
        String epl = "select intPrimitive as c0, " +
                "sum(intPrimitive, group_by:()) as sum0, " +
                "sum(intPrimitive, group_by:(theString)) as sum1 " +
                " from SupportBean#keepall";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{10, 10, 10}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{10, 30, 10}, {20, 30, 20}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 30));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{10, 60, 40}, {20, 60, 20}, {30, 60, 40}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionUngroupedHaving(EPServiceProvider epService) {
        String epl = "select * from SupportBean having sum(intPrimitive, group_by:theString) > 100";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        makeSendEvent(epService, "E1", 95);
        makeSendEvent(epService, "E2", 10);
        assertFalse(listener.isInvoked());

        makeSendEvent(epService, "E1", 10);
        assertTrue(listener.isInvoked());
        listener.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionUngroupedOrderBy(EPServiceProvider epService) throws Exception {
        String epl = "create context StartS0EndS1 start SupportBean_S0 end SupportBean_S1;" +
                "@name('out') context StartS0EndS1 select theString, sum(intPrimitive, group_by:theString) as c0 " +
                " from SupportBean#keepall " +
                " output snapshot when terminated" +
                " order by sum(intPrimitive, group_by:theString)" +
                ";";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        makeSendEvent(epService, "E1", 10);
        makeSendEvent(epService, "E2", 20);
        makeSendEvent(epService, "E1", 30);
        makeSendEvent(epService, "E3", 40);
        makeSendEvent(epService, "E2", 50);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "theString,c0".split(","), new Object[][]{
                {"E1", 40}, {"E1", 40}, {"E3", 40}, {"E2", 70}, {"E2", 70}});

        // try an empty batch
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
    }

    private void tryAssertionGroupedOnSelect(EPServiceProvider epService) throws Exception {
        String epl = "create window MyWindow#keepall as SupportBean;" +
                "insert into MyWindow select * from SupportBean;" +
                "@name('out') on SupportBean_S0 select theString, sum(intPrimitive) as c0, sum(intPrimitive, group_by:()) as c1" +
                " from MyWindow group by theString;";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        makeSendEvent(epService, "E1", 10);
        makeSendEvent(epService, "E2", 20);
        makeSendEvent(epService, "E1", 30);
        makeSendEvent(epService, "E3", 40);
        makeSendEvent(epService, "E2", 50);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "theString,c0,c1".split(","), new Object[][]{
                {"E1", 40, 150}, {"E2", 70, 150}, {"E3", 40, 150}});

        makeSendEvent(epService, "E1", 60);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "theString,c0,c1".split(","), new Object[][]{
                {"E1", 100, 210}, {"E2", 70, 210}, {"E3", 40, 210}});

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
    }

    private void tryAssertionUngroupedUnidirectionalJoin(EPServiceProvider epService) {
        String epl = "select theString, sum(intPrimitive, group_by:theString) as c0 from SupportBean#keepall, SupportBean_S0 unidirectional";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        makeSendEvent(epService, "E1", 10);
        makeSendEvent(epService, "E2", 20);
        makeSendEvent(epService, "E1", 30);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "theString,c0".split(","),
                new Object[][]{{"E1", 40}, {"E1", 40}, {"E2", 20}});

        makeSendEvent(epService, "E1", 40);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "theString,c0".split(","),
                new Object[][]{{"E1", 80}, {"E1", 80}, {"E1", 80}, {"E2", 20}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionEnumMethods(EPServiceProvider epService, boolean grouped) {
        String epl =
                "select" +
                        " window(*, group_by:()).firstOf() as c0," +
                        " window(*, group_by:theString).firstOf() as c1," +
                        " window(intPrimitive, group_by:()).firstOf() as c2," +
                        " window(intPrimitive, group_by:theString).firstOf() as c3," +
                        " first(*, group_by:()).intPrimitive as c4," +
                        " first(*, group_by:theString).intPrimitive as c5 " +
                        " from SupportBean#keepall " +
                        (grouped ? "group by theString, intPrimitive" : "");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        SupportBean b1 = makeSendEvent(epService, "E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5".split(","),
                new Object[]{b1, b1, 10, 10, 10, 10});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendTime(EPServiceProvider epService, long msec) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(msec));
    }

    private void sendEventMany(EPServiceProvider epService, String... theString) {
        for (String value : theString) {
            sendEvent(epService, value);
        }
    }

    private void sendEvent(EPServiceProvider epService, String theString) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, 0));
    }

    private SupportBean makeSendEvent(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        epService.getEPRuntime().sendEvent(b);
        return b;
    }

    private SupportBean makeSendEvent(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(b);
        return b;
    }

    private interface MyAssertion {
        public void doAssert(SupportUpdateListener listener);
    }

    private void assertCountColsAndLevels(EPServiceProvider epService, String epl, int colCount, int lvlCount) {
        String theEpl = PLAN_CALLBACK_HOOK + epl;
        epService.getEPAdministrator().createEPL(theEpl);
        Pair<AggregationGroupByLocalGroupDesc, AggregationLocalGroupByPlanForge> plan = SupportAggLevelPlanHook.getAndReset();
        assertEquals(colCount, plan.getFirst().getNumColumns());
        assertEquals(lvlCount, plan.getFirst().getLevels().length);
    }

    private void assertNoPlan(EPServiceProvider epService, String epl) {
        String theEpl = PLAN_CALLBACK_HOOK + epl;
        epService.getEPAdministrator().createEPL(theEpl);
        assertNull(SupportAggLevelPlanHook.getAndReset());
    }

    private void tryAssertionColNameRendering(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "count(*, group_by:(theString, intPrimitive)), " +
                "count(group_by:theString, *) " +
                "from SupportBean");
        assertEquals("count(*,group_by:(theString,intPrimitive))", stmt.getEventType().getPropertyNames()[0]);
        assertEquals("count(group_by:theString,*)", stmt.getEventType().getPropertyNames()[1]);
    }

    private void assertScalarColl(EventBean eventBean, Integer[] expectedC6, Integer[] expectedC7) {
        Collection c6 = (Collection) eventBean.get("c6");
        Collection c7 = (Collection) eventBean.get("c7");
        EPAssertionUtil.assertEqualsExactOrder(expectedC6, c6.toArray());
        EPAssertionUtil.assertEqualsExactOrder(expectedC7, c7.toArray());
    }
}
