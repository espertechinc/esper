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
package com.espertech.esper.regressionlib.suite.resultset.aggregate;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class ResultSetAggregateMinMax {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateMinMaxNoDataWindowSubquery());
        execs.add(new ResultSetAggregateMemoryMinHaving());
        execs.add(new ResultSetAggregateMinMaxNamedWindowWEver(false));
        execs.add(new ResultSetAggregateMinMaxNamedWindowWEver(true));
        return execs;
    }

    private static class ResultSetAggregateMinMaxNamedWindowWEver implements RegressionExecution {
        private final boolean soda;

        public ResultSetAggregateMinMaxNamedWindowWEver(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "lower,upper,lowerever,upperever".split(",");
            RegressionPath path = new RegressionPath();

            String epl = "create window NamedWindow5m#length(2) as select * from SupportBean;\n" +
                "insert into NamedWindow5m select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            epl = "@name('s0') select " +
                "min(intPrimitive) as lower, " +
                "max(intPrimitive) as upper, " +
                "minever(intPrimitive) as lowerever, " +
                "maxever(intPrimitive) as upperever from NamedWindow5m";
            env.compileDeploy(soda, epl, path).addListener("s0");

            env.sendEventBean(new SupportBean(null, 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 1, 1, 1});

            env.milestone(0);

            env.sendEventBean(new SupportBean(null, 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 5, 1, 5});

            env.milestone(1);

            env.sendEventBean(new SupportBean(null, 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 5, 1, 5});

            env.sendEventBean(new SupportBean(null, 6));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 6, 1, 6});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMinMaxNoDataWindowSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "maxi,mini,max0,min0".split(",");
            String epl = "@name('s0') select max(intPrimitive) as maxi, min(intPrimitive) as mini," +
                "(select max(id) from SupportBean_S0#lastevent) as max0, (select min(id) from SupportBean_S0#lastevent) as min0" +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 3, null, null});

            env.sendEventBean(new SupportBean("E2", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4, 3, null, null});

            env.sendEventBean(new SupportBean_S0(2));
            env.sendEventBean(new SupportBean("E3", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4, 3, 2, 2});

            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean("E4", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, 3, 1, 1});

            env.undeployAll();
            /**
             * Comment out here for sending many more events.
             *
             for (int i = 0; i < 10000000; i++) {
             env.sendEventBean(new SupportBean(null, i));
             if (i % 10000 == 0) {
             System.out.println("Sent " + i + " events");
             }
             }
             */
        }
    }

    private static class ResultSetAggregateMemoryMinHaving implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select price, min(price) as minPrice " +
                "from SupportMarketDataBean#time(30)" +
                "having price >= min(price) * (1.02)";

            env.compileDeploy(statementText).addListener("s0");


            Random random = new Random();
            // Change to perform a long-running tests, each loop is 1 second
            final int loopcount = 2;
            int loopCount = 0;

            while (true) {
                log.info("Sending batch " + loopCount);

                // send events
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < 5000; i++) {
                    double price = 50 + 49 * random.nextInt(100) / 100.0;
                    sendEvent(env, price);
                }
                long endTime = System.currentTimeMillis();

                // sleep remainder of 1 second
                long delta = startTime - endTime;
                if (delta < 950) {
                    try {
                        Thread.sleep(950 - delta);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                env.listener("s0").reset();
                loopCount++;
                if (loopCount > loopcount) {
                    break;
                }
            }

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean("DELL", price, -1L, null);
        env.sendEventBean(bean);
    }

    private final static Logger log = LoggerFactory.getLogger(ResultSetAggregateMinMax.class);
}
