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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestDataWindowIntersectExpiry extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    protected void tearDown() throws Exception {
        listener = null;
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testUniqueAndFirstLength()
    {
        init(false);

        String epl = "select irstream theString, intPrimitive from SupportBean.win:firstlength(3).std:unique(theString)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        runAssertionUniqueAndFirstLength(stmt);

        stmt.destroy();
        listener.reset();
        
        epl = "select irstream theString, intPrimitive from SupportBean.std:unique(theString).win:firstlength(3)";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        runAssertionUniqueAndFirstLength(stmt);
    }

    public void testFirstUniqueAndFirstLength()
    {
        init(false);

        String epl = "select irstream theString, intPrimitive from SupportBean.std:firstunique(theString).win:firstlength(3)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        runAssertionFirstUniqueAndLength(stmt);

        stmt.destroy();
        epl = "select irstream theString, intPrimitive from SupportBean.win:firstlength(3).std:firstunique(theString)";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        runAssertionFirstUniqueAndLength(stmt);
    }

    public void testFirstUniqueAndLengthOnDelete()
    {
        init(false);

        EPStatement nwstmt = epService.getEPAdministrator().createEPL("create window MyWindow.std:firstunique(theString).win:firstlength(3) as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 delete from MyWindow where theString = p00");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from MyWindow");
        stmt.addListener(listener);

        String[] fields = new String[] {"theString", "intPrimitive"};

        sendEvent("E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][] {{"E1", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 1});

        sendEvent("E1", 99);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][] {{"E1", 1}});
        assertFalse(listener.isInvoked());

        sendEvent("E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][] {{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
        
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][] {{"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[] {"E1", 1});

        sendEvent("E1", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][] {{"E1", 3}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 3});

        sendEvent("E1", 99);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][] {{"E1", 3}, {"E2", 2}});
        assertFalse(listener.isInvoked());

        sendEvent("E3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][] {{"E1", 3}, {"E2", 2}, {"E3", 3}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E3", 3});

        sendEvent("E3", 98);
        EPAssertionUtil.assertPropsPerRowAnyOrder(nwstmt.iterator(), fields, new Object[][] {{"E1", 3}, {"E2", 2}, {"E3", 3}});
        assertFalse(listener.isInvoked());
    }

    public void testBatchWindow()
    {
        init(false);
        EPStatement stmt;

        // test window
        stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean.win:length_batch(3).std:unique(intPrimitive) order by theString asc");
        stmt.addListener(listener);
        runAssertionUniqueAndBatch(stmt);
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean.std:unique(intPrimitive).win:length_batch(3) order by theString asc");
        stmt.addListener(listener);
        runAssertionUniqueAndBatch(stmt);
        stmt.destroy();

        // test aggregation with window
        stmt = epService.getEPAdministrator().createEPL("select count(*) as c0, sum(intPrimitive) as c1 from SupportBean.std:unique(theString).win:length_batch(3)");
        stmt.addListener(listener);
        runAssertionUniqueBatchAggreation();
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select count(*) as c0, sum(intPrimitive) as c1 from SupportBean.win:length_batch(3).std:unique(theString)");
        stmt.addListener(listener);
        runAssertionUniqueBatchAggreation();
        stmt.destroy();

        // test first-unique
        stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean.std:firstunique(theString).win:length_batch(3)");
        stmt.addListener(listener);
        runAssertionLengthBatchAndFirstUnique();
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean.win:length_batch(3).std:firstunique(theString)");
        stmt.addListener(listener);
        runAssertionLengthBatchAndFirstUnique();
        stmt.destroy();

        // test time-based expiry
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        stmt = epService.getEPAdministrator().createEPL("select * from SupportBean.std:unique(theString).win:time_batch(1)");
        stmt.addListener(listener);
        runAssertionTimeBatchAndUnique(0);
        stmt.destroy();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(100000));
        stmt = epService.getEPAdministrator().createEPL("select * from SupportBean.win:time_batch(1).std:unique(theString)");
        stmt.addListener(listener);
        runAssertionTimeBatchAndUnique(100000);
        stmt.destroy();

        try {
            stmt = epService.getEPAdministrator().createEPL("select * from SupportBean.win:time_batch(1).win:length_batch(10)");
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Error starting statement: Cannot combined multiple batch data windows into an intersection [select * from SupportBean.win:time_batch(1).win:length_batch(10)]", ex.getMessage());
        }
    }

    public void testIntersectAndDerivedValue()
    {
        init(false);
        String[] fields = new String[] {"total"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean.std:unique(intPrimitive).std:unique(intBoxed).stat:uni(doublePrimitive)");
        stmt.addListener(listener);

        sendEvent("E1", 1, 10, 100d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr(100d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {100d});

        sendEvent("E2", 2, 20, 50d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr(150d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {150d});

        sendEvent("E3", 1, 20, 20d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr(20d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {20d});
    }

    public void testIntersectGroupBy()
    {
        init(false);
        String[] fields = new String[] {"theString"};

        String text = "select irstream theString from SupportBean.std:groupwin(intPrimitive).win:length(2).std:unique(intBoxed) retain-intersection";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        sendEvent("E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1"});

        sendEvent("E2", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2"});

        sendEvent("E3", 1, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E3"});

        sendEvent("E4", 1, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E4"});
        listener.reset();

        sendEvent("E5", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E5"});
        listener.reset();

        sendEvent("E6", 1, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E5", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E6"});
        listener.reset();

        sendEvent("E7", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E5", "E6", "E7"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E4"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E7"});
        listener.reset();

        sendEvent("E8", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E6", "E7", "E8"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E5"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E8"});
        listener.reset();

        // another combination
        epService.getEPAdministrator().createEPL("select * from SupportBean.std:groupwin(theString).win:time(.0083 sec).std:firstevent()");
    }

    public void testIntersectSubselect()
    {
        init(false);

        String text = "select * from SupportBean_S0 where p00 in (select theString from SupportBean.win:length(2).std:unique(intPrimitive) retain-intersection)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        sendEvent("E1", 1);
        sendEvent("E2", 2);
        sendEvent("E3", 3); // throws out E1
        sendEvent("E4", 2); // throws out E2
        sendEvent("E5", 1); // throws out E3

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
    }

    public void testIntersectThreeUnique()
    {
        init(false);
        String[] fields = new String[] {"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean.std:unique(intPrimitive).std:unique(intBoxed).std:unique(doublePrimitive) retain-intersection");
        stmt.addListener(listener);

        sendEvent("E1", 1, 10, 100d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1"});

        sendEvent("E2", 2, 10, 200d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E2"});
        listener.reset();

        sendEvent("E3", 2, 20, 100d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E3"});
        listener.reset();

        sendEvent("E4", 1, 30, 300d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E4"});

        sendEvent("E5", 3, 40, 400d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E5"});

        sendEvent("E6", 3, 40, 300d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E6"));
        Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
        EPAssertionUtil.assertEqualsAnyOrder(result, new String[] {"E4", "E5"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E6"});
        listener.reset();
    }

    public void testIntersectPattern()
    {
        init(false);
        String[] fields = new String[] {"theString"};

        String text = "select irstream a.p00||b.p10 as theString from pattern [every a=SupportBean_S0 -> b=SupportBean_S1].std:unique(a.id).std:unique(b.id) retain-intersection";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "E2"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1E2"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E3"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "E4"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1E2", "E3E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E3E4"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E5"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "E6"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3E4", "E5E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E1E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E5E6"});
    }

    public void testIntersectTwoUnique()
    {
        init(false);
        String[] fields = new String[] {"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean.std:unique(intPrimitive).std:unique(intBoxed) retain-intersection");
        stmt.addListener(listener);

        sendEvent("E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1"});

        sendEvent("E2", 2, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E2"});
        listener.reset();

        sendEvent("E3", 1, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E3"});

        sendEvent("E4", 3, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E4"});
        listener.reset();

        sendEvent("E5", 2, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E5"});
        listener.reset();

        sendEvent("E6", 3, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E5", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E4"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E6"});
        listener.reset();

        sendEvent("E7", 3, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E7"));
        assertEquals(2, listener.getLastOldData().length);
        Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
        EPAssertionUtil.assertEqualsAnyOrder(result, new String[] {"E5", "E6"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E7"});
        listener.reset();

        sendEvent("E8", 4, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E7", "E8"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E8"});

        sendEvent("E9", 3, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E8", "E9"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E7"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E9"});
        listener.reset();

        sendEvent("E10", 2, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E8", "E10"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E9"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E10"});
        listener.reset();
    }

    public void testIntersectSorted()
    {
        init(false);
        String[] fields = new String[] {"theString"};

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean.ext:sort(2, intPrimitive).ext:sort(2, intBoxed) retain-intersection");
        stmt.addListener(listener);

        sendEvent("E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1"});

        sendEvent("E2", 2, 9);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2"});

        sendEvent("E3", 0, 0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3"));
        Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
        EPAssertionUtil.assertEqualsAnyOrder(result, new String[] {"E1", "E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E3"});
        listener.reset();

        sendEvent("E4", -1, -1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E4"});

        sendEvent("E5", 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3", "E4"));
        assertEquals(1, listener.getLastOldData().length);
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E5"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E5"});
        listener.reset();

        sendEvent("E6", 0, 0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4", "E6"));
        assertEquals(1, listener.getLastOldData().length);
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E6"});
        listener.reset();
    }

    public void testIntersectTimeWin()
    {
        init(false);

        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean.std:unique(intPrimitive).win:time(10 sec) retain-intersection");
        stmt.addListener(listener);

        runAssertionTimeWinUnique(stmt);
    }

    public void testIntersectTimeWinReversed()
    {
        init(false);

        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean.win:time(10 sec).std:unique(intPrimitive) retain-intersection");
        stmt.addListener(listener);

        runAssertionTimeWinUnique(stmt);
    }

    public void testIntersectTimeWinSODA()
    {
        init(false);

        sendTimer(0);
        String stmtText = "select irstream theString from SupportBean.win:time(10 seconds).std:unique(intPrimitive) retain-intersection";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        runAssertionTimeWinUnique(stmt);
    }

    public void testIntersectTimeWinNamedWindow()
    {
        init(false);

        sendTimer(0);
        EPStatement stmtWindow = epService.getEPAdministrator().createEPL("create window MyWindow.win:time(10 sec).std:unique(intPrimitive) retain-intersection as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 delete from MyWindow where intBoxed = id");
        stmtWindow.addListener(listener);

        runAssertionTimeWinUnique(stmtWindow);
    }

    public void testIntersectTimeWinNamedWindowDelete()
    {
        init(false);

        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window MyWindow.win:time(10 sec).std:unique(intPrimitive) retain-intersection as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 delete from MyWindow where intBoxed = id");
        stmt.addListener(listener);

        String[] fields = new String[] {"theString"};

        sendTimer(1000);
        sendEvent("E1", 1, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1"});

        sendTimer(2000);
        sendEvent("E2", 2, 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20));
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[] {"E2"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));

        sendTimer(3000);
        sendEvent("E3", 3, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E3"});
        sendEvent("E4", 3, 40);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E3"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E4"});
        listener.reset();

        sendTimer(4000);
        sendEvent("E5", 4, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E5"});
        sendEvent("E6", 4, 50);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E5"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E6"});
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4", "E6"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(50));
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[] {"E6"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E4"));

        sendTimer(10999);
        assertFalse(listener.isInvoked());
        sendTimer(11000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[] {"E1"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4"));

        sendTimer(12999);
        assertFalse(listener.isInvoked());
        sendTimer(13000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[] {"E4"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr());

        sendTimer(10000000);
        assertFalse(listener.isInvoked());
    }

    private void runAssertionTimeWinUnique(EPStatement stmt)
    {
        String[] fields = new String[] {"theString"};

        sendTimer(1000);
        sendEvent("E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1"});

        sendTimer(2000);
        sendEvent("E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2"});

        sendTimer(3000);
        sendEvent("E3", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E1"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E3"});
        listener.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3"));

        sendTimer(4000);
        sendEvent("E4", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E4"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3", "E4"));
        sendEvent("E5", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[] {"E4"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[] {"E5"});
        listener.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E2", "E3", "E5"));

        sendTimer(11999);
        assertFalse(listener.isInvoked());
        sendTimer(12000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[] {"E2"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E3","E5"));

        sendTimer(12999);
        assertFalse(listener.isInvoked());
        sendTimer(13000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[] {"E3"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E5"));

        sendTimer(13999);
        assertFalse(listener.isInvoked());
        sendTimer(14000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[] {"E5"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr());
    }

    private void sendEvent(String theString, int intPrimitive, int intBoxed, double doublePrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        bean.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(String theString, int intPrimitive, int intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private Object[][] toArr(Object ...values)
    {
        Object[][] arr = new Object[values.length][];
        for (int i = 0; i < values.length; i++)
        {
            arr[i] = new Object[] {values[i]};
        }
        return arr;
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void init(boolean isAllowMultipleDataWindows)
    {
        listener = new SupportUpdateListener();

        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(isAllowMultipleDataWindows);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S1", SupportBean_S1.class);
    }

    private void runAssertionUniqueBatchAggreation() {
        String[] fields = "c0,c1".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 11));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {3L, 10+11+12});

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 13));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 14));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 15));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {3L, 13+14+15});

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 16));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 17));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 18));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {3L, 16+17+18});

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 19));
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 21));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 22));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 23));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {3L, 20+22+23});
    }

    private void runAssertionUniqueAndBatch(EPStatement stmt) {
        String[] fields = new String[] {"theString"};

        sendEvent("E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1"));
        assertFalse(listener.isInvoked());

        sendEvent("E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E1", "E2"));
        assertFalse(listener.isInvoked());

        sendEvent("E3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][] {{"E1"}, {"E2"}, {"E3"}});
        assertNull(listener.getAndResetLastOldData());

        sendEvent("E4", 4);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E4"));
        assertFalse(listener.isInvoked());

        sendEvent("E5", 4); // throws out E5
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E5"));
        assertFalse(listener.isInvoked());

        sendEvent("E6", 4); // throws out E6
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E6"));
        assertFalse(listener.isInvoked());

        sendEvent("E7", 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E6", "E7"));
        assertFalse(listener.isInvoked());

        sendEvent("E8", 6);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][] {{"E6"}, {"E7"}, {"E8"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
        listener.reset();

        sendEvent("E8", 7);
        sendEvent("E9", 9);
        sendEvent("E9", 9);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, toArr("E8", "E9"));
        assertFalse(listener.isInvoked());

        sendEvent("E10", 11);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E10"}, {"E8"}, {"E9"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}});
        listener.reset();
    }

    private void runAssertionUniqueAndFirstLength(EPStatement stmt)
    {
        String[] fields = new String[] {"theString", "intPrimitive"};

        sendEvent("E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 1});

        sendEvent("E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 2});

        sendEvent("E1", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 3}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[] {"E1", 3});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[] {"E1", 1});
        listener.reset();

        sendEvent("E3", 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 3}, {"E2", 2}, {"E3", 30}});
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[] {"E3", 30});
        listener.reset();

        sendEvent("E4", 40);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 3}, {"E2", 2}, {"E3", 30}});
        assertFalse(listener.isInvoked());
    }

    private void runAssertionFirstUniqueAndLength(EPStatement stmt) {

        String[] fields = new String[] {"theString", "intPrimitive"};

        sendEvent("E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 1});

        sendEvent("E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 2});

        sendEvent("E2", 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 1}, {"E2", 2}});
        assertFalse(listener.isInvoked());

        sendEvent("E3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 1}, {"E2", 2}, {"E3", 3}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E3", 3});

        sendEvent("E4", 4);
        sendEvent("E4", 5);
        sendEvent("E5", 5);
        sendEvent("E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 1}, {"E2", 2}, {"E3", 3}});
        assertFalse(listener.isInvoked());
    }

    private void runAssertionTimeBatchAndUnique(long startTime) {
        String[] fields = "theString,intPrimitive".split(",");
        listener.reset();

        sendEvent("E1", 1);
        sendEvent("E2", 2);
        sendEvent("E1", 3);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime + 1000));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][] {{"E2", 2}, {"E1", 3}});
        assertNull(listener.getAndResetLastOldData());

        sendEvent("E3", 3);
        sendEvent("E3", 4);
        sendEvent("E3", 5);
        sendEvent("E4", 6);
        sendEvent("E3", 7);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime + 2000));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][] {{"E4", 6}, {"E3", 7}});
        assertNull(listener.getAndResetLastOldData());
    }

    private void runAssertionLengthBatchAndFirstUnique() {
        String[] fields = "theString,intPrimitive".split(",");

        sendEvent("E1", 1);
        sendEvent("E2", 2);
        sendEvent("E1", 3);
        assertFalse(listener.isInvoked());

        sendEvent("E3", 4);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][] {{"E1", 1}, {"E2", 2}, {"E3", 4}});
        assertNull(listener.getAndResetLastOldData());

        sendEvent("E1", 5);
        sendEvent("E4", 7);
        sendEvent("E1", 6);
        assertFalse(listener.isInvoked());

        sendEvent("E5", 9);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][] {{"E1", 5}, {"E4", 7}, {"E5", 9}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastOldData(), fields, new Object[][] {{"E1", 1}, {"E2", 2}, {"E3", 4}});
        listener.reset();
    }
}
