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
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecViewExpiryIntersect implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S1", SupportBean_S1.class);

        runAssertionUniqueAndFirstLength(epService);
        runAssertionFirstUniqueAndFirstLength(epService);
        runAssertionFirstUniqueAndLengthOnDelete(epService);
        runAssertionBatchWindow(epService);
        runAssertionIntersectAndDerivedValue(epService);
        runAssertionIntersectGroupBy(epService);
        runAssertionIntersectSubselect(epService);
        runAssertionIntersectThreeUnique(epService);
        runAssertionIntersectPattern(epService);
        runAssertionIntersectTwoUnique(epService);
        runAssertionIntersectSorted(epService);
        runAssertionIntersectTimeWin(epService);
        runAssertionIntersectTimeWinReversed(epService);
        runAssertionIntersectTimeWinSODA(epService);
        runAssertionIntersectTimeWinNamedWindow(epService);
        runAssertionIntersectTimeWinNamedWindowDelete(epService);
    }

    private void runAssertionUniqueAndFirstLength(EPServiceProvider epService) {
        String epl = "select irstream theString, intPrimitive from SupportBean#firstlength(3)#unique(theString)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionUniqueAndFirstLength(epService, listener, stmt);

        stmt.destroy();
        listener.reset();

        epl = "select irstream theString, intPrimitive from SupportBean#unique(theString)#firstlength(3)";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        tryAssertionUniqueAndFirstLength(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionFirstUniqueAndFirstLength(EPServiceProvider epService) {
        String epl = "select irstream theString, intPrimitive from SupportBean#firstunique(theString)#firstlength(3)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionFirstUniqueAndLength(epService, listener, stmt);

        stmt.destroy();
        epl = "select irstream theString, intPrimitive from SupportBean#firstlength(3)#firstunique(theString)";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        tryAssertionFirstUniqueAndLength(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionFirstUniqueAndLengthOnDelete(EPServiceProvider epService) {
        EPStatement nwstmt = epService.getEPAdministrator().createEPL("create window MyWindowOne#firstunique(theString)#firstlength(3) as SupportBean");
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
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 1}});
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1});

        sendEvent(epService, "E1", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 3}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 3});

        sendEvent(epService, "E1", 99);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 3}, {"E2", 2}});
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 3}, {"E2", 2}, {"E3", 3}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});

        sendEvent(epService, "E3", 98);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][]{{"E1", 3}, {"E2", 2}, {"E3", 3}});
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBatchWindow(EPServiceProvider epService) {
        EPStatement stmt;
        SupportUpdateListener listener = new SupportUpdateListener();

        // test window
        stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#length_batch(3)#unique(intPrimitive) order by theString asc");
        stmt.addListener(listener);
        tryAssertionUniqueAndBatch(epService, listener, stmt);
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#unique(intPrimitive)#length_batch(3) order by theString asc");
        stmt.addListener(listener);
        tryAssertionUniqueAndBatch(epService, listener, stmt);
        stmt.destroy();

        // test aggregation with window
        stmt = epService.getEPAdministrator().createEPL("select count(*) as c0, sum(intPrimitive) as c1 from SupportBean#unique(theString)#length_batch(3)");
        stmt.addListener(listener);
        tryAssertionUniqueBatchAggreation(epService, listener);
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select count(*) as c0, sum(intPrimitive) as c1 from SupportBean#length_batch(3)#unique(theString)");
        stmt.addListener(listener);
        tryAssertionUniqueBatchAggreation(epService, listener);
        stmt.destroy();

        // test first-unique
        stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#firstunique(theString)#length_batch(3)");
        stmt.addListener(listener);
        tryAssertionLengthBatchAndFirstUnique(epService, listener);
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#length_batch(3)#firstunique(theString)");
        stmt.addListener(listener);
        tryAssertionLengthBatchAndFirstUnique(epService, listener);
        stmt.destroy();

        // test time-based expiry
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#unique(theString)#time_batch(1)");
        stmt.addListener(listener);
        tryAssertionTimeBatchAndUnique(epService, listener, 0);
        stmt.destroy();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(100000));
        stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#time_batch(1)#unique(theString)");
        stmt.addListener(listener);
        tryAssertionTimeBatchAndUnique(epService, listener, 100000);
        stmt.destroy();

        try {
            epService.getEPAdministrator().createEPL("select * from SupportBean#time_batch(1)#length_batch(10)");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Cannot combined multiple batch data windows into an intersection [select * from SupportBean#time_batch(1)#length_batch(10)]", ex.getMessage());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIntersectAndDerivedValue(EPServiceProvider epService) {
        String[] fields = new String[]{"total"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#unique(intPrimitive)#unique(intBoxed)#uni(doublePrimitive)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10, 100d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr(100d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100d});

        sendEvent(epService, "E2", 2, 20, 50d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr(150d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{150d});

        sendEvent(epService, "E3", 1, 20, 20d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr(20d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20d});

        stmt.destroy();
    }

    private void runAssertionIntersectGroupBy(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        String text = "select irstream theString from SupportBean#groupwin(intPrimitive)#length(2)#unique(intBoxed) retain-intersection";
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
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E4"});
        listener.reset();

        sendEvent(epService, "E5", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E5"});
        listener.reset();

        sendEvent(epService, "E6", 1, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E5", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E6"});
        listener.reset();

        sendEvent(epService, "E7", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E5", "E6", "E7"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E4"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E7"});
        listener.reset();

        sendEvent(epService, "E8", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E6", "E7", "E8"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E5"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E8"});
        listener.reset();

        // another combination
        stmt.destroy();
        epService.getEPAdministrator().createEPL("select * from SupportBean#groupwin(theString)#time(.0083 sec)#firstevent").destroy();
    }

    private void runAssertionIntersectSubselect(EPServiceProvider epService) {
        String text = "select * from SupportBean_S0 where p00 in (select theString from SupportBean#length(2)#unique(intPrimitive) retain-intersection)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1);
        sendEvent(epService, "E2", 2);
        sendEvent(epService, "E3", 3); // throws out E1
        sendEvent(epService, "E4", 2); // throws out E2
        sendEvent(epService, "E5", 1); // throws out E3

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E2"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E3"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E4"));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E5"));
        assertTrue(listener.getAndClearIsInvoked());

        stmt.destroy();
    }

    private void runAssertionIntersectThreeUnique(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed)#unique(doublePrimitive) retain-intersection");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10, 100d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendEvent(epService, "E2", 2, 10, 200d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E2"});
        listener.reset();

        sendEvent(epService, "E3", 2, 20, 100d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E3"});
        listener.reset();

        sendEvent(epService, "E4", 1, 30, 300d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

        sendEvent(epService, "E5", 3, 40, 400d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});

        sendEvent(epService, "E6", 3, 40, 300d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E6"));
        Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
        EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E4", "E5"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E6"});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionIntersectPattern(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        String text = "select irstream a.p00||b.p10 as theString from pattern [every a=SupportBean_S0 -> b=SupportBean_S1]#unique(a.id)#unique(b.id) retain-intersection";
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

    private void runAssertionIntersectTwoUnique(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed) retain-intersection");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendEvent(epService, "E2", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E2"});
        listener.reset();

        sendEvent(epService, "E3", 1, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});

        sendEvent(epService, "E4", 3, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E4"});
        listener.reset();

        sendEvent(epService, "E5", 2, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E5"});
        listener.reset();

        sendEvent(epService, "E6", 3, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E5", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E4"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E6"});
        listener.reset();

        sendEvent(epService, "E7", 3, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E7"));
        assertEquals(2, listener.getLastOldData().length);
        Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
        EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E5", "E6"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E7"});
        listener.reset();

        sendEvent(epService, "E8", 4, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E7", "E8"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E8"});

        sendEvent(epService, "E9", 3, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E8", "E9"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E7"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E9"});
        listener.reset();

        sendEvent(epService, "E10", 2, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E8", "E10"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E9"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E10"});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionIntersectSorted(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#sort(2, intPrimitive)#sort(2, intBoxed) retain-intersection");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        sendEvent(epService, "E2", 2, 9);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        sendEvent(epService, "E3", 0, 0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3"));
        Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
        EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E1", "E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E3"});
        listener.reset();

        sendEvent(epService, "E4", -1, -1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

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

    private void runAssertionIntersectTimeWin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#unique(intPrimitive)#time(10 sec) retain-intersection");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionTimeWinUnique(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionIntersectTimeWinReversed(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#time(10 sec)#unique(intPrimitive) retain-intersection");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionTimeWinUnique(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionIntersectTimeWinSODA(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream theString from SupportBean#time(10 seconds)#unique(intPrimitive) retain-intersection";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionTimeWinUnique(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionIntersectTimeWinNamedWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmtWindow = epService.getEPAdministrator().createEPL("create window MyWindowTwo#time(10 sec)#unique(intPrimitive) retain-intersection as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowTwo select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 delete from MyWindowTwo where intBoxed = id");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtWindow.addListener(listener);

        tryAssertionTimeWinUnique(epService, listener, stmtWindow);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIntersectTimeWinNamedWindowDelete(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window MyWindowThree#time(10 sec)#unique(intPrimitive) retain-intersection as select * from SupportBean");
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
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E4"});
        listener.reset();

        sendTimer(epService, 4000);
        sendEvent(epService, "E5", 4, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});
        sendEvent(epService, "E6", 4, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E5"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E6"});
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4", "E6"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(50));
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E6"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4"));

        sendTimer(epService, 10999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 11000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4"));

        sendTimer(epService, 12999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 13000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E4"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr());

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
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E3"});
        listener.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3"));

        sendTimer(epService, 4000);
        sendEvent(epService, "E4", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3", "E4"));
        sendEvent(epService, "E5", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E4"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E5"});
        listener.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3", "E5"));

        sendTimer(epService, 11999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 12000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E5"));

        sendTimer(epService, 12999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 13000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E5"));

        sendTimer(epService, 13999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 14000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E5"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr());
    }

    private void tryAssertionUniqueBatchAggreation(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "c0,c1".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 11));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3L, 10 + 11 + 12});

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 13));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 14));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 15));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3L, 13 + 14 + 15});

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 16));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 17));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 18));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3L, 16 + 17 + 18});

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 19));
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 21));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 22));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 23));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3L, 20 + 22 + 23});
    }

    private void tryAssertionUniqueAndBatch(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        String[] fields = new String[]{"theString"};

        sendEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
        assertNull(listener.getAndResetLastOldData());

        sendEvent(epService, "E4", 4);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4"));
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E5", 4); // throws out E5
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E5"));
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E6", 4); // throws out E6
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E6"));
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E7", 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E6", "E7"));
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E8", 6);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
        listener.reset();

        sendEvent(epService, "E8", 7);
        sendEvent(epService, "E9", 9);
        sendEvent(epService, "E9", 9);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E8", "E9"));
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E10", 11);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E10"}, {"E8"}, {"E9"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}});
        listener.reset();
    }

    private void tryAssertionUniqueAndFirstLength(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        String[] fields = new String[]{"theString", "intPrimitive"};

        sendEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        sendEvent(epService, "E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        sendEvent(epService, "E1", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 3}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E1", 3});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E1", 1});
        listener.reset();

        sendEvent(epService, "E3", 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 3}, {"E2", 2}, {"E3", 30}});
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E3", 30});
        listener.reset();

        sendEvent(epService, "E4", 40);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 3}, {"E2", 2}, {"E3", 30}});
        assertFalse(listener.isInvoked());
    }

    private void tryAssertionFirstUniqueAndLength(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {

        String[] fields = new String[]{"theString", "intPrimitive"};

        sendEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        sendEvent(epService, "E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        sendEvent(epService, "E2", 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});

        sendEvent(epService, "E4", 4);
        sendEvent(epService, "E4", 5);
        sendEvent(epService, "E5", 5);
        sendEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
        assertFalse(listener.isInvoked());
    }

    private void tryAssertionTimeBatchAndUnique(EPServiceProvider epService, SupportUpdateListener listener, long startTime) {
        String[] fields = "theString,intPrimitive".split(",");
        listener.reset();

        sendEvent(epService, "E1", 1);
        sendEvent(epService, "E2", 2);
        sendEvent(epService, "E1", 3);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime + 1000));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E2", 2}, {"E1", 3}});
        assertNull(listener.getAndResetLastOldData());

        sendEvent(epService, "E3", 3);
        sendEvent(epService, "E3", 4);
        sendEvent(epService, "E3", 5);
        sendEvent(epService, "E4", 6);
        sendEvent(epService, "E3", 7);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime + 2000));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E4", 6}, {"E3", 7}});
        assertNull(listener.getAndResetLastOldData());
    }

    private void tryAssertionLengthBatchAndFirstUnique(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "theString,intPrimitive".split(",");

        sendEvent(epService, "E1", 1);
        sendEvent(epService, "E2", 2);
        sendEvent(epService, "E1", 3);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E3", 4);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}});
        assertNull(listener.getAndResetLastOldData());

        sendEvent(epService, "E1", 5);
        sendEvent(epService, "E4", 7);
        sendEvent(epService, "E1", 6);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E5", 9);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1", 5}, {"E4", 7}, {"E5", 9}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastOldData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}});
        listener.reset();
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
