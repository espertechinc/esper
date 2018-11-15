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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MultithreadContextPartitionedWCount implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(MultithreadContextPartitionedWCount.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        String[] choices = "A,B,C,D".split(",");
        trySend(env, 4, 1000, choices);
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numEvents, String[] choices) {
        if (numEvents < choices.length) {
            throw new IllegalArgumentException("Number of events must at least match number of choices");
        }

        env.advanceTime(0);
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@Name('var') create variable boolean myvar = false", path);
        env.compileDeploy("create context SegmentedByString as partition by theString from SupportBean", path);
        env.compileDeploy("@name('s0') context SegmentedByString select theString, count(*) - 1 as cnt from SupportBean output snapshot when myvar = true", path);
        SupportUpdateListener listener = new SupportUpdateListener();
        env.statement("s0").addListener(listener);

        // preload - since concurrently sending same-category events an event can be dropped
        for (int i = 0; i < choices.length; i++) {
            env.sendEventBean(new SupportBean(choices[i], 0));
        }

        EventRunnable[] runnables = new EventRunnable[numThreads];
        for (int i = 0; i < runnables.length; i++) {
            runnables[i] = new EventRunnable(env, numEvents, choices);
        }

        // start
        Thread[] threads = new Thread[runnables.length];
        for (int i = 0; i < runnables.length; i++) {
            threads[i] = new Thread(runnables[i], MultithreadContextPartitionedWCount.class.getSimpleName());
            threads[i].start();
        }

        // join
        log.info("Waiting for completion");
        for (int i = 0; i < runnables.length; i++) {
            SupportCompileDeployUtil.threadJoin(threads[i]);
        }

        Map<String, Long> totals = new HashMap<String, Long>();
        for (String choice : choices) {
            totals.put(choice, 0L);
        }

        // verify
        int sum = 0;
        for (int i = 0; i < runnables.length; i++) {
            assertNull(runnables[i].getException());
            for (Map.Entry<String, Integer> entry : runnables[i].getTotals().entrySet()) {
                Long current = totals.get(entry.getKey());
                current += entry.getValue();
                sum += entry.getValue();
                totals.put(entry.getKey(), current);
                //System.out.println("Thread " + i + " key " + entry.getKey() + " count " + entry.getValue());
            }
        }

        assertEquals(numThreads * numEvents, sum);

        env.runtime().getVariableService().setVariableValue(env.deploymentId("var"), "myvar", true);
        env.advanceTime(10000);
        EventBean[] result = listener.getLastNewData();
        assertEquals(choices.length, result.length);
        for (EventBean item : result) {
            String theString = (String) item.get("theString");
            Long count = (Long) item.get("cnt");
            //System.out.println("String " + string + " count " + count);
            assertEquals(count, totals.get(theString));
        }

        env.undeployAll();
    }

    public static class EventRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final int numEvents;
        private final String[] choices;
        private final Map<String, Integer> totals = new HashMap<String, Integer>();

        private RuntimeException exception;

        public EventRunnable(RegressionEnvironment env, int numEvents, String[] choices) {
            this.env = env;
            this.numEvents = numEvents;
            this.choices = choices;
        }

        public void run() {
            log.info("Started event send");

            try {
                for (int i = 0; i < numEvents; i++) {
                    String chosen = choices[i % choices.length];
                    env.sendEventBean(new SupportBean(chosen, 1));

                    Integer current = totals.get(chosen);
                    if (current == null) {
                        current = 0;
                    }
                    current += 1;
                    totals.put(chosen, current);
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send");
        }

        public RuntimeException getException() {
            return exception;
        }

        public Map<String, Integer> getTotals() {
            return totals;
        }
    }
}
