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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertProps;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;


public class ResultSetAggregateRate {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateRateDataNonWindowed());
        execs.add(new ResultSetAggregateRateDataWindowed());
        return execs;
    }

    // rate implementation does not require a data window (may have one)
    // advantage: not retaining events, only timestamp data points
    // disadvantage: output rate limiting without snapshot may be less accurate rate
    private static class ResultSetAggregateRateDataNonWindowed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') select rate(10) as myrate from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            tryAssertion(env, milestone);

            env.undeployAll();

            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertion(env, milestone);

            env.undeployAll();

            tryInvalidCompile(env, "select rate() from SupportBean",
                "Failed to validate select-clause expression 'rate(*)': The rate aggregation function minimally requires a numeric constant or expression as a parameter. [select rate() from SupportBean]");
            tryInvalidCompile(env, "select rate(true) from SupportBean",
                "Failed to validate select-clause expression 'rate(true)': The rate aggregation function requires a numeric constant or time period as the first parameter in the constant-value notation [select rate(true) from SupportBean]");
        }
    }

    private static class ResultSetAggregateRateDataWindowed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "myrate,myqtyrate".split(",");
            String epl = "@name('s0') select RATE(longPrimitive) as myrate, RATE(longPrimitive, intPrimitive) as myqtyrate from SupportBean#length(3)";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, 1000, 10);
            assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.milestone(0);

            sendEvent(env, 1200, 0);
            assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            sendEvent(env, 1300, 0);
            assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.milestone(1);

            sendEvent(env, 1500, 14);
            assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3 * 1000 / 500d, 14 * 1000 / 500d});

            env.milestone(2);

            sendEvent(env, 2000, 11);
            assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3 * 1000 / 800d, 25 * 1000 / 800d});

            tryInvalidCompile(env, "select rate(longPrimitive) as myrate from SupportBean",
                "Failed to validate select-clause expression 'rate(longPrimitive)': The rate aggregation function in the timestamp-property notation requires data windows [select rate(longPrimitive) as myrate from SupportBean]");
            tryInvalidCompile(env, "select rate(current_timestamp) as myrate from SupportBean#time(20)",
                "Failed to validate select-clause expression 'rate(current_timestamp())': The rate aggregation function does not allow the current runtime timestamp as a parameter [select rate(current_timestamp) as myrate from SupportBean#time(20)]");
            tryInvalidCompile(env, "select rate(theString) as myrate from SupportBean#time(20)",
                "Failed to validate select-clause expression 'rate(theString)': The rate aggregation function requires a property or expression returning a non-constant long-type value as the first parameter in the timestamp-property notation [select rate(theString) as myrate from SupportBean#time(20)]");

            env.undeployAll();
        }
    }

    private static void tryAssertion(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "myrate".split(",");

        sendTimer(env, 1000);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

        env.milestoneInc(milestone);

        sendTimer(env, 1200);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(env, 1600);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

        env.milestoneInc(milestone);

        sendTimer(env, 1600);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(env, 9000);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(env, 9200);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

        env.milestoneInc(milestone);

        sendTimer(env, 10999);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

        env.milestoneInc(milestone);

        sendTimer(env, 11100);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0.7});

        sendTimer(env, 11101);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0.8});

        env.milestoneInc(milestone);

        sendTimer(env, 11200);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0.8});

        sendTimer(env, 11600);
        sendEvent(env);
        assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0.7});
    }

    /**
     * Comment-in for rate testing with threading
     */
    /*
    public void testRateThreaded() throws Exception {

        Configuration config = new Configuration();
        config.addEventType("SupportBean", SupportBean.class);
        runtime = EPServiceProviderManager.getDefaultRuntime(config);
        runtime.initialize();

        RateSendRunnable runnable = new RateSendRunnable(runtime.eventService());
        ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);

        //String viewExpr = "select RATE(longPrimitive) as myrate from SupportBean#time(10) output every 1 sec";
        String viewExpr = "select RATE(10) as myrate from SupportBean output snapshot every 1 sec";
        EPStatement stmt = runtime.getDeploymentService().createEPL(viewExpr);
        env.statement("s0").addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                System.out.println(newEvents[0].get("myrate"));
            }
        });

        long rateDelay = 133;   // <== change here
        ScheduledFuture<?> future = timer.scheduleAtFixedRate(runnable, 0, rateDelay, TimeUnit.MILLISECONDS);
        Thread.sleep(2 * 60 * 1000);
        future.cancel(true);
    }
    */
    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void sendEvent(RegressionEnvironment env, long longPrimitive, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setLongPrimitive(longPrimitive);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env) {
        SupportBean bean = new SupportBean();
        env.sendEventBean(bean);
    }

    public static class RateSendRunnable extends TimerTask {

        private final RegressionEnvironment env;

        public RateSendRunnable(RegressionEnvironment env) {
            this.env = env;
        }

        public void run() {
            SupportBean bean = new SupportBean();
            bean.setLongPrimitive(System.currentTimeMillis());
            env.sendEventBean(bean);
        }
    }
}
