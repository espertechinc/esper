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
import com.espertech.esper.regressionlib.support.multithread.GeneratorIterator;
import com.espertech.esper.regressionlib.support.multithread.SendEventCallable;
import com.espertech.esper.regressionlib.support.util.SupportCountListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of a time window -based statement.
 */
public class MultithreadViewTimeWindow implements RegressionExecution {
    private final static Logger log = LoggerFactory.getLogger(MultithreadViewTimeWindow.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        int numThreads = 2;
        int numEvents = 10000;
        int numStmt = 25;

        log.info("Processing " + numEvents + " events for " + numThreads + " threads and " + numStmt + " statements");
        SupportCountListener[] listeners = new SupportCountListener[numStmt];
        for (int i = 0; i < numStmt; i++) {
            listeners[i] = new SupportCountListener();
            String stmtName = "stmt" + i;
            String nameAnnotation = "@name('" + stmtName + "')";
            String epl = nameAnnotation + "select irstream intPrimitive, theString as key from SupportBean#time(1 sec)";
            env.compileDeploy(epl).statement(stmtName).addListener(listeners[i]);
        }

        try {
            trySend(env, numThreads, numEvents, numStmt, listeners);
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage(), t);
        }

        env.undeployAll();
    }

    private void trySend(RegressionEnvironment env, int numThreads, int numRepeats, int numStmts, SupportCountListener[] listeners) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadViewTimeWindow.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new SendEventCallable(i, env.runtime(), new GeneratorIterator(numRepeats));
            future[i] = threadPool.submit(callable);
        }

        log.info("Waiting for threadpool shutdown");
        threadPool.shutdown();
        threadPool.awaitTermination(30, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        // set time to a large value
        log.info("Waiting for calm down");
        Thread.sleep(5000);

        // Assert results
        int totalExpected = numThreads * numRepeats;

        // assert new data
        for (int i = 0; i < numStmts; i++) {
            int count = listeners[i].getCountNew();
            assertEquals(count, totalExpected);
            int countOld = listeners[i].getCountNew();
            assertEquals(countOld, totalExpected);
        }
    }
}
