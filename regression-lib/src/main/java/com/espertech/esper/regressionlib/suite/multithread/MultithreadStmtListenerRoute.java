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
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtListenerCreateStmtCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for update listeners that route events.
 */
public class MultithreadStmtListenerRoute implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        tryListener(env, 4, 500);
    }

    private static void tryListener(RegressionEnvironment env, int numThreads, int numRoutes) {
        env.compileDeploy("@name('trigger') select * from SupportBean");
        env.compileDeploy("@name('s0') select * from SupportMarketDataBean");
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        env.statement("s0").addListener(listener);

        // Set of events routed by each listener
        Set<SupportMarketDataBean> routed = Collections.synchronizedSet(new HashSet<SupportMarketDataBean>());

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtListenerRoute.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtListenerCreateStmtCallable(i, env.runtime(), env.statement("trigger"), numRoutes, routed);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        // assert
        EventBean[] results = listener.getNewDataListFlattened();
        assertTrue(results.length >= numThreads * numRoutes);

        for (SupportMarketDataBean routedEvent : routed) {
            boolean found = false;
            for (int i = 0; i < results.length; i++) {
                if (results[i].getUnderlying() == routedEvent) {
                    found = true;
                }
            }
            assertTrue(found);
        }

        env.undeployAll();
    }
}
