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
import com.espertech.esper.client.hook.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.epl.SupportOutputLimitOpt;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.ResultAssertExecution;
import com.espertech.esper.supportregression.patternassert.ResultAssertTestResult;
import com.espertech.esper.util.SerializableObjectCopier;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static org.junit.Assert.*;

public class ExecOutputLimitRowForAll implements RegressionExecution {
    private final static String CATEGORY = "Fully-Aggregated and Un-grouped";

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MarketData", SupportMarketDataBean.class);
        configuration.addEventType("SupportBean", SupportBean.class);
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
        runAssertion17FirstNoHavingNoJoin(epService);
        runAssertion18SnapshotNoHavingNoJoin(epService);
        runAssertionOuputLastWithInsertInto(epService);
        runAssertionAggAllHaving(epService);
        runAssertionAggAllHavingJoin(epService);
        runAssertionJoinSortWindow(epService);
        runAssertionMaxTimeWindow(epService);
        runAssertionTimeWindowOutputCountLast(epService);
        runAssertionTimeBatchOutputCount(epService);
        runAssertionLimitSnapshot(epService);
        runAssertionLimitSnapshotJoin(epService);
        if (!InstrumentationHelper.ENABLED) {
            runAssertionOutputSnapshotGetValue(epService);
        }
    }

    private void runAssertion1NoneNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
                "from MarketData#time(5.5 sec)";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion2NoneNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol";
        tryAssertion12(epService, stmtText, "none");
    }

    private void runAssertion3NoneHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
                "from MarketData#time(5.5 sec) " +
                " having sum(price) > 100";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion4NoneHavingJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                " having sum(price) > 100";
        tryAssertion34(epService, stmtText, "none");
    }

    private void runAssertion5DefaultNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "output every 1 seconds";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion6DefaultNoHavingJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output every 1 seconds";
        tryAssertion56(epService, stmtText, "default");
    }

    private void runAssertion7DefaultHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
                "from MarketData#time(5.5 sec) \n" +
                "having sum(price) > 100" +
                "output every 1 seconds";
        tryAssertion78(epService, stmtText, "default");
    }

    private void runAssertion8DefaultHavingJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
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
        String stmtText = opt.getHint() + "select sum(price) " +
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
        String stmtText = opt.getHint() + "select sum(price) " +
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
        String stmtText = opt.getHint() + "select sum(price) " +
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

    private void runAssertion12AllHavingJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select sum(price) " +
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
        String stmtText = opt.getHint() + "select sum(price) " +
                "from MarketData#time(5.5 sec)" +
                "output last every 1 seconds";
        tryAssertion13_14(epService, stmtText, "last");
    }

    private void runAssertion14LastNoHavingJoin(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertion14LastNoHavingJoin(epService, outputLimitOpt);
        }
    }

    private void runAssertion14LastNoHavingJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select sum(price) " +
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

    private void runAssertion15LastHavingNoJoin(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "select sum(price) " +
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
        String stmtText = opt.getHint() + "select sum(price) " +
                "from MarketData#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "having sum(price) > 100 " +
                "output last every 1 seconds";
        tryAssertion15_16(epService, stmtText, "last");
    }

    private void runAssertion17FirstNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "output first every 1 seconds";
        tryAssertion17(epService, stmtText, "first");
    }

    private void runAssertion18SnapshotNoHavingNoJoin(EPServiceProvider epService) {
        String stmtText = "select sum(price) " +
                "from MarketData#time(5.5 sec) " +
                "output snapshot every 1 seconds";
        tryAssertion18(epService, stmtText, "first");
    }

    private void runAssertionOuputLastWithInsertInto(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            tryAssertionOuputLastWithInsertInto(epService, outputLimitOpt);
        }
    }

    private void tryAssertionOuputLastWithInsertInto(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String eplInsert = opt.getHint() + "insert into MyStream select sum(intPrimitive) as thesum from SupportBean#keepall " +
                "output last every 2 events";
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(eplInsert);

        EPStatement stmtListen = epService.getEPAdministrator().createEPL("select * from MyStream");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtListen.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "thesum".split(","), new Object[]{30});

        stmtInsert.destroy();
        stmtListen.destroy();
    }

    private void tryAssertion12(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(200, 1, new Object[][]{{25d}}, new Object[][]{{null}});
        expected.addResultInsRem(800, 1, new Object[][]{{34d}}, new Object[][]{{25d}});
        expected.addResultInsRem(1500, 1, new Object[][]{{58d}}, new Object[][]{{34d}});
        expected.addResultInsRem(1500, 2, new Object[][]{{59d}}, new Object[][]{{58d}});
        expected.addResultInsRem(2100, 1, new Object[][]{{85d}}, new Object[][]{{59d}});
        expected.addResultInsRem(3500, 1, new Object[][]{{87d}}, new Object[][]{{85d}});
        expected.addResultInsRem(4300, 1, new Object[][]{{109d}}, new Object[][]{{87d}});
        expected.addResultInsRem(4900, 1, new Object[][]{{112d}}, new Object[][]{{109d}});
        expected.addResultInsRem(5700, 0, new Object[][]{{87d}}, new Object[][]{{112d}});
        expected.addResultInsRem(5900, 1, new Object[][]{{88d}}, new Object[][]{{87d}});
        expected.addResultInsRem(6300, 0, new Object[][]{{79d}}, new Object[][]{{88d}});
        expected.addResultInsRem(7000, 0, new Object[][]{{54d}}, new Object[][]{{79d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion34(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(4300, 1, new Object[][]{{109d}}, null);
        expected.addResultInsRem(4900, 1, new Object[][]{{112d}}, new Object[][]{{109d}});
        expected.addResultInsRem(5700, 0, null, new Object[][]{{112d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion13_14(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, new Object[][]{{34d}}, new Object[][]{{null}});
        expected.addResultInsRem(2200, 0, new Object[][]{{85d}}, new Object[][]{{34d}});
        expected.addResultInsRem(3200, 0, new Object[][]{{85d}}, new Object[][]{{85d}});
        expected.addResultInsRem(4200, 0, new Object[][]{{87d}}, new Object[][]{{85d}});
        expected.addResultInsRem(5200, 0, new Object[][]{{112d}}, new Object[][]{{87d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{88d}}, new Object[][]{{112d}});
        expected.addResultInsRem(7200, 0, new Object[][]{{54d}}, new Object[][]{{88d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion15_16(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, null, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsRem(5200, 0, new Object[][]{{112d}}, new Object[][]{{109d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{112d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion78(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, null, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsRem(5200, 0, new Object[][]{{109d}, {112d}}, new Object[][]{{109d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{112d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion56(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, new Object[][]{{25d}, {34d}}, new Object[][]{{null}, {25d}});
        expected.addResultInsRem(2200, 0, new Object[][]{{58d}, {59d}, {85d}}, new Object[][]{{34d}, {58d}, {59d}});
        expected.addResultInsRem(3200, 0, new Object[][]{{85d}}, new Object[][]{{85d}});
        expected.addResultInsRem(4200, 0, new Object[][]{{87d}}, new Object[][]{{85d}});
        expected.addResultInsRem(5200, 0, new Object[][]{{109d}, {112d}}, new Object[][]{{87d}, {109d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{87d}, {88d}}, new Object[][]{{112d}, {87d}});
        expected.addResultInsRem(7200, 0, new Object[][]{{79d}, {54d}}, new Object[][]{{88d}, {79d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion17(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(200, 1, new Object[][]{{25d}}, new Object[][]{{null}});
        expected.addResultInsRem(1500, 1, new Object[][]{{58d}}, new Object[][]{{34d}});
        expected.addResultInsRem(3500, 1, new Object[][]{{87d}}, new Object[][]{{85d}});
        expected.addResultInsRem(4300, 1, new Object[][]{{109d}}, new Object[][]{{87d}});
        expected.addResultInsRem(5700, 0, new Object[][]{{87d}}, new Object[][]{{112d}});
        expected.addResultInsRem(6300, 0, new Object[][]{{79d}}, new Object[][]{{88d}});

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void tryAssertion18(EPServiceProvider epService, String stmtText, String outputLimit) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, new Object[][]{{34d}}, null);
        expected.addResultInsRem(2200, 0, new Object[][]{{85d}}, null);
        expected.addResultInsRem(3200, 0, new Object[][]{{85d}}, null);
        expected.addResultInsRem(4200, 0, new Object[][]{{87d}}, null);
        expected.addResultInsRem(5200, 0, new Object[][]{{112d}}, null);
        expected.addResultInsRem(6200, 0, new Object[][]{{88d}}, null);
        expected.addResultInsRem(7200, 0, new Object[][]{{54d}}, null);

        ResultAssertExecution execution = new ResultAssertExecution(epService, stmt, listener, expected);
        execution.execute(false);
    }

    private void runAssertionAggAllHaving(EPServiceProvider epService) {
        String stmtText = "select sum(volume) as result " +
                "from " + SupportMarketDataBean.class.getName() + "#length(10) as two " +
                "having sum(volume) > 0 " +
                "output every 5 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"result"};

        sendMDEvent(epService, 20);
        sendMDEvent(epService, -100);
        sendMDEvent(epService, 0);
        sendMDEvent(epService, 0);
        assertFalse(listener.isInvoked());

        sendMDEvent(epService, 0);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{20L}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{20L}});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionAggAllHavingJoin(EPServiceProvider epService) {
        String stmtText = "select sum(volume) as result " +
                "from " + SupportMarketDataBean.class.getName() + "#length(10) as one," +
                SupportBean.class.getName() + "#length(10) as two " +
                "where one.symbol=two.theString " +
                "having sum(volume) > 0 " +
                "output every 5 events";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"result"};
        epService.getEPRuntime().sendEvent(new SupportBean("S0", 0));

        sendMDEvent(epService, 20);
        sendMDEvent(epService, -100);
        sendMDEvent(epService, 0);
        sendMDEvent(epService, 0);
        assertFalse(listener.isInvoked());

        sendMDEvent(epService, 0);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{20L}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{20L}});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionJoinSortWindow(EPServiceProvider epService) throws Exception {
        sendTimer(epService, 0);

        String epl = "select irstream max(price) as maxVol" +
                " from " + SupportMarketDataBean.class.getName() + "#sort(1,volume desc) as s0, " +
                SupportBean.class.getName() + "#keepall as s1 where s1.theString=s0.symbol " +
                "output every 1.0d seconds";
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
        assertEquals(2, result.getSecond().length);
        assertEquals(null, result.getSecond()[0].get("maxVol"));
        assertEquals(1.0, result.getSecond()[1].get("maxVol"));

        // statement object model test
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        SerializableObjectCopier.copy(model);
        assertEquals(epl, model.toEPL());

        stmt.destroy();
    }

    private void runAssertionMaxTimeWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select irstream max(price) as maxVol" +
                " from " + SupportMarketDataBean.class.getName() + "#time(1.1 sec) " +
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
        assertEquals(1.0, result.getSecond()[1].get("maxVol"));

        stmt.destroy();
    }

    private void runAssertionTimeWindowOutputCountLast(EPServiceProvider epService) {
        String stmtText = "select count(*) as cnt from " + SupportBean.class.getName() + "#time(10 seconds) output every 10 seconds";
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
        EventBean[] newEvents = listener.getAndResetLastNewData();
        assertEquals(2, newEvents.length);
        assertEquals(1L, newEvents[0].get("cnt"));
        assertEquals(0L, newEvents[1].get("cnt"));

        sendTimer(epService, 31000);

        sendEvent(epService, "e2");
        sendEvent(epService, "e3");
        sendTimer(epService, 40000);
        newEvents = listener.getAndResetLastNewData();
        assertEquals(2, newEvents.length);
        assertEquals(1L, newEvents[0].get("cnt"));
        assertEquals(2L, newEvents[1].get("cnt"));

        stmt.destroy();
    }

    private void runAssertionTimeBatchOutputCount(EPServiceProvider epService) {
        String stmtText = "select count(*) as cnt from " + SupportBean.class.getName() + "#time_batch(10 seconds) output every 10 seconds";
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
        assertEquals(2, newEvents.length);
        // output limiting starts 10 seconds after, therefore the old batch was posted already and the cnt is zero
        assertEquals(1L, newEvents[0].get("cnt"));
        assertEquals(0L, newEvents[1].get("cnt"));

        sendTimer(epService, 50000);
        EventBean[] newData = listener.getLastNewData();
        assertEquals(0L, newData[0].get("cnt"));
        listener.reset();

        sendEvent(epService, "e2");
        sendEvent(epService, "e3");
        sendTimer(epService, 60000);
        newEvents = listener.getAndResetLastNewData();
        assertEquals(1, newEvents.length);
        assertEquals(2L, newEvents[0].get("cnt"));

        stmt.destroy();
    }

    private void runAssertionLimitSnapshot(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        sendTimer(epService, 0);
        String selectStmt = "select count(*) as cnt from " + SupportBean.class.getName() + "#time(10 seconds) where intPrimitive > 0 output snapshot every 1 seconds";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);
        sendEvent(epService, "s0", 1);

        sendTimer(epService, 500);
        sendEvent(epService, "s1", 1);
        sendEvent(epService, "s2", -1);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 1000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{2L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 1500);
        sendEvent(epService, "s4", 2);
        sendEvent(epService, "s5", 3);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 2000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{4L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(epService, "s5", 4);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 9000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{5L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{4L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 10999);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{3L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionLimitSnapshotJoin(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        sendTimer(epService, 0);
        String selectStmt = "select count(*) as cnt from " +
                SupportBean.class.getName() + "#time(10 seconds) as s, " +
                SupportMarketDataBean.class.getName() + "#keepall as m where m.symbol = s.theString and intPrimitive > 0 output snapshot every 1 seconds";

        EPStatement stmt = epService.getEPAdministrator().createEPL(selectStmt);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("s0", 0, 0L, ""));
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("s1", 0, 0L, ""));
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("s2", 0, 0L, ""));
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("s4", 0, 0L, ""));
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("s5", 0, 0L, ""));

        sendEvent(epService, "s0", 1);

        sendTimer(epService, 500);
        sendEvent(epService, "s1", 1);
        sendEvent(epService, "s2", -1);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 1000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{2L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 1500);
        sendEvent(epService, "s4", 2);
        sendEvent(epService, "s5", 3);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 2000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{4L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendEvent(epService, "s5", 4);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 9000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{5L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        // The execution of the join is after the snapshot, as joins are internal dispatch
        sendTimer(epService, 10000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{5L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 10999);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"cnt"}, new Object[][]{{3L}});
        assertNull(listener.getLastOldData());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionOutputSnapshotGetValue(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("customagg", MyContextAggFuncFactory.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        tryAssertionOutputSnapshotGetValue(epService, true);
        tryAssertionOutputSnapshotGetValue(epService, false);
    }

    private void tryAssertionOutputSnapshotGetValue(EPServiceProvider epService, boolean join) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select customagg(intPrimitive) as c0 from SupportBean" +
                        (join ? "#keepall, SupportBean_S0#lastevent" : "") +
                        " output snapshot every 3 events");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        MyContextAggFunc.resetGetValueInvocationCount();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        assertEquals(0, MyContextAggFunc.getGetValueInvocationCount());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        assertEquals(60, listener.assertOneGetNewAndReset().get("c0"));
        assertEquals(1, MyContextAggFunc.getGetValueInvocationCount());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 40));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 50));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 60));
        assertEquals(210, listener.assertOneGetNewAndReset().get("c0"));
        assertEquals(2, MyContextAggFunc.getGetValueInvocationCount());

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String s) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setDoubleBoxed(0.0);
        bean.setIntPrimitive(0);
        bean.setIntBoxed(0);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, String s, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimer(EPServiceProvider epService, long time) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMDEvent(EPServiceProvider epService, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean("S0", 0, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    public static class MyContextAggFuncFactory implements AggregationFunctionFactory {
        public void setFunctionName(String functionName) {
        }

        public void validate(AggregationValidationContext validationContext) {
        }

        public AggregationMethod newAggregator() {
            return new MyContextAggFunc();
        }

        public Class getValueType() {
            return int.class;
        }

        public AggregationFunctionFactoryCodegenType getCodegenType() {
            return AggregationFunctionFactoryCodegenType.CODEGEN_UNMANAGED;
        }

        public void rowMemberCodegen(AggregationFunctionFactoryCodegenRowMemberContext context) {
            MyContextAggFunc.rowMemberCodegen(context);
        }

        public void applyEnterCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        }

        public void applyLeaveCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        }

        public void applyEnterCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
            MyContextAggFunc.applyEnterCodegen(context);
        }

        public void applyLeaveCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
            // no code
        }

        public void clearCodegen(AggregationFunctionFactoryCodegenRowClearContext context) {
            // no code
        }

        public void getValueCodegen(AggregationFunctionFactoryCodegenRowGetValueContext context) {
            MyContextAggFunc.getValueCodegen(context);
        }
    }

    public static class MyContextAggFunc implements AggregationMethod {

        private static long getValueInvocationCount = 0;

        public static long getGetValueInvocationCount() {
            return getValueInvocationCount;
        }

        public static void incGetValueInvocationCount() {
            getValueInvocationCount++;
        }

        public static void resetGetValueInvocationCount() {
            getValueInvocationCount = 0;
        }

        private int sum;

        public static void rowMemberCodegen(AggregationFunctionFactoryCodegenRowMemberContext context) {
            context.getMembersColumnized().addMember(context.getColumn(), int.class, "sum");
        }

        public void enter(Object value) {
            int amount = (Integer) value;
            sum += amount;
        }

        public static void applyEnterCodegen(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
            context.getMethod().getBlock().declareVar(int.class, "amount", cast(Integer.class, context.getForges()[0].evaluateCodegen(Integer.class, context.getMethod(), context.getSymbols(), context.getClassScope())))
                    .assignCompound(refCol("sum", context.getColumn()), "+", ref("amount"));
        }

        public void leave(Object value) {
        }

        public Object getValue() {
            getValueInvocationCount++;
            return sum;
        }

        public static void getValueCodegen(AggregationFunctionFactoryCodegenRowGetValueContext context) {
            context.getMethod().getBlock()
                    .staticMethod(MyContextAggFunc.class, "incGetValueInvocationCount")
                    .methodReturn(refCol("sum", context.getColumn()));
        }

        public void clear() {

        }
    }
}
