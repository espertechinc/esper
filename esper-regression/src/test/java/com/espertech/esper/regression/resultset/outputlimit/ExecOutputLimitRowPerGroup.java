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
package com.espertech.esper.regression.resultset.outputlimit;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.epl.SupportOutputLimitOpt;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.ResultAssertExecution;
import com.espertech.esper.supportregression.patternassert.ResultAssertTestResult;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecOutputLimitRowPerGroup implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    private final static String CATEGORY = "Fully-Aggregated and Grouped";

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MarketData", SupportMarketDataBean.class);
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
        configuration.getEngineDefaults().getByteCodeGeneration().setIncludeDebugSymbols(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionLastNoDataWindow(epService);
        runAssertionOutputFirstHavingJoinNoJoin(epService);
        runAssertionOutputFirstCrontab(epService);
        runAssertionOutputFirstWhenThen(epService);
        runAssertionOutputFirstEveryNEvents(epService);
        runAssertionWildcardRowPerGroup(epService);
        runAssertion1NoneNoHavingNoJoin(epService);
        runAssertion2NoneNoHavingJoin(epService);
        runAssertion3NoneHavingNoJoin(epService);
        runAssertion4NoneHavingJoin(epService);
        runAssertion5DefaultNoHavingNoJoin(epService);
        runAssertion6DefaultNoHavingJoin(epService);
        runAssertion7DefaultHavingNoJoin(epService);
        runAssertion8DefaultHavingJoin(epService);
        runAssertion9AllNoHavingNoJoin(epService);
        runAssertion10AllNoHavingJoin(epService);
        runAssertion11AllHavingNoJoin(epService);
        runAssertion12AllHavingJoin(epService);
        runAssertion13LastNoHavingNoJoin(epService);
        runAssertion14LastNoHavingJoin(epService);
        runAssertion15LastHavingNoJoin(epService);
        runAssertion16LastHavingJoin(epService);
        runAssertion17FirstNoHavingNoJoin(epService);
        runAssertion17FirstNoHavingJoin(epService);
        runAssertion18SnapshotNoHavingNoJoin(epService);
        runAssertion18SnapshotNoHavingJoin(epService);
        runAssertionJoinSortWindow(epService);
        runAssertionLimitSnapshot(epService);
        runAssertionLimitSnapshotLimit(epService);
        runAssertionGroupBy_All(epService);
        runAssertionGroupBy_Default(epService);
        runAssertionMaxTimeWindow(epService);
        runAssertionNoJoinLast(epService);
        runAssertionNoOutputClauseView(epService);
        runAssertionNoOutputClauseJoin(epService);
        runAssertionNoJoinAll(epService);
        runAssertionJoinLast(epService);
        runAssertionJoinAll(epService);
        runAssertionCrontabNumberSetVariations(epService);
    }

    private void runAssertionCrontabNumberSetVariations(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString from SupportBean output all at (*/2, 8:17, lastweekday, [1, 1], *)");
        epService.getEPRuntime().sendEvent(new SupportBean());
        stmt.destroy();

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select theString from SupportBean output all at (*/2, 8:17, 30 weekday, [1, 1], *)");
        epService.getEPRuntime().sendEvent(new SupportBean());
        stmtTwo.destroy();
    }

    private void runAssertionLastNoDataWindow(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String epl = "select theString, intPrimitive as intp from SupportBean group by theString output last every 1 seconds order by theString asc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void runAssertionOutputFirstHavingJoinNoJoin(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String stmtText = "select theString, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events";
        tryOutputFirstHaving(epService, stmtText);

        String stmtTextJoin = "select theString, sum(intPrimitive) as value from MyWindow mv, SupportBean_A#keepall a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events";
        tryOutputFirstHaving(epService, stmtTextJoin);

        String stmtTextOrder = "select theString, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
        tryOutputFirstHaving(epService, stmtTextOrder);

        String stmtTextOrderJoin = "select theString, sum(intPrimitive) as value from MyWindow mv, SupportBean_A#keepall a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
        tryOutputFirstHaving(epService, stmtTextOrderJoin);
    }

    private void tryOutputFirstHaving(EPServiceProvider epService, String statementText) {
        String[] fields = "theString,value".split(",");
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));

        sendBeanEvent(epService, "E1", 10);
        sendBeanEvent(epService, "E2", 15);
        sendBeanEvent(epService, "E1", 10);
        sendBeanEvent(epService, "E2", 5);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E2", 5);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 25});

        sendBeanEvent(epService, "E2", -6);    // to 19, does not count toward condition
        sendBeanEvent(epService, "E2", 2);    // to 21, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent(epService, "E2", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 22});

        sendBeanEvent(epService, "E2", 1);    // to 23, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent(epService, "E2", 1);     // to 24
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 24});

        sendBeanEvent(epService, "E2", -10);    // to 14
        sendBeanEvent(epService, "E2", 10);    // to 24, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent(epService, "E2", 0);    // to 24, counts toward condition
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 24});

        sendBeanEvent(epService, "E2", -10);    // to 14
        sendBeanEvent(epService, "E2", 1);     // to 15
        sendBeanEvent(epService, "E2", 5);     // to 20
        sendBeanEvent(epService, "E2", 0);     // to 20
        sendBeanEvent(epService, "E2", 1);     // to 21    // counts
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E2", 0);    // to 21
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 21});

        // remove events
        sendMDEvent(epService, "E2", 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 21});

        // remove events
        sendMDEvent(epService, "E2", -10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 41});

        // remove events
        sendMDEvent(epService, "E2", -6);  // since there is 3*-10 we output the next one
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 47});

        sendMDEvent(epService, "E2", 2);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOutputFirstCrontab(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String[] fields = "theString,value".split(",");
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, sum(intPrimitive) as value from MyWindow group by theString output first at (*/2, *, *, *, *)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBeanEvent(epService, "E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

        sendTimer(epService, 2 * 60 * 1000 - 1);
        sendBeanEvent(epService, "E1", 11);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 2 * 60 * 1000);
        sendBeanEvent(epService, "E1", 12);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 33});

        sendBeanEvent(epService, "E2", 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});

        sendBeanEvent(epService, "E2", 21);
        sendTimer(epService, 4 * 60 * 1000 - 1);
        sendBeanEvent(epService, "E2", 22);
        sendBeanEvent(epService, "E1", 13);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 4 * 60 * 1000);
        sendBeanEvent(epService, "E2", 23);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 86});
        sendBeanEvent(epService, "E1", 14);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 60});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOutputFirstWhenThen(EPServiceProvider epService) {
        String[] fields = "theString,value".split(",");
        epService.getEPAdministrator().getConfiguration().addVariable("varoutone", boolean.class, false);
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, sum(intPrimitive) as value from MyWindow group by theString output first when varoutone then set varoutone = false");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBeanEvent(epService, "E1", 10);
        sendBeanEvent(epService, "E1", 11);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().setVariableValue("varoutone", true);
        sendBeanEvent(epService, "E1", 12);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 33});
        assertEquals(false, epService.getEPRuntime().getVariableValue("varoutone"));

        epService.getEPRuntime().setVariableValue("varoutone", true);
        sendBeanEvent(epService, "E2", 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});
        assertEquals(false, epService.getEPRuntime().getVariableValue("varoutone"));

        sendBeanEvent(epService, "E1", 13);
        sendBeanEvent(epService, "E2", 21);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOutputFirstEveryNEvents(EPServiceProvider epService) {
        String[] fields = "theString,value".split(",");
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, sum(intPrimitive) as value from MyWindow group by theString output first every 3 events");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBeanEvent(epService, "E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

        sendBeanEvent(epService, "E1", 12);
        sendBeanEvent(epService, "E1", 11);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E1", 13);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 46});

        this.sendMDEvent(epService, "S1", 12);
        this.sendMDEvent(epService, "S1", 11);
        assertFalse(listener.isInvoked());

        this.sendMDEvent(epService, "S1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 13});

        sendBeanEvent(epService, "E1", 14);
        sendBeanEvent(epService, "E1", 15);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E2", 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});

        // test variable
        epService.getEPAdministrator().createEPL("create variable int myvar = 1");
        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select theString, sum(intPrimitive) as value from MyWindow group by theString output first every myvar events");
        stmt.addListener(listener);

        sendBeanEvent(epService, "E3", 10);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E3", 10}});

        sendBeanEvent(epService, "E1", 5);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 47}});

        epService.getEPRuntime().setVariableValue("myvar", 2);

        sendBeanEvent(epService, "E1", 6);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E1", 7);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 60}});

        sendBeanEvent(epService, "E1", 1);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 62}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWildcardRowPerGroup(EPServiceProvider epService) {

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

        stmt.destroy();
    }

    private void runAssertion1NoneNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec)" +
                "group by symbol " +
                "order by symbol asc";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion2NoneNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "order by symbol asc";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion3NoneHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                " having sum(price) > 50";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion4NoneHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion5DefaultNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "output every 1 seconds order by symbol asc";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion6DefaultNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output every 1 seconds order by symbol asc";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion7DefaultHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) \n" +
                "group by symbol " +
                "having sum(price) > 50" +
                "output every 1 seconds";
        tryAssertion78(epService, stmtText, "default");
    }

    private void runAssertion8DefaultHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50" +
                "output every 1 seconds";
        tryAssertion78(epService, stmtText, "default");
    }

    private void runAssertion9AllNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "output all every 1 seconds " +
                "order by symbol";
        tryAssertion9_10(epService, stmtText, "all");
    }

    private void runAssertion10AllNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output all every 1 seconds " +
                "order by symbol";
        tryAssertion9_10(epService, stmtText, "all");
    }

    private void runAssertion11AllHavingNoJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion11AllHavingNoJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion11AllHavingNoJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output all every 1 seconds";
        tryAssertion11_12(epService, stmtText, "all");
    }

    private void runAssertion12AllHavingJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion12AllHavingJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion12AllHavingJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output all every 1 seconds";
        tryAssertion11_12(epService, stmtText, "all");
    }

    private void runAssertion13LastNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec)" +
                "group by symbol " +
                "output last every 1 seconds " +
                "order by symbol";
        tryAssertion13_14(epService, stmtText, "last");
    }

    private void runAssertion14LastNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output last every 1 seconds " +
                "order by symbol";
        tryAssertion13_14(epService, stmtText, "last");
    }

    private void runAssertion15LastHavingNoJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion15LastHavingNoJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion15LastHavingNoJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec)" +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output last every 1 seconds";
        tryAssertion15_16(epService, stmtText, "last");
    }

    private void runAssertion16LastHavingJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion16LastHavingJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion16LastHavingJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output last every 1 seconds";
        tryAssertion15_16(epService, stmtText, "last");
    }

    private void runAssertion17FirstNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "output first every 1 seconds";
        tryAssertion17(epService, stmtText, "first");
    }

    private void runAssertion17FirstNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output first every 1 seconds";
        tryAssertion17(epService, stmtText, "first");
    }

    private void runAssertion18SnapshotNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "output snapshot every 1 seconds " +
                "order by symbol";
        tryAssertion18(epService, stmtText, "snapshot");
    }

    private void runAssertion18SnapshotNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output snapshot every 1 seconds " +
                "order by symbol";
        tryAssertion18(epService, stmtText, "snapshot");
    }

    private void tryAssertion12(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(200, 1, new Object[][]{{"IBM", 25d}}, new Object[][]{{"IBM", null}});
        expected.addResultInsRem(800, 1, new Object[][]{{"MSFT", 9d}}, new Object[][]{{"MSFT", null}});
        expected.addResultInsRem(1500, 1, new Object[][]{{"IBM", 49d}}, new Object[][]{{"IBM", 25d}});
        expected.addResultInsRem(1500, 2, new Object[][]{{"YAH", 1d}}, new Object[][]{{"YAH", null}});
        expected.addResultInsRem(2100, 1, new Object[][]{{"IBM", 75d}}, new Object[][]{{"IBM", 49d}});
        expected.addResultInsRem(3500, 1, new Object[][]{{"YAH", 3d}}, new Object[][]{{"YAH", 1d}});
        expected.addResultInsRem(4300, 1, new Object[][]{{"IBM", 97d}}, new Object[][]{{"IBM", 75d}});
        expected.addResultInsRem(4900, 1, new Object[][]{{"YAH", 6d}}, new Object[][]{{"YAH", 3d}});
        expected.addResultInsRem(5700, 0, new Object[][]{{"IBM", 72d}}, new Object[][]{{"IBM", 97d}});
        expected.addResultInsRem(5900, 1, new Object[][]{{"YAH", 7d}}, new Object[][]{{"YAH", 6d}});
        expected.addResultInsRem(6300, 0, new Object[][]{{"MSFT", null}}, new Object[][]{{"MSFT", 9d}});
        expected.addResultInsRem(7000, 0, new Object[][]{{"IBM", 48d}, {"YAH", 6d}}, new Object[][]{{"IBM", 72d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion34(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(2100, 1, new Object[][]{{"IBM", 75d}}, null);
        expected.addResultInsRem(4300, 1, new Object[][]{{"IBM", 97d}}, new Object[][]{{"IBM", 75d}});
        expected.addResultInsRem(5700, 0, new Object[][]{{"IBM", 72d}}, new Object[][]{{"IBM", 97d}});
        expected.addResultInsRem(7000, 0, null, new Object[][]{{"IBM", 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion13_14(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, new Object[][]{{"IBM", 25d}, {"MSFT", 9d}}, new Object[][]{{"IBM", null}, {"MSFT", null}});
        expected.addResultInsRem(2200, 0, new Object[][]{{"IBM", 75d}, {"YAH", 1d}}, new Object[][]{{"IBM", 25d}, {"YAH", null}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, new Object[][]{{"YAH", 3d}}, new Object[][]{{"YAH", 1d}});
        expected.addResultInsRem(5200, 0, new Object[][]{{"IBM", 97d}, {"YAH", 6d}}, new Object[][]{{"IBM", 75d}, {"YAH", 3d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 72d}, {"YAH", 7d}}, new Object[][]{{"IBM", 97d}, {"YAH", 6d}});
        expected.addResultInsRem(7200, 0, new Object[][]{{"IBM", 48d}, {"MSFT", null}, {"YAH", 6d}}, new Object[][]{{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion15_16(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, new Object[][]{{"IBM", 75d}}, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsRem(5200, 0, new Object[][]{{"IBM", 97d}}, new Object[][]{{"IBM", 75d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 72d}}, new Object[][]{{"IBM", 97d}});
        expected.addResultInsRem(7200, 0, null, new Object[][]{{"IBM", 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion78(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, new Object[][]{{"IBM", 75d}}, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsRem(5200, 0, new Object[][]{{"IBM", 97d}}, new Object[][]{{"IBM", 75d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 72d}}, new Object[][]{{"IBM", 97d}});
        expected.addResultInsRem(7200, 0, null, new Object[][]{{"IBM", 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion56(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, new Object[][]{{"IBM", 25d}, {"MSFT", 9d}}, new Object[][]{{"IBM", null}, {"MSFT", null}});
        expected.addResultInsRem(2200, 0, new Object[][]{{"IBM", 49d}, {"IBM", 75d}, {"YAH", 1d}}, new Object[][]{{"IBM", 25d}, {"IBM", 49d}, {"YAH", null}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, new Object[][]{{"YAH", 3d}}, new Object[][]{{"YAH", 1d}});
        expected.addResultInsRem(5200, 0, new Object[][]{{"IBM", 97d}, {"YAH", 6d}}, new Object[][]{{"IBM", 75d}, {"YAH", 3d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 72d}, {"YAH", 7d}}, new Object[][]{{"IBM", 97d}, {"YAH", 6d}});
        expected.addResultInsRem(7200, 0, new Object[][]{{"IBM", 48d}, {"MSFT", null}, {"YAH", 6d}}, new Object[][]{{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion9_10(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, new Object[][]{{"IBM", 25d}, {"MSFT", 9d}}, new Object[][]{{"IBM", null}, {"MSFT", null}});
        expected.addResultInsRem(2200, 0, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}}, new Object[][]{{"IBM", 25d}, {"MSFT", 9d}, {"YAH", null}});
        expected.addResultInsRem(3200, 0, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}}, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}});
        expected.addResultInsRem(4200, 0, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 3d}}, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}});
        expected.addResultInsRem(5200, 0, new Object[][]{{"IBM", 97d}, {"MSFT", 9d}, {"YAH", 6d}}, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 3d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}}, new Object[][]{{"IBM", 97d}, {"MSFT", 9d}, {"YAH", 6d}});
        expected.addResultInsRem(7200, 0, new Object[][]{{"IBM", 48d}, {"MSFT", null}, {"YAH", 6d}}, new Object[][]{{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion11_12(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, new Object[][]{{"IBM", 75d}}, null);
        expected.addResultInsRem(3200, 0, new Object[][]{{"IBM", 75d}}, new Object[][]{{"IBM", 75d}});
        expected.addResultInsRem(4200, 0, new Object[][]{{"IBM", 75d}}, new Object[][]{{"IBM", 75d}});
        expected.addResultInsRem(5200, 0, new Object[][]{{"IBM", 97d}}, new Object[][]{{"IBM", 75d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 72d}}, new Object[][]{{"IBM", 97d}});
        expected.addResultInsRem(7200, 0, null, new Object[][]{{"IBM", 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion17(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(200, 1, new Object[][]{{"IBM", 25d}}, new Object[][]{{"IBM", null}});
        expected.addResultInsRem(800, 1, new Object[][]{{"MSFT", 9d}}, new Object[][]{{"MSFT", null}});
        expected.addResultInsRem(1500, 1, new Object[][]{{"IBM", 49d}}, new Object[][]{{"IBM", 25d}});
        expected.addResultInsRem(1500, 2, new Object[][]{{"YAH", 1d}}, new Object[][]{{"YAH", null}});
        expected.addResultInsRem(3500, 1, new Object[][]{{"YAH", 3d}}, new Object[][]{{"YAH", 1d}});
        expected.addResultInsRem(4300, 1, new Object[][]{{"IBM", 97d}}, new Object[][]{{"IBM", 75d}});
        expected.addResultInsRem(4900, 1, new Object[][]{{"YAH", 6d}}, new Object[][]{{"YAH", 3d}});
        expected.addResultInsRem(5700, 0, new Object[][]{{"IBM", 72d}}, new Object[][]{{"IBM", 97d}});
        expected.addResultInsRem(5900, 1, new Object[][]{{"YAH", 7d}}, new Object[][]{{"YAH", 6d}});
        expected.addResultInsRem(6300, 0, new Object[][]{{"MSFT", null}}, new Object[][]{{"MSFT", 9d}});
        expected.addResultInsRem(7000, 0, new Object[][]{{"IBM", 48d}, {"YAH", 6d}}, new Object[][]{{"IBM", 72d}, {"YAH", 7d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion18(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 25d}, {"MSFT", 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 3d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 97d}, {"MSFT", 9d}, {"YAH", 6d}});
        expected.addResultInsert(6200, 0, new Object[][]{{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}});
        expected.addResultInsert(7200, 0, new Object[][]{{"IBM", 48d}, {"YAH", 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertionJoinSortWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String[] fields = "symbol,maxVol".split(",");
        String epl = "select irstream symbol, max(price) as maxVol" +
                " from " + SupportMarketDataBean.class.getName() + "#sort(1, volume desc) as s0," +
                SupportBean.class.getName() + "#keepall as s1 " +
                "group by symbol output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("JOIN_KEY", -1));

        sendMDEvent(epService, "JOIN_KEY", 1d);
        sendMDEvent(epService, "JOIN_KEY", 2d);
        listener.reset();

        // moves all events out of the window,
        sendTimer(epService, 1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
        UniformPair<EventBean[]> result = listener.getDataListsFlattened();
        assertEquals(2, result.getFirst().length);
        EPAssertionUtil.assertPropsPerRow(result.getFirst(), fields, new Object[][]{{"JOIN_KEY", 1.0}, {"JOIN_KEY", 2.0}});
        assertEquals(2, result.getSecond().length);
        EPAssertionUtil.assertPropsPerRow(result.getSecond(), fields, new Object[][]{{"JOIN_KEY", null}, {"JOIN_KEY", 1.0}});

        stmt.destroy();
    }

    private void runAssertionLimitSnapshot(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String selectStmt = "select symbol, min(price) as minprice from " + SupportMarketDataBean.class.getName() +
                "#time(10 seconds) group by symbol output snapshot every 1 seconds order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        sendMDEvent(epService, "ABC", 20);

        sendTimer(epService, 500);
        sendMDEvent(epService, "IBM", 16);
        sendMDEvent(epService, "ABC", 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 1000);
        String[] fields = new String[]{"symbol", "minprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 14d}, {"IBM", 16d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 1500);
        sendMDEvent(epService, "IBM", 18);
        sendMDEvent(epService, "MSFT", 30);

        sendTimer(epService, 10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 14d}, {"IBM", 16d}, {"MSFT", 30d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"IBM", 18d}, {"MSFT", 30d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 12000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionLimitSnapshotLimit(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String selectStmt = "select symbol, min(price) as minprice from " + SupportMarketDataBean.class.getName() +
                "#time(10 seconds) as m, " +
                SupportBean.class.getName() + "#keepall as s where s.theString = m.symbol " +
                "group by symbol output snapshot every 1 seconds order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (String theString : "ABC,IBM,MSFT".split(",")) {
            epService.getEPRuntime().sendEvent(new SupportBean(theString, 1));
        }

        sendMDEvent(epService, "ABC", 20);

        sendTimer(epService, 500);
        sendMDEvent(epService, "IBM", 16);
        sendMDEvent(epService, "ABC", 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 1000);
        String[] fields = new String[]{"symbol", "minprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 14d}, {"IBM", 16d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 1500);
        sendMDEvent(epService, "IBM", 18);
        sendMDEvent(epService, "MSFT", 30);

        sendTimer(epService, 10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 14d}, {"IBM", 16d}, {"MSFT", 30d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 10500);
        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"IBM", 18d}, {"MSFT", 30d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 11500);
        sendTimer(epService, 12000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionGroupBy_All(EPServiceProvider epService) {
        String[] fields = "symbol,sum(price)".split(",");
        String eventName = SupportMarketDataBean.class.getName();
        String statementString = "select irstream symbol, sum(price) from " + eventName + "#length(5) group by symbol output all every 5 events";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        // send some events and check that only the most recent
        // ones are kept
        sendMDEvent(epService, "IBM", 1D);
        sendMDEvent(epService, "IBM", 2D);
        sendMDEvent(epService, "HP", 1D);
        sendMDEvent(epService, "IBM", 3D);
        sendMDEvent(epService, "MAC", 1D);

        assertTrue(updateListener.getAndClearIsInvoked());
        EventBean[] newData = updateListener.getLastNewData();
        assertEquals(3, newData.length);
        EPAssertionUtil.assertPropsPerRowAnyOrder(newData, fields, new Object[][]{
                {"IBM", 6d}, {"HP", 1d}, {"MAC", 1d}});
        EventBean[] oldData = updateListener.getLastOldData();
        EPAssertionUtil.assertPropsPerRowAnyOrder(oldData, fields, new Object[][]{
                {"IBM", null}, {"HP", null}, {"MAC", null}});

        statement.destroy();
    }

    private void runAssertionGroupBy_Default(EPServiceProvider epService) {
        String[] fields = "symbol,sum(price)".split(",");
        String eventName = SupportMarketDataBean.class.getName();
        String statementString = "select irstream symbol, sum(price) from " + eventName + "#length(5) group by symbol output every 5 events";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        // send some events and check that only the most recent
        // ones are kept
        sendMDEvent(epService, "IBM", 1D);
        sendMDEvent(epService, "IBM", 2D);
        sendMDEvent(epService, "HP", 1D);
        sendMDEvent(epService, "IBM", 3D);
        sendMDEvent(epService, "MAC", 1D);

        assertTrue(updateListener.getAndClearIsInvoked());
        EventBean[] newData = updateListener.getLastNewData();
        EventBean[] oldData = updateListener.getLastOldData();
        assertEquals(5, newData.length);
        assertEquals(5, oldData.length);
        EPAssertionUtil.assertPropsPerRow(newData, fields, new Object[][]{
                {"IBM", 1d}, {"IBM", 3d}, {"HP", 1d}, {"IBM", 6d}, {"MAC", 1d}});
        EPAssertionUtil.assertPropsPerRow(oldData, fields, new Object[][]{
                {"IBM", null}, {"IBM", 1d}, {"HP", null}, {"IBM", 3d}, {"MAC", null}});

        statement.destroy();
    }

    private void runAssertionMaxTimeWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String[] fields = "symbol,maxVol".split(",");
        String epl = "select irstream symbol, max(price) as maxVol" +
                " from " + SupportMarketDataBean.class.getName() + "#time(1 sec) " +
                "group by symbol output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMDEvent(epService, "SYM1", 1d);
        sendMDEvent(epService, "SYM1", 2d);
        listener.reset();

        // moves all events out of the window,
        sendTimer(epService, 1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
        UniformPair<EventBean[]> result = listener.getDataListsFlattened();
        assertEquals(3, result.getFirst().length);
        EPAssertionUtil.assertPropsPerRow(result.getFirst(), fields, new Object[][]{{"SYM1", 1.0}, {"SYM1", 2.0}, {"SYM1", null}});
        assertEquals(3, result.getSecond().length);
        EPAssertionUtil.assertPropsPerRow(result.getSecond(), fields, new Object[][]{{"SYM1", null}, {"SYM1", 1.0}, {"SYM1", 2.0}});

        stmt.destroy();
    }

    private void runAssertionNoJoinLast(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionNoJoinLast(epService, outputLimitOpt);
        }
    }

    private void tryAssertionNoJoinLast(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "output last every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryAssertionLast(epService, stmt, listener);
        stmt.destroy();
    }

    private void runAssertionNoOutputClauseView(EPServiceProvider epService) {
        String epl = "select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionSingle(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionNoOutputClauseJoin(EPServiceProvider epService) {
        String epl = "select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "       and one.theString = two.symbol " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        tryAssertionSingle(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionNoJoinAll(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionNoJoinAll(epService, outputLimitOpt);
        }
    }

    private void tryAssertionNoJoinAll(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "output all every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionAll(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionJoinLast(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionJoinLast(epService, outputLimitOpt);
        }
    }

    private void tryAssertionJoinLast(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "       and one.theString = two.symbol " +
                "group by symbol " +
                "output last every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        tryAssertionLast(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionJoinAll(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionJoinAll(epService, outputLimitOpt);
        }
    }

    private void tryAssertionJoinAll(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(5) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "       and one.theString = two.symbol " +
                "group by symbol " +
                "output all every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        tryAssertionAll(epService, stmt, listener);

        stmt.destroy();
    }

    private void tryAssertionLast(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("myAvg"));

        sendMDEvent(epService, SYMBOL_DELL, 10);
        assertFalse(listener.isInvoked());

        sendMDEvent(epService, SYMBOL_DELL, 20);
        assertEvent(listener, SYMBOL_DELL,
                null, null,
                30d, 15d);
        listener.reset();

        sendMDEvent(epService, SYMBOL_DELL, 100);
        assertFalse(listener.isInvoked());

        sendMDEvent(epService, SYMBOL_DELL, 50);
        assertEvent(listener, SYMBOL_DELL,
                30d, 15d,
                170d, 170 / 3d);
    }

    private void tryAssertionSingle(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("myAvg"));

        sendMDEvent(epService, SYMBOL_DELL, 10);
        assertTrue(listener.isInvoked());
        assertEvent(listener, SYMBOL_DELL,
                null, null,
                10d, 10d);

        sendMDEvent(epService, SYMBOL_IBM, 20);
        assertTrue(listener.isInvoked());
        assertEvent(listener, SYMBOL_IBM,
                null, null,
                20d, 20d);
    }

    private void tryAssertionAll(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("myAvg"));

        sendMDEvent(epService, SYMBOL_IBM, 70);
        assertFalse(listener.isInvoked());

        sendMDEvent(epService, SYMBOL_DELL, 10);
        assertEvents(listener, SYMBOL_IBM,
                null, null,
                70d, 70d,
                SYMBOL_DELL,
                null, null,
                10d, 10d);
        listener.reset();

        sendMDEvent(epService, SYMBOL_DELL, 20);
        assertFalse(listener.isInvoked());


        sendMDEvent(epService, SYMBOL_DELL, 100);
        assertEvents(listener, SYMBOL_IBM,
                70d, 70d,
                70d, 70d,
                SYMBOL_DELL,
                10d, 10d,
                130d, 130d / 3d);
    }

    private void assertEvent(SupportUpdateListener listener, String symbol,
                             Double oldSum, Double oldAvg,
                             Double newSum, Double newAvg) {
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

    private void assertEvents(SupportUpdateListener listener, String symbolOne,
                              Double oldSumOne, Double oldAvgOne,
                              double newSumOne, double newAvgOne,
                              String symbolTwo,
                              Double oldSumTwo, Double oldAvgTwo,
                              double newSumTwo, double newAvgTwo) {
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetDataListsFlattened(),
                "mySum,myAvg".split(","),
                new Object[][]{{newSumOne, newAvgOne}, {newSumTwo, newAvgTwo}},
                new Object[][]{{oldSumOne, oldAvgOne}, {oldSumTwo, oldAvgTwo}});
    }

    private void sendMDEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendBeanEvent(EPServiceProvider epService, String theString, int intPrimitive) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
