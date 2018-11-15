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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.wordexample.SentenceEvent;
import com.espertech.esper.runtime.client.EPRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.threadJoin;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static org.junit.Assert.assertNull;

public class MultithreadStmtStateless implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(MultithreadStmtStateless.class);

    public void run(RegressionEnvironment env) {
        trySend(env, 4, 1000);
    }

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numRepeats) {
        env.compileDeploy("@name('s0') select * from SentenceEvent[words]");
        assertStatelessStmt(env, "s0", true);

        StatelessRunnable[] runnables = new StatelessRunnable[numThreads];
        for (int i = 0; i < runnables.length; i++) {
            runnables[i] = new StatelessRunnable(env.runtime(), numRepeats);
        }

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < runnables.length; i++) {
            threads[i] = new Thread(runnables[i], MultithreadStmtStateless.class.getSimpleName());
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            threadJoin(t);
        }
        long delta = System.currentTimeMillis() - start;
        log.info("Delta=" + delta + " for " + numThreads * numRepeats + " events");

        for (StatelessRunnable r : runnables) {
            assertNull(r.getException());
        }
        env.undeployAll();
    }

    public static class StatelessRunnable implements Runnable {

        private final EPRuntime runtime;
        private final int numRepeats;

        private Throwable exception;

        public StatelessRunnable(EPRuntime runtime, int numRepeats) {
            this.runtime = runtime;
            this.numRepeats = numRepeats;
        }

        public void run() {
            try {
                for (int i = 0; i < numRepeats; i++) {
                    runtime.getEventService().sendEventBean(new SentenceEvent("This is stateless statement testing"), "SentenceEvent");

                    if (i % 10000 == 0) {
                        log.info("Thread " + Thread.currentThread().getId() + " sending event " + i);
                    }
                }
            } catch (Throwable t) {
                exception = t;
            }
        }

        public Throwable getException() {
            return exception;
        }
    }
}
