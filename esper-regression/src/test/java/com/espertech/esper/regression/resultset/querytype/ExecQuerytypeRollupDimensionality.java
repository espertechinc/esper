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
package com.espertech.esper.regression.resultset.querytype;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.epl.SupportOutputLimitOpt;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecQuerytypeRollupDimensionality implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
        configuration.getEngineDefaults().getByteCodeGeneration().setIncludeDebugSymbols(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        runAssertionGroupByWithComputation(epService);
        runAssertionOutputWhenTerminated(epService);
        runAssertionContextPartitionAlsoRollup(epService);
        runAssertionOnSelect(epService);
        runAssertionUnboundRollupUnenclosed(epService);
        runAssertionUnboundCubeUnenclosed(epService);
        runAssertionUnboundGroupingSet2LevelUnenclosed(epService);
        runAssertionBoundCube3Dim(epService);
        runAssertionBoundGroupingSet2LevelNoTopNoDetail(epService);
        runAssertionBoundGroupingSet2LevelTopAndDetail(epService);
        runAssertionUnboundCube4Dim(epService);
        runAssertionNamedWindowCube2Dim(epService);
        runAssertionNamedWindowDeleteAndRStream2Dim(epService);
        runAssertionBoundRollup2Dim(epService);
        runAssertionUnboundRollup2Dim(epService);
        runAssertionUnboundRollup1Dim(epService);
        runAssertionUnboundRollup2DimBatchWindow(epService);
        runAssertionUnboundRollup3Dim(epService);
        runAssertionMixedAccessAggregation(epService);
        runAssertionNonBoxedTypeWithRollup(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionOutputWhenTerminated(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionOutputWhenTerminated(epService, "last", outputLimitOpt);
        }

        tryAssertionOutputWhenTerminated(epService, "all", SupportOutputLimitOpt.DEFAULT);
        tryAssertionOutputWhenTerminated(epService, "snapshot", SupportOutputLimitOpt.DEFAULT);
    }

    private void tryAssertionOutputWhenTerminated(EPServiceProvider epService, String outputLimit, SupportOutputLimitOpt opt) {
        epService.getEPAdministrator().createEPL("@name('s0') create context MyContext start SupportBean_S0(id=1) end SupportBean_S0(id=0)");
        epService.getEPAdministrator().createEPL(opt.getHint() + "@name('s1') context MyContext select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean group by rollup(theString) output " + outputLimit + " when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("s1").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "c0,c1".split(","),
                new Object[][]{{"E1", 4}, {"E2", 2}, {null, 6}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 6));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "c0,c1".split(","),
                new Object[][]{{"E2", 4}, {"E1", 11}, {null, 15}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionGroupByWithComputation(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select longPrimitive as c0, sum(intPrimitive) as c1 " +
                "from SupportBean group by rollup(case when longPrimitive > 0 then 1 else 0 end)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Long.class, stmt.getEventType().getPropertyType("c0"));
        String[] fields = "c0,c1".split(",");

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{10L, 1}, {null, 1}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 2, 11));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{11L, 3}, {null, 3}});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 5, -10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{-10L, 5}, {null, 8}});

        epService.getEPRuntime().sendEvent(makeEvent("E4", 6, -11));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{-11L, 11}, {null, 14}});

        epService.getEPRuntime().sendEvent(makeEvent("E5", 3, 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{12L, 6}, {null, 17}});

        stmt.destroy();
    }

    private void runAssertionContextPartitionAlsoRollup(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context SegmentedByString partition by theString from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("context SegmentedByString select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean group by rollup(theString, intPrimitive)").addListener(listener);
        String[] fields = "c0,c1,c2".split(",");

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 1, 10L}, {"E1", null, 10L}, {null, null, 10L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 2, 20));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 2, 20L}, {"E1", null, 30L}, {null, null, 30L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 25));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 1, 25L}, {"E2", null, 25L}, {null, null, 25L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOnSelect(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("on SupportBean_S0 as s0 select mw.theString as c0, sum(mw.intPrimitive) as c1, count(*) as c2 from MyWindow mw group by rollup(mw.theString)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "c0,c1,c2".split(",");

        // {E0, 0}, {E1, 1}, {E2, 2}, {E0, 3}, {E1, 4}, {E2, 5}, {E0, 6}, {E1, 7}, {E2, 8}, {E0, 9}
        for (int i = 0; i < 10; i++) {
            String theString = "E" + i % 3;
            epService.getEPRuntime().sendEvent(new SupportBean(theString, i));
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E0", 18, 4L}, {"E1", 12, 3L}, {"E2", 15, 3L}, {null, 18 + 12 + 15, 10L}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 6));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E0", 18, 4L}, {"E1", 12 + 6, 4L}, {"E2", 15, 3L}, {null, 18 + 12 + 15 + 6, 11L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUnboundRollupUnenclosed(EPServiceProvider epService) {
        tryAssertionUnboundRollupUnenclosed(epService, "theString, rollup(intPrimitive, longPrimitive)");
        tryAssertionUnboundRollupUnenclosed(epService, "grouping sets(" +
                "(theString, intPrimitive, longPrimitive)," +
                "(theString, intPrimitive)," +
                "theString)");
        tryAssertionUnboundRollupUnenclosed(epService, "theString, grouping sets(" +
                "(intPrimitive, longPrimitive)," +
                "(intPrimitive), ())");
    }

    private void runAssertionUnboundCubeUnenclosed(EPServiceProvider epService) {
        tryAssertionUnboundCubeUnenclosed(epService, "theString, cube(intPrimitive, longPrimitive)");
        tryAssertionUnboundCubeUnenclosed(epService, "grouping sets(" +
                "(theString, intPrimitive, longPrimitive)," +
                "(theString, intPrimitive)," +
                "(theString, longPrimitive)," +
                "theString)");
        tryAssertionUnboundCubeUnenclosed(epService, "theString, grouping sets(" +
                "(intPrimitive, longPrimitive)," +
                "(intPrimitive)," +
                "(longPrimitive)," +
                "())");
    }

    private void tryAssertionUnboundCubeUnenclosed(EPServiceProvider epService, String groupBy) {

        String[] fields = "c0,c1,c2,c3".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, intPrimitive as c1, longPrimitive as c2, sum(doublePrimitive) as c3 from SupportBean " +
                "group by " + groupBy).addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100, 1000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 100L, 1000d}, {"E1", 10, null, 1000d}, {"E1", null, 100L, 1000d}, {"E1", null, null, 1000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 200, 2000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 200L, 2000d}, {"E1", 10, null, 3000d}, {"E1", null, 200L, 2000d}, {"E1", null, null, 3000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 20, 100, 4000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 20, 100L, 4000d}, {"E1", 20, null, 4000d}, {"E1", null, 100L, 5000d}, {"E1", null, null, 7000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 10, 100, 5000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 10, 100L, 5000d}, {"E2", 10, null, 5000d}, {"E2", null, 100L, 5000d}, {"E2", null, null, 5000d}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionUnboundRollupUnenclosed(EPServiceProvider epService, String groupBy) {

        String[] fields = "c0,c1,c2,c3".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, intPrimitive as c1, longPrimitive as c2, sum(doublePrimitive) as c3 from SupportBean " +
                "group by " + groupBy);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c1"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("c2"));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100, 1000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 100L, 1000d}, {"E1", 10, null, 1000d}, {"E1", null, null, 1000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 200, 2000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 200L, 2000d}, {"E1", 10, null, 3000d}, {"E1", null, null, 3000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 20, 100, 3000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 20, 100L, 3000d}, {"E1", 20, null, 3000d}, {"E1", null, null, 6000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100, 4000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 100L, 5000d}, {"E1", 10, null, 7000d}, {"E1", null, null, 10000d}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUnboundGroupingSet2LevelUnenclosed(EPServiceProvider epService) {
        tryAssertionUnboundGroupingSet2LevelUnenclosed(epService, "theString, grouping sets(intPrimitive, longPrimitive)");
        tryAssertionUnboundGroupingSet2LevelUnenclosed(epService, "grouping sets((theString, intPrimitive), (theString, longPrimitive))");
    }

    private void tryAssertionUnboundGroupingSet2LevelUnenclosed(EPServiceProvider epService, String groupBy) {

        String[] fields = "c0,c1,c2,c3".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, intPrimitive as c1, longPrimitive as c2, sum(doublePrimitive) as c3 from SupportBean " +
                "group by " + groupBy);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c1"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("c2"));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100, 1000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, null, 1000d}, {"E1", null, 100L, 1000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 20, 200, 2000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 20, null, 2000d}, {"E1", null, 200L, 2000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 200, 3000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, null, 4000d}, {"E1", null, 200L, 5000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 20, 100, 4000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 20, null, 6000d}, {"E1", null, 100L, 5000d}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBoundGroupingSet2LevelNoTopNoDetail(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean#length(4) " +
                "group by grouping sets(theString, intPrimitive)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c1"));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", null, 100L}, {null, 10, 100L}},
                new Object[][]{{"E1", null, null}, {null, 10, null}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 200));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", null, 200L}, {null, 20, 200L}},
                new Object[][]{{"E2", null, null}, {null, 20, null}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 20, 300));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", null, 400L}, {null, 20, 500L}},
                new Object[][]{{"E1", null, 100L}, {null, 20, 200L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 10, 400));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", null, 600L}, {null, 10, 500L}},
                new Object[][]{{"E2", null, 200L}, {null, 10, 100L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 500));   // removes E1/10/100
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", null, 1100L}, {"E1", null, 300L}, {null, 20, 1000L}, {null, 10, 400L}},
                new Object[][]{{"E2", null, 600L}, {"E1", null, 400L}, {null, 20, 500L}, {null, 10, 500L}});

        stmt.destroy();
    }

    private void runAssertionBoundGroupingSet2LevelTopAndDetail(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean#length(4) " +
                "group by grouping sets((), (theString, intPrimitive))");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c1"));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 100L}, {"E1", 10, 100L}},
                new Object[][]{{null, null, null}, {"E1", 10, null}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 200));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 300L}, {"E1", 10, 300L}},
                new Object[][]{{null, null, 100L}, {"E1", 10, 100L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 300));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 600L}, {"E2", 20, 300L}},
                new Object[][]{{null, null, 300L}, {"E2", 20, null}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 400));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 1000L}, {"E1", 10, 700L}},
                new Object[][]{{null, null, 600L}, {"E1", 10, 300L}});

        stmt.destroy();
    }

    private void runAssertionUnboundCube4Dim(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3,c4".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, intPrimitive as c1, longPrimitive as c2, doublePrimitive as c3, sum(intBoxed) as c4 from SupportBean " +
                "group by cube(theString, intPrimitive, longPrimitive, doublePrimitive)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c1"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("c2"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("c3"));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 10, 100, 1000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E1", 1, 10L, 100d, 1000},  // {0, 1, 2, 3}
                        {"E1", 1, 10L, null, 1000},  // {0, 1, 2}
                        {"E1", 1, null, 100d, 1000},  // {0, 1, 3}
                        {"E1", 1, null, null, 1000},  // {0, 1}
                        {"E1", null, 10L, 100d, 1000},  // {0, 2, 3}
                        {"E1", null, 10L, null, 1000},  // {0, 2}
                        {"E1", null, null, 100d, 1000},  // {0, 3}
                        {"E1", null, null, null, 1000},  // {0}
                        {null, 1, 10L, 100d, 1000},  // {1, 2, 3}
                        {null, 1, 10L, null, 1000},  // {1, 2}
                        {null, 1, null, 100d, 1000},  // {1, 3}
                        {null, 1, null, null, 1000},  // {1}
                        {null, null, 10L, 100d, 1000},  // {2, 3}
                        {null, null, 10L, null, 1000},  // {2}
                        {null, null, null, 100d, 1000},  // {3}
                        {null, null, null, null, 1000}   // {}
                });

        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 20, 100, 2000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 1, 20L, 100d, 2000},  // {0, 1, 2, 3}
                        {"E2", 1, 20L, null, 2000},  // {0, 1, 2}
                        {"E2", 1, null, 100d, 2000},  // {0, 1, 3}
                        {"E2", 1, null, null, 2000},  // {0, 1}
                        {"E2", null, 20L, 100d, 2000},  // {0, 2, 3}
                        {"E2", null, 20L, null, 2000},  // {0, 2}
                        {"E2", null, null, 100d, 2000},  // {0, 3}
                        {"E2", null, null, null, 2000},  // {0}
                        {null, 1, 20L, 100d, 2000},  // {1, 2, 3}
                        {null, 1, 20L, null, 2000},  // {1, 2}
                        {null, 1, null, 100d, 3000},  // {1, 3}
                        {null, 1, null, null, 3000},  // {1}
                        {null, null, 20L, 100d, 2000},  // {2, 3}
                        {null, null, 20L, null, 2000},  // {2}
                        {null, null, null, 100d, 3000},  // {3}
                        {null, null, null, null, 3000}   // {}
                });

        epService.getEPRuntime().sendEvent(makeEvent("E1", 2, 10, 100, 4000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E1", 2, 10L, 100d, 4000},  // {0, 1, 2, 3}
                        {"E1", 2, 10L, null, 4000},  // {0, 1, 2}
                        {"E1", 2, null, 100d, 4000},  // {0, 1, 3}
                        {"E1", 2, null, null, 4000},  // {0, 1}
                        {"E1", null, 10L, 100d, 5000},  // {0, 2, 3}
                        {"E1", null, 10L, null, 5000},  // {0, 2}
                        {"E1", null, null, 100d, 5000},  // {0, 3}
                        {"E1", null, null, null, 5000},  // {0}
                        {null, 2, 10L, 100d, 4000},  // {1, 2, 3}
                        {null, 2, 10L, null, 4000},  // {1, 2}
                        {null, 2, null, 100d, 4000},  // {1, 3}
                        {null, 2, null, null, 4000},  // {1}
                        {null, null, 10L, 100d, 5000},  // {2, 3}
                        {null, null, 10L, null, 5000},  // {2}
                        {null, null, null, 100d, 7000},  // {3}
                        {null, null, null, null, 7000}   // {}
                });

        stmt.destroy();
    }

    private void runAssertionBoundCube3Dim(EPServiceProvider epService) {
        tryAssertionBoundCube(epService, "cube(theString, intPrimitive, longPrimitive)");
        tryAssertionBoundCube(epService, "grouping sets(" +
                "(theString, intPrimitive, longPrimitive)," +
                "(theString, intPrimitive)," +
                "(theString, longPrimitive)," +
                "(theString)," +
                "(intPrimitive, longPrimitive)," +
                "(intPrimitive)," +
                "(longPrimitive)," +
                "()" +
                ")");
    }

    private void tryAssertionBoundCube(EPServiceProvider epService, String groupBy) {

        String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, " +
                "intPrimitive as c1, " +
                "longPrimitive as c2, " +
                "count(*) as c3, " +
                "sum(doublePrimitive) as c4," +
                "grouping(theString) as c5," +
                "grouping(intPrimitive) as c6," +
                "grouping(longPrimitive) as c7," +
                "grouping_id(theString, intPrimitive, longPrimitive) as c8 " +
                "from SupportBean#length(4) " +
                "group by " + groupBy).addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 10, 100));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E1", 1, 10L, 1L, 100d, 0, 0, 0, 0},  // {0, 1, 2}
                        {"E1", 1, null, 1L, 100d, 0, 0, 1, 1},  // {0, 1}
                        {"E1", null, 10L, 1L, 100d, 0, 1, 0, 2},  // {0, 2}
                        {"E1", null, null, 1L, 100d, 0, 1, 1, 3},  // {0}
                        {null, 1, 10L, 1L, 100d, 1, 0, 0, 4},  // {1, 2}
                        {null, 1, null, 1L, 100d, 1, 0, 1, 5},  // {1}
                        {null, null, 10L, 1L, 100d, 1, 1, 0, 6},  // {2}
                        {null, null, null, 1L, 100d, 1, 1, 1, 7}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 20, 200));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 1, 20L, 1L, 200d, 0, 0, 0, 0},
                        {"E2", 1, null, 1L, 200d, 0, 0, 1, 1},
                        {"E2", null, 20L, 1L, 200d, 0, 1, 0, 2},
                        {"E2", null, null, 1L, 200d, 0, 1, 1, 3},
                        {null, 1, 20L, 1L, 200d, 1, 0, 0, 4},
                        {null, 1, null, 2L, 300d, 1, 0, 1, 5},
                        {null, null, 20L, 1L, 200d, 1, 1, 0, 6},
                        {null, null, null, 2L, 300d, 1, 1, 1, 7}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 2, 10, 300));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E1", 2, 10L, 1L, 300d, 0, 0, 0, 0},
                        {"E1", 2, null, 1L, 300d, 0, 0, 1, 1},
                        {"E1", null, 10L, 2L, 400d, 0, 1, 0, 2},
                        {"E1", null, null, 2L, 400d, 0, 1, 1, 3},
                        {null, 2, 10L, 1L, 300d, 1, 0, 0, 4},
                        {null, 2, null, 1L, 300d, 1, 0, 1, 5},
                        {null, null, 10L, 2L, 400d, 1, 1, 0, 6},
                        {null, null, null, 3L, 600d, 1, 1, 1, 7}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 2, 20, 400));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 2, 20L, 1L, 400d, 0, 0, 0, 0},
                        {"E2", 2, null, 1L, 400d, 0, 0, 1, 1},
                        {"E2", null, 20L, 2L, 600d, 0, 1, 0, 2},
                        {"E2", null, null, 2L, 600d, 0, 1, 1, 3},
                        {null, 2, 20L, 1L, 400d, 1, 0, 0, 4},
                        {null, 2, null, 2L, 700d, 1, 0, 1, 5},
                        {null, null, 20L, 2L, 600d, 1, 1, 0, 6},
                        {null, null, null, 4L, 1000d, 1, 1, 1, 7}});

        // expiring/removing ("E1", 1, 10, 100)
        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 10, 500));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 1, 10L, 1L, 500d, 0, 0, 0, 0},
                        {"E1", 1, 10L, 0L, null, 0, 0, 0, 0},
                        {"E2", 1, null, 2L, 700d, 0, 0, 1, 1},
                        {"E1", 1, null, 0L, null, 0, 0, 1, 1},
                        {"E2", null, 10L, 1L, 500d, 0, 1, 0, 2},
                        {"E1", null, 10L, 1L, 300d, 0, 1, 0, 2},
                        {"E2", null, null, 3L, 1100d, 0, 1, 1, 3},
                        {"E1", null, null, 1L, 300d, 0, 1, 1, 3},
                        {null, 1, 10L, 1L, 500d, 1, 0, 0, 4},
                        {null, 1, null, 2L, 700d, 1, 0, 1, 5},
                        {null, null, 10L, 2L, 800d, 1, 1, 0, 6},
                        {null, null, null, 4L, 1400d, 1, 1, 1, 7}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowCube2Dim(EPServiceProvider epService) {
        tryAssertionNamedWindowCube2Dim(epService, "cube(theString, intPrimitive)");
        tryAssertionNamedWindowCube2Dim(epService, "grouping sets(" +
                "(theString, intPrimitive)," +
                "(theString)," +
                "(intPrimitive)," +
                "()" +
                ")");
    }

    private void tryAssertionNamedWindowCube2Dim(EPServiceProvider epService, String groupBy) {

        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean(intBoxed = 0)");
        epService.getEPAdministrator().createEPL("on SupportBean(intBoxed = 3) delete from MyWindow");

        String[] fields = "c0,c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from MyWindow " +
                "group by " + groupBy).addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 100));    // insert event
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, 10, 100L}, {null, null, 100L}},
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, 10, null}, {null, null, null}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 11, 200));    // insert event
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 11, 200L}, {"E1", null, 300L}, {null, 11, 200L}, {null, null, 300L}},
                new Object[][]{{"E1", 11, null}, {"E1", null, 100L}, {null, 11, null}, {null, null, 100L}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 300));    // insert event
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 600L}, {null, 10, 400L}, {null, null, 600L}},
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 300L}, {null, 10, 100L}, {null, null, 300L}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E2", 11, 400));    // insert event
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", 11, 400L}, {"E2", null, 400L}, {null, 11, 600L}, {null, null, 1000L}},
                new Object[][]{{"E2", 11, null}, {"E2", null, null}, {null, 11, 200L}, {null, null, 600L}});

        epService.getEPRuntime().sendEvent(makeEvent(3, null, -1, -1));    // delete-all
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, null}, {"E1", 11, null}, {"E2", 11, null},
                    {"E1", null, null}, {"E2", null, null}, {null, 10, null}, {null, 11, null}, {null, null, null}},
                new Object[][]{{"E1", 10, 400L}, {"E1", 11, 200L}, {"E2", 11, 400L},
                    {"E1", null, 600L}, {"E2", null, 400L}, {null, 10, 400L}, {null, 11, 600L}, {null, null, 1000L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowDeleteAndRStream2Dim(EPServiceProvider epService) {
        tryAssertionNamedWindowDeleteAndRStream2Dim(epService, "rollup(theString, intPrimitive)");
        tryAssertionNamedWindowDeleteAndRStream2Dim(epService, "grouping sets(" +
                "(theString, intPrimitive)," +
                "(theString)," +
                "())");
    }

    private void tryAssertionNamedWindowDeleteAndRStream2Dim(EPServiceProvider epService, String groupBy) {
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean(intBoxed = 0)");
        epService.getEPAdministrator().createEPL("on SupportBean(intBoxed = 1) as sb " +
                "delete from MyWindow mw where sb.theString = mw.theString and sb.intPrimitive = mw.intPrimitive");
        epService.getEPAdministrator().createEPL("on SupportBean(intBoxed = 2) as sb " +
                "delete from MyWindow mw where sb.theString = mw.theString and sb.intPrimitive = mw.intPrimitive and sb.longPrimitive = mw.longPrimitive");
        epService.getEPAdministrator().createEPL("on SupportBean(intBoxed = 3) delete from MyWindow");

        String[] fields = "c0,c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from MyWindow " +
                "group by " + groupBy).addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 100));    // insert event intBoxed=0
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}},
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}});

        epService.getEPRuntime().sendEvent(makeEvent(1, "E1", 10, 100));   // delete (intBoxed = 1)
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}},
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 200));   // insert
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 200L}, {"E1", null, 200L}, {null, null, 200L}},
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E2", 20, 300));   // insert
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", 20, 300L}, {"E2", null, 300L}, {null, null, 500L}},
                new Object[][]{{"E2", 20, null}, {"E2", null, null}, {null, null, 200L}});

        epService.getEPRuntime().sendEvent(makeEvent(3, null, 0, 0));   // delete all
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{
                        {"E1", 10, null}, {"E2", 20, null},
                        {"E1", null, null}, {"E2", null, null},
                        {null, null, null}},
                new Object[][]{
                        {"E1", 10, 200L}, {"E2", 20, 300L},
                        {"E1", null, 200L}, {"E2", null, 300L},
                        {null, null, 500L}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 400));   // insert
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 400L}, {null, null, 400L}},
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 20, 500));   // insert
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 20, 500L}, {"E1", null, 900L}, {null, null, 900L}},
                new Object[][]{{"E1", 20, null}, {"E1", null, 400L}, {null, null, 400L}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E2", 20, 600));   // insert
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", 20, 600L}, {"E2", null, 600L}, {null, null, 1500L}},
                new Object[][]{{"E2", 20, null}, {"E2", null, null}, {null, null, 900L}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 700));   // insert
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 1100L}, {"E1", null, 1600L}, {null, null, 2200L}},
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 900L}, {null, null, 1500L}});

        epService.getEPRuntime().sendEvent(makeEvent(3, null, 0, 0));   // delete all
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{
                        {"E1", 10, null}, {"E1", 20, null}, {"E2", 20, null},
                        {"E1", null, null}, {"E2", null, null},
                        {null, null, null}},
                new Object[][]{
                        {"E1", 10, 1100L}, {"E1", 20, 500L}, {"E2", 20, 600L},
                        {"E1", null, 1600L}, {"E2", null, 600L},
                        {null, null, 2200L}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 100));   // insert
        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 20, 200));   // insert
        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 300));   // insert
        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 20, 400));   // insert
        listener.reset();

        epService.getEPRuntime().sendEvent(makeEvent(1, "E1", 20, -1));   // delete (intBoxed = 1)
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 20, null}, {"E1", null, 400L}, {null, null, 400L}},
                new Object[][]{{"E1", 20, 600L}, {"E1", null, 1000L}, {null, null, 1000L}});

        epService.getEPRuntime().sendEvent(makeEvent(1, "E1", 10, -1));   // delete (intBoxed = 1)
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}},
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 400L}, {null, null, 400L}});

        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 100));   // insert
        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 200));   // insert
        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 10, 300));   // insert
        epService.getEPRuntime().sendEvent(makeEvent(0, "E1", 20, 400));   // insert
        epService.getEPRuntime().sendEvent(makeEvent(0, "E2", 20, 500));   // insert
        listener.reset();

        epService.getEPRuntime().sendEvent(makeEvent(2, "E1", 10, 200));   // delete specific (intBoxed = 2)
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 800L}, {null, null, 1300L}},
                new Object[][]{{"E1", 10, 600L}, {"E1", null, 1000L}, {null, null, 1500L}});

        epService.getEPRuntime().sendEvent(makeEvent(2, "E1", 10, 300));   // delete specific
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 500L}, {null, null, 1000L}},
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 800L}, {null, null, 1300L}});

        epService.getEPRuntime().sendEvent(makeEvent(2, "E1", 20, 400));   // delete specific
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 20, null}, {"E1", null, 100L}, {null, null, 600L}},
                new Object[][]{{"E1", 20, 400L}, {"E1", null, 500L}, {null, null, 1000L}});

        epService.getEPRuntime().sendEvent(makeEvent(2, "E2", 20, 500));   // delete specific
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", 20, null}, {"E2", null, null}, {null, null, 100L}},
                new Object[][]{{"E2", 20, 500L}, {"E2", null, 500L}, {null, null, 600L}});

        epService.getEPRuntime().sendEvent(makeEvent(2, "E1", 10, 100));   // delete specific
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}},
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBoundRollup2Dim(EPServiceProvider epService) {
        tryAssertionBoundRollup2Dim(epService, false);
        tryAssertionBoundRollup2Dim(epService, true);
    }

    private void tryAssertionBoundRollup2Dim(EPServiceProvider epService, boolean join) {

        String[] fields = "c0,c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#length(3) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive)").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 200));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 20, 200L}, {"E2", null, 200L}, {null, null, 300L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 300));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 11, 300L}, {"E1", null, 400L}, {null, null, 600L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 400));   // expires {theString="E1", intPrimitive=10, longPrimitive=100}
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 20, 600L}, {"E1", 10, null},
                        {"E2", null, 600L}, {"E1", null, 300L},
                        {null, null, 900L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 500));   // expires {theString="E2", intPrimitive=20, longPrimitive=200}
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 20, 900L},
                        {"E2", null, 900L},
                        {null, null, 1200L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 21, 600));   // expires {theString="E1", intPrimitive=11, longPrimitive=300}
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 21, 600L}, {"E1", 11, null},
                        {"E2", null, 1500L}, {"E1", null, null},
                        {null, null, 1500L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 21, 700));   // expires {theString="E2", intPrimitive=20, longPrimitive=400}
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 21, 1300L}, {"E2", 20, 500L},
                        {"E2", null, 1800L},
                        {null, null, 1800L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 21, 800));   // expires {theString="E2", intPrimitive=20, longPrimitive=500}
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 21, 2100L}, {"E2", 20, null},
                        {"E2", null, 2100L},
                        {null, null, 2100L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 900));   // expires {theString="E2", intPrimitive=21, longPrimitive=600}
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E1", 10, 900L}, {"E2", 21, 1500L},
                        {"E1", null, 900L}, {"E2", null, 1500L},
                        {null, null, 2400L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 1000));   // expires {theString="E2", intPrimitive=21, longPrimitive=700}
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E1", 11, 1000L}, {"E2", 21, 800L},
                        {"E1", null, 1900L}, {"E2", null, 800L},
                        {null, null, 2700L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 1100));   // expires {theString="E2", intPrimitive=21, longPrimitive=800}
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E2", 20, 1100L}, {"E2", 21, null},
                        {"E2", null, 1100L},
                        {null, null, 3000L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUnboundRollup2Dim(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean " +
                "group by rollup(theString, intPrimitive)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c1"));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 200));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 20, 200L}, {"E2", null, 200L}, {null, null, 300L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 300));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 11, 300L}, {"E1", null, 400L}, {null, null, 600L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 400));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 20, 600L}, {"E2", null, 600L}, {null, null, 1000L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 500));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 11, 800L}, {"E1", null, 900L}, {null, null, 1500L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 600));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 700L}, {"E1", null, 1500L}, {null, null, 2100L}});

        stmt.destroy();
    }

    private void runAssertionUnboundRollup1Dim(EPServiceProvider epService) {
        tryAssertionUnboundRollup1Dim(epService, "rollup(theString)");
        tryAssertionUnboundRollup1Dim(epService, "cube(theString)");
    }

    private void runAssertionUnboundRollup2DimBatchWindow(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean#length_batch(4) " +
                "group by rollup(theString, intPrimitive)").addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 200));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 300));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 400));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E2", 20, 600L}, {"E1", 11, 300L},
                    {"E1", null, 400L}, {"E2", null, 600L},
                    {null, null, 1000L}},
                new Object[][]{{"E1", 10, null}, {"E2", 20, null}, {"E1", 11, null},
                    {"E1", null, null}, {"E2", null, null},
                    {null, null, null}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 500));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 600));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 700));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 800));
        EPAssertionUtil.assertPropsPerRow(listener.getDataListsFlattened(), fields,
                new Object[][]{{"E1", 11, 1200L}, {"E2", 20, 1400L}, {"E1", 10, null},
                    {"E1", null, 1200L}, {"E2", null, 1400L},
                    {null, null, 2600L}},
                new Object[][]{{"E1", 11, 300L}, {"E2", 20, 600L}, {"E1", 10, 100L},
                    {"E1", null, 400L}, {"E2", null, 600L},
                    {null, null, 1000L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionUnboundRollup1Dim(EPServiceProvider epService, String rollup) {

        String[] fields = "c0,c1".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, sum(intPrimitive) as c1 from SupportBean " +
                "group by " + rollup).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10}, {null, 10}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 20}, {null, 30}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 30));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 40}, {null, 60}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 40));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 60}, {null, 100}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUnboundRollup3Dim(EPServiceProvider epService) {
        String rollupEpl = "rollup(theString, intPrimitive, longPrimitive)";
        tryAssertionUnboundRollup3Dim(epService, rollupEpl, false);
        tryAssertionUnboundRollup3Dim(epService, rollupEpl, true);

        String gsEpl = "grouping sets(" +
                "(theString, intPrimitive, longPrimitive)," +
                "(theString, intPrimitive)," +
                "(theString)," +
                "()" +
                ")";
        tryAssertionUnboundRollup3Dim(epService, gsEpl, false);
        tryAssertionUnboundRollup3Dim(epService, gsEpl, true);
    }

    private void tryAssertionUnboundRollup3Dim(EPServiceProvider epService, String groupByClause, boolean isJoin) {

        String[] fields = "c0,c1,c2,c3,c4".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, intPrimitive as c1, longPrimitive as c2, count(*) as c3, sum(doublePrimitive) as c4 " +
                "from SupportBean#keepall " + (isJoin ? ", SupportBean_S0#lastevent " : "") +
                "group by " + groupByClause).addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 10, 100));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 1, 10L, 1L, 100d}, {"E1", 1, null, 1L, 100d}, {"E1", null, null, 1L, 100d}, {null, null, null, 1L, 100d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 11, 200));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 1, 11L, 1L, 200d}, {"E1", 1, null, 2L, 300d}, {"E1", null, null, 2L, 300d}, {null, null, null, 2L, 300d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 2, 10, 300));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 2, 10L, 1L, 300d}, {"E1", 2, null, 1L, 300d}, {"E1", null, null, 3L, 600d}, {null, null, null, 3L, 600d}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 10, 400));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 1, 10L, 1L, 400d}, {"E2", 1, null, 1L, 400d}, {"E2", null, null, 1L, 400d}, {null, null, null, 4L, 1000d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 10, 500));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 1, 10L, 2L, 600d}, {"E1", 1, null, 3L, 800d}, {"E1", null, null, 4L, 1100d}, {null, null, null, 5L, 1500d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 11, 600));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 1, 11L, 2L, 800d}, {"E1", 1, null, 4L, 1400d}, {"E1", null, null, 5L, 1700d}, {null, null, null, 6L, 2100d}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMixedAccessAggregation(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        String epl = "select sum(intPrimitive) as c0, theString as c1, window(*) as c2 " +
                "from SupportBean#length(2) sb group by rollup(theString) order by theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        Object eventOne = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(eventOne);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{1, null, new Object[]{eventOne}}, {1, "E1", new Object[]{eventOne}}});

        Object eventTwo = new SupportBean("E1", 2);
        epService.getEPRuntime().sendEvent(eventTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{3, null, new Object[]{eventOne, eventTwo}}, {3, "E1", new Object[]{eventOne, eventTwo}}});

        Object eventThree = new SupportBean("E2", 3);
        epService.getEPRuntime().sendEvent(eventThree);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{5, null, new Object[]{eventTwo, eventThree}}, {2, "E1", new Object[]{eventTwo}}, {3, "E2", new Object[]{eventThree}}});

        Object eventFour = new SupportBean("E1", 4);
        epService.getEPRuntime().sendEvent(eventFour);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{7, null, new Object[]{eventThree, eventFour}}, {4, "E1", new Object[]{eventFour}}});

        stmt.destroy();
    }

    private void runAssertionNonBoxedTypeWithRollup(EPServiceProvider epService) {
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select intPrimitive as c0, doublePrimitive as c1, longPrimitive as c2, sum(shortPrimitive) " +
                "from SupportBean group by intPrimitive, rollup(doublePrimitive, longPrimitive)");
        assertTypesC0C1C2(stmtOne, Integer.class, Double.class, Long.class);

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select intPrimitive as c0, doublePrimitive as c1, longPrimitive as c2, sum(shortPrimitive) " +
                "from SupportBean group by grouping sets ((intPrimitive, doublePrimitive, longPrimitive))");
        assertTypesC0C1C2(stmtTwo, Integer.class, Double.class, Long.class);

        EPStatement stmtThree = epService.getEPAdministrator().createEPL("select intPrimitive as c0, doublePrimitive as c1, longPrimitive as c2, sum(shortPrimitive) " +
                "from SupportBean group by grouping sets ((intPrimitive, doublePrimitive, longPrimitive), (intPrimitive, doublePrimitive))");
        assertTypesC0C1C2(stmtThree, Integer.class, Double.class, Long.class);

        EPStatement stmtFour = epService.getEPAdministrator().createEPL("select intPrimitive as c0, doublePrimitive as c1, longPrimitive as c2, sum(shortPrimitive) " +
                "from SupportBean group by grouping sets ((doublePrimitive, intPrimitive), (longPrimitive, intPrimitive))");
        assertTypesC0C1C2(stmtFour, Integer.class, Double.class, Long.class);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String prefix = "select theString, sum(intPrimitive) from SupportBean group by ";

        // invalid rollup expressions
        tryInvalid(epService, prefix + "rollup()",
                "Incorrect syntax near ')' at line 1 column 69, please check the group-by clause [select theString, sum(intPrimitive) from SupportBean group by rollup()]");
        tryInvalid(epService, prefix + "rollup(theString, theString)",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString) [select theString, sum(intPrimitive) from SupportBean group by rollup(theString, theString)]");
        tryInvalid(epService, prefix + "rollup(x)",
                "Error starting statement: Failed to validate group-by-clause expression 'x': Property named 'x' is not valid in any stream [select theString, sum(intPrimitive) from SupportBean group by rollup(x)]");
        tryInvalid(epService, prefix + "rollup(longPrimitive)",
                "Error starting statement: Group-by with rollup requires a fully-aggregated query, the query is not full-aggregated because of property 'theString' [select theString, sum(intPrimitive) from SupportBean group by rollup(longPrimitive)]");
        tryInvalid(epService, prefix + "rollup((theString, longPrimitive), (theString, longPrimitive))",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString, longPrimitive) [select theString, sum(intPrimitive) from SupportBean group by rollup((theString, longPrimitive), (theString, longPrimitive))]");
        tryInvalid(epService, prefix + "rollup((theString, longPrimitive), (longPrimitive, theString))",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString, longPrimitive) [select theString, sum(intPrimitive) from SupportBean group by rollup((theString, longPrimitive), (longPrimitive, theString))]");
        tryInvalid(epService, prefix + "grouping sets((theString, theString))",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString) [select theString, sum(intPrimitive) from SupportBean group by grouping sets((theString, theString))]");
        tryInvalid(epService, prefix + "grouping sets(theString, theString)",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString) [select theString, sum(intPrimitive) from SupportBean group by grouping sets(theString, theString)]");
        tryInvalid(epService, prefix + "grouping sets((), ())",
                "Failed to validate the group-by clause, found duplicate specification of the overall grouping '()' [select theString, sum(intPrimitive) from SupportBean group by grouping sets((), ())]");
        tryInvalid(epService, prefix + "grouping sets(())",
                "Failed to validate the group-by clause, the overall grouping '()' cannot be the only grouping [select theString, sum(intPrimitive) from SupportBean group by grouping sets(())]");

        // invalid select clause for this type of query
        tryInvalid(epService, "select * from SupportBean group by grouping sets(theString)",
                "Group-by with rollup requires that the select-clause does not use wildcard [select * from SupportBean group by grouping sets(theString)]");
        tryInvalid(epService, "select sb.* from SupportBean sb group by grouping sets(theString)",
                "Group-by with rollup requires that the select-clause does not use wildcard [select sb.* from SupportBean sb group by grouping sets(theString)]");

        tryInvalid(epService, "@Hint('disable_reclaim_group') select theString, count(*) from SupportBean sb group by grouping sets(theString)",
                "Error starting statement: Reclaim hints are not available with rollup [@Hint('disable_reclaim_group') select theString, count(*) from SupportBean sb group by grouping sets(theString)]");
    }

    private SupportBean makeEvent(int intBoxed, String theString, int intPrimitive, long longPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        sb.setIntBoxed(intBoxed);
        return sb;
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        return sb;
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive, int intBoxed) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        sb.setIntBoxed(intBoxed);
        return sb;
    }

    private void assertTypesC0C1C2(EPStatement stmtOne, Class expectedC0, Class expectedC1, Class expectedC2) {
        assertEquals(expectedC0, stmtOne.getEventType().getPropertyType("c0"));
        assertEquals(expectedC1, stmtOne.getEventType().getPropertyType("c1"));
        assertEquals(expectedC2, stmtOne.getEventType().getPropertyType("c2"));
    }
}
