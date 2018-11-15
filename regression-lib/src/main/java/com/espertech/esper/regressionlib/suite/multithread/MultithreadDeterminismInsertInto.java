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
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.GeneratorIterator;
import com.espertech.esper.regressionlib.support.multithread.SendEventRWLockCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety and deterministic behavior when using insert-into and also listener-dispatch.
 */
public class MultithreadDeterminismInsertInto implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(MultithreadDeterminismInsertInto.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        tryChainedCountSum(env, 3, 100);
        tryMultiInsertGroup(env, 3, 10, 100);
    }

    private static void tryMultiInsertGroup(RegressionEnvironment env, int numThreads, int numStatements, int numEvents) {
        // This should fail all test in this class
        // config.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);

        // setup statements
        RegressionPath path = new RegressionPath();
        EPStatement[] insertIntoStmts = new EPStatement[numStatements];
        for (int i = 0; i < numStatements; i++) {
            String epl = "@name('s" + i + "') insert into MyStream select " + i + " as ident,count(*) as cnt from SupportBean";
            insertIntoStmts[i] = env.compileDeploy(epl, path).statement("s" + i);
        }
        env.compileDeploy("@name('final') select ident, sum(cnt) as mysum from MyStream group by ident", path);
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        env.statement("final").addListener(listener);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadDeterminismInsertInto.class));
        Future[] future = new Future[numThreads];
        ReentrantReadWriteLock sharedStartLock = new ReentrantReadWriteLock();
        sharedStartLock.writeLock().lock();
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventRWLockCallable(i, sharedStartLock, env.runtime(), new GeneratorIterator(numEvents)));
        }
        SupportCompileDeployUtil.threadSleep(100);
        sharedStartLock.writeLock().unlock();

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);

        SupportCompileDeployUtil.assertFutures(future);

        // assert result
        EventBean[] newEvents = listener.getNewDataListFlattened();
        ArrayList[] resultsPerIdent = new ArrayList[numStatements];
        for (EventBean theEvent : newEvents) {
            int ident = (Integer) theEvent.get("ident");
            if (resultsPerIdent[ident] == null) {
                resultsPerIdent[ident] = new ArrayList();
            }
            long mysum = (Long) theEvent.get("mysum");
            resultsPerIdent[ident].add(mysum);
        }

        for (int statement = 0; statement < numStatements; statement++) {
            for (int i = 0; i < numEvents - 1; i++) {
                long expected = total(i + 1);
                assertEquals("Failed for statement " + statement, expected, resultsPerIdent[statement].get(i));
            }
        }

        env.undeployAll();
    }

    private static void tryChainedCountSum(RegressionEnvironment env, int numThreads, int numEvents) {
        // setup statements
        RegressionPath path = new RegressionPath();
        env.compileDeploy("insert into MyStreamOne select count(*) as cnt from SupportBean", path);
        env.compileDeploy("@name('s0') insert into MyStreamTwo select sum(cnt) as mysum from MyStreamOne", path);
        SupportUpdateListener listener = new SupportUpdateListener();
        env.statement("s0").addListener(listener);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadDeterminismInsertInto.class));
        Future[] future = new Future[numThreads];
        ReentrantReadWriteLock sharedStartLock = new ReentrantReadWriteLock();
        sharedStartLock.writeLock().lock();
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventRWLockCallable(i, sharedStartLock, env.runtime(), new GeneratorIterator(numEvents)));
        }
        SupportCompileDeployUtil.threadSleep(100);
        sharedStartLock.writeLock().unlock();

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        // assert result
        EventBean[] newEvents = listener.getNewDataListFlattened();
        for (int i = 0; i < numEvents - 1; i++) {
            long expected = total(i + 1);
            Assert.assertEquals(expected, newEvents[i].get("mysum"));
        }

        env.undeployAll();
    }

    private static long total(int num) {
        long total = 0;
        for (int i = 1; i < num + 1; i++) {
            total += i;
        }
        return total;
    }
}
