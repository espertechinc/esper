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

public class ExecOutputLimitAggregateGrouped implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";
    private final static String CATEGORY = "Aggregated and Grouped";

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MarketData", SupportMarketDataBean.class);
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionUnaggregatedOutputFirst(epService);
        runAssertionOutputFirstHavingJoinNoJoin(epService);
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
        runAssertionHaving(epService);
        runAssertionHavingJoin(epService);
        runAssertionJoinSortWindow(epService);
        runAssertionLimitSnapshot(epService);
        runAssertionLimitSnapshotJoin(epService);
        runAssertionMaxTimeWindow(epService);
        runAssertionNoJoinLast(epService);
        runAssertionNoOutputClauseView(epService);
        runAssertionNoJoinDefault(epService);
        runAssertionJoinDefault(epService);
        runAssertionNoJoinAll(epService);
        runAssertionJoinAll(epService);
        runAssertionJoinLast(epService);
    }

    private void runAssertionUnaggregatedOutputFirst(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String[] fields = "theString,intPrimitive".split(",");
        String epl = "select * from SupportBean\n" +
                "     group by theString\n" +
                "     output first every 10 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 3});

        sendTimer(epService, 5000);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 4});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        assertFalse(listener.isInvoked());

        sendTimer(epService, 10000);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 6));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 7));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 7});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 8));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 9));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 9});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionOutputFirstHavingJoinNoJoin(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String stmtText = "select theString, longPrimitive, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events";
        tryOutputFirstHaving(epService, stmtText);

        String stmtTextJoin = "select theString, longPrimitive, sum(intPrimitive) as value from MyWindow mv, SupportBean_A#keepall a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events";
        tryOutputFirstHaving(epService, stmtTextJoin);

        String stmtTextOrder = "select theString, longPrimitive, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
        tryOutputFirstHaving(epService, stmtTextOrder);

        String stmtTextOrderJoin = "select theString, longPrimitive, sum(intPrimitive) as value from MyWindow mv, SupportBean_A#keepall a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
        tryOutputFirstHaving(epService, stmtTextOrderJoin);
    }

    private void tryOutputFirstHaving(EPServiceProvider epService, String statementText) {
        String[] fields = "theString,longPrimitive,value".split(",");
        String[] fieldsLimited = "theString,value".split(",");
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on MarketData md delete from MyWindow mw where mw.intPrimitive = md.price");
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));

        sendBeanEvent(epService, "E1", 101, 10);
        sendBeanEvent(epService, "E2", 102, 15);
        sendBeanEvent(epService, "E1", 103, 10);
        sendBeanEvent(epService, "E2", 104, 5);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E2", 105, 5);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 105L, 25});

        sendBeanEvent(epService, "E2", 106, -6);    // to 19, does not count toward condition
        sendBeanEvent(epService, "E2", 107, 2);    // to 21, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent(epService, "E2", 108, 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 108L, 22});

        sendBeanEvent(epService, "E2", 109, 1);    // to 23, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent(epService, "E2", 110, 1);     // to 24
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 110L, 24});

        sendBeanEvent(epService, "E2", 111, -10);    // to 14
        sendBeanEvent(epService, "E2", 112, 10);    // to 24, counts toward condition
        assertFalse(listener.isInvoked());
        sendBeanEvent(epService, "E2", 113, 0);    // to 24, counts toward condition
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 113L, 24});

        sendBeanEvent(epService, "E2", 114, -10);    // to 14
        sendBeanEvent(epService, "E2", 115, 1);     // to 15
        sendBeanEvent(epService, "E2", 116, 5);     // to 20
        sendBeanEvent(epService, "E2", 117, 0);     // to 20
        sendBeanEvent(epService, "E2", 118, 1);     // to 21    // counts
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E2", 119, 0);    // to 21
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 119L, 21});

        // remove events
        sendMDEvent(epService, "E2", 0);   // remove 113, 117, 119 (any order of delete!)
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLimited, new Object[]{"E2", 21});

        // remove events
        sendMDEvent(epService, "E2", -10); // remove 111, 114
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLimited, new Object[]{"E2", 41});

        // remove events
        sendMDEvent(epService, "E2", -6);  // since there is 3*0 we output the next one
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLimited, new Object[]{"E2", 47});

        sendMDEvent(epService, "E2", 2);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion1NoneNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec)" +
                "group by symbol";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion2NoneNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion3NoneHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                " having sum(price) > 50";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion4NoneHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion5DefaultNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "output every 1 seconds";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion6DefaultNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output every 1 seconds";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion7DefaultHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec) \n" +
                "group by symbol " +
                "having sum(price) > 50" +
                "output every 1 seconds";
        tryAssertion78(epService, stmtText, "default");
    }

    private void runAssertion8DefaultHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50" +
                "output every 1 seconds";
        tryAssertion78(epService, stmtText, "default");
    }

    private void runAssertion9AllNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "output all every 1 seconds " +
                "order by symbol";
        tryAssertion9_10(epService, stmtText, "all");
    }

    private void runAssertion10AllNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
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
        String stmtText = opt.getHint() + "select symbol, volume, sum(price) " +
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
        String stmtText = opt.getHint() + "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output all every 1 seconds";
        tryAssertion11_12(epService, stmtText, "all");
    }

    private void runAssertion13LastNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec)" +
                "group by symbol " +
                "output last every 1 seconds " +
                "order by symbol";
        tryAssertion13_14(epService, stmtText, "last");
    }

    private void runAssertion14LastNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
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
        String stmtText = opt.getHint() + "select symbol, volume, sum(price) " +
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
        String stmtText = opt.getHint() + "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50 " +
                "output last every 1 seconds";
        tryAssertion15_16(epService, stmtText, "last");
    }

    private void runAssertion17FirstNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "output first every 1 seconds";
        tryAssertion17(epService, stmtText, "first");
    }

    private void runAssertion17FirstNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output first every 1 seconds";
        tryAssertion17(epService, stmtText, "first");
    }

    private void runAssertion18SnapshotNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "group by symbol " +
                "output snapshot every 1 seconds";
        tryAssertion18(epService, stmtText, "snapshot");
    }

    private void tryAssertion12(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(800, 1, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 49d}});
        expected.addResultInsert(1500, 2, new Object[][]{{"YAH", 10000L, 1d}});
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 3d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 11500L, 6d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 10500L, 7d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 5000L, null}});
        expected.addResultRemove(7000, 0, new Object[][]{{"IBM", 150L, 48d}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion34(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion13_14(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 75d}, {"YAH", 10000L, 1d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}, {"YAH", 11500L, 6d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 10500L, 7d}}, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"IBM", 150L, 48d}, {"MSFT", 5000L, null}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion15_16(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion78(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion56(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 150L, 49d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 75d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}, {"YAH", 11500L, 6d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 10500L, 7d}}, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"MSFT", 5000L, null}, {"IBM", 150L, 48d}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion9_10(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 150L, 49d}, {"IBM", 155L, 75d}, {"MSFT", 5000L, 9d}, {"YAH", 10000L, 1d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 155L, 75d}, {"MSFT", 5000L, 9d}, {"YAH", 10000L, 1d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 155L, 75d}, {"MSFT", 5000L, 9d}, {"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}, {"MSFT", 5000L, 9d}, {"YAH", 11500L, 6d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 150L, 72d}, {"MSFT", 5000L, 9d}, {"YAH", 10500L, 7d}}, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, new Object[][]{{"IBM", 150L, 48d}, {"MSFT", 5000L, null}, {"YAH", 10500L, 6d}}, new Object[][]{{"IBM", 150L, 48d}, {"MSFT", 5000L, null}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion11_12(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 150L, 72d}}, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion17(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(800, 1, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 49d}});
        expected.addResultInsert(1500, 2, new Object[][]{{"YAH", 10000L, 1d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 3d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 11500L, 6d}});
        expected.addResultInsert(5700, 0, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 10500L, 7d}});
        expected.addResultInsert(6300, 0, new Object[][]{{"MSFT", 5000L, null}});
        expected.addResultInsert(7000, 0, new Object[][]{{"IBM", 150L, 48d}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion18(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 100L, 75d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 75d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 75d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 100L, 75d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 75d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 75d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 100L, 75d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 75d}, {"YAH", 10000L, 3d}, {"IBM", 155L, 75d}, {"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 100L, 97d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 97d}, {"YAH", 10000L, 6d}, {"IBM", 155L, 97d}, {"YAH", 11000L, 6d}, {"IBM", 150L, 97d}, {"YAH", 11500L, 6d}});
        expected.addResultInsert(6200, 0, new Object[][]{{"MSFT", 5000L, 9d}, {"IBM", 150L, 72d}, {"YAH", 10000L, 7d}, {"IBM", 155L, 72d}, {"YAH", 11000L, 7d}, {"IBM", 150L, 72d}, {"YAH", 11500L, 7d}, {"YAH", 10500L, 7d}});
        expected.addResultInsert(7200, 0, new Object[][]{{"IBM", 155L, 48d}, {"YAH", 11000L, 6d}, {"IBM", 150L, 48d}, {"YAH", 11500L, 6d}, {"YAH", 10500L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertionHaving(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select irstream symbol, volume, sum(price) as sumprice" +
                " from " + SupportMarketDataBean.class.getName() + "#time(10 sec) " +
                "group by symbol " +
                "having sum(price) >= 10 " +
                "output every 3 events";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionHavingDefault(epService, listener);

        stmt.destroy();
    }

    private void runAssertionHavingJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select irstream symbol, volume, sum(price) as sumprice" +
                " from " + SupportMarketDataBean.class.getName() + "#time(10 sec) as s0," +
                SupportBean.class.getName() + "#keepall as s1 " +
                "where s0.symbol = s1.theString " +
                "group by symbol " +
                "having sum(price) >= 10 " +
                "output every 3 events";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 0));

        tryAssertionHavingDefault(epService, listener);

        stmt.destroy();
    }

    private void runAssertionJoinSortWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select irstream symbol, volume, max(price) as maxVol" +
                " from " + SupportMarketDataBean.class.getName() + "#sort(1, volume) as s0," +
                SupportBean.class.getName() + "#keepall as s1 where s1.theString = s0.symbol " +
                "group by symbol output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("JOIN_KEY", -1));

        sendEvent(epService, "JOIN_KEY", 1d);
        sendEvent(epService, "JOIN_KEY", 2d);
        listener.reset();

        // moves all events out of the window,
        sendTimer(epService, 1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
        UniformPair<EventBean[]> result = listener.getDataListsFlattened();
        assertEquals(2, result.getFirst().length);
        assertEquals(1.0, result.getFirst()[0].get("maxVol"));
        assertEquals(2.0, result.getFirst()[1].get("maxVol"));
        assertEquals(1, result.getSecond().length);
        assertEquals(2.0, result.getSecond()[0].get("maxVol"));

        stmt.destroy();
    }

    private void runAssertionLimitSnapshot(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String selectStmt = "select symbol, volume, sum(price) as sumprice from " + SupportMarketDataBean.class.getName() +
                "#time(10 seconds) group by symbol output snapshot every 1 seconds";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        sendEvent(epService, "s0", 1, 20);

        sendTimer(epService, 500);
        sendEvent(epService, "IBM", 2, 16);
        sendEvent(epService, "s0", 3, 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 1000);
        String[] fields = new String[]{"symbol", "volume", "sumprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"s0", 1L, 34d}, {"IBM", 2L, 16d}, {"s0", 3L, 34d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 1500);
        sendEvent(epService, "MSFT", 4, 18);
        sendEvent(epService, "IBM", 5, 30);

        sendTimer(epService, 10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"s0", 1L, 34d}, {"IBM", 2L, 46d}, {"s0", 3L, 34d}, {"MSFT", 4L, 18d}, {"IBM", 5L, 46d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"MSFT", 4L, 18d}, {"IBM", 5L, 30d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 12000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 13000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionLimitSnapshotJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String selectStmt = "select symbol, volume, sum(price) as sumprice from " + SupportMarketDataBean.class.getName() +
                "#time(10 seconds) as m, " + SupportBean.class.getName() +
                "#keepall as s where s.theString = m.symbol group by symbol output snapshot every 1 seconds order by symbol, volume asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("ABC", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("MSFT", 3));

        sendEvent(epService, "ABC", 1, 20);

        sendTimer(epService, 500);
        sendEvent(epService, "IBM", 2, 16);
        sendEvent(epService, "ABC", 3, 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 1000);
        String[] fields = new String[]{"symbol", "volume", "sumprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 1L, 34d}, {"ABC", 3L, 34d}, {"IBM", 2L, 16d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 1500);
        sendEvent(epService, "MSFT", 4, 18);
        sendEvent(epService, "IBM", 5, 30);

        sendTimer(epService, 10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"ABC", 1L, 34d}, {"ABC", 3L, 34d}, {"IBM", 2L, 46d}, {"IBM", 5L, 46d}, {"MSFT", 4L, 18d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 10500);
        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"IBM", 5L, 30d}, {"MSFT", 4L, 18d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 11500);
        sendTimer(epService, 12000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 13000);
        assertTrue(listener.isInvoked());
        assertNull(listener.getLastNewData());
        assertNull(listener.getLastOldData());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionMaxTimeWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select irstream symbol, " +
                "volume, max(price) as maxVol" +
                " from " + SupportMarketDataBean.class.getName() + "#time(1 sec) " +
                "group by symbol output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "SYM1", 1d);
        sendEvent(epService, "SYM1", 2d);
        listener.reset();

        // moves all events out of the window,
        sendTimer(epService, 1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
        UniformPair<EventBean[]> result = listener.getDataListsFlattened();
        assertEquals(2, result.getFirst().length);
        assertEquals(1.0, result.getFirst()[0].get("maxVol"));
        assertEquals(2.0, result.getFirst()[1].get("maxVol"));
        assertEquals(2, result.getSecond().length);
        assertEquals(null, result.getSecond()[0].get("maxVol"));
        assertEquals(null, result.getSecond()[1].get("maxVol"));

        stmt.destroy();
    }

    private void runAssertionNoJoinLast(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionNoJoinLast(epService, outputLimitOpt);
        }
    }

    private void tryAssertionNoJoinLast(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = opt.getHint() +
                "select symbol, volume, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "output last every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionLast(epService, listener);

        stmt.destroy();
        listener.reset();
    }

    private void assertEvent(SupportUpdateListener listener, String symbol, Double mySum, Long volume) {
        EventBean[] newData = listener.getLastNewData();

        assertEquals(1, newData.length);

        assertEquals(symbol, newData[0].get("symbol"));
        assertEquals(mySum, newData[0].get("mySum"));
        assertEquals(volume, newData[0].get("volume"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void tryAssertionSingle(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("volume"));

        sendEvent(epService, SYMBOL_DELL, 10, 100);
        assertTrue(listener.isInvoked());
        assertEvent(listener, SYMBOL_DELL, 100d, 10L);

        sendEvent(epService, SYMBOL_IBM, 15, 50);
        assertEvent(listener, SYMBOL_IBM, 50d, 15L);
    }

    private void runAssertionNoOutputClauseView(EPServiceProvider epService) {
        String epl = "select symbol, volume, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionSingle(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionNoJoinDefault(EPServiceProvider epService) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = "select symbol, volume, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "output every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionDefault(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionJoinDefault(EPServiceProvider epService) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = "select symbol, volume, sum(price) as mySum " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(5) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol " +
                "output every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        tryAssertionDefault(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionNoJoinAll(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionNoJoinAll(epService, outputLimitOpt);
        }
    }

    private void tryAssertionNoJoinAll(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = opt.getHint() + "select symbol, volume, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "output all every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionAll(epService, stmt, listener);

        stmt.destroy();
        listener.reset();
    }

    private void runAssertionJoinAll(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionJoinAll(epService, outputLimitOpt);
        }
    }

    private void tryAssertionJoinAll(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = opt.getHint() + "select symbol, volume, sum(price) as mySum " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(5) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol " +
                "output all every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        tryAssertionAll(epService, stmt, listener);

        stmt.destroy();
        listener.reset();
    }

    private void runAssertionJoinLast(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionJoinLast(epService, outputLimitOpt);
        }
    }

    private void tryAssertionJoinLast(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = opt.getHint() +
                "select symbol, volume, sum(price) as mySum " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(5) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol " +
                "output last every 2 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        tryAssertionLast(epService, listener);

        stmt.destroy();
    }

    private void tryAssertionHavingDefault(EPServiceProvider epService, SupportUpdateListener listener) {
        sendEvent(epService, "IBM", 1, 5);
        sendEvent(epService, "IBM", 2, 6);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "IBM", 3, -3);
        String[] fields = "symbol,volume,sumprice".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"IBM", 2L, 11.0});

        sendTimer(epService, 5000);
        sendEvent(epService, "IBM", 4, 10);
        sendEvent(epService, "IBM", 5, 0);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "IBM", 6, 1);
        assertEquals(3, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"IBM", 4L, 18.0});
        EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"IBM", 5L, 18.0});
        EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields, new Object[]{"IBM", 6L, 19.0});
        listener.reset();

        sendTimer(epService, 11000);
        assertEquals(3, listener.getLastOldData().length);
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"IBM", 1L, 11.0});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"IBM", 2L, 11.0});
        listener.reset();
    }

    private void tryAssertionDefault(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("volume"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));

        sendEvent(epService, SYMBOL_IBM, 500, 20);
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(epService, SYMBOL_DELL, 10000, 51);
        String[] fields = "symbol,volume,mySum".split(",");
        UniformPair<EventBean[]> events = listener.getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_IBM)) {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_IBM, 500L, 20.0}, {SYMBOL_DELL, 10000L, 51.0}});
        } else {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_DELL, 10000L, 51.0}, {SYMBOL_IBM, 500L, 20.0}});
        }
        assertNull(listener.getLastOldData());

        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 20000, 52);
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(epService, SYMBOL_DELL, 40000, 45);
        events = listener.getDataListsFlattened();
        EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_DELL, 20000L, 51.0 + 52.0}, {SYMBOL_DELL, 40000L, 51.0 + 52.0 + 45.0}});
        assertNull(listener.getLastOldData());
    }

    private void tryAssertionAll(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("volume"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));

        sendEvent(epService, SYMBOL_IBM, 500, 20);
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(epService, SYMBOL_DELL, 10000, 51);
        String[] fields = "symbol,volume,mySum".split(",");
        UniformPair<EventBean[]> events = listener.getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_IBM)) {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_IBM, 500L, 20.0}, {SYMBOL_DELL, 10000L, 51.0}});
        } else {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_DELL, 10000L, 51.0}, {SYMBOL_IBM, 500L, 20.0}});
        }
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 20000, 52);
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(epService, SYMBOL_DELL, 40000, 45);
        events = listener.getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_IBM)) {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_IBM, 500L, 20.0}, {SYMBOL_DELL, 20000L, 51.0 + 52.0}, {SYMBOL_DELL, 40000L, 51.0 + 52.0 + 45.0}});
        } else {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_DELL, 20000L, 51.0 + 52.0}, {SYMBOL_DELL, 40000L, 51.0 + 52.0 + 45.0}, {SYMBOL_IBM, 500L, 20.0}});
        }
        assertNull(listener.getLastOldData());
    }

    private void tryAssertionLast(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "symbol,volume,mySum".split(",");
        sendEvent(epService, SYMBOL_DELL, 10000, 51);
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(epService, SYMBOL_DELL, 20000, 52);
        UniformPair<EventBean[]> events = listener.getDataListsFlattened();
        EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_DELL, 20000L, 103.0}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 30000, 70);
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(epService, SYMBOL_IBM, 10000, 20);
        events = listener.getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_DELL)) {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_DELL, 30000L, 173.0}, {SYMBOL_IBM, 10000L, 20.0}});
        } else {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                    new Object[][]{{SYMBOL_IBM, 10000L, 20.0}, {SYMBOL_DELL, 30000L, 173.0}});
        }
        assertNull(listener.getLastOldData());
    }


    private void sendEvent(EPServiceProvider epService, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendBeanEvent(EPServiceProvider epService, String theString, long longPrimitive, int intPrimitive) {
        SupportBean b = new SupportBean();
        b.setTheString(theString);
        b.setLongPrimitive(longPrimitive);
        b.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(b);
    }

    private void sendMDEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }
}
