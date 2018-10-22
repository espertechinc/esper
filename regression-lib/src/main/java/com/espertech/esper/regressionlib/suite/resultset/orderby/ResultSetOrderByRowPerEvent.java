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
package com.espertech.esper.regressionlib.suite.resultset.orderby;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

public class ResultSetOrderByRowPerEvent {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetIteratorAggregateRowPerEvent());
        execs.add(new ResultSetAliases());
        execs.add(new ResultSetRowPerEventJoinOrderFunction());
        execs.add(new ResultSetRowPerEventOrderFunction());
        execs.add(new ResultSetRowPerEventSum());
        execs.add(new ResultSetRowPerEventMaxSum());
        execs.add(new ResultSetRowPerEventSumHaving());
        execs.add(new ResultSetAggOrderWithSum());
        execs.add(new ResultSetRowPerEventJoin());
        execs.add(new ResultSetRowPerEventJoinMax());
        execs.add(new ResultSetAggHaving());
        return execs;
    }

    private static class ResultSetIteratorAggregateRowPerEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "sumPrice"};
            String epl = "@name('s0') select symbol, sum(price) as sumPrice from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "order by symbol";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("KGB"));

            sendEvent(env, "CAT", 50);
            sendEvent(env, "IBM", 49);
            sendEvent(env, "CAT", 15);
            sendEvent(env, "IBM", 100);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields,
                new Object[][]{
                    {"CAT", 214d},
                    {"CAT", 214d},
                    {"IBM", 214d},
                    {"IBM", 214d},
                });

            sendEvent(env, "KGB", 75);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields,
                new Object[][]{
                    {"CAT", 289d},
                    {"CAT", 289d},
                    {"IBM", 289d},
                    {"IBM", 289d},
                    {"KGB", 289d},
                });

            env.undeployAll();
        }
    }

    private static class ResultSetAliases implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol as mySymbol, sum(price) as mySum from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by mySymbol";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "IBM", 3);
            sendEvent(env, "IBM", 4);

            env.milestone(0);

            sendEvent(env, "CMU", 1);
            sendEvent(env, "CMU", 2);
            sendEvent(env, "CAT", 5);

            env.milestone(1);

            sendEvent(env, "CAT", 6);

            String[] fields = "mySymbol,mySum".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

            env.undeployAll();
        }
    }

    private static class ResultSetRowPerEventJoinOrderFunction implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, sum(price) from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by volume*sum(price), symbol";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "IBM", 2);

            env.milestone(0);

            sendEvent(env, "KGB", 1);
            sendEvent(env, "CMU", 3);
            sendEvent(env, "IBM", 6);
            sendEvent(env, "CAT", 6);

            env.milestone(1);

            sendEvent(env, "CAT", 5);

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("CMU"));

            env.milestone(2);

            env.sendEventBean(new SupportBeanString("KGB"));
            env.sendEventBean(new SupportBeanString("DOG"));

            String[] fields = "symbol".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT"}, {"CAT"}, {"CMU"}, {"IBM"}, {"IBM"}, {"KGB"}});

            env.undeployAll();
        }
    }

    private static class ResultSetRowPerEventOrderFunction implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, sum(price) from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by volume*sum(price), symbol";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "IBM", 2);
            sendEvent(env, "KGB", 1);
            sendEvent(env, "CMU", 3);
            sendEvent(env, "IBM", 6);

            env.milestone(0);

            sendEvent(env, "CAT", 6);
            sendEvent(env, "CAT", 5);

            String[] fields = "symbol".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT"}, {"CAT"}, {"CMU"}, {"IBM"}, {"IBM"}, {"KGB"}});

            env.undeployAll();
        }
    }

    private static class ResultSetRowPerEventSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, sum(price) from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by symbol";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "IBM", 3);
            sendEvent(env, "IBM", 4);
            sendEvent(env, "CMU", 1);
            sendEvent(env, "CMU", 2);

            env.milestone(0);

            sendEvent(env, "CAT", 5);
            sendEvent(env, "CAT", 6);

            String[] fields = "symbol,sum(price)".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

            env.undeployAll();
        }
    }

    private static class ResultSetRowPerEventMaxSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, max(sum(price)) from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by symbol";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "IBM", 3);
            sendEvent(env, "IBM", 4);

            env.milestone(0);

            sendEvent(env, "CMU", 1);
            sendEvent(env, "CMU", 2);
            sendEvent(env, "CAT", 5);

            env.milestone(1);

            sendEvent(env, "CAT", 6);

            String[] fields = "symbol,max(sum(price))".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

            env.undeployAll();
        }
    }

    private static class ResultSetRowPerEventSumHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, sum(price) from " +
                "SupportMarketDataBean#length(10) " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by symbol";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "IBM", 3);
            sendEvent(env, "IBM", 4);
            sendEvent(env, "CMU", 1);

            env.milestone(0);

            sendEvent(env, "CMU", 2);
            sendEvent(env, "CAT", 5);
            sendEvent(env, "CAT", 6);

            String[] fields = "symbol,sum(price)".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

            env.undeployAll();
        }
    }

    private static class ResultSetAggOrderWithSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, sum(price) from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by symbol, sum(price)";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "IBM", 3);
            sendEvent(env, "IBM", 4);

            env.milestone(0);

            sendEvent(env, "CMU", 1);
            sendEvent(env, "CMU", 2);
            sendEvent(env, "CAT", 5);
            sendEvent(env, "CAT", 6);

            String[] fields = "symbol,sum(price)".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

            env.undeployAll();
        }
    }

    private static class ResultSetRowPerEventJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, sum(price) from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol, sum(price)";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "IBM", 3);
            sendEvent(env, "IBM", 4);
            sendEvent(env, "CMU", 1);
            sendEvent(env, "CMU", 2);
            sendEvent(env, "CAT", 5);

            env.milestone(0);

            sendEvent(env, "CAT", 6);

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("CMU"));

            String[] fields = "symbol,sum(price)".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT", 11.0}, {"CAT", 11.0}, {"CMU", 21.0}, {"CMU", 21.0}, {"IBM", 18.0}, {"IBM", 18.0}});

            env.undeployAll();
        }
    }

    private static class ResultSetRowPerEventJoinMax implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, max(sum(price)) from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "IBM", 3);
            sendEvent(env, "IBM", 4);
            sendEvent(env, "CMU", 1);
            sendEvent(env, "CMU", 2);

            env.milestone(0);

            sendEvent(env, "CAT", 5);
            sendEvent(env, "CAT", 6);

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));

            env.milestone(1);

            env.sendEventBean(new SupportBeanString("CMU"));

            String[] fields = "symbol,max(sum(price))".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT", 11.0}, {"CAT", 11.0}, {"CMU", 21.0}, {"CMU", 21.0}, {"IBM", 18.0}, {"IBM", 18.0}});

            env.undeployAll();
        }
    }

    private static class ResultSetAggHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, sum(price) from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by symbol";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendEvent(env, "IBM", 3);
            sendEvent(env, "IBM", 4);
            sendEvent(env, "CMU", 1);
            sendEvent(env, "CMU", 2);

            env.milestone(1);

            sendEvent(env, "CAT", 5);
            sendEvent(env, "CAT", 6);

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("CMU"));

            String[] fields = "symbol,sum(price)".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"CAT", 11.0}, {"CAT", 11.0}, {"CMU", 21.0}, {"CMU", 21.0}, {"IBM", 18.0}, {"IBM", 18.0}});

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }
}
