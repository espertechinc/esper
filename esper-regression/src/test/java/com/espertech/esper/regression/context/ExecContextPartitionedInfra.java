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
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPContextPartitionAdminImpl;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecContextPartitionedInfra implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAggregatedSubquery(epService);
        runAssertionOnDeleteAndUpdate(epService);
        runAssertionCreateIndex(epService);
        runAssertionSegmentedOnSelect(epService);
        runAssertionSegmentedNWConsumeAll(epService);
        runAssertionSegmentedNWConsumeSameContext(epService);
        runAssertionSegmentedOnMergeUpdateSubq(epService);
    }

    private void runAssertionAggregatedSubquery(EPServiceProvider epService) {
        tryAssertionAggregatedSubquery(epService, true);
        tryAssertionAggregatedSubquery(epService, false);
    }

    private void runAssertionOnDeleteAndUpdate(EPServiceProvider epService) {
        tryAssertionOnDeleteAndUpdate(epService, true);
        tryAssertionOnDeleteAndUpdate(epService, false);
    }

    private void runAssertionCreateIndex(EPServiceProvider epService) throws Exception {
        tryAssertionCreateIndex(epService, true);
        tryAssertionCreateIndex(epService, false);
    }

    private void runAssertionSegmentedOnSelect(EPServiceProvider epService) {
        tryAssertionSegmentedOnSelect(epService, true);
        tryAssertionSegmentedOnSelect(epService, false);
    }

    public void tryAssertionSegmentedOnSelect(EPServiceProvider epService, boolean namedWindow) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString " +
                "partition by theString from SupportBean, p00 from SupportBean_S0");

        String eplCreate = namedWindow ?
                "@Name('named window') context SegmentedByString create window MyInfra#keepall as SupportBean" :
                "@Name('table') context SegmentedByString create table MyInfra(theString string primary key, intPrimitive int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("@Name('insert') context SegmentedByString insert into MyInfra select theString, intPrimitive from SupportBean");

        String[] fieldsNW = new String[]{"theString", "intPrimitive"};
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("context SegmentedByString " +
                "on SupportBean_S0 select mywin.* from MyInfra as mywin");
        SupportUpdateListener listenerSelect = new SupportUpdateListener();
        stmtSelect.addListener(listenerSelect);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 3));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listenerSelect.getAndResetLastNewData(), fieldsNW, new Object[][]{{"G1", 1}, {"G1", 3}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G2"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listenerSelect.getAndResetLastNewData(), fieldsNW, new Object[][]{{"G2", 2}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void tryAssertionCreateIndex(EPServiceProvider epService, boolean namedWindow) throws Exception {
        String epl = "@name('create-ctx') create context SegmentedByCustomer " +
                "  initiated by SupportBean_S0 s0 " +
                "  terminated by SupportBean_S1(p00 = p10);" +
                "" +
                "@name('create-infra') context SegmentedByCustomer\n" +
                (namedWindow ?
                        "create window MyInfra#keepall as SupportBean;" :
                        "create table MyInfra(theString string primary key, intPrimitive int);") +
                "" +
                (namedWindow ?
                        "@name('insert-into-window') insert into MyInfra select theString, intPrimitive from SupportBean;" :
                        "@name('insert-into-table') context SegmentedByCustomer insert into MyInfra select theString, intPrimitive from SupportBean;") +
                "" +
                "@name('create-index') context SegmentedByCustomer\n" +
                "create index MyIndex on MyInfra(intPrimitive);";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "B"));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfra where intPrimitive = 1", new ContextPartitionSelector[]{new EPContextPartitionAdminImpl.CPSelectorById(1)});
        EPAssertionUtil.assertPropsPerRow(result.getArray(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(3, "A"));

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
    }

    private void tryAssertionOnDeleteAndUpdate(EPServiceProvider epService, boolean namedWindow) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString " +
                "partition by theString from SupportBean, p00 from SupportBean_S0, p10 from SupportBean_S1");

        String[] fieldsNW = new String[]{"theString", "intPrimitive"};
        String eplCreate = namedWindow ?
                "@Name('named window') context SegmentedByString create window MyInfra#keepall as SupportBean" :
                "@Name('named window') context SegmentedByString create table MyInfra(theString string primary key, intPrimitive int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        String eplInsert = namedWindow ?
                "@Name('insert') insert into MyInfra select theString, intPrimitive from SupportBean" :
                "@Name('insert') context SegmentedByString insert into MyInfra select theString, intPrimitive from SupportBean";
        epService.getEPAdministrator().createEPL(eplInsert);

        SupportUpdateListener listenerSelect = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('selectit') context SegmentedByString select irstream * from MyInfra").addListener(listenerSelect);

        // Delete testing
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL("@Name('on-delete') context SegmentedByString on SupportBean_S0 delete from MyInfra");

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G1", 1});
        } else {
            assertFalse(listenerSelect.isInvoked());
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G0"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G2"));
        assertFalse(listenerSelect.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G1"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetOldAndReset(), fieldsNW, new Object[]{"G1", 1});
        }

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 20});
        }

        epService.getEPRuntime().sendEvent(new SupportBean("G3", 3));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G3", 3});
        }

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 21));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 21});
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G2"));
        if (namedWindow) {
            EPAssertionUtil.assertPropsPerRow(listenerSelect.getLastOldData(), fieldsNW, new Object[][]{{"G2", 20}, {"G2", 21}});
        }
        listenerSelect.reset();

        stmtDelete.destroy();

        // update testing
        EPStatement stmtUpdate = epService.getEPAdministrator().createEPL("@Name('on-merge') context SegmentedByString on SupportBean_S0 update MyInfra set intPrimitive = intPrimitive + 1");

        epService.getEPRuntime().sendEvent(new SupportBean("G4", 4));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G4", 4});
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G0"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G2"));
        assertFalse(listenerSelect.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G4"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerSelect.getLastNewData()[0], fieldsNW, new Object[]{"G4", 5});
            EPAssertionUtil.assertProps(listenerSelect.getLastOldData()[0], fieldsNW, new Object[]{"G4", 4});
            listenerSelect.reset();
        }

        epService.getEPRuntime().sendEvent(new SupportBean("G5", 5));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G5", 5});
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G5"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerSelect.getLastNewData()[0], fieldsNW, new Object[]{"G5", 6});
            EPAssertionUtil.assertProps(listenerSelect.getLastOldData()[0], fieldsNW, new Object[]{"G5", 5});
            listenerSelect.reset();
        }

        stmtUpdate.destroy();
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void tryAssertionAggregatedSubquery(EPServiceProvider epService, boolean namedWindow) {
        epService.getEPAdministrator().createEPL("create context SegmentedByString partition by theString from SupportBean, p00 from SupportBean_S0");
        String eplCreate = namedWindow ?
                "context SegmentedByString create window MyInfra#keepall as SupportBean" :
                "context SegmentedByString create table MyInfra (theString string primary key, intPrimitive int)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("@Name('insert') context SegmentedByString insert into MyInfra select theString, intPrimitive from SupportBean");

        EPStatement stmt = epService.getEPAdministrator().createEPL("@Audit context SegmentedByString " +
                "select *, (select max(intPrimitive) from MyInfra) as mymax from SupportBean_S0");
        SupportUpdateListener listenerSelect = new SupportUpdateListener();
        stmt.addListener(listenerSelect);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E2"));
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), "mymax".split(","), new Object[]{20});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), "mymax".split(","), new Object[]{10});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E3"));
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), "mymax".split(","), new Object[]{null});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionSegmentedNWConsumeAll(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL("@Name('named window') context SegmentedByString create window MyWindow#lastevent as SupportBean");
        SupportUpdateListener listenerNamedWindow = new SupportUpdateListener();
        stmtNamedWindow.addListener(listenerNamedWindow);
        epService.getEPAdministrator().createEPL("@Name('insert') insert into MyWindow select * from SupportBean");

        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("@Name('select') select * from MyWindow");
        SupportUpdateListener listenerSelect = new SupportUpdateListener();
        stmtSelect.addListener(listenerSelect);

        String[] fields = new String[]{"theString", "intPrimitive"};
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listenerNamedWindow.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        EPAssertionUtil.assertProps(listenerNamedWindow.assertOneGetNewAndReset(), fields, new Object[]{"G2", 20});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"G2", 20});

        stmtSelect.destroy();

        // Out-of-context consumer not initialized
        EPStatement stmtSelectCount = epService.getEPAdministrator().createEPL("@Name('select') select count(*) as cnt from MyWindow");
        stmtSelectCount.addListener(listenerSelect);
        EPAssertionUtil.assertProps(stmtSelectCount.iterator().next(), "cnt".split(","), new Object[]{0L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSegmentedNWConsumeSameContext(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL("@Name('named window') context SegmentedByString create window MyWindow#keepall as SupportBean");
        SupportUpdateListener listenerNamedWindow = new SupportUpdateListener();
        stmtNamedWindow.addListener(listenerNamedWindow);
        epService.getEPAdministrator().createEPL("@Name('insert') insert into MyWindow select * from SupportBean");

        String[] fieldsNW = new String[]{"theString", "intPrimitive"};
        String[] fieldsCnt = new String[]{"theString", "cnt"};
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("@Name('select') context SegmentedByString select theString, count(*) as cnt from MyWindow group by theString");
        SupportUpdateListener listenerSelect = new SupportUpdateListener();
        stmtSelect.addListener(listenerSelect);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listenerNamedWindow.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G1", 10});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsCnt, new Object[]{"G1", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        EPAssertionUtil.assertProps(listenerNamedWindow.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 20});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsCnt, new Object[]{"G2", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listenerNamedWindow.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G1", 11});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsCnt, new Object[]{"G1", 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 21));
        EPAssertionUtil.assertProps(listenerNamedWindow.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 21});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fieldsCnt, new Object[]{"G2", 2L});

        stmtSelect.destroy();

        // In-context consumer not initialized
        EPStatement stmtSelectCount = epService.getEPAdministrator().createEPL("@Name('select') context SegmentedByString select count(*) as cnt from MyWindow");
        stmtSelectCount.addListener(listenerSelect);
        try {
            // EPAssertionUtil.assertProps(stmtSelectCount.iterator().next(), "cnt".split(","), new Object[] {0L});
            stmtSelectCount.iterator();
        } catch (UnsupportedOperationException ex) {
            assertEquals("Iterator not supported on statements that have a context attached", ex.getMessage());
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSegmentedOnMergeUpdateSubq(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString " +
                "partition by theString from SupportBean, p00 from SupportBean_S0, p10 from SupportBean_S1");

        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL("@Name('named window') context SegmentedByString create window MyWindow#keepall as SupportBean");
        SupportUpdateListener listenerNamedWindow = new SupportUpdateListener();
        stmtNamedWindow.addListener(listenerNamedWindow);
        epService.getEPAdministrator().createEPL("@Name('insert') insert into MyWindow select * from SupportBean");

        String[] fieldsNW = new String[]{"theString", "intPrimitive"};
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("@Name('on-merge') context SegmentedByString " +
                "on SupportBean_S0 " +
                "merge MyWindow " +
                "when matched then " +
                "  update set intPrimitive = (select id from SupportBean_S1#lastevent)");
        SupportUpdateListener listenerSelect = new SupportUpdateListener();
        stmtSelect.addListener(listenerSelect);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        EPAssertionUtil.assertProps(listenerNamedWindow.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(99, "G1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G1"));
        EPAssertionUtil.assertProps(listenerNamedWindow.getLastNewData()[0], fieldsNW, new Object[]{"G1", 99});
        EPAssertionUtil.assertProps(listenerNamedWindow.getLastOldData()[0], fieldsNW, new Object[]{"G1", 1});
        listenerNamedWindow.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        EPAssertionUtil.assertProps(listenerNamedWindow.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 2});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(98, "Gx"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G2"));
        EPAssertionUtil.assertProps(listenerNamedWindow.getLastNewData()[0], fieldsNW, new Object[]{"G2", 2});
        EPAssertionUtil.assertProps(listenerNamedWindow.getLastOldData()[0], fieldsNW, new Object[]{"G2", 2});
        listenerNamedWindow.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("G3", 3));
        EPAssertionUtil.assertProps(listenerNamedWindow.assertOneGetNewAndReset(), fieldsNW, new Object[]{"G3", 3});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "Gx"));
        assertFalse(listenerNamedWindow.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }
}
