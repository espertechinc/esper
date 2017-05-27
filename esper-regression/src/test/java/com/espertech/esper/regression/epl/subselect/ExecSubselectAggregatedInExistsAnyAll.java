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
package com.espertech.esper.regression.epl.subselect;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecSubselectAggregatedInExistsAnyAll implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {

        for (Class clazz : Arrays.asList(SupportBean.class, SupportValueEvent.class, SupportIdAndValueEvent.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S1", SupportBean_S1.class);

        runAssertionInSimple(epService);
        runAssertionExistsSimple(epService);

        runAssertionUngroupedWOHavingWRelOpAllAnySome(epService);
        runAssertionUngroupedWOHavingWEqualsAllAnySome(epService);
        runAssertionUngroupedWOHavingWIn(epService);
        runAssertionUngroupedWOHavingWExists(epService);

        runAssertionUngroupedWHavingWRelOpAllAnySome(epService);
        runAssertionUngroupedWHavingWEqualsAllAnySome(epService);
        runAssertionUngroupedWHavingWIn(epService);
        runAssertionUngroupedWHavingWExists(epService);

        runAssertionGroupedWOHavingWRelOpAllAnySome(epService);
        runAssertionGroupedWOHavingWEqualsAllAnySome(epService);
        runAssertionGroupedWOHavingWIn(epService);
        runAssertionGroupedWOHavingWExists(epService);

        runAssertionGroupedWHavingWRelOpAllAnySome(epService);
        runAssertionGroupedWHavingWEqualsAllAnySome(epService);
        runAssertionGroupedWHavingWIn(epService);
        runAssertionGroupedWHavingWExists(epService);
    }

    private void runAssertionUngroupedWHavingWIn(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl = "select value in (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c1 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", -1));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false});

        stmt.destroy();
    }

    private void runAssertionGroupedWHavingWIn(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl = "select value in (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) != 'E1') as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) != 'E1') as c1 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false});

        stmt.destroy();
    }

    private void runAssertionGroupedWOHavingWIn(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl = "select value in (select sum(intPrimitive) from SupportBean#keepall group by theString) as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall group by theString) as c1 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 19));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true});
        sendVEAndAssert(epService, listener, fields, 11, new Object[]{true, false});

        stmt.destroy();
    }

    private void runAssertionUngroupedWOHavingWIn(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl = "select value in (select sum(intPrimitive) from SupportBean#keepall) as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall) as c1 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", -1));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false});

        stmt.destroy();
    }


    private void runAssertionGroupedWOHavingWRelOpAllAnySome(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall group by theString) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall group by theString) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall group by theString) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 19));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 9));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true, true});

        stmt.destroy();
    }

    private void runAssertionGroupedWHavingWRelOpAllAnySome(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) not in ('E1', 'E3')) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) not in ('E1', 'E3')) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) not in ('E1', 'E3')) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 19));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 9));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 9));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true, true});

        stmt.destroy();
    }

    private void runAssertionGroupedWOHavingWEqualsAllAnySome(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall group by theString) as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall group by theString) as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall group by theString) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true, true});

        stmt.destroy();
    }

    private void runAssertionUngroupedWOHavingWEqualsAllAnySome(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall) as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall) as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, false, false});

        stmt.destroy();
    }

    private void runAssertionUngroupedWHavingWEqualsAllAnySome(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null, null});

        stmt.destroy();
    }

    private void runAssertionGroupedWHavingWEqualsAllAnySome(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall group by theString having first(theString) != 'E1') as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall group by theString having first(theString) != 'E1') as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall group by theString having first(theString) != 'E1') as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 11));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, true, true});

        stmt.destroy();
    }

    private void runAssertionUngroupedWHavingWExists(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl = "select exists (select sum(intPrimitive) from SupportBean having sum(intPrimitive) < 15) as c0," +
                "not exists (select sum(intPrimitive) from SupportBean  having sum(intPrimitive) < 15) as c1 from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        sendVEAndAssert(epService, listener, fields, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        sendVEAndAssert(epService, listener, fields, new Object[]{false, true});

        stmt.destroy();
    }

    private void runAssertionUngroupedWOHavingWExists(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl = "select exists (select sum(intPrimitive) from SupportBean) as c0," +
                "not exists (select sum(intPrimitive) from SupportBean) as c1 from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        sendVEAndAssert(epService, listener, fields, new Object[]{true, false});

        stmt.destroy();
    }

    private void runAssertionGroupedWOHavingWExists(EPServiceProvider epService) {
        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL("create window MyWindow#keepall as (key string, anint int)");
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyWindow(key, anint) select id, value from SupportIdAndValueEvent");

        String[] fields = "c0,c1".split(",");
        String epl = "select exists (select sum(anint) from MyWindow group by key) as c0," +
                "not exists (select sum(anint) from MyWindow group by key) as c1 from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportIdAndValueEvent("E1", 19));
        sendVEAndAssert(epService, listener, fields, new Object[]{true, false});

        epService.getEPRuntime().executeQuery("delete from MyWindow");

        sendVEAndAssert(epService, listener, fields, new Object[]{false, true});

        stmt.destroy();
        stmtNamedWindow.destroy();
        stmtInsert.destroy();
    }

    private void runAssertionGroupedWHavingWExists(EPServiceProvider epService) {
        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL("create window MyWindow#keepall as (key string, anint int)");
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyWindow(key, anint) select id, value from SupportIdAndValueEvent");

        String[] fields = "c0,c1".split(",");
        String epl = "select exists (select sum(anint) from MyWindow group by key having sum(anint) < 15) as c0," +
                "not exists (select sum(anint) from MyWindow group by key having sum(anint) < 15) as c1 from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportIdAndValueEvent("E1", 19));
        sendVEAndAssert(epService, listener, fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportIdAndValueEvent("E2", 12));
        sendVEAndAssert(epService, listener, fields, new Object[]{true, false});

        epService.getEPRuntime().executeQuery("delete from MyWindow");

        sendVEAndAssert(epService, listener, fields, new Object[]{false, true});

        stmt.destroy();
        stmtNamedWindow.destroy();
        stmtInsert.destroy();
    }

    private void runAssertionUngroupedWHavingWRelOpAllAnySome(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall having last(theString) not in ('E1', 'E3')) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall having last(theString) not in ('E1', 'E3')) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall having last(theString) not in ('E1', 'E3')) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 19));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 9));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", -1000));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, false, false});

        stmt.destroy();
    }

    private void runAssertionUngroupedWOHavingWRelOpAllAnySome(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall) as c2 " +
                "from SupportValueEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendVEAndAssert(epService, listener, fields, 10, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", -1000));
        sendVEAndAssert(epService, listener, fields, 10, new Object[]{false, false, false});

        stmt.destroy();
    }

    private void runAssertionExistsSimple(EPServiceProvider epService) {
        String stmtText = "select id from S0 where exists (select max(id) from S1#length(3))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEventS0(epService, 1);
        assertEquals(1, listener.assertOneGetNewAndReset().get("id"));

        sendEventS1(epService, 100);
        sendEventS0(epService, 2);
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        stmt.destroy();
    }

    private void runAssertionInSimple(EPServiceProvider epService) {
        String stmtText = "select id from S0 where id in (select max(id) from S1#length(2))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEventS0(epService, 1);
        assertFalse(listener.isInvoked());

        sendEventS1(epService, 100);
        sendEventS0(epService, 2);
        assertFalse(listener.isInvoked());

        sendEventS0(epService, 100);
        assertEquals(100, listener.assertOneGetNewAndReset().get("id"));

        sendEventS0(epService, 200);
        assertFalse(listener.isInvoked());

        sendEventS1(epService, -1);
        sendEventS1(epService, -1);
        sendEventS0(epService, -1);
        assertEquals(-1, listener.assertOneGetNewAndReset().get("id"));

        stmt.destroy();
    }

    private void sendVEAndAssert(EPServiceProvider epService, SupportUpdateListener listener, String[] fields, int value, Object[] expected) {
        epService.getEPRuntime().sendEvent(new SupportValueEvent(value));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
    }

    private void sendVEAndAssert(EPServiceProvider epService, SupportUpdateListener listener, String[] fields, Object[] expected) {
        epService.getEPRuntime().sendEvent(new SupportValueEvent(-1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
    }

    private void sendEventS0(EPServiceProvider epService, int id) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(id));
    }

    private void sendEventS1(EPServiceProvider epService, int id) {
        epService.getEPRuntime().sendEvent(new SupportBean_S1(id));
    }
}
