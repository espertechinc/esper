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
package com.espertech.esper.regression.epl;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Arrays;

public class TestSubselectAggregatedInExistsAnyAll extends TestCase {
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.startTest(epService, this.getClass(), getName());
        }
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.endTest();
        }
        listener = null;
    }

    public void testAggregatedInExistsAnyAll() {
        for (Class clazz : Arrays.asList(SupportBean.class, SupportValueEvent.class, SupportIdAndValueEvent.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S1", SupportBean_S1.class);

        runAssertionInSimple();
        runAssertionExistsSimple();

        runAssertionUngroupedWOHavingWRelOpAllAnySome();
        runAssertionUngroupedWOHavingWEqualsAllAnySome();
        runAssertionUngroupedWOHavingWIn();
        runAssertionUngroupedWOHavingWExists();

        runAssertionUngroupedWHavingWRelOpAllAnySome();
        runAssertionUngroupedWHavingWEqualsAllAnySome();
        runAssertionUngroupedWHavingWIn();
        runAssertionUngroupedWHavingWExists();

        runAssertionGroupedWOHavingWRelOpAllAnySome();
        runAssertionGroupedWOHavingWEqualsAllAnySome();
        runAssertionGroupedWOHavingWIn();
        runAssertionGroupedWOHavingWExists();

        runAssertionGroupedWHavingWRelOpAllAnySome();
        runAssertionGroupedWHavingWEqualsAllAnySome();
        runAssertionGroupedWHavingWIn();
        runAssertionGroupedWHavingWExists();
    }

    private void runAssertionUngroupedWHavingWIn() {
        String[] fields = "c0,c1".split(",");
        String epl = "select value in (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c1 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(fields, 10, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        sendVEAndAssert(fields, 10, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendVEAndAssert(fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", -1));
        sendVEAndAssert(fields, 10, new Object[]{true, false});

        stmt.destroy();
    }

    private void runAssertionGroupedWHavingWIn() {
        String[] fields = "c0,c1".split(",");
        String epl = "select value in (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) != 'E1') as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) != 'E1') as c1 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        sendVEAndAssert(fields, 10, new Object[]{true, false});

        stmt.destroy();
    }

    private void runAssertionGroupedWOHavingWIn() {
        String[] fields = "c0,c1".split(",");
        String epl = "select value in (select sum(intPrimitive) from SupportBean#keepall group by theString) as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall group by theString) as c1 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 19));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(fields, 10, new Object[]{false, true});
        sendVEAndAssert(fields, 11, new Object[]{true, false});

        stmt.destroy();
    }

    private void runAssertionUngroupedWOHavingWIn() {
        String[] fields = "c0,c1".split(",");
        String epl = "select value in (select sum(intPrimitive) from SupportBean#keepall) as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall) as c1 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(fields, 10, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        sendVEAndAssert(fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", -1));
        sendVEAndAssert(fields, 10, new Object[]{true, false});

        stmt.destroy();
    }


    private void runAssertionGroupedWOHavingWRelOpAllAnySome() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall group by theString) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall group by theString) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall group by theString) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 19));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 9));
        sendVEAndAssert(fields, 10, new Object[]{false, true, true});

        stmt.destroy();
    }

    private void runAssertionGroupedWHavingWRelOpAllAnySome() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) not in ('E1', 'E3')) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) not in ('E1', 'E3')) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) not in ('E1', 'E3')) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 19));
        sendVEAndAssert(fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 9));
        sendVEAndAssert(fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 9));
        sendVEAndAssert(fields, 10, new Object[]{false, true, true});

        stmt.destroy();
    }

    private void runAssertionGroupedWOHavingWEqualsAllAnySome() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall group by theString) as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall group by theString) as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall group by theString) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(fields, 10, new Object[]{false, true, true});

        stmt.destroy();
    }

    private void runAssertionUngroupedWOHavingWEqualsAllAnySome() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall) as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall) as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(fields, 10, new Object[]{false, false, false});

        stmt.destroy();
    }

    private void runAssertionUngroupedWHavingWEqualsAllAnySome() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        sendVEAndAssert(fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendVEAndAssert(fields, 10, new Object[]{false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        sendVEAndAssert(fields, 10, new Object[]{null, null, null});

        stmt.destroy();
    }

    private void runAssertionGroupedWHavingWEqualsAllAnySome() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall group by theString having first(theString) != 'E1') as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall group by theString having first(theString) != 'E1') as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall group by theString having first(theString) != 'E1') as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        sendVEAndAssert(fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 11));
        sendVEAndAssert(fields, 10, new Object[]{false, true, true});

        stmt.destroy();
    }

    private void runAssertionUngroupedWHavingWExists() {
        String[] fields = "c0,c1".split(",");
        String epl = "select exists (select sum(intPrimitive) from SupportBean having sum(intPrimitive) < 15) as c0," +
                "not exists (select sum(intPrimitive) from SupportBean  having sum(intPrimitive) < 15) as c1 from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        sendVEAndAssert(fields, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        sendVEAndAssert(fields, new Object[]{false, true});

        stmt.destroy();
    }

    private void runAssertionUngroupedWOHavingWExists() {
        String[] fields = "c0,c1".split(",");
        String epl = "select exists (select sum(intPrimitive) from SupportBean) as c0," +
                "not exists (select sum(intPrimitive) from SupportBean) as c1 from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        sendVEAndAssert(fields, new Object[]{true, false});

        stmt.destroy();
    }

    private void runAssertionGroupedWOHavingWExists() {
        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL("create window MyWindow#keepall as (key string, anint int)");
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyWindow(key, anint) select id, value from SupportIdAndValueEvent");

        String[] fields = "c0,c1".split(",");
        String epl = "select exists (select sum(anint) from MyWindow group by key) as c0," +
                "not exists (select sum(anint) from MyWindow group by key) as c1 from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportIdAndValueEvent("E1", 19));
        sendVEAndAssert(fields, new Object[]{true, false});

        epService.getEPRuntime().executeQuery("delete from MyWindow");

        sendVEAndAssert(fields, new Object[]{false, true});

        stmt.destroy();
        stmtNamedWindow.destroy();
        stmtInsert.destroy();
    }

    private void runAssertionGroupedWHavingWExists() {
        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL("create window MyWindow#keepall as (key string, anint int)");
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyWindow(key, anint) select id, value from SupportIdAndValueEvent");

        String[] fields = "c0,c1".split(",");
        String epl = "select exists (select sum(anint) from MyWindow group by key having sum(anint) < 15) as c0," +
                "not exists (select sum(anint) from MyWindow group by key having sum(anint) < 15) as c1 from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportIdAndValueEvent("E1", 19));
        sendVEAndAssert(fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportIdAndValueEvent("E2", 12));
        sendVEAndAssert(fields, new Object[]{true, false});

        epService.getEPRuntime().executeQuery("delete from MyWindow");

        sendVEAndAssert(fields, new Object[]{false, true});

        stmt.destroy();
        stmtNamedWindow.destroy();
        stmtInsert.destroy();
    }

    private void runAssertionUngroupedWHavingWRelOpAllAnySome() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall having last(theString) not in ('E1', 'E3')) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall having last(theString) not in ('E1', 'E3')) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall having last(theString) not in ('E1', 'E3')) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 19));
        sendVEAndAssert(fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 9));
        sendVEAndAssert(fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", -1000));
        sendVEAndAssert(fields, 10, new Object[]{false, false, false});

        stmt.destroy();
    }

    private void runAssertionUngroupedWOHavingWRelOpAllAnySome() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendVEAndAssert(fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        sendVEAndAssert(fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", -1000));
        sendVEAndAssert(fields, 10, new Object[]{false, false, false});

        stmt.destroy();
    }

    private void runAssertionExistsSimple()
    {
        String stmtText = "select id from S0 where exists (select max(id) from S1#length(3))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEventS0(1);
        assertEquals(1, listener.assertOneGetNewAndReset().get("id"));

        sendEventS1(100);
        sendEventS0(2);
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));
    }

    private void runAssertionInSimple()
    {
        String stmtText = "select id from S0 where id in (select max(id) from S1#length(2))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEventS0(1);
        assertFalse(listener.isInvoked());

        sendEventS1(100);
        sendEventS0(2);
        assertFalse(listener.isInvoked());

        sendEventS0(100);
        assertEquals(100, listener.assertOneGetNewAndReset().get("id"));

        sendEventS0(200);
        assertFalse(listener.isInvoked());

        sendEventS1(-1);
        sendEventS1(-1);
        sendEventS0(-1);
        assertEquals(-1, listener.assertOneGetNewAndReset().get("id"));
    }

    private void sendVEAndAssert(String[] fields, int value, Object[] expected) {
        epService.getEPRuntime().sendEvent(new SupportValueEvent(value));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
    }

    private void sendVEAndAssert(String[] fields, Object[] expected) {
        epService.getEPRuntime().sendEvent(new SupportValueEvent(-1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
    }

    private void sendEventS0(int id)
    {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(id));
    }

    private void sendEventS1(int id)
    {
        epService.getEPRuntime().sendEvent(new SupportBean_S1(id));
    }
}
