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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.regression.support.ResultAssertExecution;
import com.espertech.esper.regression.support.ResultAssertTestResult;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestOutputLimitSimple extends TestCase
{
    private final static String JOIN_KEY = "KEY";
    private final static String CATEGORY = "Un-aggregated and Un-grouped";

    private EPServiceProvider epService;
    private long currentTime;
    private SupportUpdateListener listener;

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
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec)";
        runAssertion12(stmtText, "none");
    }

    public void test2NoneNoHavingJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol";
        runAssertion12(stmtText, "none");
    }

    public void test3NoneHavingNoJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec) " +
                            " having price > 10";
        runAssertion34(stmtText, "none");
    }

    public void test4NoneHavingJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            " having price > 10";
        runAssertion34(stmtText, "none");
    }

    public void test5DefaultNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec) " +
                            "output every 1 seconds";
        runAssertion56(stmtText, "default");
    }
    
    public void test6DefaultNoHavingJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "output every 1 seconds";
        runAssertion56(stmtText, "default");
    }

    public void test7DefaultHavingNoJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec) \n" +
                            "having price > 10" +
                            "output every 1 seconds";
        runAssertion78(stmtText, "default");
    }

    public void test8DefaultHavingJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "having price > 10" +
                            "output every 1 seconds";
        runAssertion78(stmtText, "default");
    }

    public void test9AllNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec) " +
                            "output all every 1 seconds";
        runAssertion56(stmtText, "all");
    }

    public void test10AllNoHavingJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "output all every 1 seconds";
        runAssertion56(stmtText, "all");
    }

    public void test11AllHavingNoJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec) " +
                            "having price > 10" +
                            "output all every 1 seconds";
        runAssertion78(stmtText, "all");
    }

    public void test12AllHavingJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "having price > 10" +
                            "output all every 1 seconds";
        runAssertion78(stmtText, "all");
    }

    public void test13LastNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec)" +
                            "output last every 1 seconds";
        runAssertion13_14(stmtText, "last");
    }

    public void test14LastNoHavingJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "output last every 1 seconds";
        runAssertion13_14(stmtText, "last");
    }

    public void test15LastHavingNoJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec)" +
                            "having price > 10 " +
                            "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test16LastHavingJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "having price > 10 " +
                            "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test17FirstNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec) " +
                            "output first every 1 seconds";
        runAssertion17(stmtText, "first");
    }

    public void test18SnapshotNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, price " +
                            "from MarketData.win:time(5.5 sec) " +
                            "output snapshot every 1 seconds";
        runAssertion18(stmtText, "first");
    }

    public void testOutputEveryTimePeriod()
    {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));

        String stmtText = "select symbol from MarketData.win:keepall() output snapshot every 1 day 2 hours 3 minutes 4 seconds 5 milliseconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        sendMDEvent("E1", 0);

        long deltaSec = 26 * 60 * 60 + 3 * 60 + 4;
        long deltaMSec = deltaSec * 1000 + 5 + 2000;
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(deltaMSec - 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(deltaMSec));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("symbol"));
    }

    public void testOutputEveryTimePeriodVariable()
    {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        epService.getEPAdministrator().getConfiguration().addVariable("D", int.class, 1);
        epService.getEPAdministrator().getConfiguration().addVariable("H", int.class, 2);
        epService.getEPAdministrator().getConfiguration().addVariable("M", int.class, 3);
        epService.getEPAdministrator().getConfiguration().addVariable("S", int.class, 4);
        epService.getEPAdministrator().getConfiguration().addVariable("MS", int.class, 5);

        String stmtText = "select symbol from MarketData.win:keepall() output snapshot every D days H hours M minutes S seconds MS milliseconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        sendMDEvent("E1", 0);

        long deltaSec = 26 * 60 * 60 + 3 * 60 + 4;
        long deltaMSec = deltaSec * 1000 + 5 + 2000;
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(deltaMSec - 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(deltaMSec));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("symbol"));

        // test statement model
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
    }

    private void runAssertion34(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        String fields[] = new String[] {"symbol", "volume", "price"};

        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultInsert(1500, 1, new Object[][] {{"IBM", 150L, 24d}});
        expected.addResultInsert(2100, 1, new Object[][] {{"IBM", 155L, 26d}});
        expected.addResultInsert(4300, 1, new Object[][] {{"IBM", 150L, 22d}});
        expected.addResultRemove(5700, 0, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultRemove(7000, 0, new Object[][] {{"IBM", 150L, 24d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion15_16(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);

        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 150L, 22d}});
        expected.addResultInsRem(6200, 0, null, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][] {{"IBM", 150L, 24d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion12(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultInsert(800, 1, new Object[][] {{"MSFT", 5000L, 9d}});
        expected.addResultInsert(1500, 1, new Object[][] {{"IBM", 150L, 24d}});
        expected.addResultInsert(1500, 2, new Object[][] {{"YAH", 10000L, 1d}});
        expected.addResultInsert(2100, 1, new Object[][] {{"IBM", 155L, 26d}});
        expected.addResultInsert(3500, 1, new Object[][] {{"YAH", 11000L, 2d}});
        expected.addResultInsert(4300, 1, new Object[][] {{"IBM", 150L, 22d}});
        expected.addResultInsert(4900, 1, new Object[][] {{"YAH", 11500L, 3d}});
        expected.addResultRemove(5700, 0, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultInsert(5900, 1, new Object[][] {{"YAH", 10500L, 1d}});
        expected.addResultRemove(6300, 0, new Object[][] {{"MSFT", 5000L, 9d}});
        expected.addResultRemove(7000, 0, new Object[][] {{"IBM", 150L, 24d}, {"YAH", 10000L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion13_14(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][] {{"YAH", 11000L, 2d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"YAH", 11500L, 3d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"YAH", 10500L, 1d}}, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][] {{"YAH", 10000L, 1d}, });

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion78(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 150L, 24d}, {"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 150L, 22d}});
        expected.addResultInsRem(6200, 0, null, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][] {{"IBM", 150L, 24d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion56(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][] {{"YAH", 11000L, 2d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 150L, 22d}, {"YAH", 11500L, 3d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"YAH", 10500L, 1d}}, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][] {{"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, });

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion17(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultInsert(1500, 1, new Object[][] {{"IBM", 150L, 24d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(3500, 1, new Object[][] {{"YAH", 11000L, 2d}});
        expected.addResultInsert(4300, 1, new Object[][] {{"IBM", 150L, 22d}});
        expected.addResultRemove(5700, 0, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultRemove(6300, 0, new Object[][] {{"MSFT", 5000L, 9d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    private void runAssertion18(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}});
        expected.addResultInsert(3200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}});
        expected.addResultInsert(4200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}, {"YAH", 11000L, 2d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}, {"YAH", 11000L, 2d}, {"IBM", 150L, 22d}, {"YAH", 11500L, 3d}});
        expected.addResultInsert(6200, 0, new Object[][] {{"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}, {"YAH", 11000L, 2d}, {"IBM", 150L, 22d}, {"YAH", 11500L, 3d}, {"YAH", 10500L, 1d}});
        expected.addResultInsert(7200, 0, new Object[][] {{"IBM", 155L, 26d}, {"YAH", 11000L, 2d}, {"IBM", 150L, 22d}, {"YAH", 11500L, 3d}, {"YAH", 10500L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute();
    }

    public void testAggAllHaving()
    {
        String stmtText = "select symbol, volume " +
                            "from " + SupportMarketDataBean.class.getName() + ".win:length(10) as two " +
                            "having volume > 0 " +
                            "output every 5 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String fields[] = new String[] {"symbol", "volume"};

        sendMDEvent("S0", 20);
        sendMDEvent("IBM", -1);
        sendMDEvent("MSFT", -2);
        sendMDEvent("YAH", 10);
        assertFalse(listener.isInvoked());

        sendMDEvent("IBM", 0);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"S0", 20L}, {"YAH", 10L}});
        listener.reset();
    }

    public void testAggAllHavingJoin()
    {
        String stmtText = "select symbol, volume " +
                            "from " + SupportMarketDataBean.class.getName() + ".win:length(10) as one," +
                            SupportBean.class.getName() + ".win:length(10) as two " +
                            "where one.symbol=two.theString " +
                            "having volume > 0 " +
                            "output every 5 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String fields[] = new String[] {"symbol", "volume"};
        epService.getEPRuntime().sendEvent(new SupportBean("S0", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("MSFT", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("YAH", 0));

        sendMDEvent("S0", 20);
        sendMDEvent("IBM", -1);
        sendMDEvent("MSFT", -2);
        sendMDEvent("YAH", 10);
        assertFalse(listener.isInvoked());

        sendMDEvent("IBM", 0);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"S0", 20L}, {"YAH", 10L}});
        listener.reset();
    }

    public void testIterator()
	{
        String[] fields = new String[] {"symbol", "price"};
        String statementString = "select symbol, theString, price from " +
    	            SupportMarketDataBean.class.getName() + ".win:length(10) as one, " +
    	            SupportBeanString.class.getName() + ".win:length(100) as two " +
                    "where one.symbol = two.theString " +
                    "output every 3 events";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));

        // Output limit clause ignored when iterating, for both joins and no-join
        sendEvent("CAT", 50);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{"CAT", 50d}});

        sendEvent("CAT", 60);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, new Object[][]{{"CAT", 50d}, {"CAT", 60d}});

        sendEvent("IBM", 70);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, new Object[][]{{"CAT", 50d}, {"CAT", 60d}, {"IBM", 70d}});

        sendEvent("IBM", 90);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, new Object[][]{{"CAT", 50d}, {"CAT", 60d}, {"IBM", 70d}, {"IBM", 90d}});
    }

    public void testLimitEventJoin()
	{
		String eventName1 = SupportBean.class.getName();
		String eventName2 = SupportBean_A.class.getName();
		String joinStatement =
			"select * from " +
				eventName1 + ".win:length(5) as event1," +
				eventName2 + ".win:length(5) as event2" +
			" where event1.theString = event2.id";
		String outputStmt1 = joinStatement + " output every 1 events";
	   	String outputStmt3 = joinStatement + " output every 3 events";

	   	EPStatement fireEvery1 = epService.getEPAdministrator().createEPL(outputStmt1);
		EPStatement fireEvery3 = epService.getEPAdministrator().createEPL(outputStmt3);

	   	SupportUpdateListener updateListener1 = new SupportUpdateListener();
		fireEvery1.addListener(updateListener1);
		SupportUpdateListener updateListener3 = new SupportUpdateListener();
		fireEvery3.addListener(updateListener3);

		// send event 1
		sendJoinEvents("IBM");

		assertTrue(updateListener1.getAndClearIsInvoked());
		assertEquals(1, updateListener1.getLastNewData().length);
		assertNull(updateListener1.getLastOldData());

		assertFalse(updateListener3.getAndClearIsInvoked());
		assertNull(updateListener3.getLastNewData());
		assertNull(updateListener3.getLastOldData());

		// send event 2
		sendJoinEvents("MSFT");

		assertTrue(updateListener1.getAndClearIsInvoked());
		assertEquals(1, updateListener1.getLastNewData().length);
		assertNull(updateListener1.getLastOldData());

	   	assertFalse(updateListener3.getAndClearIsInvoked());
		assertNull(updateListener3.getLastNewData());
		assertNull(updateListener3.getLastOldData());

		// send event 3
		sendJoinEvents("YAH");

		assertTrue(updateListener1.getAndClearIsInvoked());
		assertEquals(1, updateListener1.getLastNewData().length);
		assertNull(updateListener1.getLastOldData());

		assertTrue(updateListener3.getAndClearIsInvoked());
		assertEquals(3, updateListener3.getLastNewData().length);
		assertNull(updateListener3.getLastOldData());
	}

    public void testLimitTime(){
    	String eventName = SupportBean.class.getName();
    	String selectStatement = "select * from " + eventName + ".win:length(5)";

    	// test integer seconds
    	String statementString1 = selectStatement +
    		" output every 3 seconds";
    	timeCallback(statementString1, 3000);

    	// test fractional seconds
    	String statementString2 = selectStatement +
    	" output every 3.3 seconds";
    	timeCallback(statementString2, 3300);

    	// test integer minutes
    	String statementString3 = selectStatement +
    	" output every 2 minutes";
    	timeCallback(statementString3, 120000);

    	// test fractional minutes
    	String statementString4 =
    		"select * from " +
    			eventName + ".win:length(5)" +
    		" output every .05 minutes";
    	timeCallback(statementString4, 3000);
    }

    public void testTimeBatchOutputEvents()
    {
        String stmtText = "select * from " + SupportBean.class.getName() + ".win:time_batch(10 seconds) output every 10 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(0);
        sendTimer(10000);
        assertFalse(listener.isInvoked());
        sendTimer(20000);
        assertFalse(listener.isInvoked());

        sendEvent("e1");
        sendTimer(30000);
        assertFalse(listener.isInvoked());
        sendTimer(40000);
        EventBean[] newEvents = listener.getAndResetLastNewData();
        assertEquals(1, newEvents.length);
        assertEquals("e1", newEvents[0].get("theString"));
        listener.reset();

        sendTimer(50000);
        assertTrue(listener.isInvoked());
        listener.reset();

        sendTimer(60000);
        assertTrue(listener.isInvoked());
        listener.reset();

        sendTimer(70000);
        assertTrue(listener.isInvoked());
        listener.reset();

        sendEvent("e2");
        sendEvent("e3");
        sendTimer(80000);
        newEvents = listener.getAndResetLastNewData();
        assertEquals(2, newEvents.length);
        assertEquals("e2", newEvents[0].get("theString"));
        assertEquals("e3", newEvents[1].get("theString"));

        sendTimer(90000);
        assertTrue(listener.isInvoked());
        listener.reset();
    }

    public void testSimpleNoJoinAll()
	{
	    String viewExpr = "select longBoxed " +
	    "from " + SupportBean.class.getName() + ".win:length(3) " +
	    "output all every 2 events";

	    runAssertAll(createStmtAndListenerNoJoin(viewExpr));

	    viewExpr = "select longBoxed " +
	    "from " + SupportBean.class.getName() + ".win:length(3) " +
	    "output every 2 events";

	    runAssertAll(createStmtAndListenerNoJoin(viewExpr));

	    viewExpr = "select * " +
	    "from " + SupportBean.class.getName() + ".win:length(3) " +
	    "output every 2 events";

	    runAssertAll(createStmtAndListenerNoJoin(viewExpr));
	}

	public void testSimpleNoJoinLast()
    {
        String viewExpr = "select longBoxed " +
        "from " + SupportBean.class.getName() + ".win:length(3) " +
        "output last every 2 events";

        runAssertLast(createStmtAndListenerNoJoin(viewExpr));

        viewExpr = "select * " +
        "from " + SupportBean.class.getName() + ".win:length(3) " +
        "output last every 2 events";

        runAssertLast(createStmtAndListenerNoJoin(viewExpr));
    }

    public void testSimpleJoinAll()
	{
	    String viewExpr = "select longBoxed  " +
	    "from " + SupportBeanString.class.getName() + ".win:length(3) as one, " +
	    SupportBean.class.getName() + ".win:length(3) as two " +
	    "output all every 2 events";

		runAssertAll(createStmtAndListenerJoin(viewExpr));
	}

    private SupportUpdateListener createStmtAndListenerNoJoin(String viewExpr) {
		epService.initialize();
		SupportUpdateListener updateListener = new SupportUpdateListener();
		EPStatement view = epService.getEPAdministrator().createEPL(viewExpr);
	    view.addListener(updateListener);

	    return updateListener;
	}

	private void runAssertAll(SupportUpdateListener updateListener)
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
	    assertEquals(2L, updateListener.getLastNewData()[1].get("longBoxed"));
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

	public void testSimpleJoinLast()
	{
	    String viewExpr = "select longBoxed " +
	    "from " + SupportBeanString.class.getName() + ".win:length(3) as one, " +
	    SupportBean.class.getName() + ".win:length(3) as two " +
	    "output last every 2 events";

		runAssertLast(createStmtAndListenerJoin(viewExpr));
	}

    public void testLimitEventSimple()
    {
        SupportUpdateListener updateListener1 = new SupportUpdateListener();
        SupportUpdateListener updateListener2 = new SupportUpdateListener();
        SupportUpdateListener updateListener3 = new SupportUpdateListener();

        String eventName = SupportBean.class.getName();
        String selectStmt = "select * from " + eventName + ".win:length(5)";
        String statement1 = selectStmt +
            " output every 1 events";
        String statement2 = selectStmt +
            " output every 2 events";
        String statement3 = selectStmt +
            " output every 3 events";

        EPStatement rateLimitStmt1 = epService.getEPAdministrator().createEPL(statement1);
        rateLimitStmt1.addListener(updateListener1);
        EPStatement rateLimitStmt2 = epService.getEPAdministrator().createEPL(statement2);
        rateLimitStmt2.addListener(updateListener2);
        EPStatement rateLimitStmt3 = epService.getEPAdministrator().createEPL(statement3);
        rateLimitStmt3.addListener(updateListener3);

        // send event 1
        sendEvent("IBM");

        assertTrue(updateListener1.getAndClearIsInvoked());
        assertEquals(1,updateListener1.getLastNewData().length);
        assertNull(updateListener1.getLastOldData());

        assertFalse(updateListener2.getAndClearIsInvoked());
        assertNull(updateListener2.getLastNewData());
        assertNull(updateListener2.getLastOldData());

        assertFalse(updateListener3.getAndClearIsInvoked());
        assertNull(updateListener3.getLastNewData());
        assertNull(updateListener3.getLastOldData());

        // send event 2
        sendEvent("MSFT");

        assertTrue(updateListener1.getAndClearIsInvoked());
        assertEquals(1,updateListener1.getLastNewData().length);
        assertNull(updateListener1.getLastOldData());

        assertTrue(updateListener2.getAndClearIsInvoked());
        assertEquals(2,updateListener2.getLastNewData().length);
        assertNull(updateListener2.getLastOldData());

        assertFalse(updateListener3.getAndClearIsInvoked());

        // send event 3
        sendEvent("YAH");

        assertTrue(updateListener1.getAndClearIsInvoked());
        assertEquals(1,updateListener1.getLastNewData().length);
        assertNull(updateListener1.getLastOldData());

        assertFalse(updateListener2.getAndClearIsInvoked());

        assertTrue(updateListener3.getAndClearIsInvoked());
        assertEquals(3,updateListener3.getLastNewData().length);
        assertNull(updateListener3.getLastOldData());
    }

    public void testLimitSnapshot()
    {
        SupportUpdateListener listener = new SupportUpdateListener();

        sendTimer(0);
        String selectStmt = "select * from " + SupportBean.class.getName() + ".win:time(10) output snapshot every 3 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);

        sendTimer(1000);
        sendEvent("IBM");
        sendEvent("MSFT");
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(2000);
        sendEvent("YAH");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"IBM"}, {"MSFT"}, {"YAH"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(3000);
        sendEvent("s4");
        sendEvent("s5");
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(10000);
        sendEvent("s6");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"IBM"}, {"MSFT"}, {"YAH"}, {"s4"}, {"s5"}, {"s6"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(11000);
        sendEvent("s7");
        assertFalse(listener.isInvoked());

        sendEvent("s8");
        assertFalse(listener.isInvoked());

        sendEvent("s9");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"YAH"}, {"s4"}, {"s5"}, {"s6"}, {"s7"}, {"s8"}, {"s9"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(14000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s6"}, {"s7"}, {"s8"}, {"s9"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent("s10");
        sendEvent("s11");
        assertFalse(listener.isInvoked());

        sendTimer(23000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s10"}, {"s11"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent("s12");
        assertFalse(listener.isInvoked());
    }

    public void testLimitSnapshotJoin()
    {
        SupportUpdateListener listener = new SupportUpdateListener();

        sendTimer(0);
        String selectStmt = "select theString from " + SupportBean.class.getName() + ".win:time(10) as s," +
                SupportMarketDataBean.class.getName() + ".win:keepall() as m where s.theString = m.symbol output snapshot every 3 events order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);

        for (String symbol : "s0,s1,s2,s3,s4,s5,s6,s7,s8,s9,s10,s11".split(","))
        {
            epService.getEPRuntime().sendEvent(new SupportMarketDataBean(symbol, 0, 0L, ""));
        }

        sendTimer(1000);
        sendEvent("s0");
        sendEvent("s1");
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(2000);
        sendEvent("s2");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s0"}, {"s1"}, {"s2"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(3000);
        sendEvent("s4");
        sendEvent("s5");
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(10000);
        sendEvent("s6");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s0"}, {"s1"}, {"s2"}, {"s4"}, {"s5"}, {"s6"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(11000);
        sendEvent("s7");
        assertFalse(listener.isInvoked());

        sendEvent("s8");
        assertFalse(listener.isInvoked());

        sendEvent("s9");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s2"}, {"s4"}, {"s5"}, {"s6"}, {"s7"}, {"s8"}, {"s9"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(14000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s6"}, {"s7"}, {"s8"}, {"s9"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent("s10");
        sendEvent("s11");
        assertFalse(listener.isInvoked());

        sendTimer(23000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s10"}, {"s11"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent("s12");
        assertFalse(listener.isInvoked());
    }

    public void testSnapshotMonthScoped() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime("2002-02-01T9:00:00.000");
        epService.getEPAdministrator().createEPL("select * from SupportBean.std:lastevent() output snapshot every 1 month").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        sendCurrentTimeWithMinus("2002-03-01T9:00:00.000", 1);
        assertFalse(listener.getAndClearIsInvoked());

        sendCurrentTime("2002-03-01T9:00:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "theString".split(","), new Object[][] {{"E1"}});
    }

    public void testFirstMonthScoped() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime("2002-02-01T9:00:00.000");
        epService.getEPAdministrator().createEPL("select * from SupportBean.std:lastevent() output first every 1 month").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        sendCurrentTimeWithMinus("2002-03-01T9:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        sendCurrentTime("2002-03-01T9:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "theString".split(","), new Object[][] {{"E4"}});
    }

    private SupportUpdateListener createStmtAndListenerJoin(String viewExpr) {
		epService.initialize();

		SupportUpdateListener updateListener = new SupportUpdateListener();
		EPStatement view = epService.getEPAdministrator().createEPL(viewExpr);
	    view.addListener(updateListener);

	    epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));

	    return updateListener;
	}

	private void runAssertLast(SupportUpdateListener updateListener)
	{
		// send an event
	    sendEvent(1);

	    // check no update
	    assertFalse(updateListener.getAndClearIsInvoked());

	    // send another event
	    sendEvent(2);

	    // check update, only the last event present
	    assertTrue(updateListener.getAndClearIsInvoked());
	    assertEquals(1, updateListener.getLastNewData().length);
	    assertEquals(2L, updateListener.getLastNewData()[0].get("longBoxed"));
	    assertNull(updateListener.getLastOldData());
	}

    private void sendTimer(long time)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendEvent(String s)
	{
	    SupportBean bean = new SupportBean();
	    bean.setTheString(s);
	    bean.setDoubleBoxed(0.0);
	    bean.setIntPrimitive(0);
	    bean.setIntBoxed(0);
	    epService.getEPRuntime().sendEvent(bean);
	}

    private void timeCallback(String statementString, int timeToCallback) {
    	// clear any old events
        epService.initialize();

    	// set the clock to 0
    	currentTime = 0;
    	sendTimeEvent(0);

    	// create the EPL statement and add a listener
    	EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
    	SupportUpdateListener updateListener = new SupportUpdateListener();
    	statement.addListener(updateListener);
    	updateListener.reset();

    	// send an event
    	sendEvent("IBM");

    	// check that the listener hasn't been updated
        sendTimeEvent(timeToCallback - 1);
    	assertFalse(updateListener.getAndClearIsInvoked());

    	// update the clock
    	sendTimeEvent(timeToCallback);

    	// check that the listener has been updated
    	assertTrue(updateListener.getAndClearIsInvoked());
    	assertEquals(1, updateListener.getLastNewData().length);
    	assertNull(updateListener.getLastOldData());

    	// send another event
    	sendEvent("MSFT");

    	// check that the listener hasn't been updated
    	assertFalse(updateListener.getAndClearIsInvoked());

    	// update the clock
    	sendTimeEvent(timeToCallback);

    	// check that the listener has been updated
    	assertTrue(updateListener.getAndClearIsInvoked());
    	assertEquals(1, updateListener.getLastNewData().length);
    	assertNull(updateListener.getLastOldData());

    	// don't send an event
    	// check that the listener hasn't been updated
    	assertFalse(updateListener.getAndClearIsInvoked());

    	// update the clock
    	sendTimeEvent(timeToCallback);

    	// check that the listener has been updated
    	assertTrue(updateListener.getAndClearIsInvoked());
    	assertNull(updateListener.getLastNewData());
    	assertNull(updateListener.getLastOldData());

    	// don't send an event
    	// check that the listener hasn't been updated
    	assertFalse(updateListener.getAndClearIsInvoked());

    	// update the clock
    	sendTimeEvent(timeToCallback);

    	// check that the listener has been updated
    	assertTrue(updateListener.getAndClearIsInvoked());
    	assertNull(updateListener.getLastNewData());
    	assertNull(updateListener.getLastOldData());

    	// send several events
    	sendEvent("YAH");
    	sendEvent("s4");
    	sendEvent("s5");

    	// check that the listener hasn't been updated
    	assertFalse(updateListener.getAndClearIsInvoked());

    	// update the clock
    	sendTimeEvent(timeToCallback);

    	// check that the listener has been updated
    	assertTrue(updateListener.getAndClearIsInvoked());
    	assertEquals(3, updateListener.getLastNewData().length);
    	assertNull(updateListener.getLastOldData());
    }

    private void sendTimeEvent(int timeIncrement){
    	currentTime += timeIncrement;
        CurrentTimeEvent theEvent = new CurrentTimeEvent(currentTime);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendJoinEvents(String s)
	{
	    SupportBean event1 = new SupportBean();
	    event1.setTheString(s);
	    event1.setDoubleBoxed(0.0);
	    event1.setIntPrimitive(0);
	    event1.setIntBoxed(0);

	    SupportBean_A event2 = new SupportBean_A(s);

	    epService.getEPRuntime().sendEvent(event1);
	    epService.getEPRuntime().sendEvent(event2);
	}

    private void sendMDEvent(String symbol, long volume)
	{
	    SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
	    epService.getEPRuntime().sendEvent(bean);
	}

    private void sendEvent(String symbol, double price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendCurrentTime(String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
