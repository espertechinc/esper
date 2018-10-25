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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.Locking;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.multithread.GeneratorIterator;
import com.espertech.esper.regressionlib.support.multithread.SendEventRWLockCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.*;

/**
 * Test for multithread-safety and deterministic behavior when using insert-into.
 */
public class MultithreadDeterminismInsertIntoLockConfig {
    private static final Logger log = LoggerFactory.getLogger(MultithreadDeterminismInsertIntoLockConfig.class);

    public void run(Configuration configuration) {
        trySendCountFollowedBy(4, 100, Locking.SUSPEND, configuration);
        trySendCountFollowedBy(4, 100, Locking.SPIN, configuration);
    }

    private static void trySendCountFollowedBy(int numThreads, int numEvents, Locking locking, Configuration configuration) {
        configuration.getRuntime().getThreading().setInsertIntoDispatchLocking(locking);
        configuration.getRuntime().getThreading().setInsertIntoDispatchTimeout(5000); // 5 second timeout
        configuration.getCommon().addEventType(SupportBean.class);

        // This should fail all test in this class
        // config.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);
        EPRuntime runtime = EPRuntimeProvider.getRuntime(MultithreadDeterminismInsertIntoLockConfig.class.getSimpleName(), configuration);
        runtime.initialize();

        // setup statements
        RegressionPath path = new RegressionPath();
        String eplInsert = "insert into MyStream select count(*) as cnt from SupportBean";
        EPCompiled compiledInsert = compile(eplInsert, configuration, path);
        path.add(compiledInsert);
        EPDeployment deployedInsert = deploy(compiledInsert, runtime);
        deployedInsert.getStatements()[0].addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                log.debug(".update cnt=" + newEvents[0].get("cnt"));
            }
        });

        SupportUpdateListener[] listeners = new SupportUpdateListener[numEvents];
        for (int i = 0; i < numEvents; i++) {
            String text = "select * from pattern [MyStream(cnt=" + (i + 1) + ") -> MyStream(cnt=" + (i + 2) + ")]";
            EPCompiled compiled = compile(text, configuration, path);
            EPDeployment deployedPattern = deploy(compiled, runtime);
            listeners[i] = new SupportUpdateListener();
            deployedPattern.getStatements()[0].addListener(listeners[i]);
        }

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadDeterminismInsertIntoLockConfig.class));
        Future[] future = new Future[numThreads];
        ReentrantReadWriteLock sharedStartLock = new ReentrantReadWriteLock();
        sharedStartLock.writeLock().lock();
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventRWLockCallable(i, sharedStartLock, runtime, new GeneratorIterator(numEvents)));
        }
        threadSleep(100);
        sharedStartLock.writeLock().unlock();

        threadPool.shutdown();
        threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        assertFutures(future);

        // assert result
        for (int i = 0; i < numEvents - 1; i++) {
            Assert.assertEquals("Listener not invoked: #" + i, 1, listeners[i].getNewDataList().size());
        }

        runtime.destroy();
    }
}
