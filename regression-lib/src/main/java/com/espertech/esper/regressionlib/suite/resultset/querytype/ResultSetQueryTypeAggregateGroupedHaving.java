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
package com.espertech.esper.regressionlib.suite.resultset.querytype;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class ResultSetQueryTypeAggregateGroupedHaving {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeGroupByHaving(false));
        execs.add(new ResultSetQueryTypeGroupByHaving(true));
        execs.add(new ResultSetQueryTypeSumOneView());
        execs.add(new ResultSetQueryTypeSumJoin());
        return execs;
    }

    private static class ResultSetQueryTypeGroupByHaving implements RegressionExecution {
        private final boolean join;

        public ResultSetQueryTypeGroupByHaving(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String epl = !join ?
                "@name('s0') select * from SupportBean#length_batch(3) group by theString having count(*) > 1" :
                "@name('s0') select theString, intPrimitive from SupportBean_S0#lastevent, SupportBean#length_batch(3) group by theString having count(*) > 1";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean("E1", 10));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean("E2", 21));

            EventBean[] received = env.listener("s0").getNewDataListFlattened();
            EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive".split(","),
                new Object[][]{{"E2", 20}, {"E2", 21}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSumOneView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select irstream symbol, volume, sum(price) as mySum " +
                "from SupportMarketDataBean#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "having sum(price) >= 50";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionSum(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSumJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select irstream symbol, volume, sum(price) as mySum " +
                "from SupportBeanString#length(100) as one, " +
                "SupportMarketDataBean#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol " +
                "having sum(price) >= 50";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
            env.sendEventBean(new SupportBeanString(SYMBOL_IBM));

            tryAssertionSum(env);

            env.undeployAll();
        }
    }

    private static void tryAssertionSum(RegressionEnvironment env) {
        // assert select result type
        Assert.assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("symbol"));
        Assert.assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("volume"));
        Assert.assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("mySum"));

        String[] fields = "symbol,volume,mySum".split(",");
        sendEvent(env, SYMBOL_DELL, 10000, 49);
        assertFalse(env.listener("s0").isInvoked());

        sendEvent(env, SYMBOL_DELL, 20000, 54);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{SYMBOL_DELL, 20000L, 103d});

        sendEvent(env, SYMBOL_IBM, 1000, 10);
        assertFalse(env.listener("s0").isInvoked());

        sendEvent(env, SYMBOL_IBM, 5000, 20);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{SYMBOL_DELL, 10000L, 54d});

        sendEvent(env, SYMBOL_IBM, 6000, 5);
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        env.sendEventBean(bean);
    }
}
