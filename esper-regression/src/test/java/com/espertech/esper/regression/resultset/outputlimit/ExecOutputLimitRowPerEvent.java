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
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.epl.SupportOutputLimitOpt;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.ResultAssertExecution;
import com.espertech.esper.supportregression.patternassert.ResultAssertExecutionTestSelector;
import com.espertech.esper.supportregression.patternassert.ResultAssertTestResult;

import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecOutputLimitRowPerEvent implements RegressionExecution {
    private static final String EVENT_NAME = SupportMarketDataBean.class.getName();
    private final static String JOIN_KEY = "KEY";
    private final static String CATEGORY = "Aggregated and Un-grouped";

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
        runAssertion11AllHavingNoJoinHinted(epService);
        runAssertion12AllHavingJoin(epService);
        runAssertion13LastNoHavingNoJoin(epService);
        runAssertion14LastNoHavingJoin(epService);
        runAssertion15LastHavingNoJoin(epService);
        runAssertion16LastHavingJoin(epService);
        runAssertion17FirstNoHavingNoJoinIStreamOnly(epService);
        runAssertion17FirstNoHavingNoJoinIRStream(epService);
        runAssertion18SnapshotNoHavingNoJoin(epService);
        runAssertionHaving(epService);
        runAssertionHavingJoin(epService);
        runAssertionMaxTimeWindow(epService);
        runAssertionLimitSnapshot(epService);
        runAssertionLimitSnapshotJoin(epService);
        runAssertionJoinSortWindow(epService);
        runAssertionRowPerEventNoJoinLast(epService);
        runAssertionRowPerEventJoinAll(epService);
        runAssertionRowPerEventJoinLast(epService);
        runAssertionTime(epService);
        runAssertionCount(epService);
    }

    private void runAssertion1NoneNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec)";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion2NoneNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion3NoneHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                " having sum(price) > 100";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion4NoneHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                " having sum(price) > 100";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion5DefaultNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "output every 1 seconds";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion6DefaultNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output every 1 seconds";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion7DefaultHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) \n" +
                "having sum(price) > 100" +
                "output every 1 seconds";
        tryAssertion78(epService, stmtText, "default");
    }

    private void runAssertion8DefaultHavingJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "having sum(price) > 100" +
                "output every 1 seconds";
        tryAssertion78(epService, stmtText, "default");
    }

    private void runAssertion9AllNoHavingNoJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion9AllNoHavingNoJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion9AllNoHavingNoJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "output all every 1 seconds";
        tryAssertion56(epService, stmtText, "all");
    }

    private void runAssertion10AllNoHavingJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion10AllNoHavingJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion10AllNoHavingJoin(EPServiceProvider epService, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output all every 1 seconds";
        tryAssertion56(epService, stmtText, "all");
    }

    private void runAssertion11AllHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "having sum(price) > 100" +
                "output all every 1 seconds";
        tryAssertion78(epService, stmtText, "all");
    }

    private void runAssertion11AllHavingNoJoinHinted(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion11AllHavingNoJoinHinted(epService, outputLimitOpt);
        }
    }

    private void runAssertion11AllHavingNoJoinHinted(EPServiceProvider epService, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "having sum(price) > 100" +
                "output all every 1 seconds";
        tryAssertion78(epService, stmtText, "all");
    }

    private void runAssertion12AllHavingJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion12AllHavingJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion12AllHavingJoin(EPServiceProvider epService, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "having sum(price) > 100" +
                "output all every 1 seconds";
        tryAssertion78(epService, stmtText, "all");
    }

    private void runAssertion13LastNoHavingNoJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion13LastNoHavingNoJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion13LastNoHavingNoJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec)" +
                "output last every 1 seconds";
        tryAssertion13_14(epService, stmtText, "last");
    }

    private void runAssertion14LastNoHavingJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion14LastNoHavingJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion14LastNoHavingJoin(EPServiceProvider epService, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output last every 1 seconds";
        tryAssertion13_14(epService, stmtText, "last");
    }

    private void runAssertion15LastHavingNoJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion15LastHavingNoJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion15LastHavingNoJoin(EPServiceProvider epService, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec)" +
                "having sum(price) > 100 " +
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
                "having sum(price) > 100 " +
                "output last every 1 seconds";
        tryAssertion15_16(epService, stmtText, "last");
    }

    private void runAssertion17FirstNoHavingNoJoinIStreamOnly(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "output first every 1 seconds";
        tryAssertion17IStreamOnly(epService, stmtText, "first");
    }

    private void runAssertion17FirstNoHavingNoJoinIRStream(EPServiceProvider epService) {
        String stmtText = "select irstream symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "output first every 1 seconds";
        tryAssertion17IRStream(epService, stmtText, "first");
    }

    private void runAssertion18SnapshotNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "output snapshot every 1 seconds";
        tryAssertion18(epService, stmtText, "first");
    }

    private void tryAssertion12(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 25d}});
        expected.addResultInsert(800, 1, new Object[][]{{"MSFT", 34d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 58d}});
        expected.addResultInsert(1500, 2, new Object[][]{{"YAH", 59d}});
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 85d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 109d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 112d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 87d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 88d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 79d}});
        expected.addResultRemove(7000, 0, new Object[][]{{"IBM", 54d}, {"YAH", 54d}});

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
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 109d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 112d}});

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
        expected.addResultInsert(1200, 0, new Object[][]{{"MSFT", 34d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 85d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"YAH", 112d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 88d}}, new Object[][]{{"IBM", 87d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"YAH", 54d}});

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
        expected.addResultInsRem(2200, 0, null, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"YAH", 112d}});
        expected.addResultInsRem(6200, 0, null, null);
        expected.addResultInsRem(7200, 0, null, null);

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
        expected.addResultInsRem(2200, 0, null, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsRem(5200, 0, new Object[][]{{"IBM", 109d}, {"YAH", 112d}}, null);
        expected.addResultInsRem(6200, 0, null, null);
        expected.addResultInsRem(7200, 0, null, null);

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
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 25d}, {"MSFT", 34d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 58d}, {"YAH", 59d}, {"IBM", 85d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 109d}, {"YAH", 112d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 88d}}, new Object[][]{{"IBM", 87d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"MSFT", 79d}, {"IBM", 54d}, {"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion17IStreamOnly(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 58d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 109d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 88d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected, ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED);
        execution.execute(false);
    }

    private void tryAssertion17IRStream(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 58d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 109d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 87d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 79d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected, ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED);
        execution.execute(false);
    }

    private void tryAssertion18(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 34d}, {"MSFT", 34d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 85d}, {"MSFT", 85d}, {"IBM", 85d}, {"YAH", 85d}, {"IBM", 85d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 85d}, {"MSFT", 85d}, {"IBM", 85d}, {"YAH", 85d}, {"IBM", 85d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 87d}, {"MSFT", 87d}, {"IBM", 87d}, {"YAH", 87d}, {"IBM", 87d}, {"YAH", 87d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 112d}, {"MSFT", 112d}, {"IBM", 112d}, {"YAH", 112d}, {"IBM", 112d}, {"YAH", 112d}, {"IBM", 112d}, {"YAH", 112d}});
        expected.addResultInsert(6200, 0, new Object[][]{{"MSFT", 88d}, {"IBM", 88d}, {"YAH", 88d}, {"IBM", 88d}, {"YAH", 88d}, {"IBM", 88d}, {"YAH", 88d}, {"YAH", 88d}});
        expected.addResultInsert(7200, 0, new Object[][]{{"IBM", 54d}, {"YAH", 54d}, {"IBM", 54d}, {"YAH", 54d}, {"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertionHaving(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select symbol, avg(price) as avgPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#time(3 sec) " +
                "having avg(price) > 10" +
                "output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionHaving(epService, listener);
    }

    private void runAssertionHavingJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select symbol, avg(price) as avgPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#time(3 sec) as md, " +
                SupportBean.class.getName() + "#keepall as s where s.theString = md.symbol " +
                "having avg(price) > 10" +
                "output every 1 seconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("SYM1", -1));

        tryAssertionHaving(epService, listener);
    }

    private void tryAssertionHaving(EPServiceProvider epService, SupportUpdateListener listener) {
        sendEvent(epService, "SYM1", 10d);
        sendEvent(epService, "SYM1", 11d);
        sendEvent(epService, "SYM1", 9);

        sendTimer(epService, 1000);
        String[] fields = "symbol,avgPrice".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"SYM1", 10.5});

        sendEvent(epService, "SYM1", 13d);
        sendEvent(epService, "SYM1", 10d);
        sendEvent(epService, "SYM1", 9);
        sendTimer(epService, 2000);

        assertEquals(3, listener.getLastNewData().length);
        assertNull(listener.getLastOldData());
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"SYM1", 43 / 4.0}, {"SYM1", 53.0 / 5.0}, {"SYM1", 62 / 6.0}});
    }

    private void runAssertionMaxTimeWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select irstream volume, max(price) as maxVol" +
                " from " + SupportMarketDataBean.class.getName() + "#time(1 sec) " +
                "output every 1 seconds";
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
    }

    private void runAssertionLimitSnapshot(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String selectStmt = "select symbol, sum(price) as sumprice from " + SupportMarketDataBean.class.getName() +
                "#time(10 seconds) output snapshot every 1 seconds order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        sendEvent(epService, "ABC", 20);

        sendTimer(epService, 500);
        sendEvent(epService, "IBM", 16);
        sendEvent(epService, "MSFT", 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 1000);
        String[] fields = new String[]{"symbol", "sumprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 50d}, {"IBM", 50d}, {"MSFT", 50d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 1500);
        sendEvent(epService, "YAH", 18);
        sendEvent(epService, "s4", 30);

        sendTimer(epService, 10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 98d}, {"IBM", 98d}, {"MSFT", 98d}, {"YAH", 98d}, {"s4", 98d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"YAH", 48d}, {"s4", 48d}});
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
        String selectStmt = "select irstream symbol, sum(price) as sumprice from " + SupportMarketDataBean.class.getName() +
                "#time(10 seconds) as m, " + SupportBean.class.getName() +
                "#keepall as s where s.theString = m.symbol output snapshot every 1 seconds order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("ABC", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("IBM", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("MSFT", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("YAH", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("s4", 5));

        sendEvent(epService, "ABC", 20);

        sendTimer(epService, 500);
        sendEvent(epService, "IBM", 16);
        sendEvent(epService, "MSFT", 14);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 1000);
        String[] fields = new String[]{"symbol", "sumprice"};
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 50d}, {"IBM", 50d}, {"MSFT", 50d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 1500);
        sendEvent(epService, "YAH", 18);
        sendEvent(epService, "s4", 30);

        sendTimer(epService, 10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"ABC", 98d}, {"IBM", 98d}, {"MSFT", 98d}, {"YAH", 98d}, {"s4", 98d}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 10500);
        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"YAH", 48d}, {"s4", 48d}});
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

    private void runAssertionJoinSortWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select irstream volume, max(price) as maxVol" +
                " from " + SupportMarketDataBean.class.getName() + "#sort(1, volume desc) as s0," +
                SupportBean.class.getName() + "#keepall as s1 " +
                "output every 1 seconds";
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

    private void runAssertionRowPerEventNoJoinLast(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionRowPerEventNoJoinLast(epService, outputLimitOpt);
        }
    }

    private void tryAssertionRowPerEventNoJoinLast(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "select longBoxed, sum(longBoxed) as result " +
                "from " + SupportBean.class.getName() + "#length(3) " +
                "having sum(longBoxed) > 0 " +
                "output last every 2 events";

        tryAssertLastSum(epService, createStmtAndListenerNoJoin(epService, epl));

        epl = opt.getHint() + "select longBoxed, sum(longBoxed) as result " +
                "from " + SupportBean.class.getName() + "#length(3) " +
                "output last every 2 events";
        tryAssertLastSum(epService, createStmtAndListenerNoJoin(epService, epl));
    }

    private void runAssertionRowPerEventJoinAll(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionRowPerEventJoinAll(epService, outputLimitOpt);
        }
    }

    private void tryAssertionRowPerEventJoinAll(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "select longBoxed, sum(longBoxed) as result " +
                "from " + SupportBeanString.class.getName() + "#length(3) as one, " +
                SupportBean.class.getName() + "#length(3) as two " +
                "having sum(longBoxed) > 0 " +
                "output all every 2 events";

        tryAssertAllSum(epService, createStmtAndListenerJoin(epService, epl));

        epl = opt.getHint() + "select longBoxed, sum(longBoxed) as result " +
                "from " + SupportBeanString.class.getName() + "#length(3) as one, " +
                SupportBean.class.getName() + "#length(3) as two " +
                "output every 2 events";

        tryAssertAllSum(epService, createStmtAndListenerJoin(epService, epl));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRowPerEventJoinLast(EPServiceProvider epService) {
        String epl = "select longBoxed, sum(longBoxed) as result " +
                "from " + SupportBeanString.class.getName() + "#length(3) as one, " +
                SupportBean.class.getName() + "#length(3) as two " +
                "having sum(longBoxed) > 0 " +
                "output last every 2 events";

        tryAssertLastSum(epService, createStmtAndListenerJoin(epService, epl));

        epl = "select longBoxed, sum(longBoxed) as result " +
                "from " + SupportBeanString.class.getName() + "#length(3) as one, " +
                SupportBean.class.getName() + "#length(3) as two " +
                "output last every 2 events";

        tryAssertLastSum(epService, createStmtAndListenerJoin(epService, epl));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTime(EPServiceProvider epService) {
        // Set the clock to 0
        AtomicLong currentTime = new AtomicLong();
        sendTimeEventRelative(epService, 0, currentTime);

        // Create the EPL statement and add a listener
        String statementText = "select symbol, sum(volume) from " + EVENT_NAME + "#length(5) output first every 3 seconds";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);
        updateListener.reset();

        // Send the first event of the batch; should be output
        sendMarketDataEvent(epService, 10L);
        assertEvent(updateListener, 10L);

        // Send another event, not the first, for aggregation
        // update only, no output
        sendMarketDataEvent(epService, 20L);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Update time
        sendTimeEventRelative(epService, 3000, currentTime);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Send first event of the next batch, should be output.
        // The aggregate value is computed over all events
        // received: 10 + 20 + 30 = 60
        sendMarketDataEvent(epService, 30L);
        assertEvent(updateListener, 60L);

        // Send the next event of the batch, no output
        sendMarketDataEvent(epService, 40L);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Update time
        sendTimeEventRelative(epService, 3000, currentTime);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Send first event of third batch
        sendMarketDataEvent(epService, 1L);
        assertEvent(updateListener, 101L);

        // Update time
        sendTimeEventRelative(epService, 3000, currentTime);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Update time: no first event this batch, so a callback
        // is made at the end of the interval
        sendTimeEventRelative(epService, 3000, currentTime);
        assertFalse(updateListener.getAndClearIsInvoked());

        statement.destroy();
    }

    private void runAssertionCount(EPServiceProvider epService) {
        // Create the EPL statement and add a listener
        String statementText = "select symbol, sum(volume) from " + EVENT_NAME + "#length(5) output first every 3 events";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);
        updateListener.reset();

        // Send the first event of the batch, should be output
        sendEventLong(epService, 10L);
        assertEvent(updateListener, 10L);

        // Send the second event of the batch, not output, used
        // for updating the aggregate value only
        sendEventLong(epService, 20L);
        assertFalse(updateListener.getAndClearIsInvoked());

        // Send the third event of the batch, still not output,
        // but should reset the batch
        sendEventLong(epService, 30L);
        assertFalse(updateListener.getAndClearIsInvoked());

        // First event, next batch, aggregate value should be
        // 10 + 20 + 30 + 40 = 100
        sendEventLong(epService, 40L);
        assertEvent(updateListener, 100L);

        // Next event again not output
        sendEventLong(epService, 50L);
        assertFalse(updateListener.getAndClearIsInvoked());

        statement.destroy();
    }

    private void sendEventLong(EPServiceProvider epService, long volume) {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("DELL", 0.0, volume, null));
    }

    private SupportUpdateListener createStmtAndListenerNoJoin(EPServiceProvider epService, String epl) {
        SupportUpdateListener updateListener = new SupportUpdateListener();
        EPStatement view = epService.getEPAdministrator().createEPL(epl);
        view.addListener(updateListener);

        return updateListener;
    }

    private void tryAssertAllSum(EPServiceProvider epService, SupportUpdateListener updateListener) {
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
        assertEquals(1L, updateListener.getLastNewData()[0].get("result"));
        assertEquals(2L, updateListener.getLastNewData()[1].get("longBoxed"));
        assertEquals(3L, updateListener.getLastNewData()[1].get("result"));
        assertNull(updateListener.getLastOldData());
    }

    private void tryAssertLastSum(EPServiceProvider epService, SupportUpdateListener updateListener) {
        // send an event
        sendEvent(epService, 1);

        // check no update
        assertFalse(updateListener.getAndClearIsInvoked());

        // send another event
        sendEvent(epService, 2);

        // check update, all events present
        assertTrue(updateListener.getAndClearIsInvoked());
        assertEquals(1, updateListener.getLastNewData().length);
        assertEquals(2L, updateListener.getLastNewData()[0].get("longBoxed"));
        assertEquals(3L, updateListener.getLastNewData()[0].get("result"));
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

    private void sendMarketDataEvent(EPServiceProvider epService, long volume) {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("SYM1", 0, volume, null));
    }

    private void sendTimeEventRelative(EPServiceProvider epService, int timeIncrement, AtomicLong currentTime) {
        currentTime.addAndGet(timeIncrement);
        CurrentTimeEvent theEvent = new CurrentTimeEvent(currentTime.get());
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private SupportUpdateListener createStmtAndListenerJoin(EPServiceProvider epService, String epl) {
        SupportUpdateListener updateListener = new SupportUpdateListener();
        EPStatement view = epService.getEPAdministrator().createEPL(epl);
        view.addListener(updateListener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));

        return updateListener;
    }

    private void assertEvent(SupportUpdateListener updateListener, long volume) {
        assertTrue(updateListener.getAndClearIsInvoked());
        assertTrue(updateListener.getLastNewData() != null);
        assertEquals(1, updateListener.getLastNewData().length);
        assertEquals(volume, updateListener.getLastNewData()[0].get("sum(volume)"));
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimer(EPServiceProvider epService, long time) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
