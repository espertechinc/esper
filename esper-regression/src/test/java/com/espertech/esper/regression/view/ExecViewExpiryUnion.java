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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecViewExpiryUnion implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S1", SupportBean_S1.class);

        runAssertionFirstUniqueAndLengthOnDelete(epService);
        runAssertionFirstUniqueAndFirstLength(epService);
        runAssertionBatchWindow(epService);
        runAssertionUnionAndDerivedValue(epService);
        runAssertionUnionGroupBy(epService);
        runAssertionUnionSubselect(epService);
        runAssertionUnionThreeUnique(epService);
        runAssertionUnionPattern(epService);
        runAssertionUnionTwoUnique(epService);
        runAssertionUnionSorted(epService);
        runAssertionUnionTimeWin(epService);
        runAssertionUnionTimeWinSODA(epService);
        runAssertionUnionTimeWinNamedWindow(epService);
        runAssertionUnionTimeWinNamedWindowDelete(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionFirstUniqueAndLengthOnDelete(EPServiceProvider epService) {
        EPStatement nwstmt = epService.getEPAdministrator().createEPL("create window MyWindowOne#firstunique(theString)#firstlength(3) retain-union as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 delete from MyWindowOne where theString = p00");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from MyWindowOne");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"theString", "intPrimitive"};

        sendEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        sendEvent(epService, "E1", 99);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 99}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 99});

        sendEvent(epService, "E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 99}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E2", 2}});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], "theString".split(","), new Object[]{"E1"});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], "theString".split(","), new Object[]{"E1"});
        listener.reset();

        sendEvent(epService, "E1", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 3}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 3});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFirstUniqueAndFirstLength(EPServiceProvider epService) {
        String epl = "select irstream theString, intPrimitive from SupportBean#firstlength(3)#firstunique(theString) retain-union";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionFirstUniqueAndFirstLength(epService, listener, stmt);

        stmt.destroy();
        listener.reset();

        epl = "select irstream theString, intPrimitive from SupportBean#firstunique(theString)#firstlength(3) retain-union";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        tryAssertionFirstUniqueAndFirstLength(epService, listener, stmt);

        stmt.destroy();
    }

    private void tryAssertionFirstUniqueAndFirstLength(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        String[] fields = new String[]{"theString", "intPrimitive"};

        sendEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        sendEvent(epService, "E1", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 2});

        sendEvent(epService, "E2", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1});

        sendEvent(epService, "E2", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}});
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(epService, "E3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}, {"E3", 3}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});

        sendEvent(epService, "E3", 4);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}, {"E3", 3}});
        assertFalse(listener.getAndClearIsInvoked());
    }

    private void runAssertionBatchWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#length_batch(3)#unique(intPrimitive) retain-union");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendEvent(epService, "E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        sendEvent(epService, "E3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});

        sendEvent(epService, "E4", 4);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

        sendEvent(epService, "E5", 4);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});

        sendEvent(epService, "E6", 4);     // remove stream is E1, E2, E3
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6"});

        sendEvent(epService, "E7", 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6", "E7"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E7"});

        sendEvent(epService, "E8", 6);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E5", "E4", "E6", "E7", "E8"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E8"});

        sendEvent(epService, "E9", 7);     // remove stream is E4, E5, E6; E4 and E5 get removed as their
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E6", "E7", "E8", "E9"));
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"E4"}, {"E5"}});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E9"});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionUnionAndDerivedValue(EPServiceProvider epService) {
        String[] fields = new String[]{"total"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#unique(intPrimitive)#unique(intBoxed)#uni(doublePrimitive) retain-union");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10, 100d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr(100d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100d});

        sendEvent(epService, "E2", 2, 20, 50d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr(150d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{150d});

        sendEvent(epService, "E3", 1, 20, 20d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr(170d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{170d});

        stmt.destroy();
    }

    private void runAssertionUnionGroupBy(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        String text = "select irstream theString from SupportBean#groupwin(intPrimitive)#length(2)#unique(intBoxed) retain-union";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendEvent(epService, "E2", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        sendEvent(epService, "E3", 1, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});

        sendEvent(epService, "E4", 1, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

        sendEvent(epService, "E5", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});

        sendEvent(epService, "E6", 1, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E4", "E5", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E6"});
        listener.reset();

        sendEvent(epService, "E7", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E4", "E5", "E6", "E7"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E7"});
        listener.reset();

        sendEvent(epService, "E8", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E5", "E6", "E7", "E8"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E8"});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionUnionSubselect(EPServiceProvider epService) {
        String text = "select * from SupportBean_S0 where p00 in (select theString from SupportBean#length(2)#unique(intPrimitive) retain-union)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1);
        sendEvent(epService, "E2", 2);
        sendEvent(epService, "E3", 3);
        sendEvent(epService, "E4", 2); // throws out E1
        sendEvent(epService, "E5", 1); // retains E3

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E2"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E3"));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E4"));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E5"));
        assertTrue(listener.getAndClearIsInvoked());

        stmt.destroy();
    }

    private void runAssertionUnionThreeUnique(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed)#unique(doublePrimitive) retain-union");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10, 100d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendEvent(epService, "E2", 2, 10, 200d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        sendEvent(epService, "E3", 2, 20, 100d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});

        sendEvent(epService, "E4", 1, 30, 300d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E4"});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionUnionPattern(EPServiceProvider epService) {
        String[] fields = new String[]{"string"};

        String text = "select irstream a.p00||b.p10 as string from pattern [every a=SupportBean_S0 -> b=SupportBean_S1]#unique(a.id)#unique(b.id) retain-union";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "E2"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1E2"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E3"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "E4"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1E2", "E3E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3E4"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E5"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "E6"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3E4", "E5E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E1E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E5E6"});

        stmt.destroy();
    }

    private void runAssertionUnionTwoUnique(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed) retain-union");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendEvent(epService, "E2", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        sendEvent(epService, "E3", 1, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E3"});
        listener.reset();

        sendEvent(epService, "E4", 1, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E4"});
        listener.reset();

        sendEvent(epService, "E5", 2, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});

        sendEvent(epService, "E6", 3, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E5", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E6"});
        listener.reset();

        sendEvent(epService, "E7", 3, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E5", "E6", "E7"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E7"});

        sendEvent(epService, "E8", 4, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E5", "E7", "E8"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E6"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E8"});
        listener.reset();

        sendEvent(epService, "E9", 3, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E5", "E7", "E8", "E9"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E9"});

        sendEvent(epService, "E10", 2, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E8", "E9", "E10"));
        assertEquals(2, listener.getLastOldData().length);
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E5"});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"E7"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E10"});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionUnionSorted(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#sort(2, intPrimitive)#sort(2, intBoxed) retain-union");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendEvent(epService, "E2", 2, 9);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        sendEvent(epService, "E3", 0, 0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});

        sendEvent(epService, "E4", -1, -1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4"));
        assertEquals(2, listener.getLastOldData().length);
        Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
        EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E1", "E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E4"});
        listener.reset();

        sendEvent(epService, "E5", 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4"));
        assertEquals(1, listener.getLastOldData().length);
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E5"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E5"});
        listener.reset();

        sendEvent(epService, "E6", 0, 0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E6"));
        assertEquals(1, listener.getLastOldData().length);
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E6"});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionUnionTimeWin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#unique(intPrimitive)#time(10 sec) retain-union");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionTimeWinUnique(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionUnionTimeWinSODA(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream theString from SupportBean#time(10 seconds)#unique(intPrimitive) retain-union";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionTimeWinUnique(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionUnionTimeWinNamedWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmtWindow = epService.getEPAdministrator().createEPL("create window MyWindowTwo#time(10 sec)#unique(intPrimitive) retain-union as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowTwo select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 delete from MyWindowTwo where intBoxed = id");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtWindow.addListener(listener);

        tryAssertionTimeWinUnique(epService, listener, stmtWindow);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUnionTimeWinNamedWindowDelete(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window MyWindowThree#time(10 sec)#unique(intPrimitive) retain-union as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowThree select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 delete from MyWindowThree where intBoxed = id");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"theString"};

        sendTimer(epService, 1000);
        sendEvent(epService, "E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendTimer(epService, 2000);
        sendEvent(epService, "E2", 2, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20));
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));

        sendTimer(epService, 3000);
        sendEvent(epService, "E3", 3, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});
        sendEvent(epService, "E4", 3, 40);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

        sendTimer(epService, 4000);
        sendEvent(epService, "E5", 4, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E3", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});
        sendEvent(epService, "E6", 4, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E3", "E4", "E5", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E3", "E4", "E5", "E6"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(50));
        assertEquals(2, listener.getLastOldData().length);
        Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
        EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E5", "E6"});
        listener.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E3", "E4"));

        sendTimer(epService, 12999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 13000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4"));

        sendTimer(epService, 10000000);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionTimeWinUnique(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        String[] fields = new String[]{"theString"};

        sendTimer(epService, 1000);
        sendEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendTimer(epService, 2000);
        sendEvent(epService, "E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        sendTimer(epService, 3000);
        sendEvent(epService, "E3", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});

        sendTimer(epService, 4000);
        sendEvent(epService, "E4", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});
        sendEvent(epService, "E5", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5"));
        sendEvent(epService, "E6", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6"));

        sendTimer(epService, 5000);
        sendEvent(epService, "E7", 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E7"});
        sendEvent(epService, "E8", 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E8"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8"));

        sendTimer(epService, 6000);
        sendEvent(epService, "E9", 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E9"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9"));

        sendTimer(epService, 10999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 11000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9"));

        sendTimer(epService, 12999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 13000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E4", "E5", "E6", "E7", "E8", "E9"));

        sendTimer(epService, 14000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E4"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E5", "E6", "E7", "E8", "E9"));

        sendTimer(epService, 15000);
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E7"});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"E8"});
        listener.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E5", "E6", "E9"));

        sendTimer(epService, 1000000);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E5", "E6", "E9"));
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String text = null;

        text = "select theString from SupportBean#groupwin(theString)#unique(theString)#merge(intPrimitive) retain-union";
        tryInvalid(epService, text, "Error starting statement: Error attaching view to parent view: Groupwin view for this merge view could not be found among parent views [select theString from SupportBean#groupwin(theString)#unique(theString)#merge(intPrimitive) retain-union]");

        text = "select theString from SupportBean#groupwin(theString)#groupwin(intPrimitive)#unique(theString)#unique(intPrimitive) retain-union";
        tryInvalid(epService, text, "Error starting statement: Multiple groupwin-declarations are not supported [select theString from SupportBean#groupwin(theString)#groupwin(intPrimitive)#unique(theString)#unique(intPrimitive) retain-union]");
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive, int intBoxed, double doublePrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        bean.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private Object[][] toArr(Object... values) {
        Object[][] arr = new Object[values.length][];
        for (int i = 0; i < values.length; i++) {
            arr[i] = new Object[]{values[i]};
        }
        return arr;
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
