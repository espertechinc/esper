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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestTableRollup extends TestCase {
    private EPServiceProvider epService;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testRollupOneDim() {
        SupportUpdateListener listenerQuery = new SupportUpdateListener();
        SupportUpdateListener listenerOut = new SupportUpdateListener();
        String[] fieldsOut = "theString,total".split(",");

        epService.getEPAdministrator().createEPL("create table MyTable(pk string primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTable insert into MyStream select theString, sum(intPrimitive) as total from SupportBean.win:length(4) group by rollup(theString)").addListener(listenerOut);
        epService.getEPAdministrator().createEPL("select MyTable[p00].total as c0 from SupportBean_S0").addListener(listenerQuery);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertValuesListener(listenerQuery, new Object[][]{{null, 10}, {"E1", 10}, {"E2", null}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E1", 10}, {null, 10}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 200));
        assertValuesListener(listenerQuery, new Object[][]{{null, 210}, {"E1", 10}, {"E2", 200}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E2", 200}, {null, 210}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        assertValuesListener(listenerQuery, new Object[][]{{null, 221}, {"E1", 21}, {"E2", 200}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E1", 21}, {null, 221}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 201));
        assertValuesListener(listenerQuery, new Object[][]{{null, 422}, {"E1", 21}, {"E2", 401}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E2", 401}, {null, 422}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12)); // {"E1", 10} leaving window
        assertValuesListener(listenerQuery, new Object[][]{{null, 424}, {"E1", 23}, {"E2", 401}});
        EPAssertionUtil.assertPropsPerRow(listenerOut.getAndResetLastNewData(), fieldsOut, new Object[][]{{"E1", 23}, {null, 424}});
    }

    public void testRollupTwoDim() {
        String[] fields = "k0,k1,total".split(",");
        epService.getEPAdministrator().createEPL("create objectarray schema MyEvent(k0 int, k1 int, col int)");
        epService.getEPAdministrator().createEPL("create table MyTable(k0 int primary key, k1 int primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTable insert into MyStream select sum(col) as total from MyEvent.win:length(3) group by rollup(k0,k1)");

        epService.getEPRuntime().sendEvent(new Object[] {1, 10, 100}, "MyEvent");
        epService.getEPRuntime().sendEvent(new Object[] {2, 10, 200}, "MyEvent");
        epService.getEPRuntime().sendEvent(new Object[] {1, 20, 300}, "MyEvent");

        assertValuesIterate(fields, new Object[][]{{null, null, 600}, {1, null, 400}, {2, null, 200},
                {1, 10, 100}, {2, 10, 200}, {1, 20, 300}});

        epService.getEPRuntime().sendEvent(new Object[] {1, 10, 400}, "MyEvent"); // expires {1, 10, 100}

        assertValuesIterate(fields, new Object[][]{{null, null, 900}, {1, null, 700}, {2, null, 200},
                {1, 10, 400}, {2, 10, 200}, {1, 20, 300}});
    }

    public void testGroupingSetThreeDim() {
        epService.getEPAdministrator().createEPL("create objectarray schema MyEvent(k0 string, k1 string, k2 string, col int)");
        epService.getEPAdministrator().createEPL("create table MyTable(k0 string primary key, k1 string primary key, k2 string primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTable insert into MyStream select sum(col) as total from MyEvent.win:length(3) group by grouping sets(k0,k1,k2)");

        String[] fields = "k0,k1,k2,total".split(",");
        epService.getEPRuntime().sendEvent(new Object[] {1, 10, 100, 1000}, "MyEvent");
        epService.getEPRuntime().sendEvent(new Object[] {2, 10, 200, 2000}, "MyEvent");
        epService.getEPRuntime().sendEvent(new Object[] {1, 20, 300, 3000}, "MyEvent");

        assertValuesIterate(fields, new Object[][]{
                {1, null, null, 4000}, {2, null, null, 2000},
                {null, 10, null, 3000}, {null, 20, null, 3000},
                {null, null, 100, 1000}, {null, null, 200, 2000}, {null, null, 300, 3000}});

        epService.getEPRuntime().sendEvent(new Object[] {1, 10, 400, 4000}, "MyEvent"); // expires {1, 10, 100, 1000}

        assertValuesIterate(fields, new Object[][]{
                {1, null, null, 7000}, {2, null, null, 2000},
                {null, 10, null, 6000}, {null, 20, null, 3000},
                {null, null, 100, null}, {null, null, 400, 4000}, {null, null, 200, 2000}, {null, null, 300, 3000}});
    }

    private void assertValuesIterate(String[] fields, Object[][] objects) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyTable");
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields, objects);
    }

    private void assertValuesListener(SupportUpdateListener listenerQuery, Object[][] objects) {
        for (int i = 0; i < objects.length; i++) {
            String p00 = (String) objects[i][0];
            Integer expected = (Integer) objects[i][1];
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00));
            assertEquals("Failed at " + i + " for key " + p00, expected, listenerQuery.assertOneGetNewAndReset().get("c0"));
        }
    }
}
