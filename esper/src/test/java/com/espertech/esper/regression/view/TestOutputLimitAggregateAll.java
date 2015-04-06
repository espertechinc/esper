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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.regression.support.ResultAssertTestResult;
import com.espertech.esper.regression.support.ResultAssertExecution;
import junit.framework.TestCase;

public class TestOutputLimitAggregateAll extends TestCase
{
    private static final String EVENT_NAME = SupportMarketDataBean.class.getName();
    private final static String JOIN_KEY = "KEY";

    private SupportUpdateListener listener;
	private EPServiceProvider epService;
    private long currentTime;
    private final static String CATEGORY = "Aggregated and Un-grouped";

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MarketData", SupportMarketDataBean.class);
        config.addEventType("SupportBean", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void test1NoneNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec)";
        runAssertion12(stmtText, "none");
    }

    public void test2NoneNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol";
        runAssertion12(stmtText, "none");
    }

    public void test3NoneHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            " having sum(price) > 100";
        runAssertion34(stmtText, "none");
    }

    public void test4NoneHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            " having sum(price) > 100";
        runAssertion34(stmtText, "none");
    }

    public void test5DefaultNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "output every 1 seconds";
        runAssertion56(stmtText, "default");
    }

    public void test6DefaultNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "output every 1 seconds";
        runAssertion56(stmtText, "default");
    }

    public void test7DefaultHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) \n" +
                            "having sum(price) > 100" +
                            "output every 1 seconds";
        runAssertion78(stmtText, "default");
    }

    public void test8DefaultHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "having sum(price) > 100" +
                            "output every 1 seconds";
        runAssertion78(stmtText, "default");
    }

    public void test9AllNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "output all every 1 seconds";
        runAssertion56(stmtText, "all");
    }

    public void test10AllNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "output all every 1 seconds";
        runAssertion56(stmtText, "all");
    }

    public void test11AllHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "having sum(price) > 100" +
                            "output all every 1 seconds";
        runAssertion78(stmtText, "all");
    }

    public void test12AllHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "having sum(price) > 100" +
                            "output all every 1 seconds";
        runAssertion78(stmtText, "all");
    }

    public void test13LastNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec)" +
                            "output last every 1 seconds";
        runAssertion13_14(stmtText, "last");
    }

    public void test14LastNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "output last every 1 seconds";
        runAssertion13_14(stmtText, "last");
    }

    public void test15LastHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec)" +
                            "having sum(price) > 100 " +
                            "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test16LastHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "having sum(price) > 100 " +
                            "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test17FirstNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "output first every 1 seconds";
        runAssertion17(stmtText, "first");
    }

    public void test18SnapshotNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "output snapshot every 1 seconds";
        runAssertion18(stmtText, "first");
    }

    private void runAssertion12(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][] {{"IBM", 25d}});
        expected.addResultInsert(800, 1, new Object[][] {{"MSFT", 34d}});
        expected.addResultInsert(1500, 1, new Object[][] {{"IBM", 58d}});
        expected.addResultInsert(1500, 2, new Object[][] {{"YAH", 59d}});
        expected.addResultInsert(2100, 1, new Object[][] {{"IBM", 85d}});
        expected.addResultInsert(3500, 1, new Object[][] {{"YAH", 87d}});
        expected.addResultInsert(4300, 1, new Object[][] {{"IBM", 109d}});
        expected.addResultInsert(4900, 1, new Object[][] {{"YAH", 112d}});
        expected.addResultRemove(5700, 0, new Object[][] {{"IBM", 87d}});
        expected.addResultInsert(5900, 1, new Object[][] {{"YAH", 88d}});
        expected.addResultRemove(6300, 0, new Object[][] {{"MSFT", 79d}});
        expected.addResultRemove(7000, 0, new Object[][] {{"IBM", 54d}, {"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion34(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(4300, 1, new Object[][] {{"IBM", 109d}});
        expected.addResultInsert(4900, 1, new Object[][] {{"YAH", 112d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion13_14(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"MSFT", 34d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 85d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][] {{"YAH", 87d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"YAH", 112d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"YAH", 88d}}, new Object[][] {{"IBM", 87d}});
        expected.addResultRemove(7200, 0, new Object[][] {{"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion15_16(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, null, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][] {{"YAH", 112d}});
        expected.addResultInsRem(6200, 0, null, null);
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion78(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, null, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsRem(5200, 0, new Object[][] {{"IBM", 109d}, {"YAH", 112d}}, null);
        expected.addResultInsRem(6200, 0, null, null);
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion56(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 25d}, {"MSFT", 34d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 58d}, {"YAH", 59d}, {"IBM", 85d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][] {{"YAH", 87d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 109d}, {"YAH", 112d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"YAH", 88d}}, new Object[][] {{"IBM", 87d}});
        expected.addResultRemove(7200, 0, new Object[][] {{"MSFT", 79d}, {"IBM", 54d}, {"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion17(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][] {{"IBM", 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 58d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(3500, 1, new Object[][] {{"YAH", 87d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 109d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 87d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 79d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion18(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 34d}, {"MSFT", 34d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 85d}, {"MSFT", 85d}, {"IBM", 85d}, {"YAH", 85d}, {"IBM", 85d}});
        expected.addResultInsert(3200, 0, new Object[][] {{"IBM", 85d}, {"MSFT", 85d}, {"IBM", 85d}, {"YAH", 85d}, {"IBM", 85d}});
        expected.addResultInsert(4200, 0, new Object[][] {{"IBM", 87d}, {"MSFT", 87d}, {"IBM", 87d}, {"YAH", 87d}, {"IBM", 87d}, {"YAH", 87d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 112d}, {"MSFT", 112d}, {"IBM", 112d}, {"YAH", 112d}, {"IBM", 112d}, {"YAH", 112d}, {"IBM", 112d}, {"YAH", 112d}});
        expected.addResultInsert(6200, 0, new Object[][] {{"MSFT", 88d}, {"IBM", 88d}, {"YAH", 88d}, {"IBM", 88d}, {"YAH", 88d}, {"IBM", 88d}, {"YAH", 88d}, {"YAH", 88d}});
        expected.addResultInsert(7200, 0, new Object[][] {{"IBM", 54d}, {"YAH", 54d}, {"IBM", 54d}, {"YAH", 54d}, {"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    public void testHaving()
    {
        sendTimer(0);

        String viewExpr = "select symbol, avg(price) as avgPrice " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:time(3 sec) " +
                          "having avg(price) > 10" +
                          "output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        stmt.addListener(listener);

        runHavingAssertion();
    }

    public void testHavingJoin()
    {
        sendTimer(0);

        String viewExpr = "select symbol, avg(price) as avgPrice " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:time(3 sec) as md, " +
                          SupportBean.class.getName() + ".win:keepall() as s where s.theString = md.symbol " +
                          "having avg(price) > 10" +
                          "output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("SYM1", -1));

        runHavingAssertion();
    }

    private void runHavingAssertion()
    {
        sendEvent("SYM1", 10d);
        sendEvent("SYM1", 11d);
        sendEvent("SYM1", 9);

        sendTimer(1000);
        String fields[] = "symbol,avgPrice".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"SYM1", 10.5});

        sendEvent("SYM1", 13d);
        sendEvent("SYM1", 10d);
        sendEvent("SYM1", 9);
        sendTimer(2000);

        assertEquals(3, listener.getLastNewData().length);
        assertNull(listener.getLastOldData());
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"SYM1", 43 / 4.0}, {"SYM1", 53.0 / 5.0}, {"SYM1", 62 / 6.0}});
    }

    public void testMaxTimeWindow()
    {
        sendTimer(0);

        String viewExpr = "select irstream volume, max(price) as maxVol" +
                          " from " + SupportMarketDataBean.class.getName() + ".win:time(1 sec) " +
                          "output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        stmt.addListener(listener);

        sendEvent("SYM1", 1d);
        sendEvent("SYM1", 2d);
        listener.reset();

        // moves all events out of the window,
        sendTimer(1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
        UniformPair<EventBean[]> result = listener.getDataListsFlattened();
        assertEquals(2, result.getFirst().length);
        assertEquals(1.0, result.getFirst()[0].get("maxVol"));
        assertEquals(2.0, result.getFirst()[1].get("maxVol"));
        assertEquals(2, result.getSecond().length);
        assertEquals(null, result.getSecond()[0].get("maxVol"));
        assertEquals(null, result.getSecond()[1].get("maxVol"));
    }

    public void testLimitSnapshot()
    {
        sendTimer(0);
        String selectStmt = "select symbol, sum(price) as sumprice from " + SupportMarketDataBean.class.getName() +
                ".win:time(10 seconds) output snapshot every 1 seconds order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);
        sendEvent("ABC", 20);

        sendTimer(500);
        sendEvent("IBM", 16);
        sendEvent("MSFT", 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(1000);
        String fields[] = new String[] {"symbol", "sumprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 50d}, {"IBM", 50d}, {"MSFT", 50d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(1500);
        sendEvent("YAH", 18);
        sendEvent("s4", 30);

        sendTimer(10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 98d}, {"IBM", 98d}, {"MSFT", 98d}, {"YAH", 98d}, {"s4", 98d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"YAH", 48d}, {"s4", 48d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(12000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(13000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();
    }

    public void testLimitSnapshotJoin()
    {
        sendTimer(0);
        String selectStmt = "select symbol, sum(price) as sumprice from " + SupportMarketDataBean.class.getName() +
                ".win:time(10 seconds) as m, " + SupportBean.class.getName() +
                ".win:keepall() as s where s.theString = m.symbol output snapshot every 1 seconds order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("ABC", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("MSFT", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("YAH", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("s4", 5));

        sendEvent("ABC", 20);

        sendTimer(500);
        sendEvent("IBM", 16);
        sendEvent("MSFT", 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(1000);
        String fields[] = new String[] {"symbol", "sumprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 50d}, {"IBM", 50d}, {"MSFT", 50d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(1500);
        sendEvent("YAH", 18);
        sendEvent("s4", 30);

        sendTimer(10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 98d}, {"IBM", 98d}, {"MSFT", 98d}, {"YAH", 98d}, {"s4", 98d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(10500);
        sendTimer(11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"YAH", 48d}, {"s4", 48d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(11500);
        sendTimer(12000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(13000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();
    }

    public void testJoinSortWindow()
    {
        sendTimer(0);

        String viewExpr = "select irstream volume, max(price) as maxVol" +
                          " from " + SupportMarketDataBean.class.getName() + ".ext:sort(1, volume desc) as s0," +
                          SupportBean.class.getName() + ".win:keepall() as s1 " +
                          "output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("JOIN_KEY", -1));

        sendEvent("JOIN_KEY", 1d);
        sendEvent("JOIN_KEY", 2d);
        listener.reset();

        // moves all events out of the window,
        sendTimer(1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
        UniformPair<EventBean[]> result = listener.getDataListsFlattened();
        assertEquals(2, result.getFirst().length);
        assertEquals(1.0, result.getFirst()[0].get("maxVol"));
        assertEquals(2.0, result.getFirst()[1].get("maxVol"));
        assertEquals(1, result.getSecond().length);
        assertEquals(2.0, result.getSecond()[0].get("maxVol"));
    }

	public void testAggregateAllNoJoinLast()
	{
	    String viewExpr = "select longBoxed, sum(longBoxed) as result " +
	    "from " + SupportBean.class.getName() + ".win:length(3) " +
	    "having sum(longBoxed) > 0 " +
	    "output last every 2 events";

	    runAssertLastSum(createStmtAndListenerNoJoin(viewExpr));

	    viewExpr = "select longBoxed, sum(longBoxed) as result " +
	    "from " + SupportBean.class.getName() + ".win:length(3) " +
	    "output last every 2 events";

	    runAssertLastSum(createStmtAndListenerNoJoin(viewExpr));
	}

	public void testAggregateAllJoinAll()
	{
	    String viewExpr = "select longBoxed, sum(longBoxed) as result " +
                        "from " + SupportBeanString.class.getName() + ".win:length(3) as one, " +
                        SupportBean.class.getName() + ".win:length(3) as two " +
                        "having sum(longBoxed) > 0 " +
                        "output all every 2 events";

	    runAssertAllSum(createStmtAndListenerJoin(viewExpr));

	    viewExpr = "select longBoxed, sum(longBoxed) as result " +
                    "from " + SupportBeanString.class.getName() + ".win:length(3) as one, " +
                    SupportBean.class.getName() + ".win:length(3) as two " +
                    "output every 2 events";

	    runAssertAllSum(createStmtAndListenerJoin(viewExpr));
	}

	public void testAggregateAllJoinLast()
    {
        String viewExpr = "select longBoxed, sum(longBoxed) as result " +
        "from " + SupportBeanString.class.getName() + ".win:length(3) as one, " +
        SupportBean.class.getName() + ".win:length(3) as two " +
        "having sum(longBoxed) > 0 " +
        "output last every 2 events";

        runAssertLastSum(createStmtAndListenerJoin(viewExpr));

        viewExpr = "select longBoxed, sum(longBoxed) as result " +
        "from " + SupportBeanString.class.getName() + ".win:length(3) as one, " +
        SupportBean.class.getName() + ".win:length(3) as two " +
        "output last every 2 events";

        runAssertLastSum(createStmtAndListenerJoin(viewExpr));
    }

    public void testTime()
    {
        // Set the clock to 0
        currentTime = 0;
        sendTimeEventRelative(0);

        // Create the EPL statement and add a listener
        String statementText = "select symbol, sum(volume) from " + EVENT_NAME + ".win:length(5) output first every 3 seconds";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);
        updateListener.reset();

        // Send the first event of the batch; should be output
        sendMarketDataEvent(10L);
        assertEvent(updateListener, 10L);

        // Send another event, not the first, for aggregation
        // update only, no output
        sendMarketDataEvent(20L);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Update time
        sendTimeEventRelative(3000);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Send first event of the next batch, should be output.
        // The aggregate value is computed over all events
        // received: 10 + 20 + 30 = 60
        sendMarketDataEvent(30L);
        assertEvent(updateListener, 60L);

        // Send the next event of the batch, no output
        sendMarketDataEvent(40L);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Update time
        sendTimeEventRelative(3000);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Send first event of third batch
        sendMarketDataEvent(1L);
        assertEvent(updateListener, 101L);

        // Update time
        sendTimeEventRelative(3000);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Update time: no first event this batch, so a callback
        // is made at the end of the interval
        sendTimeEventRelative(3000);
        assertTrue(updateListener.getAndClearIsInvoked());
        assertNull(updateListener.getLastNewData());
        assertNull(updateListener.getLastOldData());
    }

    public void testCount()
    {
        // Create the EPL statement and add a listener
        String statementText = "select symbol, sum(volume) from " + EVENT_NAME + ".win:length(5) output first every 3 events";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);
        updateListener.reset();

        // Send the first event of the batch, should be output
        sendEventLong(10L);
        assertEvent(updateListener, 10L);

        // Send the second event of the batch, not output, used
        // for updating the aggregate value only
        sendEventLong(20L);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Send the third event of the batch, still not output,
        // but should reset the batch
        sendEventLong(30L);
        assertFalse(updateListener.getAndClearIsInvoked());

        // First event, next batch, aggregate value should be
        // 10 + 20 + 30 + 40 = 100
        sendEventLong(40L);
        assertEvent(updateListener, 100L);

        // Next event again not output
        sendEventLong(50L);
        assertFalse(updateListener.getAndClearIsInvoked());
    }

    private void sendEventLong(long volume)
    {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("DELL", 0.0, volume, null));
    }

    private SupportUpdateListener createStmtAndListenerNoJoin(String viewExpr) {
		epService.initialize();
		SupportUpdateListener updateListener = new SupportUpdateListener();
		EPStatement view = epService.getEPAdministrator().createEPL(viewExpr);
	    view.addListener(updateListener);

	    return updateListener;
	}

	private void runAssertAllSum(SupportUpdateListener updateListener)
	{
		// send an event
	    sendEvent(1);

	    // check no update
	    assertFalse(updateListener.getAndClearIsInvoked());

	    // send another event
	    sendEvent(2);

	    // check update, all events present
	    assertTrue(updateListener.getAndClearIsInvoked());
	    assertEquals(2, updateListener.getLastNewData().length);
	    assertEquals(1L, updateListener.getLastNewData()[0].get("longBoxed"));
	    assertEquals(1L, updateListener.getLastNewData()[0].get("result"));
	    assertEquals(2L, updateListener.getLastNewData()[1].get("longBoxed"));
	    assertEquals(3L, updateListener.getLastNewData()[1].get("result"));
	    assertNull(updateListener.getLastOldData());
	}

	private void runAssertLastSum(SupportUpdateListener updateListener)
	{
		// send an event
	    sendEvent(1);

	    // check no update
	    assertFalse(updateListener.getAndClearIsInvoked());

	    // send another event
	    sendEvent(2);

	    // check update, all events present
	    assertTrue(updateListener.getAndClearIsInvoked());
	    assertEquals(1, updateListener.getLastNewData().length);
	    assertEquals(2L, updateListener.getLastNewData()[0].get("longBoxed"));
	    assertEquals(3L, updateListener.getLastNewData()[0].get("result"));
	    assertNull(updateListener.getLastOldData());
	}

    private void sendEvent(long longBoxed, int intBoxed, short shortBoxed)
	{
	    SupportBean bean = new SupportBean();
	    bean.setTheString(JOIN_KEY);
	    bean.setLongBoxed(longBoxed);
	    bean.setIntBoxed(intBoxed);
	    bean.setShortBoxed(shortBoxed);
	    epService.getEPRuntime().sendEvent(bean);
	}

	private void sendEvent(long longBoxed)
	{
	    sendEvent(longBoxed, 0, (short)0);
	}

    private void sendMarketDataEvent(long volume)
    {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("SYM1", 0, volume, null));
    }

    private void sendTimeEventRelative(int timeIncrement){
        currentTime += timeIncrement;
        CurrentTimeEvent theEvent = new CurrentTimeEvent(currentTime);
        epService.getEPRuntime().sendEvent(theEvent);
    }

	private SupportUpdateListener createStmtAndListenerJoin(String viewExpr) {
		epService.initialize();

		SupportUpdateListener updateListener = new SupportUpdateListener();
		EPStatement view = epService.getEPAdministrator().createEPL(viewExpr);
	    view.addListener(updateListener);

	    epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));

	    return updateListener;
	}

    private void assertEvent(SupportUpdateListener updateListener, long volume)
    {
        assertTrue(updateListener.getAndClearIsInvoked());
        assertTrue(updateListener.getLastNewData() != null);
        assertEquals(1, updateListener.getLastNewData().length);
        assertEquals(volume, updateListener.getLastNewData()[0].get("sum(volume)"));
    }

    private void sendEvent(String symbol, double price)
	{
	    SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
	    epService.getEPRuntime().sendEvent(bean);
	}

    private void sendTimer(long time)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
