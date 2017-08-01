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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecTableRollup implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionRollupOneDim(epService);
        runAssertionRollupTwoDim(epService);
        runAssertionGroupingSetThreeDim(epService);
    }

    private void runAssertionRollupOneDim(EPServiceProvider epService) {
        SupportUpdateListener listenerQuery = new SupportUpdateListener();
        SupportUpdateListener listenerOut = new SupportUpdateListener();
        String[] fieldsOut = "theString,total".split(",");

        epService.getEPAdministrator().createEPL("create table MyTableR1D(pk string primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTableR1D insert into MyStreamOne select theString, sum(intPrimitive) as total from SupportBean#length(4) group by rollup(theString)").addListener(listenerOut);
        epService.getEPAdministrator().createEPL("select MyTableR1D[p00].total as c0 from SupportBean_S0").addListener(listenerQuery);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertValuesListener(epService, listenerQuery, new Object[][]{{null, 10}, {"E1", 10}, {"E2", null}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E1", 10}, {null, 10}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 200));
        assertValuesListener(epService, listenerQuery, new Object[][]{{null, 210}, {"E1", 10}, {"E2", 200}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E2", 200}, {null, 210}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        assertValuesListener(epService, listenerQuery, new Object[][]{{null, 221}, {"E1", 21}, {"E2", 200}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E1", 21}, {null, 221}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 201));
        assertValuesListener(epService, listenerQuery, new Object[][]{{null, 422}, {"E1", 21}, {"E2", 401}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E2", 401}, {null, 422}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12)); // {"E1", 10} leaving window
        assertValuesListener(epService, listenerQuery, new Object[][]{{null, 424}, {"E1", 23}, {"E2", 401}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E1", 23}, {null, 424}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRollupTwoDim(EPServiceProvider epService) {
        String[] fields = "k0,k1,total".split(",");
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventTwo(k0 int, k1 int, col int)");
        epService.getEPAdministrator().createEPL("create table MyTableR2D(k0 int primary key, k1 int primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTableR2D insert into MyStreamTwo select sum(col) as total from MyEventTwo#length(3) group by rollup(k0,k1)");

        epService.getEPRuntime().sendEvent(new Object[]{1, 10, 100}, "MyEventTwo");
        epService.getEPRuntime().sendEvent(new Object[]{2, 10, 200}, "MyEventTwo");
        epService.getEPRuntime().sendEvent(new Object[]{1, 20, 300}, "MyEventTwo");

        assertValuesIterate(epService, "MyTableR2D", fields, new Object[][]{{null, null, 600}, {1, null, 400}, {2, null, 200},
            {1, 10, 100}, {2, 10, 200}, {1, 20, 300}});

        epService.getEPRuntime().sendEvent(new Object[]{1, 10, 400}, "MyEventTwo"); // expires {1, 10, 100}

        assertValuesIterate(epService, "MyTableR2D", fields, new Object[][]{{null, null, 900}, {1, null, 700}, {2, null, 200},
            {1, 10, 400}, {2, 10, 200}, {1, 20, 300}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionGroupingSetThreeDim(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventThree(k0 int, k1 int, k2 int, col int)");
        epService.getEPAdministrator().createEPL("create table MyTableGS3D(k0 int primary key, k1 int primary key, k2 int primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTableGS3D insert into MyStreamThree select sum(col) as total from MyEventThree#length(3) group by grouping sets(k0,k1,k2)");

        String[] fields = "k0,k1,k2,total".split(",");
        epService.getEPRuntime().sendEvent(new Object[]{1, 10, 100, 1000}, "MyEventThree");
        epService.getEPRuntime().sendEvent(new Object[]{2, 10, 200, 2000}, "MyEventThree");
        epService.getEPRuntime().sendEvent(new Object[]{1, 20, 300, 3000}, "MyEventThree");

        assertValuesIterate(epService, "MyTableGS3D", fields, new Object[][]{
                {1, null, null, 4000}, {2, null, null, 2000},
                {null, 10, null, 3000}, {null, 20, null, 3000},
                {null, null, 100, 1000}, {null, null, 200, 2000}, {null, null, 300, 3000}});

        epService.getEPRuntime().sendEvent(new Object[]{1, 10, 400, 4000}, "MyEventThree"); // expires {1, 10, 100, 1000}

        assertValuesIterate(epService, "MyTableGS3D", fields, new Object[][]{
                {1, null, null, 7000}, {2, null, null, 2000},
                {null, 10, null, 6000}, {null, 20, null, 3000},
                {null, null, 100, null}, {null, null, 400, 4000}, {null, null, 200, 2000}, {null, null, 300, 3000}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertValuesIterate(EPServiceProvider epService, String name, String[] fields, Object[][] objects) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from " + name);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields, objects);
    }

    private void assertValuesListener(EPServiceProvider epService, SupportUpdateListener listenerQuery, Object[][] objects) {
        for (int i = 0; i < objects.length; i++) {
            String p00 = (String) objects[i][0];
            Integer expected = (Integer) objects[i][1];
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00));
            assertEquals("Failed at " + i + " for key " + p00, expected, listenerQuery.assertOneGetNewAndReset().get("c0"));
        }
    }
}
