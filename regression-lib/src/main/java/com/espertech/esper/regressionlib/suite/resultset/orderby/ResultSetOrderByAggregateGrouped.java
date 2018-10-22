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
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertNull;

public class ResultSetOrderByAggregateGrouped {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAliasesAggregationCompile());
        execs.add(new ResultSetAliasesAggregationOM());
        execs.add(new ResultSetAliases());
        execs.add(new ResultSetGroupBySwitch());
        execs.add(new ResultSetGroupBySwitchJoin());
        execs.add(new ResultSetLastJoin());
        execs.add(new ResultSetIterator());
        execs.add(new ResultSetLast());
        return execs;
    }

    private static class ResultSetAliasesAggregationCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, volume, sum(price) as mySum from " +
                "SupportMarketDataBean#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol";
            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertionDefault(env);

            env.undeployAll();
        }
    }

    private static class ResultSetAliasesAggregationOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("symbol", "volume").add(Expressions.sum("price"), "mySum"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getSimpleName()).addView(View.create("length", Expressions.constant(20)))));
            model.setGroupByClause(GroupByClause.create("symbol"));
            model.setOutputLimitClause(OutputLimitClause.create(6));
            model.setOrderByClause(OrderByClause.create(Expressions.sum("price")).add("symbol", false));
            model = SerializableObjectCopier.copyMayFail(model);

            String epl = "select symbol, volume, sum(price) as mySum from " +
                "SupportMarketDataBean#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol";
            Assert.assertEquals(epl, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            tryAssertionDefault(env);

            env.undeployAll();
        }
    }

    private static class ResultSetAliases implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, volume, sum(price) as mySum from " +
                "SupportMarketDataBean#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by mySum, symbol";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionDefault(env);

            env.undeployAll();
        }
    }

    private static class ResultSetGroupBySwitch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Instead of the row-per-group behavior, these should
            // get row-per-event behavior since there are properties
            // in the order-by that are not in the select expression.
            String epl = "@name('s0') select symbol, sum(price) from " +
                "SupportMarketDataBean#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol, volume";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionDefaultNoVolume(env);

            env.undeployAll();
        }
    }

    private static class ResultSetGroupBySwitchJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('s0') select symbol, sum(price) from " +
                    "SupportMarketDataBean#length(20) as one, " +
                    "SupportBeanString#length(100) as two " +
                    "where one.symbol = two.theString " +
                    "group by symbol " +
                    "output every 6 events " +
                    "order by sum(price), symbol, volume";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("CMU"));
            env.sendEventBean(new SupportBeanString("KGB"));
            env.sendEventBean(new SupportBeanString("DOG"));

            tryAssertionDefaultNoVolume(env);

            env.undeployAll();
        }
    }

    private static class ResultSetLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, volume, sum(price) from " +
                "SupportMarketDataBean#length(20) " +
                "group by symbol " +
                "output last every 6 events " +
                "order by sum(price)";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionLast(env);

            env.undeployAll();
        }
    }

    private static class ResultSetLastJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, volume, sum(price) from " +
                "SupportMarketDataBean#length(20) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "output last every 6 events " +
                "order by sum(price)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));
            env.sendEventBean(new SupportBeanString("CMU"));
            env.sendEventBean(new SupportBeanString("KGB"));
            env.sendEventBean(new SupportBeanString("DOG"));

            tryAssertionLast(env);

            env.undeployAll();
        }
    }

    private static class ResultSetIterator implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "theString", "sumPrice"};
            String epl = "@name('s0') select symbol, theString, sum(price) as sumPrice from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "order by symbol";
            env.compileDeploy(epl).addListener("s0");
            sendJoinEvents(env);
            sendEvent(env, "CAT", 50);
            sendEvent(env, "IBM", 49);
            sendEvent(env, "CAT", 15);
            sendEvent(env, "IBM", 100);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{
                    {"CAT", "CAT", 65d},
                    {"CAT", "CAT", 65d},
                    {"IBM", "IBM", 149d},
                    {"IBM", "IBM", 149d},
                });

            sendEvent(env, "KGB", 75);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{
                    {"CAT", "CAT", 65d},
                    {"CAT", "CAT", 65d},
                    {"IBM", "IBM", 149d},
                    {"IBM", "IBM", 149d},
                    {"KGB", "KGB", 75d},
                });

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        env.sendEventBean(bean);
    }

    private static void sendJoinEvents(RegressionEnvironment env) {
        env.sendEventBean(new SupportBeanString("CAT"));
        env.sendEventBean(new SupportBeanString("IBM"));
        env.sendEventBean(new SupportBeanString("CMU"));
        env.sendEventBean(new SupportBeanString("KGB"));
        env.sendEventBean(new SupportBeanString("DOG"));
    }

    private static void tryAssertionDefault(RegressionEnvironment env) {
        sendEvent(env, "IBM", 110, 3);

        env.milestone(0);

        sendEvent(env, "IBM", 120, 4);
        sendEvent(env, "CMU", 130, 1);

        env.milestone(1);

        sendEvent(env, "CMU", 140, 2);
        sendEvent(env, "CAT", 150, 5);

        env.milestone(2);

        sendEvent(env, "CAT", 160, 6);

        String[] fields = "symbol,volume,mySum".split(",");
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"CMU", 130L, 1.0}, {"CMU", 140L, 3.0}, {"IBM", 110L, 3.0},
                {"CAT", 150L, 5.0}, {"IBM", 120L, 7.0}, {"CAT", 160L, 11.0}});
        assertNull(env.listener("s0").getLastOldData());
    }

    private static void tryAssertionDefaultNoVolume(RegressionEnvironment env) {
        sendEvent(env, "IBM", 110, 3);
        sendEvent(env, "IBM", 120, 4);
        sendEvent(env, "CMU", 130, 1);

        env.milestone(0);

        sendEvent(env, "CMU", 140, 2);
        sendEvent(env, "CAT", 150, 5);

        env.milestone(1);

        sendEvent(env, "CAT", 160, 6);

        String[] fields = "symbol,sum(price)".split(",");
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"CMU", 1.0}, {"CMU", 3.0}, {"IBM", 3.0},
                {"CAT", 5.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        assertNull(env.listener("s0").getLastOldData());
    }

    private static void tryAssertionLast(RegressionEnvironment env) {
        sendEvent(env, "IBM", 101, 3);
        sendEvent(env, "IBM", 102, 4);

        env.milestone(0);

        sendEvent(env, "CMU", 103, 1);
        sendEvent(env, "CMU", 104, 2);

        env.milestone(1);

        sendEvent(env, "CAT", 105, 5);
        sendEvent(env, "CAT", 106, 6);

        String[] fields = "symbol,volume,sum(price)".split(",");
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"CMU", 104L, 3.0}, {"IBM", 102L, 7.0}, {"CAT", 106L, 11.0}});
        assertNull(env.listener("s0").getLastOldData());

        sendEvent(env, "IBM", 201, 3);
        sendEvent(env, "IBM", 202, 4);

        env.milestone(2);

        sendEvent(env, "CMU", 203, 5);
        sendEvent(env, "CMU", 204, 5);
        sendEvent(env, "DOG", 205, 0);
        sendEvent(env, "DOG", 206, 1);

        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"DOG", 206L, 1.0}, {"CMU", 204L, 13.0}, {"IBM", 202L, 14.0}});
        assertNull(env.listener("s0").getLastOldData());
    }
}
