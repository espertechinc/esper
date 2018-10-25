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
package com.espertech.esper.regressionlib.suite.resultset.outputlimit;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.epl.SupportOutputLimitOpt;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertExecution;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertTestResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;

public class ResultSetOutputLimitRowPerGroupRollup {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetOutputLast(false));
        execs.add(new ResultSetOutputLast(true));
        execs.add(new ResultSetOutputLastSorted(false));
        execs.add(new ResultSetOutputLastSorted(true));
        execs.add(new ResultSetOutputAll(false));
        execs.add(new ResultSetOutputAll(true));
        execs.add(new ResultSetOutputAllSorted(false));
        execs.add(new ResultSetOutputAllSorted(true));
        execs.add(new ResultSetOutputDefault(false));
        execs.add(new ResultSetOutputDefault(true));
        execs.add(new ResultSetOutputDefaultSorted(false));
        execs.add(new ResultSetOutputDefaultSorted(true));
        execs.add(new ResultSetOutputFirstHaving(false));
        execs.add(new ResultSetOutputFirstHaving(true));
        execs.add(new ResultSetOutputFirstSorted(false));
        execs.add(new ResultSetOutputFirstSorted(true));
        execs.add(new ResultSetOutputFirst(false));
        execs.add(new ResultSetOutputFirst(true));
        execs.add(new ResultSet3OutputLimitAll());
        execs.add(new ResultSet4OutputLimitLast());
        execs.add(new ResultSet1NoOutputLimit());
        execs.add(new ResultSet2OutputLimitDefault());
        execs.add(new ResultSet5OutputLimitFirst());
        execs.add(new ResultSet6OutputLimitSnapshot(false));
        execs.add(new ResultSet6OutputLimitSnapshot(true));
        execs.add(new ResultSetOutputSnapshotOrderWLimit());
        return execs;
    }

    private static class ResultSetOutputSnapshotOrderWLimit implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select theString as c0, sum(intPrimitive) as c1 from SupportBean group by rollup(theString) " +
                "output snapshot every 1 seconds " +
                "order by sum(intPrimitive) " +
                "limit 3";
            env.compileDeploy(epl).addListener("s0");


            sendEvent(env, "E1", 12);
            sendEvent(env, "E2", 11);
            sendEvent(env, "E3", 10);
            sendEvent(env, "E4", 13);
            sendEvent(env, "E2", 5);

            sendTimer(env, 1000);

            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), "c0,c1".split(","), new Object[][]{{"E3", 10}, {"E1", 12}, {"E4", 13}});

            env.undeployAll();
        }
    }

    private static class ResultSet1NoOutputLimit implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec)" +
                "group by rollup(symbol)";
            sendTimer(env, 0);
            env.compileDeploy(stmtText).addListener("s0");

            String[] fields = new String[]{"symbol", "sum(price)"};
            ResultAssertTestResult expected = new ResultAssertTestResult("NoOutputLimit", null, fields);
            expected.addResultInsRem(200, 1, new Object[][]{{"IBM", 25d}, {null, 25d}}, new Object[][]{{"IBM", null}, {null, null}});
            expected.addResultInsRem(800, 1, new Object[][]{{"MSFT", 9d}, {null, 34d}}, new Object[][]{{"MSFT", null}, {null, 25d}});
            expected.addResultInsRem(1500, 1, new Object[][]{{"IBM", 49d}, {null, 58d}}, new Object[][]{{"IBM", 25d}, {null, 34d}});
            expected.addResultInsRem(1500, 2, new Object[][]{{"YAH", 1d}, {null, 59d}}, new Object[][]{{"YAH", null}, {null, 58d}});
            expected.addResultInsRem(2100, 1, new Object[][]{{"IBM", 75d}, {null, 85d}}, new Object[][]{{"IBM", 49d}, {null, 59d}});
            expected.addResultInsRem(3500, 1, new Object[][]{{"YAH", 3d}, {null, 87d}}, new Object[][]{{"YAH", 1d}, {null, 85d}});
            expected.addResultInsRem(4300, 1, new Object[][]{{"IBM", 97d}, {null, 109d}}, new Object[][]{{"IBM", 75d}, {null, 87d}});
            expected.addResultInsRem(4900, 1, new Object[][]{{"YAH", 6d}, {null, 112d}}, new Object[][]{{"YAH", 3d}, {null, 109d}});
            expected.addResultInsRem(5700, 0, new Object[][]{{"IBM", 72d}, {null, 87d}}, new Object[][]{{"IBM", 97d}, {null, 112d}});
            expected.addResultInsRem(5900, 1, new Object[][]{{"YAH", 7d}, {null, 88d}}, new Object[][]{{"YAH", 6d}, {null, 87d}});
            expected.addResultInsRem(6300, 0, new Object[][]{{"MSFT", null}, {null, 79d}}, new Object[][]{{"MSFT", 9d}, {null, 88d}});
            expected.addResultInsRem(7000, 0, new Object[][]{{"IBM", 48d}, {"YAH", 6d}, {null, 54d}}, new Object[][]{{"IBM", 72d}, {"YAH", 7d}, {null, 79d}});

            ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
            execution.execute(false);
        }
    }

    private static class ResultSet2OutputLimitDefault implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec)" +
                "group by rollup(symbol)" +
                "output every 1 seconds";
            sendTimer(env, 0);
            env.compileDeploy(stmtText).addListener("s0");

            String[] fields = new String[]{"symbol", "sum(price)"};
            ResultAssertTestResult expected = new ResultAssertTestResult("DefaultOutputLimit", null, fields);
            expected.addResultInsRem(1200, 0,
                new Object[][]{{"IBM", 25d}, {null, 25d}, {"MSFT", 9d}, {null, 34d}},
                new Object[][]{{"IBM", null}, {null, null}, {"MSFT", null}, {null, 25d}});
            expected.addResultInsRem(2200, 0,
                new Object[][]{{"IBM", 49d}, {null, 58d}, {"YAH", 1d}, {null, 59d}, {"IBM", 75d}, {null, 85d}},
                new Object[][]{{"IBM", 25d}, {null, 34d}, {"YAH", null}, {null, 58d}, {"IBM", 49d}, {null, 59d}});
            expected.addResultInsRem(3200, 0, null, null);
            expected.addResultInsRem(4200, 0,
                new Object[][]{{"YAH", 3d}, {null, 87d}},
                new Object[][]{{"YAH", 1d}, {null, 85d}});
            expected.addResultInsRem(5200, 0,
                new Object[][]{{"IBM", 97d}, {null, 109d}, {"YAH", 6d}, {null, 112d}},
                new Object[][]{{"IBM", 75d}, {null, 87d}, {"YAH", 3d}, {null, 109d}});
            expected.addResultInsRem(6200, 0,
                new Object[][]{{"IBM", 72d}, {null, 87d}, {"YAH", 7d}, {null, 88d}},
                new Object[][]{{"IBM", 97d}, {null, 112d}, {"YAH", 6d}, {null, 87d}});
            expected.addResultInsRem(7200, 0,
                new Object[][]{{"MSFT", null}, {null, 79d}, {"IBM", 48d}, {"YAH", 6d}, {null, 54d}},
                new Object[][]{{"MSFT", 9d}, {null, 88d}, {"IBM", 72d}, {"YAH", 7d}, {null, 79d}});

            ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
            execution.execute(false);
        }
    }

    private static class ResultSet3OutputLimitAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                if (env.isHA() && outputLimitOpt == SupportOutputLimitOpt.DISABLED) {
                    continue;
                }
                runAssertion3OutputLimitAll(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion3OutputLimitAll(RegressionEnvironment env, SupportOutputLimitOpt outputLimitOpt) {
        String stmtText = outputLimitOpt.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec)" +
            "group by rollup(symbol)" +
            "output all every 1 seconds";
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult("AllOutputLimit", null, fields);
        expected.addResultInsRem(1200, 0,
            new Object[][]{{"IBM", 25d}, {"MSFT", 9d}, {null, 34d}},
            new Object[][]{{"IBM", null}, {"MSFT", null}, {null, null}});
        expected.addResultInsRem(2200, 0,
            new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}, {null, 85d}},
            new Object[][]{{"IBM", 25d}, {"MSFT", 9d}, {"YAH", null}, {null, 34d}});
        expected.addResultInsRem(3200, 0,
            new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}, {null, 85d}},
            new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}, {null, 85d}});
        expected.addResultInsRem(4200, 0,
            new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 3d}, {null, 87d}},
            new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}, {null, 85d}});
        expected.addResultInsRem(5200, 0,
            new Object[][]{{"IBM", 97d}, {"MSFT", 9d}, {"YAH", 6d}, {null, 112d}},
            new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 3d}, {null, 87d}});
        expected.addResultInsRem(6200, 0,
            new Object[][]{{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}, {null, 88d}},
            new Object[][]{{"IBM", 97d}, {"MSFT", 9d}, {"YAH", 6d}, {null, 112d}});
        expected.addResultInsRem(7200, 0,
            new Object[][]{{"IBM", 48d}, {"MSFT", null}, {"YAH", 6d}, {null, 54d}},
            new Object[][]{{"IBM", 72d}, {"MSFT", 9d}, {"YAH", 7d}, {null, 88d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(true);
    }

    private static class ResultSet4OutputLimitLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion4OutputLimitLast(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion4OutputLimitLast(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec)" +
            "group by rollup(symbol)" +
            "output last every 1 seconds";
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult("AllOutputLimit", null, fields);
        expected.addResultInsRem(1200, 0,
            new Object[][]{{"IBM", 25d}, {"MSFT", 9d}, {null, 34d}},
            new Object[][]{{"IBM", null}, {"MSFT", null}, {null, null}});
        expected.addResultInsRem(2200, 0,
            new Object[][]{{"IBM", 75d}, {"YAH", 1d}, {null, 85d}},
            new Object[][]{{"IBM", 25d}, {"YAH", null}, {null, 34d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0,
            new Object[][]{{"YAH", 3d}, {null, 87d}},
            new Object[][]{{"YAH", 1d}, {null, 85d}});
        expected.addResultInsRem(5200, 0,
            new Object[][]{{"IBM", 97d}, {"YAH", 6d}, {null, 112d}},
            new Object[][]{{"IBM", 75d}, {"YAH", 3d}, {null, 87d}});
        expected.addResultInsRem(6200, 0,
            new Object[][]{{"IBM", 72d}, {"YAH", 7d}, {null, 88d}},
            new Object[][]{{"IBM", 97d}, {"YAH", 6d}, {null, 112d}});
        expected.addResultInsRem(7200, 0,
            new Object[][]{{"MSFT", null}, {"IBM", 48d}, {"YAH", 6d}, {null, 54d}},
            new Object[][]{{"MSFT", 9d}, {"IBM", 72d}, {"YAH", 7d}, {null, 88d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(true);
    }

    private static class ResultSet5OutputLimitFirst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec)" +
                "group by rollup(symbol)" +
                "output first every 1 seconds";
            sendTimer(env, 0);
            env.compileDeploy(stmtText).addListener("s0");

            String[] fields = new String[]{"symbol", "sum(price)"};
            ResultAssertTestResult expected = new ResultAssertTestResult("AllOutputLimit", null, fields);
            expected.addResultInsRem(200, 1, new Object[][]{{"IBM", 25d}, {null, 25d}}, new Object[][]{{"IBM", null}, {null, null}});
            expected.addResultInsRem(800, 1, new Object[][]{{"MSFT", 9d}}, new Object[][]{{"MSFT", null}});
            expected.addResultInsRem(1500, 1, new Object[][]{{"IBM", 49d}, {null, 58d}}, new Object[][]{{"IBM", 25d}, {null, 34d}});
            expected.addResultInsRem(1500, 2, new Object[][]{{"YAH", 1d}}, new Object[][]{{"YAH", null}});
            expected.addResultInsRem(3500, 1, new Object[][]{{"YAH", 3d}, {null, 87d}}, new Object[][]{{"YAH", 1d}, {null, 85d}});
            expected.addResultInsRem(4300, 1, new Object[][]{{"IBM", 97d}}, new Object[][]{{"IBM", 75d}});
            expected.addResultInsRem(4900, 1, new Object[][]{{"YAH", 6d}, {null, 112d}}, new Object[][]{{"YAH", 3d}, {null, 109d}});
            expected.addResultInsRem(5700, 0, new Object[][]{{"IBM", 72d}}, new Object[][]{{"IBM", 97d}});
            expected.addResultInsRem(5900, 1, new Object[][]{{"YAH", 7d}, {null, 88d}}, new Object[][]{{"YAH", 6d}, {null, 87d}});
            expected.addResultInsRem(6300, 0, new Object[][]{{"MSFT", null}}, new Object[][]{{"MSFT", 9d}});
            expected.addResultInsRem(7000, 0, new Object[][]{{"IBM", 48d}, {"YAH", 6d}, {null, 54d}}, new Object[][]{{"IBM", 72d}, {"YAH", 7d}, {null, 79d}});

            ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
            execution.execute(false);
        }
    }

    private static class ResultSet6OutputLimitSnapshot implements RegressionExecution {
        private final boolean join;

        public ResultSet6OutputLimitSnapshot(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec)" +
                (join ? ",SupportBean#keepall " : " ") +
                "group by rollup(symbol)" +
                "output snapshot every 1 seconds";
            sendTimer(env, 0);
            env.compileDeploy(stmtText).addListener("s0");
            env.sendEventBean(new SupportBean());

            if (join) { // join has different results
                env.undeployAll();
                return;
            }

            String[] fields = new String[]{"symbol", "sum(price)"};
            ResultAssertTestResult expected = new ResultAssertTestResult("AllOutputLimit", null, fields);
            expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 25d}, {"MSFT", 9d}, {null, 34.0}});
            expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}, {null, 85.0}});
            expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 1d}, {null, 85.0}});
            expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 75d}, {"MSFT", 9d}, {"YAH", 3d}, {null, 87.0}});
            expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 97d}, {"MSFT", 9d}, {"YAH", 6d}, {null, 112.0}});
            expected.addResultInsert(6200, 0, new Object[][]{{"MSFT", 9d}, {"IBM", 72d}, {"YAH", 7d}, {null, 88.0}});
            expected.addResultInsert(7200, 0, new Object[][]{{"IBM", 48d}, {"YAH", 6d}, {null, 54.0}});

            ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
            execution.execute(false);
        }
    }

    private static class ResultSetOutputFirstHaving implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputFirstHaving(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            env.advanceTime(0);

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#time(3.5 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive) " +
                "having sum(longPrimitive) > 100 " +
                "output first every 1 second";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(makeEvent("E1", 1, 10L));
            env.sendEventBean(makeEvent("E1", 2, 20L));
            env.sendEventBean(makeEvent("E1", 1, 30L));
            env.advanceTime(1000);
            env.sendEventBean(makeEvent("E2", 1, 40L));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(makeEvent("E1", 2, 50L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", null, 110L}, {null, null, 150L}},
                new Object[][]{{"E1", null, 110L}, {null, null, 150L}});

            // pass 1 second
            env.advanceTime(2000);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(makeEvent("E1", 1, 60L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", null, 170L}, {null, null, 210L}},
                new Object[][]{{"E1", null, 170L}, {null, null, 210L}});

            // pass 1 second
            env.advanceTime(3000);

            env.sendEventBean(makeEvent("E1", 1, 70L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 170L}, {"E1", null, 240L}, {null, null, 280L}},
                new Object[][]{{"E1", 1, 170L}, {"E1", null, 240L}, {null, null, 280L}});

            env.advanceTime(4000); // removes the first 3 events
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 130L}, {"E1", null, 180L}, {null, null, 220L}},
                new Object[][]{{"E1", 1, 130L}, {"E1", null, 180L}, {null, null, 220L}});

            env.sendEventBean(makeEvent("E1", 1, 80L));
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(5000); // removes the second 2 events
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", null, 210L}, {null, null, 210L}},
                new Object[][]{{"E1", null, 210L}, {null, null, 210L}});

            env.sendEventBean(makeEvent("E1", 1, 90L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 300L}},
                new Object[][]{{"E1", 1, 300L}});

            env.advanceTime(6000); // removes the third 1 event
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 240L}, {"E1", null, 240L}, {null, null, 240L}},
                new Object[][]{{"E1", 1, 240L}, {"E1", null, 240L}, {null, null, 240L}});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputFirst implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputFirst(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            env.advanceTime(0);

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#time(3.5 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive) " +
                "output first every 1 second";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));

            env.milestone(0);

            env.sendEventBean(makeEvent("E1", 1, 10L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 10L}, {"E1", null, 10L}, {null, null, 10L}},
                new Object[][]{{"E1", 1, null}, {"E1", null, null}, {null, null, null}});

            env.milestone(1);

            env.sendEventBean(makeEvent("E1", 2, 20L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 2, 20L}},
                new Object[][]{{"E1", 2, null}});

            env.milestone(2);

            // pass 1 second
            env.sendEventBean(makeEvent("E1", 1, 30L));
            env.advanceTime(1000);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(makeEvent("E2", 1, 40L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", 1, 40L}, {"E2", null, 40L}, {null, null, 100L}},
                new Object[][]{{"E2", 1, null}, {"E2", null, null}, {null, null, 60L}});

            env.milestone(3);

            env.sendEventBean(makeEvent("E1", 2, 50L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 2, 70L}, {"E1", null, 110L}},
                new Object[][]{{"E1", 2, 20L}, {"E1", null, 60L}});

            env.milestone(4);

            // pass 1 second
            env.advanceTime(2000);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(makeEvent("E1", 1, 60L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 100L}, {"E1", null, 170L}, {null, null, 210L}},
                new Object[][]{{"E1", 1, 40L}, {"E1", null, 110L}, {null, null, 150L}});

            env.milestone(5);

            // pass 1 second
            env.advanceTime(3000);

            env.sendEventBean(makeEvent("E1", 1, 70L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 170L}, {"E1", null, 240L}, {null, null, 280L}},
                new Object[][]{{"E1", 1, 100L}, {"E1", null, 170L}, {null, null, 210L}});

            env.advanceTime(4000); // removes the first 3 events
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 130L}, {"E1", 2, 50L}, {"E1", null, 180L}, {null, null, 220L}},
                new Object[][]{{"E1", 1, 170L}, {"E1", 2, 70L}, {"E1", null, 240L}, {null, null, 280L}});

            env.milestone(6);

            env.sendEventBean(makeEvent("E1", 1, 80L));
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(5000); // removes the second 2 events
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E2", 1, null}, {"E1", 2, null}, {"E2", null, null},
                    {"E1", null, 210L}, {null, null, 210L}},
                new Object[][]{
                    {"E2", 1, 40L}, {"E1", 2, 50L}, {"E2", null, 40L},
                    {"E1", null, 260L}, {null, null, 300L}});

            env.sendEventBean(makeEvent("E1", 1, 90L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 300L}},
                new Object[][]{{"E1", 1, 210L}});

            env.milestone(7);

            env.advanceTime(6000); // removes the third 1 event
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 240L}, {"E1", null, 240L}, {null, null, 240L}},
                new Object[][]{{"E1", 1, 300L}, {"E1", null, 300L}, {null, null, 300L}});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputFirstSorted implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputFirstSorted(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            env.advanceTime(0);

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#time(3.5 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive) " +
                "output first every 1 second " +
                "order by theString, intPrimitive";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(makeEvent("E1", 1, 10L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 10L}, {"E1", null, 10L}, {"E1", 1, 10L}},
                new Object[][]{{null, null, null}, {"E1", null, null}, {"E1", 1, null}});

            env.sendEventBean(makeEvent("E1", 2, 20L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 2, 20L}},
                new Object[][]{{"E1", 2, null}});

            // pass 1 second
            env.sendEventBean(makeEvent("E1", 1, 30L));
            env.advanceTime(1000);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(makeEvent("E2", 1, 40L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 100L}, {"E2", null, 40L}, {"E2", 1, 40L}},
                new Object[][]{{null, null, 60L}, {"E2", null, null}, {"E2", 1, null}});

            env.sendEventBean(makeEvent("E1", 2, 50L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", null, 110L}, {"E1", 2, 70L}},
                new Object[][]{{"E1", null, 60L}, {"E1", 2, 20L}});

            // pass 1 second
            env.advanceTime(2000);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(makeEvent("E1", 1, 60L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 210L}, {"E1", null, 170L}, {"E1", 1, 100L}},
                new Object[][]{{null, null, 150L}, {"E1", null, 110L}, {"E1", 1, 40L}});

            // pass 1 second
            env.advanceTime(3000);

            env.sendEventBean(makeEvent("E1", 1, 70L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 280L}, {"E1", null, 240L}, {"E1", 1, 170L}},
                new Object[][]{{null, null, 210L}, {"E1", null, 170L}, {"E1", 1, 100L}});

            env.advanceTime(4000); // removes the first 3 events
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 220L}, {"E1", null, 180L}, {"E1", 1, 130L}, {"E1", 2, 50L}},
                new Object[][]{{null, null, 280L}, {"E1", null, 240L}, {"E1", 1, 170L}, {"E1", 2, 70L}});

            env.sendEventBean(makeEvent("E1", 1, 80L));
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(5000); // removes the second 2 events
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 210L}, {"E1", null, 210L}, {"E1", 2, null},
                    {"E2", null, null}, {"E2", 1, null}},
                new Object[][]{{null, null, 300L}, {"E1", null, 260L}, {"E1", 2, 50L},
                    {"E2", null, 40L}, {"E2", 1, 40L}});

            env.sendEventBean(makeEvent("E1", 1, 90L));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 1, 300L}},
                new Object[][]{{"E1", 1, 210L}});

            env.advanceTime(6000); // removes the third 1 event
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 240L}, {"E1", null, 240L}, {"E1", 1, 240L}},
                new Object[][]{{null, null, 300L}, {"E1", null, 300L}, {"E1", 1, 300L}});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputDefault implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputDefault(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            env.advanceTime(0);

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#time(3.5 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive) " +
                "output every 1 second";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(makeEvent("E1", 1, 10L));
            env.sendEventBean(makeEvent("E1", 2, 20L));
            env.sendEventBean(makeEvent("E1", 1, 30L));
            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E1", 1, 10L}, {"E1", null, 10L}, {null, null, 10L},
                    {"E1", 2, 20L}, {"E1", null, 30L}, {null, null, 30L},
                    {"E1", 1, 40L}, {"E1", null, 60L}, {null, null, 60L}},
                new Object[][]{
                    {"E1", 1, null}, {"E1", null, null}, {null, null, null},
                    {"E1", 2, null}, {"E1", null, 10L}, {null, null, 10L},
                    {"E1", 1, 10L}, {"E1", null, 30L}, {null, null, 30L}});

            env.sendEventBean(makeEvent("E2", 1, 40L));
            env.sendEventBean(makeEvent("E1", 2, 50L));
            env.advanceTime(2000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E2", 1, 40L}, {"E2", null, 40L}, {null, null, 100L},
                    {"E1", 2, 70L}, {"E1", null, 110L}, {null, null, 150L}},
                new Object[][]{
                    {"E2", 1, null}, {"E2", null, null}, {null, null, 60L},
                    {"E1", 2, 20L}, {"E1", null, 60L}, {null, null, 100L}});

            env.sendEventBean(makeEvent("E1", 1, 60L));
            env.advanceTime(3000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E1", 1, 100L}, {"E1", null, 170L}, {null, null, 210L}},
                new Object[][]{
                    {"E1", 1, 40L}, {"E1", null, 110L}, {null, null, 150L}});

            env.sendEventBean(makeEvent("E1", 1, 70L));    // removes the first 3 events
            env.advanceTimeSpan(4000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E1", 1, 170L}, {"E1", null, 240L}, {null, null, 280L},
                    {"E1", 1, 130L}, {"E1", 2, 50L}, {"E1", null, 180L}, {null, null, 220L},
                },
                new Object[][]{
                    {"E1", 1, 100L}, {"E1", null, 170L}, {null, null, 210L},
                    {"E1", 1, 170L}, {"E1", 2, 70L}, {"E1", null, 240L}, {null, null, 280L},
                });

            env.sendEventBean(makeEvent("E1", 1, 80L));    // removes the second 2 events
            env.advanceTimeSpan(5000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E1", 1, 210L}, {"E1", null, 260L}, {null, null, 300L},
                    {"E2", 1, null}, {"E1", 2, null}, {"E2", null, null}, {"E1", null, 210L}, {null, null, 210L},
                },
                new Object[][]{
                    {"E1", 1, 130L}, {"E1", null, 180L}, {null, null, 220L},
                    {"E2", 1, 40L}, {"E1", 2, 50L}, {"E2", null, 40L}, {"E1", null, 260L}, {null, null, 300L},
                });

            env.sendEventBean(makeEvent("E1", 1, 90L));    // removes the third 1 event
            env.advanceTimeSpan(6000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E1", 1, 300L}, {"E1", null, 300L}, {null, null, 300L},
                    {"E1", 1, 240L}, {"E1", null, 240L}, {null, null, 240L}},
                new Object[][]{
                    {"E1", 1, 210L}, {"E1", null, 210L}, {null, null, 210L},
                    {"E1", 1, 300L}, {"E1", null, 300L}, {null, null, 300L}});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputDefaultSorted implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputDefaultSorted(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            env.advanceTime(0);

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#time(3.5 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive) " +
                "output every 1 second " +
                "order by theString, intPrimitive";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(makeEvent("E1", 1, 10L));
            env.sendEventBean(makeEvent("E1", 2, 20L));
            env.sendEventBean(makeEvent("E1", 1, 30L));
            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {null, null, 10L}, {null, null, 30L}, {null, null, 60L},
                    {"E1", null, 10L}, {"E1", null, 30L}, {"E1", null, 60L},
                    {"E1", 1, 10L}, {"E1", 1, 40L}, {"E1", 2, 20L}},
                new Object[][]{
                    {null, null, null}, {null, null, 10L}, {null, null, 30L},
                    {"E1", null, null}, {"E1", null, 10L}, {"E1", null, 30L},
                    {"E1", 1, null}, {"E1", 1, 10L}, {"E1", 2, null}});

            env.sendEventBean(makeEvent("E2", 1, 40L));
            env.sendEventBean(makeEvent("E1", 2, 50L));
            env.advanceTime(2000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {null, null, 100L}, {null, null, 150L},
                    {"E1", null, 110L}, {"E1", 2, 70L},
                    {"E2", null, 40L}, {"E2", 1, 40L}},
                new Object[][]{
                    {null, null, 60L}, {null, null, 100L},
                    {"E1", null, 60L}, {"E1", 2, 20L},
                    {"E2", null, null}, {"E2", 1, null}});

            env.sendEventBean(makeEvent("E1", 1, 60L));
            env.advanceTime(3000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {null, null, 210L}, {"E1", null, 170L}, {"E1", 1, 100L}},
                new Object[][]{
                    {null, null, 150L}, {"E1", null, 110L}, {"E1", 1, 40L}});

            env.sendEventBean(makeEvent("E1", 1, 70L));    // removes the first 3 events
            env.advanceTimeSpan(4000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {null, null, 280L}, {null, null, 220L},
                    {"E1", null, 240L}, {"E1", null, 180L},
                    {"E1", 1, 170L}, {"E1", 1, 130L}, {"E1", 2, 50L}},
                new Object[][]{
                    {null, null, 210L}, {null, null, 280L},
                    {"E1", null, 170L}, {"E1", null, 240L},
                    {"E1", 1, 100L}, {"E1", 1, 170L}, {"E1", 2, 70L}});

            env.sendEventBean(makeEvent("E1", 1, 80L));    // removes the second 2 events
            env.advanceTimeSpan(5000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {null, null, 300L}, {null, null, 210L},
                    {"E1", null, 260L}, {"E1", null, 210L},
                    {"E1", 1, 210L}, {"E1", 2, null}, {"E2", null, null}, {"E2", 1, null}},
                new Object[][]{
                    {null, null, 220L}, {null, null, 300L},
                    {"E1", null, 180L}, {"E1", null, 260L},
                    {"E1", 1, 130L}, {"E1", 2, 50L}, {"E2", null, 40L}, {"E2", 1, 40L}});

            env.sendEventBean(makeEvent("E1", 1, 90L));    // removes the third 1 event
            env.advanceTimeSpan(6000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {null, null, 300L}, {null, null, 240L},
                    {"E1", null, 300L}, {"E1", null, 240L},
                    {"E1", 1, 300L}, {"E1", 1, 240L}},
                new Object[][]{
                    {null, null, 210L}, {null, null, 300L},
                    {"E1", null, 210L}, {"E1", null, 300L},
                    {"E1", 1, 210L}, {"E1", 1, 300L}});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputAll implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputAll(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertionOutputAll(env, join, outputLimitOpt);
            }
        }
    }

    private static void runAssertionOutputAll(RegressionEnvironment env, boolean join, SupportOutputLimitOpt opt) {
        String[] fields = "c0,c1,c2".split(",");
        env.advanceTime(0);

        String epl = opt.getHint() + "@Name('s0')" +
            "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
            "from SupportBean#time(3.5 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
            "group by rollup(theString, intPrimitive) " +
            "output all every 1 second";
        env.compileDeploy(epl).addListener("s0");
        env.sendEventBean(new SupportBean_S0(1));

        env.sendEventBean(makeEvent("E1", 1, 10L));
        env.sendEventBean(makeEvent("E1", 2, 20L));
        env.sendEventBean(makeEvent("E1", 1, 30L));
        env.advanceTime(1000);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 40L}, {"E1", 2, 20L}, {"E1", null, 60L}, {null, null, 60L}},
            new Object[][]{{"E1", 1, null}, {"E1", 2, null}, {"E1", null, null}, {null, null, null}});

        env.sendEventBean(makeEvent("E2", 1, 40L));
        env.sendEventBean(makeEvent("E1", 2, 50L));
        env.advanceTime(2000);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 40L}, {"E1", 2, 70L}, {"E2", 1, 40L}, {"E1", null, 110L}, {"E2", null, 40L}, {null, null, 150L}},
            new Object[][]{{"E1", 1, 40L}, {"E1", 2, 20L}, {"E2", 1, null}, {"E1", null, 60L}, {"E2", null, null}, {null, null, 60L}});

        env.sendEventBean(makeEvent("E1", 1, 60L));
        env.advanceTime(3000);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 100L}, {"E1", 2, 70L}, {"E2", 1, 40L}, {"E1", null, 170L}, {"E2", null, 40L}, {null, null, 210L}},
            new Object[][]{{"E1", 1, 40L}, {"E1", 2, 70L}, {"E2", 1, 40L}, {"E1", null, 110L}, {"E2", null, 40L}, {null, null, 150L}});

        env.sendEventBean(makeEvent("E1", 1, 70L));    // removes the first 3 events
        env.advanceTimeSpan(4000);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 130L}, {"E1", 2, 50L}, {"E2", 1, 40L}, {"E1", null, 180L}, {"E2", null, 40L}, {null, null, 220L}},
            new Object[][]{{"E1", 1, 100L}, {"E1", 2, 70L}, {"E2", 1, 40L}, {"E1", null, 170L}, {"E2", null, 40L}, {null, null, 210L}});

        env.sendEventBean(makeEvent("E1", 1, 80L));    // removes the second 2 events
        env.advanceTimeSpan(5000);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 210L}, {"E1", 2, null}, {"E2", 1, null}, {"E1", null, 210L}, {"E2", null, null}, {null, null, 210L}},
            new Object[][]{{"E1", 1, 130L}, {"E1", 2, 50L}, {"E2", 1, 40L}, {"E1", null, 180L}, {"E2", null, 40L}, {null, null, 220L}});

        env.sendEventBean(makeEvent("E1", 1, 90L));    // removes the third 1 event
        env.advanceTimeSpan(6000);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 240L}, {"E1", 2, null}, {"E2", 1, null}, {"E1", null, 240L}, {"E2", null, null}, {null, null, 240L}},
            new Object[][]{{"E1", 1, 210L}, {"E1", 2, null}, {"E2", 1, null}, {"E1", null, 210L}, {"E2", null, null}, {null, null, 210L}});

        env.undeployAll();
    }

    private static class ResultSetOutputAllSorted implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputAllSorted(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            env.advanceTime(0);

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#time(3.5 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive) " +
                "output all every 1 second " +
                "order by theString, intPrimitive";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(makeEvent("E1", 1, 10L));
            env.sendEventBean(makeEvent("E1", 2, 20L));
            env.sendEventBean(makeEvent("E1", 1, 30L));
            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 60L}, {"E1", null, 60L}, {"E1", 1, 40L}, {"E1", 2, 20L}},
                new Object[][]{{null, null, null}, {"E1", null, null}, {"E1", 1, null}, {"E1", 2, null}});

            env.sendEventBean(makeEvent("E2", 1, 40L));
            env.sendEventBean(makeEvent("E1", 2, 50L));
            env.advanceTime(2000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 150L}, {"E1", null, 110L}, {"E1", 1, 40L}, {"E1", 2, 70L}, {"E2", null, 40L}, {"E2", 1, 40L}},
                new Object[][]{{null, null, 60L}, {"E1", null, 60L}, {"E1", 1, 40L}, {"E1", 2, 20L}, {"E2", null, null}, {"E2", 1, null}});

            env.sendEventBean(makeEvent("E1", 1, 60L));
            env.advanceTime(3000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 210L}, {"E1", null, 170L}, {"E1", 1, 100L}, {"E1", 2, 70L}, {"E2", null, 40L}, {"E2", 1, 40L}},
                new Object[][]{{null, null, 150L}, {"E1", null, 110L}, {"E1", 1, 40L}, {"E1", 2, 70L}, {"E2", null, 40L}, {"E2", 1, 40L}});

            env.sendEventBean(makeEvent("E1", 1, 70L));    // removes the first 3 events
            env.advanceTimeSpan(4000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 220L}, {"E1", null, 180L}, {"E1", 1, 130L}, {"E1", 2, 50L}, {"E2", null, 40L}, {"E2", 1, 40L}},
                new Object[][]{{null, null, 210L}, {"E1", null, 170L}, {"E1", 1, 100L}, {"E1", 2, 70L}, {"E2", null, 40L}, {"E2", 1, 40L}});

            env.sendEventBean(makeEvent("E1", 1, 80L));    // removes the second 2 events
            env.advanceTimeSpan(5000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 210L}, {"E1", null, 210L}, {"E1", 1, 210L}, {"E1", 2, null}, {"E2", null, null}, {"E2", 1, null}},
                new Object[][]{{null, null, 220L}, {"E1", null, 180L}, {"E1", 1, 130L}, {"E1", 2, 50L}, {"E2", null, 40L}, {"E2", 1, 40L}});

            env.sendEventBean(makeEvent("E1", 1, 90L));    // removes the third 1 event
            env.advanceTimeSpan(6000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 240L}, {"E1", null, 240L}, {"E1", 1, 240L}, {"E1", 2, null}, {"E2", null, null}, {"E2", 1, null}},
                new Object[][]{{null, null, 210L}, {"E1", null, 210L}, {"E1", 1, 210L}, {"E1", 2, null}, {"E2", null, null}, {"E2", 1, null}});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputLast implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputLast(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertionOutputLast(env, join, outputLimitOpt, milestone);
            }
        }
    }

    private static void runAssertionOutputLast(RegressionEnvironment env, boolean join, SupportOutputLimitOpt opt, AtomicInteger milestone) {
        String[] fields = "c0,c1,c2".split(",");
        env.advanceTime(0);

        String epl = opt.getHint() + "@Name('s0')" +
            "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
            "from SupportBean#time(3.5 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
            "group by rollup(theString, intPrimitive) " +
            "output last every 1 second";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1));

        env.sendEventBean(makeEvent("E1", 1, 10L));
        env.sendEventBean(makeEvent("E1", 2, 20L));
        env.sendEventBean(makeEvent("E1", 1, 30L));

        env.milestoneInc(milestone);

        env.advanceTime(1000);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 40L}, {"E1", 2, 20L}, {"E1", null, 60L}, {null, null, 60L}},
            new Object[][]{{"E1", 1, null}, {"E1", 2, null}, {"E1", null, null}, {null, null, null}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E2", 1, 40L));
        env.sendEventBean(makeEvent("E1", 2, 50L));
        env.advanceTime(2000);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E2", 1, 40L}, {"E1", 2, 70L}, {"E2", null, 40L}, {"E1", null, 110L}, {null, null, 150L}},
            new Object[][]{{"E2", 1, null}, {"E1", 2, 20L}, {"E2", null, null}, {"E1", null, 60L}, {null, null, 60L}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E1", 1, 60L));
        env.advanceTime(3000);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 100L}, {"E1", null, 170L}, {null, null, 210L}},
            new Object[][]{{"E1", 1, 40L}, {"E1", null, 110L}, {null, null, 150L}});

        env.sendEventBean(makeEvent("E1", 1, 70L));
        env.advanceTimeSpan(4000); // removes the first 3 events
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 130L}, {"E1", 2, 50L}, {"E1", null, 180L}, {null, null, 220L}},
            new Object[][]{{"E1", 1, 100L}, {"E1", 2, 70L}, {"E1", null, 170L}, {null, null, 210L}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E1", 1, 80L));
        env.advanceTimeSpan(5000); // removes the second 2 events
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 210L}, {"E2", 1, null}, {"E1", 2, null}, {"E1", null, 210L}, {"E2", null, null}, {null, null, 210L}},
            new Object[][]{{"E1", 1, 130L}, {"E2", 1, 40L}, {"E1", 2, 50L}, {"E1", null, 180L}, {"E2", null, 40L}, {null, null, 220L}});

        env.sendEventBean(makeEvent("E1", 1, 90L));
        env.advanceTimeSpan(6000); // removes the third 1 event
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
            new Object[][]{{"E1", 1, 240L}, {"E1", null, 240L}, {null, null, 240L}},
            new Object[][]{{"E1", 1, 210L}, {"E1", null, 210L}, {null, null, 210L}});

        env.undeployAll();
    }

    private static class ResultSetOutputLastSorted implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputLastSorted(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2".split(",");
            env.advanceTime(0);

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#time(3.5 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive) " +
                "output last every 1 second " +
                "order by theString, intPrimitive";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(makeEvent("E1", 1, 10L));
            env.sendEventBean(makeEvent("E1", 2, 20L));
            env.sendEventBean(makeEvent("E1", 1, 30L));
            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 60L}, {"E1", null, 60L}, {"E1", 1, 40L}, {"E1", 2, 20L}},
                new Object[][]{{null, null, null}, {"E1", null, null}, {"E1", 1, null}, {"E1", 2, null}});

            env.sendEventBean(makeEvent("E2", 1, 40L));
            env.sendEventBean(makeEvent("E1", 2, 50L));
            env.advanceTime(2000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 150L}, {"E1", null, 110L}, {"E1", 2, 70L}, {"E2", null, 40L}, {"E2", 1, 40L}},
                new Object[][]{{null, null, 60L}, {"E1", null, 60L}, {"E1", 2, 20L}, {"E2", null, null}, {"E2", 1, null}});

            env.sendEventBean(makeEvent("E1", 1, 60L));
            env.advanceTime(3000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 210L}, {"E1", null, 170L}, {"E1", 1, 100L}},
                new Object[][]{{null, null, 150L}, {"E1", null, 110L}, {"E1", 1, 40L}});

            env.sendEventBean(makeEvent("E1", 1, 70L));    // removes the first 3 events
            env.advanceTimeSpan(4000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 220L}, {"E1", null, 180L}, {"E1", 1, 130L}, {"E1", 2, 50L}},
                new Object[][]{{null, null, 210L}, {"E1", null, 170L}, {"E1", 1, 100L}, {"E1", 2, 70L}});

            env.sendEventBean(makeEvent("E1", 1, 80L));    // removes the second 2 events
            env.advanceTimeSpan(5000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 210L}, {"E1", null, 210L}, {"E1", 1, 210L}, {"E1", 2, null}, {"E2", null, null}, {"E2", 1, null}},
                new Object[][]{{null, null, 220L}, {"E1", null, 180L}, {"E1", 1, 130L}, {"E1", 2, 50L}, {"E2", null, 40L}, {"E2", 1, 40L}});

            env.sendEventBean(makeEvent("E1", 1, 90L));    // removes the third 1 event
            env.advanceTimeSpan(6000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 240L}, {"E1", null, 240L}, {"E1", 1, 240L}},
                new Object[][]{{null, null, 210L}, {"E1", null, 210L}, {"E1", 1, 210L}});

            env.undeployAll();
        }
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        return sb;
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }
}
