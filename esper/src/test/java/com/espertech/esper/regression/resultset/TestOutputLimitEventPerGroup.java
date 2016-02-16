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
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.regression.support.ResultAssertExecution;
import com.espertech.esper.regression.support.ResultAssertTestResult;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestOutputLimitEventPerGroup extends TestCase
{
    private static String SYMBOL_DELL = "DELL";
    private static String SYMBOL_IBM = "IBM";

    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private final static String CATEGORY = "Fully-Aggregated and Grouped";

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

    public void testLastNoDataWindow() {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String epl = "select theString, intPrimitive as intp from SupportBean group by theString output last every 1 seconds order by theString asc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 31));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 22));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), new String[]{"theString", "intp"}, new Object[][]{{"E1", 3}, {"E2", 21}, {"E3", 31}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 31));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 33));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), new String[]{"theString", "intp"}, new Object[][]{{"E1", 5}, {"E3", 33}});
}

    public void testOutputFirstHavingJoinNoJoin() {

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String stmtText = "select theString, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events";
        tryOutputFirstHaving(stmtText);

        String stmtTextJoin = "select theString, sum(intPrimitive) as value from MyWindow mv, SupportBean_A.win:keepall() a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events";
        tryOutputFirstHaving(stmtTextJoin);

        String stmtTextOrder = "select theString, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
        tryOutputFirstHaving(stmtTextOrder);

        String stmtTextOrderJoin = "select theString, sum(intPrimitive) as value from MyWindow mv, SupportBean_A.win:keepall() a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
        tryOutputFirstHaving(stmtTextOrderJoin);
    }

    private void tryOutputFirstHaving(String statementText) {
        String[] fields = "theString,value".split(",");
        epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));

        sendBeanEvent("E1", 10);
        sendBeanEvent("E2", 15);
        sendBeanEvent("E1", 10);
        sendBeanEvent("E2", 5);
        assertFalse(listener.isInvoked());

        sendBeanEvent("E2", 5);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 25});

        sendBeanEvent("E2", -6);    // to 19, does not count toward condition
        sendBeanEvent("E2", 2);    // to 21, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent("E2", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 22});

        sendBeanEvent("E2", 1);    // to 23, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent("E2", 1);     // to 24
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 24});

        sendBeanEvent("E2", -10);    // to 14
        sendBeanEvent("E2", 10);    // to 24, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent("E2", 0);    // to 24, counts toward condition
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 24});

        sendBeanEvent("E2", -10);    // to 14
        sendBeanEvent("E2", 1);     // to 15
        sendBeanEvent("E2", 5);     // to 20
        sendBeanEvent("E2", 0);     // to 20
        sendBeanEvent("E2", 1);     // to 21    // counts
        assertFalse(listener.isInvoked());

        sendBeanEvent("E2", 0);    // to 21
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 21});
        
        // remove events
        sendMDEvent("E2", 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 21});

        // remove events
        sendMDEvent("E2", -10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 41});

        // remove events
        sendMDEvent("E2", -6);  // since there is 3*-10 we output the next one
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 47});

        sendMDEvent("E2", 2);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testOutputFirstCrontab() {
        sendTimer(0);
        String[] fields = "theString,value".split(",");
        epService.getEPAdministrator().getConfiguration().addVariable("varout", boolean.class, false);
        epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, sum(intPrimitive) as value from MyWindow group by theString output first at (*/2, *, *, *, *)");
        stmt.addListener(listener);

        sendBeanEvent("E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

        sendTimer(2 * 60 * 1000 - 1);
        sendBeanEvent("E1", 11);
        assertFalse(listener.isInvoked());

        sendTimer(2 * 60 * 1000);
        sendBeanEvent("E1", 12);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 33});

        sendBeanEvent("E2", 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});

        sendBeanEvent("E2", 21);
        sendTimer(4 * 60 * 1000 - 1);
        sendBeanEvent("E2", 22);
        sendBeanEvent("E1", 13);
        assertFalse(listener.isInvoked());

        sendTimer(4 * 60 * 1000);
        sendBeanEvent("E2", 23);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 86});
        sendBeanEvent("E1", 14);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 60});
    }

    public void testOutputFirstWhenThen() {
        String[] fields = "theString,value".split(",");
        epService.getEPAdministrator().getConfiguration().addVariable("varout", boolean.class, false);
        epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, sum(intPrimitive) as value from MyWindow group by theString output first when varout then set varout = false");
        stmt.addListener(listener);

        sendBeanEvent("E1", 10);
        sendBeanEvent("E1", 11);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().setVariableValue("varout", true);
        sendBeanEvent("E1", 12);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 33});
        assertEquals(false, epService.getEPRuntime().getVariableValue("varout"));

        epService.getEPRuntime().setVariableValue("varout", true);
        sendBeanEvent("E2", 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});
        assertEquals(false, epService.getEPRuntime().getVariableValue("varout"));

        sendBeanEvent("E1", 13);
        sendBeanEvent("E2", 21);
        assertFalse(listener.isInvoked());
    }

    public void testOutputFirstEveryNEvents() {
        String[] fields = "theString,value".split(",");
        epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, sum(intPrimitive) as value from MyWindow group by theString output first every 3 events");
        stmt.addListener(listener);

        sendBeanEvent("E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

        sendBeanEvent("E1", 12);
        sendBeanEvent("E1", 11);
        assertFalse(listener.isInvoked());
        
        sendBeanEvent("E1", 13);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 46});

        this.sendMDEvent("S1", 12);
        this.sendMDEvent("S1", 11);
        assertFalse(listener.isInvoked());

        this.sendMDEvent("S1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 13});

        sendBeanEvent("E1", 14);
        sendBeanEvent("E1", 15);
        assertFalse(listener.isInvoked());

        sendBeanEvent("E2", 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});

        // test variable
        epService.getEPAdministrator().createEPL("create variable int myvar = 1");
        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select theString, sum(intPrimitive) as value from MyWindow group by theString output first every myvar events");
        stmt.addListener(listener);
        
        sendBeanEvent("E3", 10);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E3", 10}});

        sendBeanEvent("E1", 5);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 47}});

        epService.getEPRuntime().setVariableValue("myvar", 2);

        sendBeanEvent("E1", 6);
        assertFalse(listener.isInvoked());

        sendBeanEvent("E1", 7);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 60}});

        sendBeanEvent("E1", 1);
        assertFalse(listener.isInvoked());

        sendBeanEvent("E1", 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 62}});
    }

    public void testWildcardEventPerGroup() {

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean group by theString output last every 3 events order by theString asc");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("ATT", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 100));

        EventBean[] events = listener.getNewDataListFlattened();
        listener.reset();
        assertEquals(2, events.length);
        assertEquals("ATT", events[0].get("theString"));
        assertEquals(11, events[0].get("intPrimitive"));
        assertEquals("IBM", events[1].get("theString"));
        assertEquals(100, events[1].get("intPrimitive"));
        stmt.destroy();

        // All means each event
        stmt = epService.getEPAdministrator().createEPL("select * from SupportBean group by theString output all every 3 events");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("ATT", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 100));

        events = listener.getNewDataListFlattened();
        assertEquals(3, events.length);
        assertEquals("IBM", events[0].get("theString"));
        assertEquals(10, events[0].get("intPrimitive"));
        assertEquals("ATT", events[1].get("theString"));
        assertEquals(11, events[1].get("intPrimitive"));
        assertEquals("IBM", events[2].get("theString"));
        assertEquals(100, events[2].get("intPrimitive"));
    }
    
    public void test1NoneNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                          "from MarketData.win:time(5.5 sec)" +
                          "group by symbol " +
                          "order by symbol asc";
        runAssertion12(stmtText, "none");
    }

    public void test2NoneNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "order by symbol asc";
        runAssertion12(stmtText, "none");
    }

    public void test3NoneHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "group by symbol " +
                            " having sum(price) > 50";
        runAssertion34(stmtText, "none");
    }

    public void test4NoneHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "having sum(price) > 50";
        runAssertion34(stmtText, "none");
    }

    public void test5DefaultNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "group by symbol " +
                            "output every 1 seconds order by symbol asc";
        runAssertion56(stmtText, "default");
    }

    public void test6DefaultNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "output every 1 seconds order by symbol asc";
        runAssertion56(stmtText, "default");
    }

    public void test7DefaultHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) \n"  +
                            "group by symbol " +
                            "having sum(price) > 50" +
                            "output every 1 seconds";
        runAssertion78(stmtText, "default");
    }

    public void test8DefaultHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "having sum(price) > 50" +
                            "output every 1 seconds";
        runAssertion78(stmtText, "default");
    }

    public void test9AllNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "group by symbol " +
                            "output all every 1 seconds " +
                            "order by symbol";
        runAssertion9_10(stmtText, "all");
    }

    public void test10AllNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "output all every 1 seconds " +
                            "order by symbol";
        runAssertion9_10(stmtText, "all");
    }

    public void test11AllHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "group by symbol " +
                            "having sum(price) > 50 " +
                            "output all every 1 seconds";
        runAssertion11_12(stmtText, "all");
    }

    public void test11AllHavingNoJoinHinted()
    {
        String stmtText = "@Hint('enable_outputlimit_opt') select symbol, sum(price) " +
                "from MarketData.win:time(5.5 sec) " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output all every 1 seconds";
        runAssertion11_12(stmtText, "all");
    }

    public void test12AllHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "having sum(price) > 50 " +
                            "output all every 1 seconds";
        runAssertion11_12(stmtText, "all");
    }

    public void test12AllHavingJoinHinted()
    {
        String stmtText = "@Hint('enable_outputlimit_opt') select symbol, sum(price) " +
                "from MarketData.win:time(5.5 sec), " +
                "SupportBean.win:keepall() where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output all every 1 seconds";
        runAssertion11_12(stmtText, "all");
    }

    public void test13LastNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec)" +
                            "group by symbol " +
                            "output last every 1 seconds " +
                            "order by symbol";
        runAssertion13_14(stmtText, "last");
    }

    public void test14LastNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "output last every 1 seconds " +
                            "order by symbol";
        runAssertion13_14(stmtText, "last");
    }

    public void test15LastHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec)" +
                            "group by symbol " +
                            "having sum(price) > 50 " +
                            "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test15LastHavingNoJoinHinted()
    {
        String stmtText = "@Hint('enable_outputlimit_opt') select symbol, sum(price) " +
                "from MarketData.win:time(5.5 sec)" +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test16LastHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "having sum(price) > 50 " +
                            "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test16LastHavingJoinHinted()
    {
        String stmtText = "@Hint('enable_outputlimit_opt') select symbol, sum(price) " +
                "from MarketData.win:time(5.5 sec), " +
                "SupportBean.win:keepall() where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output last every 1 seconds";
        runAssertion15_16(stmtText, "last");
    }

    public void test17FirstNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "group by symbol " +
                            "output first every 1 seconds";
        runAssertion17(stmtText, "first");
    }

    public void test17FirstNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "output first every 1 seconds";
        runAssertion17(stmtText, "first");
    }

    public void test18SnapshotNoHavingNoJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec) " +
                            "group by symbol " +
                            "output snapshot every 1 seconds " +
                            "order by symbol";
        runAssertion18(stmtText, "snapshot");
    }

    public void test18SnapshotNoHavingJoin()
    {
        String stmtText = "select symbol, sum(price) " +
                            "from MarketData.win:time(5.5 sec), " +
                            "SupportBean.win:keepall() where theString=symbol " +
                            "group by symbol " +
                            "output snapshot every 1 seconds " +
                            "order by symbol";
        runAssertion18(stmtText, "snapshot");
    }

    private void runAssertion12(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(200, 1, new Object[][] {{"IBM", 25d}}, new Object[][] {{"IBM", null}});
        expected.addResultInsRem(800, 1, new Object[][] {{"MSFT", 9d}}, new Object[][] {{"MSFT", null}});
        expected.addResultInsRem(1500, 1, new Object[][] {{"IBM", 49d}}, new Object[][] {{"IBM", 25d}});
        expected.addResultInsRem(1500, 2, new Object[][] {{"YAH", 1d}}, new Object[][] {{"YAH", null}});
        expected.addResultInsRem(2100, 1, new Object[][] {{"IBM", 75d}}, new Object[][] {{"IBM", 49d}});
        expected.addResultInsRem(3500, 1, new Object[][] {{"YAH", 3d}}, new Object[][] {{"YAH", 1d}});
        expected.addResultInsRem(4300, 1, new Object[][] {{"IBM", 97d}}, new Object[][] {{"IBM", 75d}});
        expected.addResultInsRem(4900, 1, new Object[][] {{"YAH", 6d}}, new Object[][] {{"YAH", 3d}});
        expected.addResultInsRem(5700, 0, new Object[][] {{"IBM", 72d}}, new Object[][] {{"IBM", 97d}});
        expected.addResultInsRem(5900, 1, new Object[][] {{"YAH", 7d}}, new Object[][] {{"YAH", 6d}});
        expected.addResultInsRem(6300, 0, new Object[][] {{"MSFT", null}}, new Object[][] {{"MSFT", 9d}});
        expected.addResultInsRem(7000, 0, new Object[][] {{"IBM", 48d}, {"YAH", 6d}}, new Object[][] {{"IBM", 72d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion34(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(2100, 1, new Object[][] {{"IBM", 75d}}, null);
        expected.addResultInsRem(4300, 1, new Object[][] {{"IBM", 97d}}, new Object[][] {{"IBM", 75d}});
        expected.addResultInsRem(5700, 0, new Object[][] {{"IBM", 72d}}, new Object[][] {{"IBM", 97d}});
        expected.addResultInsRem(7000, 0, null, new Object[][] {{"IBM", 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion13_14(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, new Object[][] {{"IBM", 25d}, {"MSFT", 9d}}, new Object[][] {{"IBM", null}, {"MSFT", null}});
        expected.addResultInsRem(2200, 0, new Object[][] {{"IBM", 75d}, {"YAH", 1d}}, new Object[][] {{"IBM", 25d}, {"YAH", null}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, new Object[][] {{"YAH", 3d}}, new Object[][] {{"YAH", 1d}});
        expected.addResultInsRem(5200, 0, new Object[][] {{"IBM", 97d}, {"YAH", 6d}}, new Object[][] {{"IBM", 75d}, {"YAH", 3d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"IBM", 72d}, {"YAH", 7d}}, new Object[][] {{"IBM", 97d}, {"YAH", 6d}});
        expected.addResultInsRem(7200, 0, new Object[][] {{"IBM", 48d}, {"MSFT", null}, {"YAH", 6d}}, new Object[][] {{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion15_16(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, new Object[][] {{"IBM", 75d}}, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsRem(5200, 0, new Object[][] {{"IBM", 97d}}, new Object[][] {{"IBM", 75d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"IBM", 72d}}, new Object[][] {{"IBM", 97d}});
        expected.addResultInsRem(7200, 0, null, new Object[][] {{"IBM", 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion78(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, new Object[][] {{"IBM", 75d}}, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsRem(5200, 0, new Object[][] {{"IBM", 97d}}, new Object[][] {{"IBM", 75d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"IBM", 72d}}, new Object[][] {{"IBM", 97d}});
        expected.addResultInsRem(7200, 0, null, new Object[][] {{"IBM", 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion56(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, new Object[][] {{"IBM", 25d}, {"MSFT", 9d}}, new Object[][] {{"IBM", null}, {"MSFT", null}});
        expected.addResultInsRem(2200, 0, new Object[][] {{"IBM", 49d}, {"IBM", 75d}, {"YAH", 1d}}, new Object[][] {{"IBM", 25d}, {"IBM", 49d}, {"YAH", null}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, new Object[][] {{"YAH", 3d}}, new Object[][] {{"YAH", 1d}});
        expected.addResultInsRem(5200, 0, new Object[][] {{"IBM", 97d}, {"YAH", 6d}}, new Object[][] {{"IBM", 75d}, {"YAH", 3d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"IBM", 72d}, {"YAH", 7d}}, new Object[][] {{"IBM", 97d}, {"YAH", 6d}});
        expected.addResultInsRem(7200, 0, new Object[][] {{"IBM", 48d}, {"MSFT", null}, {"YAH", 6d}}, new Object[][] {{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion9_10(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, new Object[][] {{"IBM", 25d}, {"MSFT", 9d}}, new Object[][] {{"IBM", null}, {"MSFT", null}});
        expected.addResultInsRem(2200, 0, new Object[][] {{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}}, new Object[][] {{"IBM", 25d}, {"MSFT", 9d}, {"YAH", null}});
        expected.addResultInsRem(3200, 0, new Object[][] {{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}}, new Object[][] {{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}});
        expected.addResultInsRem(4200, 0, new Object[][] {{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 3d}}, new Object[][] {{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}});
        expected.addResultInsRem(5200, 0, new Object[][] {{"IBM", 97d}, {"MSFT", 9d}, {"YAH", 6d}}, new Object[][] {{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 3d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}}, new Object[][] {{"IBM", 97d}, {"MSFT", 9d}, {"YAH", 6d}});
        expected.addResultInsRem(7200, 0, new Object[][] {{"IBM", 48d}, {"MSFT", null}, {"YAH", 6d}}, new Object[][] {{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion11_12(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, new Object[][] {{"IBM", 75d}}, null);
        expected.addResultInsRem(3200, 0, new Object[][] {{"IBM", 75d}}, new Object[][] {{"IBM", 75d}});
        expected.addResultInsRem(4200, 0, new Object[][] {{"IBM", 75d}}, new Object[][] {{"IBM", 75d}});
        expected.addResultInsRem(5200, 0, new Object[][] {{"IBM", 97d}}, new Object[][] {{"IBM", 75d}});
        expected.addResultInsRem(6200, 0, new Object[][] {{"IBM", 72d}}, new Object[][] {{"IBM", 97d}});
        expected.addResultInsRem(7200, 0, null, new Object[][] {{"IBM", 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion17(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(200, 1, new Object[][] {{"IBM", 25d}}, new Object[][] {{"IBM", null}});
        expected.addResultInsRem(800, 1, new Object[][] {{"MSFT", 9d}}, new Object[][] {{"MSFT", null}});
        expected.addResultInsRem(1500, 1, new Object[][] {{"IBM", 49d}}, new Object[][] {{"IBM", 25d}});
        expected.addResultInsRem(1500, 2, new Object[][] {{"YAH", 1d}}, new Object[][] {{"YAH", null}});
        expected.addResultInsRem(3500, 1, new Object[][] {{"YAH", 3d}}, new Object[][] {{"YAH", 1d}});
        expected.addResultInsRem(4300, 1, new Object[][] {{"IBM", 97d}}, new Object[][] {{"IBM", 75d}});
        expected.addResultInsRem(4900, 1, new Object[][] {{"YAH", 6d}}, new Object[][] {{"YAH", 3d}});
        expected.addResultInsRem(5700, 0, new Object[][] {{"IBM", 72d}}, new Object[][] {{"IBM", 97d}});
        expected.addResultInsRem(5900, 1, new Object[][] {{"YAH", 7d}}, new Object[][] {{"YAH", 6d}});
        expected.addResultInsRem(6300, 0, new Object[][] {{"MSFT", null}}, new Object[][] {{"MSFT", 9d}});
        expected.addResultInsRem(7000, 0, new Object[][] {{"IBM", 48d}, {"YAH", 6d}}, new Object[][] {{"IBM", 72d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertion18(String stmtText, String outputLimit)
    {
        sendTimer(0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String fields[] = new String[] {"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][] {{"IBM", 25d}, {"MSFT", 9d}});
        expected.addResultInsert(2200, 0, new Object[][] {{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}});
        expected.addResultInsert(3200, 0, new Object[][] {{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}});
        expected.addResultInsert(4200, 0, new Object[][] {{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 3d}});
        expected.addResultInsert(5200, 0, new Object[][] {{"IBM", 97d}, {"MSFT", 9d}, {"YAH", 6d}});
        expected.addResultInsert(6200, 0, new Object[][] {{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}});
        expected.addResultInsert(7200, 0, new Object[][] {{"IBM", 48d}, {"YAH", 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    public void testJoinSortWindow()
    {
        sendTimer(0);

        String fields[] = "symbol,maxVol".split(",");
        String viewExpr = "select irstream symbol, max(price) as maxVol" +
                          " from " + SupportMarketDataBean.class.getName() + ".ext:sort(1, volume desc) as s0," +
                          SupportBean.class.getName() + ".win:keepall() as s1 " +
                          "group by symbol output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("JOIN_KEY", -1));

        sendMDEvent("JOIN_KEY", 1d);
        sendMDEvent("JOIN_KEY", 2d);
        listener.reset();

        // moves all events out of the window,
        sendTimer(1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
        UniformPair<EventBean[]> result = listener.getDataListsFlattened();
        assertEquals(2, result.getFirst().length);
        EPAssertionUtil.assertPropsPerRow(result.getFirst(), fields, new Object[][]{{"JOIN_KEY", 1.0}, {"JOIN_KEY", 2.0}});
        assertEquals(2, result.getSecond().length);
        EPAssertionUtil.assertPropsPerRow(result.getSecond(), fields, new Object[][]{{"JOIN_KEY", null}, {"JOIN_KEY", 1.0}});
    }

    public void testLimitSnapshot()
    {
        sendTimer(0);
        String selectStmt = "select symbol, min(price) as minprice from " + SupportMarketDataBean.class.getName() +
                ".win:time(10 seconds) group by symbol output snapshot every 1 seconds order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);
        sendMDEvent("ABC", 20);

        sendTimer(500);
        sendMDEvent("IBM", 16);
        sendMDEvent("ABC", 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(1000);
        String fields[] = new String[] {"symbol", "minprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 14d}, {"IBM", 16d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(1500);
        sendMDEvent("IBM", 18);
        sendMDEvent("MSFT", 30);

        sendTimer(10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 14d}, {"IBM", 16d}, {"MSFT", 30d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"IBM", 18d}, {"MSFT", 30d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(12000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();
    }

    public void testLimitSnapshotLimit()
    {
        sendTimer(0);
        String selectStmt = "select symbol, min(price) as minprice from " + SupportMarketDataBean.class.getName() +
                ".win:time(10 seconds) as m, " +
                SupportBean.class.getName() + ".win:keepall() as s where s.theString = m.symbol " +
                "group by symbol output snapshot every 1 seconds order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);

        for (String theString : "ABC,IBM,MSFT".split(","))
        {
            epService.getEPRuntime().sendEvent(new SupportBean(theString, 1));
        }

        sendMDEvent("ABC", 20);

        sendTimer(500);
        sendMDEvent("IBM", 16);
        sendMDEvent("ABC", 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(1000);
        String fields[] = new String[] {"symbol", "minprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 14d}, {"IBM", 16d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(1500);
        sendMDEvent("IBM", 18);
        sendMDEvent("MSFT", 30);

        sendTimer(10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 14d}, {"IBM", 16d}, {"MSFT", 30d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(10500);
        sendTimer(11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"IBM", 18d}, {"MSFT", 30d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(11500);
        sendTimer(12000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();
    }

    public void testGroupBy_All()
    {
        String fields[] = "symbol,sum(price)".split(",");
    	String eventName = SupportMarketDataBean.class.getName();
    	String statementString = "select irstream symbol, sum(price) from " + eventName + ".win:length(5) group by symbol output all every 5 events";
    	EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
    	SupportUpdateListener updateListener = new SupportUpdateListener();
    	statement.addListener(updateListener);

    	// send some events and check that only the most recent
    	// ones are kept
    	sendMDEvent("IBM", 1D);
    	sendMDEvent("IBM", 2D);
    	sendMDEvent("HP", 1D);
    	sendMDEvent("IBM", 3D);
    	sendMDEvent("MAC", 1D);

    	assertTrue(updateListener.getAndClearIsInvoked());
    	EventBean[] newData = updateListener.getLastNewData();
    	assertEquals(3, newData.length);
        EPAssertionUtil.assertPropsPerRowAnyOrder(newData, fields, new Object[][]{
                {"IBM", 6d}, {"HP", 1d}, {"MAC", 1d}});
    	EventBean[] oldData = updateListener.getLastOldData();
        EPAssertionUtil.assertPropsPerRowAnyOrder(oldData, fields, new Object[][]{
                {"IBM", null}, {"HP", null}, {"MAC", null}});
    }

    public void testGroupBy_Default()
    {
        String fields[] = "symbol,sum(price)".split(",");
    	String eventName = SupportMarketDataBean.class.getName();
    	String statementString = "select irstream symbol, sum(price) from " + eventName + ".win:length(5) group by symbol output every 5 events";
    	EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
    	SupportUpdateListener updateListener = new SupportUpdateListener();
    	statement.addListener(updateListener);

    	// send some events and check that only the most recent
    	// ones are kept
    	sendMDEvent("IBM", 1D);
    	sendMDEvent("IBM", 2D);
    	sendMDEvent("HP", 1D);
    	sendMDEvent("IBM", 3D);
    	sendMDEvent("MAC", 1D);

    	assertTrue(updateListener.getAndClearIsInvoked());
    	EventBean[] newData = updateListener.getLastNewData();
        EventBean[] oldData = updateListener.getLastOldData();
    	assertEquals(5, newData.length);
        assertEquals(5, oldData.length);
        EPAssertionUtil.assertPropsPerRow(newData, fields, new Object[][]{
                {"IBM", 1d}, {"IBM", 3d}, {"HP", 1d}, {"IBM", 6d}, {"MAC", 1d}});
        EPAssertionUtil.assertPropsPerRow(oldData, fields, new Object[][]{
                {"IBM", null}, {"IBM", 1d}, {"HP", null}, {"IBM", 3d}, {"MAC", null}});        
    }

    public void testMaxTimeWindow()
    {
        sendTimer(0);

        String fields[] = "symbol,maxVol".split(",");
        String viewExpr = "select irstream symbol, max(price) as maxVol" +
                          " from " + SupportMarketDataBean.class.getName() + ".win:time(1 sec) " +
                          "group by symbol output every 1 seconds";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        sendMDEvent("SYM1", 1d);
        sendMDEvent("SYM1", 2d);
        listener.reset();

        // moves all events out of the window,
        sendTimer(1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
        UniformPair<EventBean[]> result = listener.getDataListsFlattened();
        assertEquals(3, result.getFirst().length);
        EPAssertionUtil.assertPropsPerRow(result.getFirst(), fields, new Object[][]{{"SYM1", 1.0}, {"SYM1", 2.0}, {"SYM1", null}});
        assertEquals(3, result.getSecond().length);
        EPAssertionUtil.assertPropsPerRow(result.getSecond(), fields, new Object[][]{{"SYM1", null}, {"SYM1", 1.0}, {"SYM1", 2.0}});
    }

    public void testNoJoinLast() {
        runAssertionNoJoinLast(true);
        runAssertionNoJoinLast(false);
    }

    private void runAssertionNoJoinLast(boolean hinted)
	{
        String hint = hinted ? "@Hint('enable_outputlimit_opt') " : "";
	    String viewExpr = hint + "select irstream symbol," +
	                             "sum(price) as mySum," +
	                             "avg(price) as myAvg " +
	                      "from " + SupportMarketDataBean.class.getName() + ".win:length(3) " +
	                      "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
	                      "group by symbol " +
	                      "output last every 2 events";

	    EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
	    selectTestView.addListener(listener);
	    runAssertionLast(selectTestView);
        selectTestView.destroy();
	}

    public void testNoOutputClauseView()
    {
    	String viewExpr = "select irstream symbol," +
    	"sum(price) as mySum," +
    	"avg(price) as myAvg " +
    	"from " + SupportMarketDataBean.class.getName() + ".win:length(3) " +
    	"where symbol='DELL' or symbol='IBM' or symbol='GE' " +
    	"group by symbol";

    	EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
    	selectTestView.addListener(listener);

    	runAssertionSingle(selectTestView);
    }

    public void testNoOutputClauseJoin()
    {
    	String viewExpr = "select irstream symbol," +
    	"sum(price) as mySum," +
    	"avg(price) as myAvg " +
    	"from " + SupportBeanString.class.getName() + ".win:length(100) as one, " +
    	SupportMarketDataBean.class.getName() + ".win:length(3) as two " +
    	"where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
    	"       and one.theString = two.symbol " +
    	"group by symbol";

    	EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
    	selectTestView.addListener(listener);

    	epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
    	epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
    	epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

    	runAssertionSingle(selectTestView);
    }

    public void testNoJoinAll() {
        runAssertionNoJoinAll(false);
        runAssertionNoJoinAll(true);
    }

	private void runAssertionNoJoinAll(boolean hinted)
    {
        String hint = hinted ? "@Hint('enable_outputlimit_opt') " : "";
        String viewExpr = hint + "select irstream symbol," +
                                 "sum(price) as mySum," +
                                 "avg(price) as myAvg " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:length(5) " +
                          "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                          "group by symbol " +
                          "output all every 2 events";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runAssertionAll(selectTestView);

        selectTestView.destroy();
    }

    public void testJoinLast() {
        runAssertionJoinLast(true);
        runAssertionJoinLast(false);
    }

    public void runAssertionJoinLast(boolean hinted)
	{
        String hint = hinted ? "@Hint('enable_outputlimit_opt') " : "";
        String viewExpr = hint + "select irstream symbol," +
	                             "sum(price) as mySum," +
	                             "avg(price) as myAvg " +
	                      "from " + SupportBeanString.class.getName() + ".win:length(100) as one, " +
	                                SupportMarketDataBean.class.getName() + ".win:length(3) as two " +
	                      "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
	                      "       and one.theString = two.symbol " +
	                      "group by symbol " +
	                      "output last every 2 events";

	    EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
	    selectTestView.addListener(listener);

	    epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
	    epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
	    epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

	    runAssertionLast(selectTestView);

        selectTestView.destroy();
	}

    public void testJoinAll() {
        runAssertionJoinAll(false);
        runAssertionJoinAll(true);
    }

	private void runAssertionJoinAll(boolean hinted)
    {
        String hint = hinted ? "@Hint('enable_outputlimit_opt') " : "";
        String viewExpr = hint + "select irstream symbol," +
                                 "sum(price) as mySum," +
                                 "avg(price) as myAvg " +
                          "from " + SupportBeanString.class.getName() + ".win:length(100) as one, " +
                                    SupportMarketDataBean.class.getName() + ".win:length(5) as two " +
                          "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                          "       and one.theString = two.symbol " +
                          "group by symbol " +
                          "output all every 2 events";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        runAssertionAll(selectTestView);

        selectTestView.destroy();
    }

    private void runAssertionLast(EPStatement selectTestView)
	{
	    // assert select result type
	    assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
	    assertEquals(Double.class, selectTestView.getEventType().getPropertyType("mySum"));
	    assertEquals(Double.class, selectTestView.getEventType().getPropertyType("myAvg"));

	    sendMDEvent(SYMBOL_DELL, 10);
	    assertFalse(listener.isInvoked());

	    sendMDEvent(SYMBOL_DELL, 20);
	    assertEvent(SYMBOL_DELL,
	            null, null,
	            30d, 15d);
	    listener.reset();

	    sendMDEvent(SYMBOL_DELL, 100);
	    assertFalse(listener.isInvoked());

	    sendMDEvent(SYMBOL_DELL, 50);
	    assertEvent(SYMBOL_DELL,
	    		30d, 15d,
	            170d, 170/3d);
	}

    private void runAssertionSingle(EPStatement selectTestView)
	{
	    // assert select result type
	    assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
	    assertEquals(Double.class, selectTestView.getEventType().getPropertyType("mySum"));
	    assertEquals(Double.class, selectTestView.getEventType().getPropertyType("myAvg"));

	    sendMDEvent(SYMBOL_DELL, 10);
	    assertTrue(listener.isInvoked());
	    assertEvent(SYMBOL_DELL,
            	null, null,
            	10d, 10d);

	    sendMDEvent(SYMBOL_IBM, 20);
	    assertTrue(listener.isInvoked());
	    assertEvent(SYMBOL_IBM,
	            	null, null,
	            	20d, 20d);
	}

	private void runAssertionAll(EPStatement selectTestView)
    {
        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("mySum"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("myAvg"));

        sendMDEvent(SYMBOL_IBM, 70);
        assertFalse(listener.isInvoked());

        sendMDEvent(SYMBOL_DELL, 10);
        assertEvents(SYMBOL_IBM,
        		null, null,
        		70d, 70d,
        		SYMBOL_DELL,
                null, null,
                10d, 10d);
	    listener.reset();

        sendMDEvent(SYMBOL_DELL, 20);
        assertFalse(listener.isInvoked());


        sendMDEvent(SYMBOL_DELL, 100);
        assertEvents(SYMBOL_IBM,
        		70d, 70d,
        		70d, 70d,
        		SYMBOL_DELL,
                10d, 10d,
                130d, 130d/3d);
    }

    private void assertEvent(String symbol,
                             Double oldSum, Double oldAvg,
                             Double newSum, Double newAvg)
    {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbol, oldData[0].get("symbol"));
        assertEquals(oldSum, oldData[0].get("mySum"));
        assertEquals(oldAvg, oldData[0].get("myAvg"));

        assertEquals(symbol, newData[0].get("symbol"));
        assertEquals(newSum, newData[0].get("mySum"));
        assertEquals("newData myAvg wrong", newAvg, newData[0].get("myAvg"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void assertEvents(String symbolOne,
                              Double oldSumOne, Double oldAvgOne,
                              double newSumOne, double newAvgOne,
                              String symbolTwo,
                              Double oldSumTwo, Double oldAvgTwo,
                              double newSumTwo, double newAvgTwo)
    {
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetDataListsFlattened(),
                "mySum,myAvg".split(","),
                new Object[][] {{newSumOne, newAvgOne}, {newSumTwo, newAvgTwo}},
                new Object[][] {{oldSumOne, oldAvgOne}, {oldSumTwo, oldAvgTwo}});
    }

    private void sendMDEvent(String symbol, double price)
	{
	    SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
	    epService.getEPRuntime().sendEvent(bean);
	}

    private void sendBeanEvent(String theString, int intPrimitive)
	{
	    epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
	}

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
