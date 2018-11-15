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
package com.espertech.esper.regressionlib.suite.multithread;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

public class MultithreadContextPartitionedWTerm implements RegressionExecutionWithConfigure {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void configure(Configuration configuration) {
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
        configuration.getRuntime().getExecution().setPrioritized(true);
    }

    public void run(RegressionEnvironment env) {
        env.advanceTime(0);
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create context MyCtx partition by theString from SupportBean terminated after 1 second", path);
        env.compileDeploy("@name('s0') context MyCtx select count(*) as cnt from SupportBean output last when terminated", path);
        SupportUpdateListener listener = new SupportUpdateListener();
        env.statement("s0").addListener(listener);

        AtomicBoolean latch = new AtomicBoolean(true);
        // With 0-sleep or 1-sleep the counts start to drop because the event is chasing the context partition.
        Thread t = new Thread(new MyTimeAdvancingRunnable(env, latch, 10, -1), MultithreadContextPartitionedWTerm.class.getSimpleName());
        t.start();

        int numEvents = 10000;
        for (int i = 0; i < numEvents; i++) {
            env.sendEventBean(new SupportBean());
        }
        latch.set(false);
        SupportCompileDeployUtil.threadJoin(t);
        env.advanceTime(Integer.MAX_VALUE);

        long total = 0;
        EventBean[] deliveries = listener.getNewDataListFlattened();
        for (EventBean event : deliveries) {
            long count = (Long) event.get("cnt");
            total += count;
        }
        assertEquals(numEvents, total);

        env.undeployAll();
    }

    public static class MyTimeAdvancingRunnable implements Runnable {
        private final RegressionEnvironment env;
        private final AtomicBoolean latch;
        private final long threadSleepTime;
        private final long maxNumAdvances;

        public MyTimeAdvancingRunnable(RegressionEnvironment env, AtomicBoolean latch, long threadSleepTime, long maxNumAdvances) {
            this.env = env;
            this.latch = latch;
            this.threadSleepTime = threadSleepTime;
            this.maxNumAdvances = maxNumAdvances;
        }

        public void run() {
            long time = 1000;
            long numAdvances = 0;
            try {
                while (latch.get() && (maxNumAdvances == -1 || numAdvances < maxNumAdvances)) {
                    env.advanceTime(time);
                    numAdvances++;
                    time += 1000;
                    SupportCompileDeployUtil.threadSleep(threadSleepTime);
                }
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
    }
}
