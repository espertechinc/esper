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
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportOutputLimitOpt;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.ResultAssertExecution;
import com.espertech.esper.supportregression.patternassert.ResultAssertExecutionTestSelector;
import com.espertech.esper.supportregression.patternassert.ResultAssertTestResult;

import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecOutputLimitSimple implements RegressionExecution {
    private final static String JOIN_KEY = "KEY";
    private final static String CATEGORY = "Un-aggregated and Un-grouped";

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MarketData", SupportMarketDataBean.class);
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
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
        runAssertion17FirstNoHavingNoJoinIStream(epService);
        runAssertion17FirstNoHavingJoinIStream(epService);
        runAssertion17FirstNoHavingNoJoinIRStream(epService);
        runAssertion17FirstNoHavingJoinIRStream(epService);
        runAssertion18SnapshotNoHavingNoJoin(epService);
        runAssertionOutputFirstUnidirectionalJoinNamedWindow(epService);
        runAssertionOutputEveryTimePeriod(epService);
        runAssertionOutputEveryTimePeriodVariable(epService);
        runAssertionAggAllHaving(epService);
        runAssertionAggAllHavingJoin(epService);
        runAssertionIterator(epService);
        runAssertionLimitEventJoin(epService);
        runAssertionLimitTime(epService);
        runAssertionTimeBatchOutputEvents(epService);
        runAssertionSimpleNoJoinAll(epService);
        runAssertionSimpleNoJoinLast(epService);
        runAssertionSimpleJoinAll(epService);
        runAssertionSimpleJoinLast(epService);
        runAssertionLimitEventSimple(epService);
        runAssertionLimitSnapshot(epService);
        runAssertionFirstSimpleHavingAndNoHaving(epService);
        runAssertionLimitSnapshotJoin(epService);
        runAssertionSnapshotMonthScoped(epService);
        runAssertionFirstMonthScoped(epService);
    }

    private void runAssertion1NoneNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec)";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion2NoneNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion3NoneHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec) " +
                " having price > 10";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion4NoneHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                " having price > 10";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion5DefaultNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec) " +
                "output every 1 seconds";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion6DefaultNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output every 1 seconds";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion7DefaultHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec) \n" +
                "having price > 10" +
                "output every 1 seconds";
        tryAssertion78(epService, stmtText, "default");
    }

    private void runAssertion8DefaultHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "having price > 10" +
                "output every 1 seconds";
        tryAssertion78(epService, stmtText, "default");
    }

    private void runAssertion9AllNoHavingNoJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion9AllNoHavingNoJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion9AllNoHavingNoJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, volume, price " +
                "from MarketData#time(5.5 sec) " +
                "output all every 1 seconds";
        tryAssertion56(epService, stmtText, "all");
    }

    private void runAssertion10AllNoHavingJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion10AllNoHavingJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion10AllNoHavingJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, volume, price " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output all every 1 seconds";
        tryAssertion56(epService, stmtText, "all");
    }

    private void runAssertion11AllHavingNoJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion11AllHavingNoJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion11AllHavingNoJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, volume, price " +
                "from MarketData#time(5.5 sec) " +
                "having price > 10" +
                "output all every 1 seconds";
        tryAssertion78(epService, stmtText, "all");
    }

    private void runAssertion12AllHavingJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion12AllHavingJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion12AllHavingJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, volume, price " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "having price > 10" +
                "output all every 1 seconds";
        tryAssertion78(epService, stmtText, "all");
    }

    private void runAssertion13LastNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec)" +
                "output last every 1 seconds";
        tryAssertion13_14(epService, stmtText, "last");
    }

    private void runAssertion14LastNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output last every 1 seconds";
        tryAssertion13_14(epService, stmtText, "last");
    }

    private void runAssertion15LastHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec)" +
                "having price > 10 " +
                "output last every 1 seconds";
        tryAssertion15_16(epService, stmtText, "last");
    }

    private void runAssertion16LastHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "having price > 10 " +
                "output last every 1 seconds";
        tryAssertion15_16(epService, stmtText, "last");
    }

    private void runAssertion17FirstNoHavingNoJoinIStream(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec) " +
                "output first every 1 seconds";
        tryAssertion17IStream(epService, stmtText, "first");
    }

    private void runAssertion17FirstNoHavingJoinIStream(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec)," +
                "SupportBean#keepall where theString=symbol " +
                "output first every 1 seconds";
        tryAssertion17IStream(epService, stmtText, "first");
    }

    private void runAssertion17FirstNoHavingNoJoinIRStream(EPServiceProvider epService) {
        String stmtText = "select irstream symbol, volume, price " +
                "from MarketData#time(5.5 sec) " +
                "output first every 1 seconds";
        tryAssertion17IRStream(epService, stmtText, "first");
    }

    private void runAssertion17FirstNoHavingJoinIRStream(EPServiceProvider epService) {
        String stmtText = "select irstream symbol, volume, price " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output first every 1 seconds";
        tryAssertion17IRStream(epService, stmtText, "first");
    }

    private void runAssertion18SnapshotNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, price " +
                "from MarketData#time(5.5 sec) " +
                "output snapshot every 1 seconds";
        tryAssertion18(epService, stmtText, "first");
    }

    private void runAssertionOutputFirstUnidirectionalJoinNamedWindow(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String[] fields = "c0,c1".split(",");
        String epl =
                "create window MyWindow#keepall as SupportBean_S0;\n" +
                        "insert into MyWindow select * from SupportBean_S0;\n" +
                        "@name('join') select myWindow.id as c0, s1.id as c1\n" +
                        "from SupportBean_S1 as s1 unidirectional, MyWindow as myWindow\n" +
                        "where myWindow.p00 = s1.p10\n" +
                        "output first every 1 minutes;";
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("join").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "a"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "b"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1000, "b"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 1000});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1001, "b"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1002, "a"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(60 * 1000));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1003, "a"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 1003});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1004, "a"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(120 * 1000));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1005, "a"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 1005});

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(result.getDeploymentId());
    }

    private void runAssertionOutputEveryTimePeriod(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));

        String stmtText = "select symbol from MarketData#keepall output snapshot every 1 day 2 hours 3 minutes 4 seconds 5 milliseconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        sendMDEvent(epService, "E1", 0);

        long deltaSec = 26 * 60 * 60 + 3 * 60 + 4;
        long deltaMSec = deltaSec * 1000 + 5 + 2000;
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(deltaMSec - 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(deltaMSec));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("symbol"));

        stmt.destroy();
    }

    private void runAssertionOutputEveryTimePeriodVariable(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        epService.getEPAdministrator().getConfiguration().addVariable("D", int.class, 1);
        epService.getEPAdministrator().getConfiguration().addVariable("H", int.class, 2);
        epService.getEPAdministrator().getConfiguration().addVariable("M", int.class, 3);
        epService.getEPAdministrator().getConfiguration().addVariable("S", int.class, 4);
        epService.getEPAdministrator().getConfiguration().addVariable("MS", int.class, 5);

        String stmtText = "select symbol from MarketData#keepall output snapshot every D days H hours M minutes S seconds MS milliseconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        sendMDEvent(epService, "E1", 0);

        long deltaSec = 26 * 60 * 60 + 3 * 60 + 4;
        long deltaMSec = deltaSec * 1000 + 5 + 2000;
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(deltaMSec - 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(deltaMSec));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("symbol"));

        // test statement model
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());

        stmt.destroy();
    }

    private void tryAssertion34(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"symbol", "volume", "price"};

        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 24d}});
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 155L, 26d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7000, 0, new Object[][]{{"IBM", 150L, 24d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion15_16(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);

        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"IBM", 150L, 24d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion12(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(800, 1, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 24d}});
        expected.addResultInsert(1500, 2, new Object[][]{{"YAH", 10000L, 1d}});
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 155L, 26d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 11500L, 3d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 10500L, 1d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultRemove(7000, 0, new Object[][]{{"IBM", 150L, 24d}, {"YAH", 10000L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion13_14(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"YAH", 11500L, 3d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 10500L, 1d}}, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"YAH", 10000L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion78(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 150L, 24d}, {"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"IBM", 150L, 24d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion56(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 22d}, {"YAH", 11500L, 3d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 10500L, 1d}}, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion17IStream(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 24d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 10500L, 1.0d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected, ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED);
        execution.execute(false);
    }

    private void tryAssertion17IRStream(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 24d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 5000L, 9d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected, ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED);
        execution.execute(false);
    }

    private void tryAssertion18(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}, {"YAH", 11000L, 2d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}, {"YAH", 11000L, 2d}, {"IBM", 150L, 22d}, {"YAH", 11500L, 3d}});
        expected.addResultInsert(6200, 0, new Object[][]{{"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}, {"YAH", 11000L, 2d}, {"IBM", 150L, 22d}, {"YAH", 11500L, 3d}, {"YAH", 10500L, 1d}});
        expected.addResultInsert(7200, 0, new Object[][]{{"IBM", 155L, 26d}, {"YAH", 11000L, 2d}, {"IBM", 150L, 22d}, {"YAH", 11500L, 3d}, {"YAH", 10500L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertionAggAllHaving(EPServiceProvider epService) {
        String stmtText = "select symbol, volume " +
                "from " + SupportMarketDataBean.class.getName() + "#length(10) as two " +
                "having volume > 0 " +
                "output every 5 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"symbol", "volume"};

        sendMDEvent(epService, "S0", 20);
        sendMDEvent(epService, "IBM", -1);
        sendMDEvent(epService, "MSFT", -2);
        sendMDEvent(epService, "YAH", 10);
        assertFalse(listener.isInvoked());

        sendMDEvent(epService, "IBM", 0);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"S0", 20L}, {"YAH", 10L}});

        stmt.destroy();
    }

    private void runAssertionAggAllHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume " +
                "from " + SupportMarketDataBean.class.getName() + "#length(10) as one," +
                SupportBean.class.getName() + "#length(10) as two " +
                "where one.symbol=two.theString " +
                "having volume > 0 " +
                "output every 5 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"symbol", "volume"};
        epService.getEPRuntime().sendEvent(new SupportBean("S0", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("MSFT", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("YAH", 0));

        sendMDEvent(epService, "S0", 20);
        sendMDEvent(epService, "IBM", -1);
        sendMDEvent(epService, "MSFT", -2);
        sendMDEvent(epService, "YAH", 10);
        assertFalse(listener.isInvoked());

        sendMDEvent(epService, "IBM", 0);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"S0", 20L}, {"YAH", 10L}});

        stmt.destroy();
    }

    private void runAssertionIterator(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "price"};
        String statementString = "select symbol, theString, price from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 3 events";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));

        // Output limit clause ignored when iterating, for both joins and no-join
        sendEvent(epService, "CAT", 50);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{"CAT", 50d}});

        sendEvent(epService, "CAT", 60);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, new Object[][]{{"CAT", 50d}, {"CAT", 60d}});

        sendEvent(epService, "IBM", 70);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, new Object[][]{{"CAT", 50d}, {"CAT", 60d}, {"IBM", 70d}});

        sendEvent(epService, "IBM", 90);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, new Object[][]{{"CAT", 50d}, {"CAT", 60d}, {"IBM", 70d}, {"IBM", 90d}});

        statement.destroy();
    }

    private void runAssertionLimitEventJoin(EPServiceProvider epService) {
        String eventName1 = SupportBean.class.getName();
        String eventName2 = SupportBean_A.class.getName();
        String joinStatement =
                "select * from " +
                        eventName1 + "#length(5) as event1," +
                        eventName2 + "#length(5) as event2" +
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
        sendJoinEvents(epService, "IBM");

        assertTrue(updateListener1.getAndClearIsInvoked());
        assertEquals(1, updateListener1.getLastNewData().length);
        assertNull(updateListener1.getLastOldData());

        assertFalse(updateListener3.getAndClearIsInvoked());
        assertNull(updateListener3.getLastNewData());
        assertNull(updateListener3.getLastOldData());

        // send event 2
        sendJoinEvents(epService, "MSFT");

        assertTrue(updateListener1.getAndClearIsInvoked());
        assertEquals(1, updateListener1.getLastNewData().length);
        assertNull(updateListener1.getLastOldData());

        assertFalse(updateListener3.getAndClearIsInvoked());
        assertNull(updateListener3.getLastNewData());
        assertNull(updateListener3.getLastOldData());

        // send event 3
        sendJoinEvents(epService, "YAH");

        assertTrue(updateListener1.getAndClearIsInvoked());
        assertEquals(1, updateListener1.getLastNewData().length);
        assertNull(updateListener1.getLastOldData());

        assertTrue(updateListener3.getAndClearIsInvoked());
        assertEquals(3, updateListener3.getLastNewData().length);
        assertNull(updateListener3.getLastOldData());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLimitTime(EPServiceProvider epService) {
        String eventName = SupportBean.class.getName();
        String selectStatement = "select * from " + eventName + "#length(5)";

        // test integer seconds
        String statementString1 = selectStatement +
                " output every 3 seconds";
        timeCallback(epService, statementString1, 3000);

        // test fractional seconds
        String statementString2 = selectStatement +
                " output every 3.3 seconds";
        timeCallback(epService, statementString2, 3300);

        // test integer minutes
        String statementString3 = selectStatement +
                " output every 2 minutes";
        timeCallback(epService, statementString3, 120000);

        // test fractional minutes
        String statementString4 =
                "select * from " +
                        eventName + "#length(5)" +
                        " output every .05 minutes";
        timeCallback(epService, statementString4, 3000);
    }

    private void runAssertionTimeBatchOutputEvents(EPServiceProvider epService) {
        String stmtText = "select * from " + SupportBean.class.getName() + "#time_batch(10 seconds) output every 10 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(epService, 0);
        sendTimer(epService, 10000);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 20000);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "e1");
        sendTimer(epService, 30000);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 40000);
        EventBean[] newEvents = listener.getAndResetLastNewData();
        assertEquals(1, newEvents.length);
        assertEquals("e1", newEvents[0].get("theString"));
        listener.reset();

        sendTimer(epService, 50000);
        assertTrue(listener.isInvoked());
        listener.reset();

        sendTimer(epService, 60000);
        assertTrue(listener.isInvoked());
        listener.reset();

        sendTimer(epService, 70000);
        assertTrue(listener.isInvoked());
        listener.reset();

        sendEvent(epService, "e2");
        sendEvent(epService, "e3");
        sendTimer(epService, 80000);
        newEvents = listener.getAndResetLastNewData();
        assertEquals(2, newEvents.length);
        assertEquals("e2", newEvents[0].get("theString"));
        assertEquals("e3", newEvents[1].get("theString"));

        sendTimer(epService, 90000);
        assertTrue(listener.isInvoked());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionSimpleNoJoinAll(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionSimpleNoJoinAll(epService, outputLimitOpt);
        }
    }

    private void tryAssertionSimpleNoJoinAll(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "select longBoxed " +
                "from " + SupportBean.class.getName() + "#length(3) " +
                "output all every 2 events";

        tryAssertAll(epService, createStmtAndListenerNoJoin(epService, epl));

        epl = opt.getHint() + "select longBoxed " +
                "from " + SupportBean.class.getName() + "#length(3) " +
                "output every 2 events";

        tryAssertAll(epService, createStmtAndListenerNoJoin(epService, epl));

        epl = opt.getHint() + "select * " +
                "from " + SupportBean.class.getName() + "#length(3) " +
                "output every 2 events";

        tryAssertAll(epService, createStmtAndListenerNoJoin(epService, epl));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSimpleNoJoinLast(EPServiceProvider epService) {
        String epl = "select longBoxed " +
                "from " + SupportBean.class.getName() + "#length(3) " +
                "output last every 2 events";

        tryAssertLast(epService, createStmtAndListenerNoJoin(epService, epl));

        epl = "select * " +
                "from " + SupportBean.class.getName() + "#length(3) " +
                "output last every 2 events";

        tryAssertLast(epService, createStmtAndListenerNoJoin(epService, epl));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSimpleJoinAll(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionSimpleJoinAll(epService, outputLimitOpt);
        }
    }

    private void tryAssertionSimpleJoinAll(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "select longBoxed  " +
                "from " + SupportBeanString.class.getName() + "#length(3) as one, " +
                SupportBean.class.getName() + "#length(3) as two " +
                "output all every 2 events";

        tryAssertAll(epService, createStmtAndListenerJoin(epService, epl));
        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportUpdateListener createStmtAndListenerNoJoin(EPServiceProvider epService, String epl) {
        SupportUpdateListener updateListener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(updateListener);
        return updateListener;
    }

    private void tryAssertAll(EPServiceProvider epService, SupportUpdateListener updateListener) {
        // send an event
        sendEvent(epService, 1);

        // check no update
        assertFalse(updateListener.getAndClearIsInvoked());

        // send another event
        sendEvent(epService, 2);

        // check update, all events present
        assertTrue(updateListener.getAndClearIsInvoked());
        assertEquals(2, updateListener.getLastNewData().length);
        assertEquals(1L, updateListener.getLastNewData()[0].get("longBoxed"));
        assertEquals(2L, updateListener.getLastNewData()[1].get("longBoxed"));
        assertNull(updateListener.getLastOldData());
    }

    private void sendEvent(EPServiceProvider epService, long longBoxed, int intBoxed, short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, long longBoxed) {
        sendEvent(epService, longBoxed, 0, (short) 0);
    }

    private void runAssertionSimpleJoinLast(EPServiceProvider epService) {
        String epl = "select longBoxed " +
                "from " + SupportBeanString.class.getName() + "#length(3) as one, " +
                SupportBean.class.getName() + "#length(3) as two " +
                "output last every 2 events";

        tryAssertLast(epService, createStmtAndListenerJoin(epService, epl));
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLimitEventSimple(EPServiceProvider epService) {
        SupportUpdateListener updateListener1 = new SupportUpdateListener();
        SupportUpdateListener updateListener2 = new SupportUpdateListener();
        SupportUpdateListener updateListener3 = new SupportUpdateListener();

        String eventName = SupportBean.class.getName();
        String selectStmt = "select * from " + eventName + "#length(5)";
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
        sendEvent(epService, "IBM");

        assertTrue(updateListener1.getAndClearIsInvoked());
        assertEquals(1, updateListener1.getLastNewData().length);
        assertNull(updateListener1.getLastOldData());

        assertFalse(updateListener2.getAndClearIsInvoked());
        assertNull(updateListener2.getLastNewData());
        assertNull(updateListener2.getLastOldData());

        assertFalse(updateListener3.getAndClearIsInvoked());
        assertNull(updateListener3.getLastNewData());
        assertNull(updateListener3.getLastOldData());

        // send event 2
        sendEvent(epService, "MSFT");

        assertTrue(updateListener1.getAndClearIsInvoked());
        assertEquals(1, updateListener1.getLastNewData().length);
        assertNull(updateListener1.getLastOldData());

        assertTrue(updateListener2.getAndClearIsInvoked());
        assertEquals(2, updateListener2.getLastNewData().length);
        assertNull(updateListener2.getLastOldData());

        assertFalse(updateListener3.getAndClearIsInvoked());

        // send event 3
        sendEvent(epService, "YAH");

        assertTrue(updateListener1.getAndClearIsInvoked());
        assertEquals(1, updateListener1.getLastNewData().length);
        assertNull(updateListener1.getLastOldData());

        assertFalse(updateListener2.getAndClearIsInvoked());

        assertTrue(updateListener3.getAndClearIsInvoked());
        assertEquals(3, updateListener3.getLastNewData().length);
        assertNull(updateListener3.getLastOldData());
    }

    private void runAssertionLimitSnapshot(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        sendTimer(epService, 0);
        String selectStmt = "select * from " + SupportBean.class.getName() + "#time(10) output snapshot every 3 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);

        sendTimer(epService, 1000);
        sendEvent(epService, "IBM");
        sendEvent(epService, "MSFT");
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 2000);
        sendEvent(epService, "YAH");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"IBM"}, {"MSFT"}, {"YAH"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 3000);
        sendEvent(epService, "s4");
        sendEvent(epService, "s5");
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 10000);
        sendEvent(epService, "s6");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"IBM"}, {"MSFT"}, {"YAH"}, {"s4"}, {"s5"}, {"s6"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 11000);
        sendEvent(epService, "s7");
        assertFalse(listener.isInvoked());

        sendEvent(epService, "s8");
        assertFalse(listener.isInvoked());

        sendEvent(epService, "s9");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"YAH"}, {"s4"}, {"s5"}, {"s6"}, {"s7"}, {"s8"}, {"s9"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 14000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s6"}, {"s7"}, {"s8"}, {"s9"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(epService, "s10");
        sendEvent(epService, "s11");
        assertFalse(listener.isInvoked());

        sendTimer(epService, 23000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s10"}, {"s11"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(epService, "s12");
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionFirstSimpleHavingAndNoHaving(EPServiceProvider epService) {
        tryAssertionFirstSimpleHavingAndNoHaving(epService, "");
        tryAssertionFirstSimpleHavingAndNoHaving(epService, "having intPrimitive != 0");
    }

    private void tryAssertionFirstSimpleHavingAndNoHaving(EPServiceProvider epService, String having) {
        String epl = "select theString from SupportBean " + having + " output first every 3 events";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E4"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionLimitSnapshotJoin(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        sendTimer(epService, 0);
        String selectStmt = "select theString from " + SupportBean.class.getName() + "#time(10) as s," +
                SupportMarketDataBean.class.getName() + "#keepall as m where s.theString = m.symbol output snapshot every 3 events order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);

        for (String symbol : "s0,s1,s2,s3,s4,s5,s6,s7,s8,s9,s10,s11".split(",")) {
            epService.getEPRuntime().sendEvent(new SupportMarketDataBean(symbol, 0, 0L, ""));
        }

        sendTimer(epService, 1000);
        sendEvent(epService, "s0");
        sendEvent(epService, "s1");
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 2000);
        sendEvent(epService, "s2");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s0"}, {"s1"}, {"s2"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 3000);
        sendEvent(epService, "s4");
        sendEvent(epService, "s5");
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 10000);
        sendEvent(epService, "s6");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s0"}, {"s1"}, {"s2"}, {"s4"}, {"s5"}, {"s6"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 11000);
        sendEvent(epService, "s7");
        assertFalse(listener.isInvoked());

        sendEvent(epService, "s8");
        assertFalse(listener.isInvoked());

        sendEvent(epService, "s9");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s2"}, {"s4"}, {"s5"}, {"s6"}, {"s7"}, {"s8"}, {"s9"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 14000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s6"}, {"s7"}, {"s8"}, {"s9"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(epService, "s10");
        sendEvent(epService, "s11");
        assertFalse(listener.isInvoked());

        sendTimer(epService, 23000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"s10"}, {"s11"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(epService, "s12");
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionSnapshotMonthScoped(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from SupportBean#lastevent output snapshot every 1 month").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        assertFalse(listener.getAndClearIsInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E1"}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFirstMonthScoped(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from SupportBean#lastevent output first every 1 month").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E4"}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportUpdateListener createStmtAndListenerJoin(EPServiceProvider epService, String epl) {
        SupportUpdateListener updateListener = new SupportUpdateListener();
        EPStatement view = epService.getEPAdministrator().createEPL(epl);
        view.addListener(updateListener);
        epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));
        return updateListener;
    }

    private void tryAssertLast(EPServiceProvider epService, SupportUpdateListener updateListener) {
        // send an event
        sendEvent(epService, 1);

        // check no update
        assertFalse(updateListener.getAndClearIsInvoked());

        // send another event
        sendEvent(epService, 2);

        // check update, only the last event present
        assertTrue(updateListener.getAndClearIsInvoked());
        assertEquals(1, updateListener.getLastNewData().length);
        assertEquals(2L, updateListener.getLastNewData()[0].get("longBoxed"));
        assertNull(updateListener.getLastOldData());
    }

    private void sendTimer(EPServiceProvider epService, long time) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendEvent(EPServiceProvider epService, String s) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setDoubleBoxed(0.0);
        bean.setIntPrimitive(0);
        bean.setIntBoxed(0);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void timeCallback(EPServiceProvider epService, String statementString, int timeToCallback) {
        // set the clock to 0
        AtomicLong currentTime = new AtomicLong();
        sendTimeEvent(epService, 0, currentTime);

        // create the EPL statement and add a listener
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);
        updateListener.reset();

        // send an event
        sendEvent(epService, "IBM");

        // check that the listener hasn't been updated
        sendTimeEvent(epService, timeToCallback - 1, currentTime);
        assertFalse(updateListener.getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(epService, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(updateListener.getAndClearIsInvoked());
        assertEquals(1, updateListener.getLastNewData().length);
        assertNull(updateListener.getLastOldData());

        // send another event
        sendEvent(epService, "MSFT");

        // check that the listener hasn't been updated
        assertFalse(updateListener.getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(epService, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(updateListener.getAndClearIsInvoked());
        assertEquals(1, updateListener.getLastNewData().length);
        assertNull(updateListener.getLastOldData());

        // don't send an event
        // check that the listener hasn't been updated
        assertFalse(updateListener.getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(epService, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(updateListener.getAndClearIsInvoked());
        assertNull(updateListener.getLastNewData());
        assertNull(updateListener.getLastOldData());

        // don't send an event
        // check that the listener hasn't been updated
        assertFalse(updateListener.getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(epService, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(updateListener.getAndClearIsInvoked());
        assertNull(updateListener.getLastNewData());
        assertNull(updateListener.getLastOldData());

        // send several events
        sendEvent(epService, "YAH");
        sendEvent(epService, "s4");
        sendEvent(epService, "s5");

        // check that the listener hasn't been updated
        assertFalse(updateListener.getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(epService, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(updateListener.getAndClearIsInvoked());
        assertEquals(3, updateListener.getLastNewData().length);
        assertNull(updateListener.getLastOldData());
    }

    private void sendTimeEvent(EPServiceProvider epService, int timeIncrement, AtomicLong currentTime) {
        currentTime.addAndGet(timeIncrement);
        CurrentTimeEvent theEvent = new CurrentTimeEvent(currentTime.get());
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendJoinEvents(EPServiceProvider epService, String s) {
        SupportBean event1 = new SupportBean();
        event1.setTheString(s);
        event1.setDoubleBoxed(0.0);
        event1.setIntPrimitive(0);
        event1.setIntBoxed(0);

        SupportBean_A event2 = new SupportBean_A(s);

        epService.getEPRuntime().sendEvent(event1);
        epService.getEPRuntime().sendEvent(event2);
    }

    private void sendMDEvent(EPServiceProvider epService, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
