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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;

public class ExecQuerytypeRollupHavingAndOrderBy implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
        configuration.getEngineDefaults().getByteCodeGeneration().setIncludeDebugSymbols(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        runAssertionHaving(epService, false);
        runAssertionHaving(epService, true);

        runAssertionIteratorWindow(epService, false);
        runAssertionIteratorWindow(epService, true);

        runAssertionOrderByTwoCriteriaAsc(epService, false);
        runAssertionOrderByTwoCriteriaAsc(epService, true);

        runAssertionUnidirectional(epService);
        runAssertionOrderByOneCriteriaDesc(epService);
    }

    private void runAssertionIteratorWindow(EPServiceProvider epService, boolean join) {

        String[] fields = "c0,c1".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#length(3) " + (join ? ", SupportBean_S0#keepall " : "") +
                "group by rollup(theString)");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {null, 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {null, 3}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 4}, {"E2", 2}, {null, 6}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 6}, {"E1", 3}, {null, 9}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUnidirectional(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean_S0 unidirectional, SupportBean#keepall " +
                "group by cube(theString, intPrimitive)").addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 200));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 300));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 400));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E1", 10, 100L},
                        {"E2", 20, 600L},
                        {"E1", 11, 300L},
                        {"E1", null, 400L},
                        {"E2", null, 600L},
                        {null, 10, 100L},
                        {null, 20, 600L},
                        {null, 11, 300L},
                        {null, null, 1000L}
                });

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{
                        {"E1", 10, 101L},
                        {"E2", 20, 600L},
                        {"E1", 11, 300L},
                        {"E1", null, 401L},
                        {"E2", null, 600L},
                        {null, 10, 101L},
                        {null, 20, 600L},
                        {null, 11, 300L},
                        {null, null, 1001L}
                });

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionHaving(EPServiceProvider epService, boolean join) {

        // test having on the aggregation alone
        String[] fields = "c0,c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#keepall " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive)" +
                "having sum(longPrimitive) > 1000").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 200));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 300));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 400));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 500));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{null, null, 1500L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 20, 600));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 20, 1200L}, {"E2", null, 1200L}, {null, null, 2100L}});
        epService.getEPAdministrator().destroyAllStatements();

        // test having on the aggregation alone
        String[] fieldsC0C1 = "c0,c1".split(",");
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#keepall " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString) " +
                "having " +
                "(theString is null and sum(intPrimitive) > 100) " +
                "or " +
                "(theString is not null and sum(intPrimitive) > 200)").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 50));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fieldsC0C1,
                new Object[][]{{null, 120}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", -300));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 200));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fieldsC0C1,
                new Object[][]{{"E1", 250}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 500));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fieldsC0C1,
                new Object[][]{{"E2", 570}, {null, 520}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOrderByTwoCriteriaAsc(EPServiceProvider epService, boolean join) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String[] fields = "c0,c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#time_batch(1 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive) " +
                "order by theString, intPrimitive").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        epService.getEPRuntime().sendEvent(makeEvent("E2", 10, 100));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 200));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 300));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 400));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 1000L},
                    {"E1", null, 900L},
                    {"E1", 10, 300L},
                    {"E1", 11, 600L},
                    {"E2", null, 100L},
                    {"E2", 10, 100L},
                },
                new Object[][]{{null, null, null},
                    {"E1", null, null},
                    {"E1", 10, null},
                    {"E1", 11, null},
                    {"E2", null, null},
                    {"E2", 10, null},
                });

        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 500));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 600));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 12, 700));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 1800L},
                    {"E1", null, 1800L},
                    {"E1", 10, 600L},
                    {"E1", 11, 500L},
                    {"E1", 12, 700L},
                    {"E2", null, null},
                    {"E2", 10, null},
                },
                new Object[][]{{null, null, 1000L},
                    {"E1", null, 900L},
                    {"E1", 10, 300L},
                    {"E1", 11, 600L},
                    {"E1", 12, null},
                    {"E2", null, 100L},
                    {"E2", 10, 100L},
                });

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOrderByOneCriteriaDesc(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String[] fields = "c0,c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('s1')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean#time_batch(1 sec) " +
                "group by rollup(theString, intPrimitive) " +
                "order by theString desc").addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("E2", 10, 100));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 200));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 300));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 400));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{
                        {"E2", 10, 100L},
                        {"E2", null, 100L},
                        {"E1", 11, 600L},
                        {"E1", 10, 300L},
                        {"E1", null, 900L},
                        {null, null, 1000L},
                },
                new Object[][]{
                        {"E2", 10, null},
                        {"E2", null, null},
                        {"E1", 11, null},
                        {"E1", 10, null},
                        {"E1", null, null},
                        {null, null, null},
                });

        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 500));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 600));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 12, 700));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{
                        {"E2", 10, null},
                        {"E2", null, null},
                        {"E1", 11, 500L},
                        {"E1", 10, 600L},
                        {"E1", 12, 700L},
                        {"E1", null, 1800L},
                        {null, null, 1800L},
                },
                new Object[][]{
                        {"E2", 10, 100L},
                        {"E2", null, 100L},
                        {"E1", 11, 600L},
                        {"E1", 10, 300L},
                        {"E1", 12, null},
                        {"E1", null, 900L},
                        {null, null, 1000L},
                });

        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        return sb;
    }
}
