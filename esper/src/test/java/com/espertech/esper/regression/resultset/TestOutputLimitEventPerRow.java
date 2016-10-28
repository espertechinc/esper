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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.regression.support.ResultAssertTestResult;
import com.espertech.esper.regression.support.ResultAssertExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import junit.framework.TestCase;

public class TestOutputLimitEventPerRow extends TestCase
{
    private static String SYMBOL_DELL = "DELL";
    private static String SYMBOL_IBM = "IBM";

    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private final static String CATEGORY = "Aggregated and Grouped";

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

    public void testUnaggregatedOutputFirst() {
        sendTimer(0);

        String[] fields = "theString,intPrimitive".split(",");
        String epl = "select * from SupportBean\n" +
                "     group by theString\n" +
                "     output first every 10 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 3});

        sendTimer(5000);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E3", 4});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        assertFalse(listener.isInvoked());

        sendTimer(10000);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 6));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 7));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 7});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 8));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 9));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 9});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        assertFalse(listener.isInvoked());
    }

    public void testOutputFirstHavingJoinNoJoin() {

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String stmtText = "select theString, longPrimitive, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events";
        tryOutputFirstHaving(stmtText);

        String stmtTextJoin = "select theString, longPrimitive, sum(intPrimitive) as value from MyWindow mv, SupportBean_A#keepall() a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events";
        tryOutputFirstHaving(stmtTextJoin);

        String stmtTextOrder = "select theString, longPrimitive, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
        tryOutputFirstHaving(stmtTextOrder);

        String stmtTextOrderJoin = "select theString, longPrimitive, sum(intPrimitive) as value from MyWindow mv, SupportBean_A#keepall() a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
        tryOutputFirstHaving(stmtTextOrderJoin);
    }

    private void tryOutputFirstHaving(String statementText) {
        String[] fields = "theString,longPrimitive,value".split(",");
        String[] fieldsLimited = "theString,value".split(",");
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall() as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));

        sendBeanEvent("E1", 101, 10);
        sendBeanEvent("E2", 102, 15);
        sendBeanEvent("E1", 103, 10);
        sendBeanEvent("E2", 104, 5);
        assertFalse(listener.isInvoked());

        sendBeanEvent("E2", 105, 5);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 105L, 25});

        sendBeanEvent("E2", 106, -6);    // to 19, does not count toward condition
        sendBeanEvent("E2", 107, 2);    // to 21, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent("E2", 108, 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 108L, 22});

        sendBeanEvent("E2", 109, 1);    // to 23, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent("E2", 110, 1);     // to 24
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 110L, 24});

        sendBeanEvent("E2", 111, -10);    // to 14
        sendBeanEvent("E2", 112, 10);    // to 24, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent("E2", 113, 0);    // to 24, counts toward condition
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 113L, 24});

        sendBeanEvent("E2", 114, -10);    // to 14
        sendBeanEvent("E2", 115, 1);     // to 15
        sendBeanEvent("E2", 116, 5);     // to 20
        sendBeanEvent("E2", 117, 0);     // to 20
        sendBeanEvent("E2", 118, 1);     // to 21    // counts
        assertFalse(listener.isInvoked());

        sendBeanEvent("E2", 119, 0);    // to 21
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 119L, 21});

        // remove events
        sendMDEvent("E2", 0);   // remove 113, 117, 119 (any order of delete!)
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLimited, new Object[]{"E2", 21});

        // remove events
        sendMDEvent("E2", -10); // remove 111, 114
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLimited, new Object[]{"E2", 41});

        // remove events
        sendMDEvent("E2", -6);  // since there is 3*0 we output the next one
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLimited, new Object[]{"E2", 47});

        sendMDEvent("E2", 2);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void test1NoneNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                          "from MarketData#time(5.5 sec)" +
                          "group by symbol";
        runAssertion12(stmtText, "none");
    }

    public void test2NoneNoHavingJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec), " +
                            "SupportBean#keepall() where theString=symbol " +
                          "group by symbol";
        runAssertion12(stmtText, "none");
    }

    public void test3NoneHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec) " +
                            "group by symbol " +
                            " having sum(price) > 50";
        runAssertion34(stmtText, "none");
    }

    public void test4NoneHavingJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec), " +
                            "SupportBean#keepall() where theString=symbol " +
                            "group by symbol " +
                            "having sum(price) > 50";
        runAssertion34(stmtText, "none");
    }

    public void test5DefaultNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec) " +
                            "group by symbol " +
                            "output every 1 seconds";
        runAssertion56(stmtText, "default");
    }

    public void test6DefaultNoHavingJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec), " +
                            "SupportBean#keepall() where theString=symbol " +
                            "group by symbol " +
                            "output every 1 seconds";
        runAssertion56(stmtText, "default");
    }

    public void test7DefaultHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec) \n"  +
                            "group by symbol " +
                            "having sum(price) > 50" +
                            "output every 1 seconds";
        runAssertion78(stmtText, "default");
    }

    public void test8DefaultHavingJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec), " +
                            "SupportBean#keepall() where theString=symbol " +
                            "group by symbol " +
                            "having sum(price) > 50" +
                            "output every 1 seconds";
        runAssertion78(stmtText, "default");
    }

    public void test9AllNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec) " +
                            "group by symbol " +
                            "output all every 1 seconds " +
                            "order by symbol";
        runAssertion9_10(stmtText, "all");
    }

    public void test10AllNoHavingJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec), " +
                            "SupportBean#keepall() where theString=symbol " +
                            "group by symbol " +
                            "output all every 1 seconds " +
                            "order by symbol";
        runAssertion9_10(stmtText, "all");
    }

    public void test11AllHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec) " +
                            "group by symbol " +
                            "having sum(price) > 50 " +
                            "output all every 1 seconds";
        runAssertion11_12(stmtText, "all");
    }

    public void test11AllHavingNoJoinHinted()
    {
        String stmtText = "@Hint('enable_outputlimit_opt') select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output all every 1 seconds";
        runAssertion11_12(stmtText, "all");
    }

    public void test12AllHavingJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec), " +
                            "SupportBean#keepall() where theString=symbol " +
                            "group by symbol " +
                            "having sum(price) > 50 " +
                            "output all every 1 seconds";
        runAssertion11_12(stmtText, "all");
    }

    public void test12AllHavingJoinHinted()
    {
        String stmtText = "@Hint('enable_outputlimit_opt') select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall() where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output all every 1 seconds";
        runAssertion11_12(stmtText, "all");
    }

    public void test13LastNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec)" +
                            "group by symbol " +
                            "output last every 1 seconds " +
                            "order by symbol";
        runAssertion13_14(stmtText, "last");
    }

    public void test14LastNoHavingJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec), " +
                            "SupportBean#keepall() where theString=symbol " +
                            "group by symbol " +
                            "output last every 1 seconds " +
                            "order by symbol";
        runAssertion13_14(stmtText, "last");
    }

    public void test15LastHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec)" +
                            "group by symbol " +
                            "having sum(price) > 50 " +
                            "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test15LastHavingNoJoinHinted()
    {
        String stmtText = "@Hint('enable_outputlimit_opt') select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec)" +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test16LastHavingJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec), " +
                            "SupportBean#keepall() where theString=symbol " +
                            "group by symbol " +
                            "having sum(price) > 50 " +
                            "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test16LastHavingJoinHinted()
    {
        String stmtText = "@Hint('enable_outputlimit_opt') select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall() where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test17FirstNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec) " +
                            "group by symbol " +
                            "output first every 1 seconds";
        runAssertion17(stmtText, "first");
    }

    public void test17FirstNoHavingJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec), " +
                            "SupportBean#keepall() where theString=symbol " +
                            "group by symbol " +
                            "output first every 1 seconds";
        runAssertion17(stmtText, "first");
    }

    public void test18SnapshotNoHavingNoJoin()
    {
        String stmtText = "select symbol, volume, sum(price) " +
                            "from MarketData#time(5.5 sec) " +
                            "group by symbol " +
                            "output snapshot every 1 seconds";
        runAssertion18(stmtText, "snapshot");
    }

    private void runAssertion12(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultInsert(800, 1, new Object[][] {{"MSFT", 5000L, 9d}});
        expected.addResultInsert(1500, 1, new Object[][] {{"IBM", 150L, 49d}});
        expected.addResultInsert(1500, 2, new Object[][] {{"YAH", 10000L, 1d}});
        expected.addResultInsert(2100, 1, new Object[][] {{"IBM", 155L, 75d}});
        expected.addResultInsert(3500, 1, new Object[][] {{"YAH", 11000L, 3d}});
        expected.addResultInsert(4300, 1, new Object[][] {{"IBM", 150L, 97d}});
        expected.addResultInsert(4900, 1, new Object[][] {{"YAH", 11500L, 6d}});
        expected.addResultRemove(5700, 0, new Object[][] {{"IBM", 100L, 72d}});
        expected.addResultInsert(5900, 1, new Object[][] {{"YAH", 10500L, 7d}});
        expected.addResultRemove(6300, 0, new Object[][] {{"MSFT", 5000L, null}});
        expected.addResultRemove(7000, 0, new Object[][] {{"IBM", 150L, 48d}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion34(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(2100, 1, new Object[][] {{"IBM", 155L, 75d}});
        expected.addResultInsert(4300, 1, new Object[][] {{"IBM", 150L, 97d}});
        expected.addResultRemove(5700, 0, new Object[][] {{"IBM", 100L, 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion13_14(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 155L, 75d}, {"YAH", 10000L, 1d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][] {{"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 150L, 97d}, {"YAH", 11500L, 6d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"YAH", 10500L, 7d}}, new Object[][] {{"IBM", 100L, 72d}});
        expected.addResultRemove(7200, 0, new Object[][] {{"IBM", 150L, 48d}, {"MSFT", 5000L, null}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion15_16(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 155L, 75d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 150L, 97d}});
        expected.addResultInsRem(6200, 0, null, new Object[][] {{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion78(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 155L, 75d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 150L, 97d}});
        expected.addResultInsRem(6200, 0, null, new Object[][] {{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion56(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 150L, 49d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 75d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][] {{"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 150L, 97d}, {"YAH", 11500L, 6d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"YAH", 10500L, 7d}}, new Object[][] {{"IBM", 100L, 72d}});
        expected.addResultRemove(7200, 0, new Object[][] {{"MSFT", 5000L, null}, {"IBM", 150L, 48d}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion9_10(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 150L, 49d}, {"IBM", 155L, 75d}, {"MSFT", 5000L, 9d}, {"YAH", 10000L, 1d}});
        expected.addResultInsert(3200, 0, new Object[][] {{"IBM", 155L, 75d}, {"MSFT", 5000L, 9d}, {"YAH", 10000L, 1d}});
        expected.addResultInsert(4200, 0, new Object[][] {{"IBM", 155L, 75d}, {"MSFT", 5000L, 9d}, {"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 150L, 97d}, {"MSFT", 5000L, 9d}, {"YAH", 11500L, 6d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"IBM", 150L, 72d}, {"MSFT", 5000L, 9d}, {"YAH", 10500L, 7d}}, new Object[][] {{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, new Object[][] {{"IBM", 150L, 48d}, {"MSFT", 5000L, null}, {"YAH", 10500L, 6d}}, new Object[][] {{"IBM", 150L, 48d}, {"MSFT", 5000L, null}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion11_12(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 155L, 75d}});
        expected.addResultInsert(3200, 0, new Object[][] {{"IBM", 155L, 75d}});
        expected.addResultInsert(4200, 0, new Object[][] {{"IBM", 155L, 75d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 150L, 97d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"IBM", 150L, 72d}}, new Object[][] {{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion17(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][] {{"IBM", 100L, 25d}});
        expected.addResultInsert(800, 1, new Object[][] {{"MSFT", 5000L, 9d}});
        expected.addResultInsert(1500, 1, new Object[][] {{"IBM", 150L, 49d}});
        expected.addResultInsert(1500, 2, new Object[][] {{"YAH", 10000L, 1d}});
        expected.addResultInsert(3500, 1, new Object[][] {{"YAH", 11000L, 3d}});
        expected.addResultInsert(4300, 1, new Object[][] {{"IBM", 150L, 97d}});
        expected.addResultInsert(4900, 1, new Object[][] {{"YAH", 11500L, 6d}});
        expected.addResultInsert(5700, 0, new Object[][] {{"IBM", 100L, 72d}});
        expected.addResultInsert(5900, 1, new Object[][] {{"YAH", 10500L, 7d}});
        expected.addResultInsert(6300, 0, new Object[][] {{"MSFT", 5000L, null}});
        expected.addResultInsert(7000, 0, new Object[][] {{"IBM", 150L, 48d}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion18(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 100L, 75d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 75d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 75d}});
        expected.addResultInsert(3200, 0, new Object[][] {{"IBM", 100L, 75d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 75d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 75d}});
        expected.addResultInsert(4200, 0, new Object[][] {{"IBM", 100L, 75d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 75d}, {"YAH", 10000L, 3d}, {"IBM", 155L, 75d}, {"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 100L, 97d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 97d}, {"YAH", 10000L, 6d}, {"IBM", 155L, 97d}, {"YAH", 11000L, 6d}, {"IBM", 150L, 97d}, {"YAH", 11500L, 6d}});
        expected.addResultInsert(6200, 0, new Object[][] {{"MSFT", 5000L, 9d}, {"IBM", 150L, 72d}, {"YAH", 10000L, 7d}, {"IBM", 155L, 72d}, {"YAH", 11000L, 7d}, {"IBM", 150L, 72d}, {"YAH", 11500L, 7d}, {"YAH", 10500L, 7d}});
        expected.addResultInsert(7200, 0, new Object[][] {{"IBM", 155L, 48d}, {"YAH", 11000L, 6d}, {"IBM", 150L, 48d}, {"YAH", 11500L, 6d}, {"YAH", 10500L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    public void testHaving()
    {
        sendTimer(0);

        String viewExpr = "select irstream symbol, volume, sum(price) as sumprice" +
                          " from " + SupportMarketDataBean.class.getName() + "#time(10 sec) " +
                          "group by symbol " +
                          "having sum(price) >= 10 " +
                          "output every 3 events";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        stmt.addListener(listener);

        runAssertionHavingDefault();
    }

    public void testHavingJoin()
    {
        sendTimer(0);

        String viewExpr = "select irstream symbol, volume, sum(price) as sumprice" +
                          " from " + SupportMarketDataBean.class.getName() + "#time(10 sec) as s0," +
                          SupportBean.class.getName() + "#keepall() as s1 " +
                          "where s0.symbol = s1.theString " +
                          "group by symbol " +
                          "having sum(price) >= 10 " +
                          "output every 3 events";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 0));

        runAssertionHavingDefault();
    }

    public void testJoinSortWindow()
    {
        sendTimer(0);

        String viewExpr = "select irstream symbol, volume, max(price) as maxVol" +
                          " from " + SupportMarketDataBean.class.getName() + "#sort(1, volume) as s0," +
                          SupportBean.class.getName() + "#keepall() as s1 where s1.theString = s0.symbol " +
                          "group by symbol output every 1 seconds";
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

    public void testLimitSnapshot()
    {
        sendTimer(0);
        String selectStmt = "select symbol, volume, sum(price) as sumprice from " + SupportMarketDataBean.class.getName() +
                "#time(10 seconds) group by symbol output snapshot every 1 seconds";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);
        sendEvent("s0", 1, 20);

        sendTimer(500);
        sendEvent("IBM", 2, 16);
        sendEvent("s0", 3, 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(1000);
        String fields[] = new String[] {"symbol", "volume", "sumprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"s0", 1L, 34d}, {"IBM", 2L, 16d}, {"s0", 3L, 34d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(1500);
        sendEvent("MSFT", 4, 18);
        sendEvent("IBM", 5, 30);

        sendTimer(10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"s0", 1L, 34d}, {"IBM", 2L, 46d}, {"s0", 3L, 34d}, {"MSFT", 4L, 18d}, {"IBM", 5L, 46d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"MSFT", 4L, 18d}, {"IBM", 5L, 30d}});
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
        String selectStmt = "select symbol, volume, sum(price) as sumprice from " + SupportMarketDataBean.class.getName() +
                "#time(10 seconds) as m, " + SupportBean.class.getName() +
                "#keepall() as s where s.theString = m.symbol group by symbol output snapshot every 1 seconds order by symbol, volume asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("ABC", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("MSFT", 3));

        sendEvent("ABC", 1, 20);

        sendTimer(500);
        sendEvent("IBM", 2, 16);
        sendEvent("ABC", 3, 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(1000);
        String fields[] = new String[] {"symbol", "volume", "sumprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 1L, 34d}, {"ABC", 3L, 34d}, {"IBM", 2L, 16d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(1500);
        sendEvent("MSFT", 4, 18);
        sendEvent("IBM", 5, 30);

        sendTimer(10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"ABC", 1L, 34d}, {"ABC", 3L, 34d}, {"IBM", 2L, 46d}, {"IBM", 5L, 46d}, {"MSFT", 4L, 18d},});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(10500);
        sendTimer(11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"IBM", 5L, 30d}, {"MSFT", 4L, 18d}});
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

    public void testMaxTimeWindow()
    {
        sendTimer(0);

        String viewExpr = "select irstream symbol, " +
                                  "volume, max(price) as maxVol" +
                          " from " + SupportMarketDataBean.class.getName() + "#time(1 sec) " +
                          "group by symbol output every 1 seconds";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

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

    public void testNoJoinLast() {
        runAssertionNoJoinLast(true);
        runAssertionNoJoinLast(false);
    }

    private void runAssertionNoJoinLast(boolean hinted)
	{
        String hint = hinted ? "@Hint('enable_outputlimit_opt') " : "";

        // Every event generates a new row, this time we sum the price by symbol and output volume
	    String viewExpr = hint +
                          "select symbol, volume, sum(price) as mySum " +
	                      "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
	                      "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
	                      "group by symbol " +
	                      "output last every 2 events";

	    EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
	    selectTestView.addListener(listener);

	    runAssertionLast();

        selectTestView.destroy();
        listener.reset();
	}

    private void assertEvent(String symbol, Double mySum, Long volume)
	{
	    EventBean[] newData = listener.getLastNewData();

	    assertEquals(1, newData.length);

	    assertEquals(symbol, newData[0].get("symbol"));
	    assertEquals(mySum, newData[0].get("mySum"));
	    assertEquals(volume, newData[0].get("volume"));

	    listener.reset();
	    assertFalse(listener.isInvoked());
	}

	private void runAssertionSingle(EPStatement selectTestView)
	{
	    // assert select result type
	    assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
	    assertEquals(Double.class, selectTestView.getEventType().getPropertyType("mySum"));
	    assertEquals(Long.class, selectTestView.getEventType().getPropertyType("volume"));

	    sendEvent(SYMBOL_DELL, 10, 100);
	    assertTrue(listener.isInvoked());
	    assertEvent(SYMBOL_DELL, 100d, 10L);

	    sendEvent(SYMBOL_IBM, 15, 50);
	    assertEvent(SYMBOL_IBM, 50d, 15L);
	}

	public void testNoOutputClauseView()
	{
	    String viewExpr = "select symbol, volume, sum(price) as mySum " +
	                      "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
	                      "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
	                      "group by symbol ";

	    EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
	    selectTestView.addListener(listener);

	    runAssertionSingle(selectTestView);
	}

	public void testNoJoinDefault()
    {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String viewExpr = "select symbol, volume, sum(price) as mySum " +
                          "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                          "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                          "group by symbol " +
                          "output every 2 events";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runAssertionDefault(selectTestView);
    }

    public void testJoinDefault()
	{
	    // Every event generates a new row, this time we sum the price by symbol and output volume
	    String viewExpr = "select symbol, volume, sum(price) as mySum " +
	                      "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
	                                SupportMarketDataBean.class.getName() + "#length(5) as two " +
	                      "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
	                      "  and one.theString = two.symbol " +
	                      "group by symbol " +
	                      "output every 2 events";

	    EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
	    selectTestView.addListener(listener);

	    epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
	    epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

	    runAssertionDefault(selectTestView);
	}

    public void testNoJoinAll() {
        runAssertionNoJoinAll(false);
        runAssertionNoJoinAll(true);
    }

    private void runAssertionNoJoinAll(boolean hinted)
    {
        String hint = hinted ? "@Hint('enable_outputlimit_opt') " : "";

        // Every event generates a new row, this time we sum the price by symbol and output volume
        String viewExpr = hint + "select symbol, volume, sum(price) as mySum " +
                          "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                          "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                          "group by symbol " +
                          "output all every 2 events";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runAssertionAll(selectTestView);

        selectTestView.destroy();
        listener.reset();
    }

    public void testJoinAll()
    {
        runAssertionJoinAll(false);
        runAssertionJoinAll(true);
    }

    private void runAssertionJoinAll(boolean hinted)
    {
        String hint = hinted ? "@Hint('enable_outputlimit_opt') " : "";

        // Every event generates a new row, this time we sum the price by symbol and output volume
        String viewExpr = hint + "select symbol, volume, sum(price) as mySum " +
                          "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                                    SupportMarketDataBean.class.getName() + "#length(5) as two " +
                          "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                          "  and one.theString = two.symbol " +
                          "group by symbol " +
                          "output all every 2 events";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        runAssertionAll(selectTestView);

        selectTestView.destroy();
        listener.reset();
    }

    public void testJoinLast() {
        runAssertionJoinLast(true);
        runAssertionJoinLast(false);
    }

	private void runAssertionJoinLast(boolean hinted)
	{
        String hint = hinted ? "@Hint('enable_outputlimit_opt') " : "";

        // Every event generates a new row, this time we sum the price by symbol and output volume
	    String viewExpr = hint +
                          "select symbol, volume, sum(price) as mySum " +
	                      "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
	                                SupportMarketDataBean.class.getName() + "#length(5) as two " +
	                      "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
	                      "  and one.theString = two.symbol " +
	                      "group by symbol " +
	                      "output last every 2 events";

	    EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
	    selectTestView.addListener(listener);

	    epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
	    epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

	    runAssertionLast();

        listener.reset();
        selectTestView.destroy();
	}

    private void runAssertionHavingDefault()
    {
        sendEvent("IBM", 1, 5);
        sendEvent("IBM", 2, 6);
        assertFalse(listener.isInvoked());

        sendEvent("IBM", 3, -3);
        String fields[] = "symbol,volume,sumprice".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"IBM", 2L, 11.0});

        sendTimer(5000);
        sendEvent("IBM", 4, 10);
        sendEvent("IBM", 5, 0);
        assertFalse(listener.isInvoked());

        sendEvent("IBM", 6, 1);
        assertEquals(3, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"IBM", 4L, 18.0});
        EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"IBM", 5L, 18.0});
        EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields, new Object[]{"IBM", 6L, 19.0});
        listener.reset();

        sendTimer(11000);
        assertEquals(3, listener.getLastOldData().length);
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"IBM", 1L, 11.0});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"IBM", 2L, 11.0});
        listener.reset();
    }

    private void runAssertionDefault(EPStatement selectTestView)
    {
    	// assert select result type
    	assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
    	assertEquals(Long.class, selectTestView.getEventType().getPropertyType("volume"));
    	assertEquals(Double.class, selectTestView.getEventType().getPropertyType("mySum"));

    	sendEvent(SYMBOL_IBM, 500, 20);
    	assertFalse(listener.getAndClearIsInvoked());

    	sendEvent(SYMBOL_DELL, 10000, 51);
        String fields[] = "symbol,volume,mySum".split(",");
        UniformPair<EventBean[]> events = listener.getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_IBM))
        {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_IBM, 500L, 20.0}, {SYMBOL_DELL, 10000L, 51.0}});
        }
        else
        {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_DELL, 10000L, 51.0}, {SYMBOL_IBM, 500L, 20.0}});
        }
        assertNull(listener.getLastOldData());

        listener.reset();

        sendEvent(SYMBOL_DELL, 20000, 52);
    	assertFalse(listener.getAndClearIsInvoked());

    	sendEvent(SYMBOL_DELL, 40000, 45);
        events = listener.getDataListsFlattened();
        EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_DELL, 20000L, 51.0 + 52.0}, {SYMBOL_DELL, 40000L, 51.0 + 52.0 + 45.0}});
        assertNull(listener.getLastOldData());
    }

    private void runAssertionAll(EPStatement selectTestView)
    {
    	// assert select result type
    	assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
    	assertEquals(Long.class, selectTestView.getEventType().getPropertyType("volume"));
    	assertEquals(Double.class, selectTestView.getEventType().getPropertyType("mySum"));

    	sendEvent(SYMBOL_IBM, 500, 20);
    	assertFalse(listener.getAndClearIsInvoked());

    	sendEvent(SYMBOL_DELL, 10000, 51);
        String fields[] = "symbol,volume,mySum".split(",");
        UniformPair<EventBean[]> events = listener.getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_IBM))
        {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_IBM, 500L, 20.0}, {SYMBOL_DELL, 10000L, 51.0}});
        }
        else
        {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_DELL, 10000L, 51.0}, {SYMBOL_IBM, 500L, 20.0}});
        }
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(SYMBOL_DELL, 20000, 52);
    	assertFalse(listener.getAndClearIsInvoked());

    	sendEvent(SYMBOL_DELL, 40000, 45);
        events = listener.getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_IBM))
        {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_IBM, 500L, 20.0}, {SYMBOL_DELL, 20000L, 51.0 + 52.0}, {SYMBOL_DELL, 40000L, 51.0 + 52.0 + 45.0}});
        }
        else
        {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_DELL, 20000L, 51.0 + 52.0}, {SYMBOL_DELL, 40000L, 51.0 + 52.0 + 45.0}, {SYMBOL_IBM, 500L, 20.0}});
        }
        assertNull(listener.getLastOldData());
    }

	private void runAssertionLast()
    {
        String fields[] = "symbol,volume,mySum".split(",");
        sendEvent(SYMBOL_DELL, 10000, 51);
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(SYMBOL_DELL, 20000, 52);
        UniformPair<EventBean[]> events = listener.getDataListsFlattened();
        EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_DELL, 20000L, 103.0}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(SYMBOL_DELL, 30000, 70);
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(SYMBOL_IBM, 10000, 20);
        events = listener.getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_DELL))
        {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_DELL, 30000L, 173.0}, {SYMBOL_IBM, 10000L, 20.0}});
        }
        else
        {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_IBM, 10000L, 20.0}, {SYMBOL_DELL, 30000L, 173.0}});
        }
        assertNull(listener.getLastOldData());
    }


    private void sendEvent(String symbol, long volume, double price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(String symbol, double price)
	{
	    SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
	    epService.getEPRuntime().sendEvent(bean);
	}

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendBeanEvent(String theString, long longPrimitive, int intPrimitive)
	{
        SupportBean b = new SupportBean();
        b.setTheString(theString);
        b.setLongPrimitive(longPrimitive);
        b.setIntPrimitive(intPrimitive);
	    epService.getEPRuntime().sendEvent(b);
	}

    private void sendMDEvent(String symbol, double price)
	{
	    SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
	    epService.getEPRuntime().sendEvent(bean);
	}

    private static final Logger log = LoggerFactory.getLogger(TestOutputLimitEventPerRow.class);
}
