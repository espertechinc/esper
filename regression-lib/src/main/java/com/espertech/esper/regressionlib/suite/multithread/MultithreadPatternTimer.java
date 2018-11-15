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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.support.bean.SupportByteArrEventLongId;
import com.espertech.esper.regressionlib.support.util.SupportCountListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MultithreadPatternTimer implements RegressionExecutionWithConfigure {
    private final static Logger log = LoggerFactory.getLogger(MultithreadPatternTimer.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void configure(Configuration configuration) {
        configuration.getCommon().addEventType(SupportByteArrEventLongId.class);
        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
    }

    public boolean haWithCOnly() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        // configure
        int numThreads = 2;
        int numStatements = 100;
        int numEvents = 50000;

        // set up threading
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        ThreadPoolExecutor threadpool = new ThreadPoolExecutor(numThreads, numThreads, 1000, TimeUnit.SECONDS, queue, new SupportThreadFactory(MultithreadPatternTimer.class));
        threadpool.prestartAllCoreThreads();

        // create statements
        log.info("Creating statements");
        for (int i = 0; i < numStatements; i++) {
            String statementName = "s" + i;
            String stmtText = String.format("@name('s%d')select * from pattern [ every e1=SupportByteArrEventLongId(id=%d) -> timer:interval(1 seconds)]", i, i);
            env.compileDeploy(stmtText);
            env.statement(statementName).addListener(new SupportCountListener());
        }

        // submit events
        long startTime = System.currentTimeMillis();
        log.info("Submitting " + numEvents + " events to queue");
        Random random = new Random(new Date().getTime());
        for (int i = 0; i < numEvents; i++) {
            final SupportByteArrEventLongId event = new SupportByteArrEventLongId(random.nextInt(numStatements), 0);
            Runnable sendEventRunnable = new Runnable() {
                public void run() {
                    env.sendEventBean(event);
                }
            };
            threadpool.submit(sendEventRunnable);
        }

        log.info("Waiting for completion");
        while (!queue.isEmpty()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                fail();
            }
            log.info("Queue size is " + queue.size());
        }
        long endTime = System.currentTimeMillis();
        log.info("Time to complete: " + (endTime - startTime) / 1000 + " sec");

        // wait for completion
        log.info("Waiting for remaining callbacks");
        long startWaitTime = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }
            int countTotal = getCount(env, numStatements);
            if (countTotal >= numEvents) {
                break;
            }
            if (System.currentTimeMillis() - startWaitTime > 20000) {
                fail();
            }
            log.info("Waiting for remaining callbacks: " + countTotal + " of " + numEvents);
        }

        // assert
        int total = getCount(env, numStatements);
        assertEquals(numEvents, total);

        env.undeployAll();
    }

    private int getCount(RegressionEnvironment env, int numStatements) {
        int total = 0;
        for (int i = 0; i < numStatements; i++) {
            SupportCountListener listener = (SupportCountListener) env.statement("s" + Integer.toString(i)).getUpdateListeners().next();
            total += listener.getCountNew();
        }
        return total;
    }
}
