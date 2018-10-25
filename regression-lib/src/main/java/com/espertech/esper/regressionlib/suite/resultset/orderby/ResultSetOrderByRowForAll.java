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
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class ResultSetOrderByRowForAll {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetNoOutputRateJoin());
        execs.add(new ResultSetOutputDefault(false));
        execs.add(new ResultSetOutputDefault(true));
        return execs;
    }

    private static class ResultSetOutputDefault implements RegressionExecution {
        private final boolean join;

        public ResultSetOutputDefault(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream sum(intPrimitive) as c0, last(theString) as c1 from SupportBean#length(2) " +
                (join ? ",SupportBean_A#keepall " : "") +
                "output every 3 events order by sum(intPrimitive) desc";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 11));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E3", 12));

            String[] fields = "c0,c1".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{23, "E3"}, {21, "E2"}, {10, "E1"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields, new Object[][]{{21, "E2"}, {10, "E1"}, {null, null}});

            env.undeployAll();
        }
    }

    private static class ResultSetNoOutputRateJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"sumPrice"};
            String epl = "@name('s0')select sum(price) as sumPrice from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "order by price";
            env.compileDeploy(epl).addListener("s0");

            sendJoinEvents(env);
            sendEvent(env, "CAT", 50);
            sendEvent(env, "IBM", 49);
            sendEvent(env, "CAT", 15);
            sendEvent(env, "IBM", 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{214d}});

            sendEvent(env, "KGB", 75);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{289d}});

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void sendJoinEvents(RegressionEnvironment env) {
        env.sendEventBean(new SupportBeanString("CAT"));
        env.sendEventBean(new SupportBeanString("IBM"));
        env.sendEventBean(new SupportBeanString("CMU"));
        env.sendEventBean(new SupportBeanString("KGB"));
        env.sendEventBean(new SupportBeanString("DOG"));
    }
}
