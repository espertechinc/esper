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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultSetOrderByRowPerGroup {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetNoHavingNoJoin());
        execs.add(new ResultSetHavingNoJoin());
        execs.add(new ResultSetNoHavingJoin());
        execs.add(new ResultSetHavingJoin());
        execs.add(new ResultSetHavingJoinAlias());
        execs.add(new ResultSetLast());
        execs.add(new ResultSetLastJoin());
        execs.add(new ResultSetIteratorRowPerGroup());
        execs.add(new ResultSetOrderByLast());
        return execs;
    }

    private static class ResultSetNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream symbol, sum(price) as mysum from " +
                "SupportMarketDataBean#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol";
            env.compileDeploy(epl).addListener("s0");
            tryAssertionNoHaving(env, milestone);
            env.undeployAll();
        }
    }

    private static class ResultSetHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream symbol, sum(price) as mysum from " +
                "SupportMarketDataBean#length(20) " +
                "group by symbol " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by sum(price), symbol";
            env.compileDeploy(epl).addListener("s0");
            tryAssertionHaving(env, milestone);
            env.undeployAll();
        }
    }

    private static class ResultSetNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream symbol, sum(price) as mysum from " +
                "SupportMarketDataBean#length(20) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("CMU"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBeanString("KGB"));
            env.sendEventBean(new SupportBeanString("DOG"));

            tryAssertionNoHaving(env, milestone);

            env.undeployAll();
        }
    }

    private static class ResultSetHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream symbol, sum(price) as mysum from " +
                "SupportMarketDataBean#length(20) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by sum(price), symbol";

            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBeanString("CMU"));
            env.sendEventBean(new SupportBeanString("KGB"));
            env.sendEventBean(new SupportBeanString("DOG"));

            tryAssertionHaving(env, milestone);

            env.undeployAll();
        }
    }

    private static class ResultSetHavingJoinAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream symbol, sum(price) as mysum from " +
                "SupportMarketDataBean#length(20) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by mysum, symbol";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("CMU"));
            env.sendEventBean(new SupportBeanString("KGB"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBeanString("DOG"));

            tryAssertionHaving(env, milestone);

            env.undeployAll();
        }
    }

    private static class ResultSetLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream symbol, sum(price) as mysum from " +
                "SupportMarketDataBean#length(20) " +
                "group by symbol " +
                "output last every 6 events " +
                "order by sum(price), symbol";
            env.compileDeploy(epl).addListener("s0");
            tryAssertionLast(env, milestone);
            env.undeployAll();
        }
    }

    private static class ResultSetLastJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream symbol, sum(price) as mysum from " +
                "SupportMarketDataBean#length(20) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "output last every 6 events " +
                "order by sum(price), symbol";

            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString("CAT"));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("CMU"));
            env.sendEventBean(new SupportBeanString("KGB"));
            env.sendEventBean(new SupportBeanString("DOG"));

            tryAssertionLast(env, milestone);

            env.undeployAll();
        }
    }

    private static class ResultSetIteratorRowPerGroup implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] fields = new String[]{"symbol", "sumPrice"};
            String epl = "@name('s0') select symbol, sum(price) as sumPrice from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "order by symbol";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("CMU"));
            env.sendEventBean(new SupportBeanString("KGB"));
            env.sendEventBean(new SupportBeanString("DOG"));

            sendEvent(env, "CAT", 50);
            sendEvent(env, "IBM", 49);
            sendEvent(env, "CAT", 15);
            sendEvent(env, "IBM", 100);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields,
                new Object[][]{
                    {"CAT", 65d},
                    {"IBM", 149d},
                });

            sendEvent(env, "KGB", 75);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields,
                new Object[][]{
                    {"CAT", 65d},
                    {"IBM", 149d},
                    {"KGB", 75d},
                });

            env.undeployAll();
        }
    }

    private static class ResultSetOrderByLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select last(intPrimitive) as c0, theString as c1  " +
                "from SupportBean#length_batch(5) group by theString order by last(intPrimitive) desc";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 11));
            env.sendEventBean(new SupportBean("E3", 12));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 13));
            env.sendEventBean(new SupportBean("E1", 14));

            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), "c0,c1".split(","), new Object[][]{{14, "E1"}, {13, "E2"}, {12, "E3"}});

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void tryAssertionLast(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "symbol,mysum".split(",");

        sendEvent(env, "IBM", 3);
        sendEvent(env, "IBM", 4);
        sendEvent(env, "CMU", 1);

        env.milestoneInc(milestone);

        sendEvent(env, "CMU", 2);
        sendEvent(env, "CAT", 5);

        env.milestoneInc(milestone);

        sendEvent(env, "CAT", 6);

        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"CMU", 3.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields,
            new Object[][]{{"CAT", null}, {"CMU", null}, {"IBM", null}});

        sendEvent(env, "IBM", 3);
        sendEvent(env, "IBM", 4);
        sendEvent(env, "CMU", 5);
        sendEvent(env, "CMU", 5);
        sendEvent(env, "DOG", 0);

        env.milestoneInc(milestone);

        sendEvent(env, "DOG", 1);

        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"DOG", 1.0}, {"CMU", 13.0}, {"IBM", 14.0}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields,
            new Object[][]{{"DOG", null}, {"CMU", 3.0}, {"IBM", 7.0}});
    }

    private static void tryAssertionNoHaving(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "symbol,mysum".split(",");

        sendEvent(env, "IBM", 3);
        sendEvent(env, "IBM", 4);

        env.milestoneInc(milestone);

        sendEvent(env, "CMU", 1);
        sendEvent(env, "CMU", 2);
        sendEvent(env, "CAT", 5);
        sendEvent(env, "CAT", 6);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"CMU", 1.0}, {"CMU", 3.0}, {"IBM", 3.0}, {"CAT", 5.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields,
            new Object[][]{{"CAT", null}, {"CMU", null}, {"IBM", null}, {"CMU", 1.0}, {"IBM", 3.0}, {"CAT", 5.0}});
        env.listener("s0").reset();

        env.milestoneInc(milestone);

        sendEvent(env, "IBM", 3);
        sendEvent(env, "IBM", 4);
        sendEvent(env, "CMU", 5);
        sendEvent(env, "CMU", 5);
        sendEvent(env, "DOG", 0);

        env.milestoneInc(milestone);

        sendEvent(env, "DOG", 1);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"DOG", 0.0}, {"DOG", 1.0}, {"CMU", 8.0}, {"IBM", 10.0}, {"CMU", 13.0}, {"IBM", 14.0}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields,
            new Object[][]{{"DOG", null}, {"DOG", 0.0}, {"CMU", 3.0}, {"IBM", 7.0}, {"CMU", 8.0}, {"IBM", 10.0}});
    }

    private static void tryAssertionHaving(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "symbol,mysum".split(",");

        sendEvent(env, "IBM", 3);
        sendEvent(env, "IBM", 4);
        sendEvent(env, "CMU", 1);
        sendEvent(env, "CMU", 2);
        sendEvent(env, "CAT", 5);

        env.milestoneInc(milestone);

        sendEvent(env, "CAT", 6);

        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"CMU", 1.0}, {"CMU", 3.0}, {"IBM", 3.0}, {"CAT", 5.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields,
            new Object[][]{{"CMU", 1.0}, {"IBM", 3.0}, {"CAT", 5.0}});
        env.listener("s0").reset();

        sendEvent(env, "IBM", 3);

        env.milestoneInc(milestone);

        sendEvent(env, "IBM", 4);
        sendEvent(env, "CMU", 5);
        sendEvent(env, "CMU", 5);
        sendEvent(env, "DOG", 0);

        env.milestoneInc(milestone);

        sendEvent(env, "DOG", 1);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"DOG", 1.0}, {"CMU", 8.0}, {"IBM", 10.0}, {"CMU", 13.0}, {"IBM", 14.0}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields,
            new Object[][]{{"CMU", 3.0}, {"IBM", 7.0}, {"CMU", 8.0}, {"IBM", 10.0}});
    }
}
