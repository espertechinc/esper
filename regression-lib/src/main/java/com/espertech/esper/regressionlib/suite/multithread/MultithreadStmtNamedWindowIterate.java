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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtNamedWindowIterateCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;

import java.util.concurrent.*;

/**
 * Test for multithread-safety of insert-into and aggregation per group.
 */
public class MultithreadStmtNamedWindowIterate implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = setupStmts(env);
        tryIterate(env, path, 4, 250);
        env.undeployAll();

        path = setupStmts(env);
        tryIterate(env, path, 2, 500);
        env.undeployAll();
    }

    private static RegressionPath setupStmts(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyWindow#groupwin(theString)#keepall as select theString, longPrimitive from SupportBean", path);
        env.compileDeploy("insert into MyWindow(theString, longPrimitive) select symbol, volume from SupportMarketDataBean", path);
        return path;
    }

    private static void tryIterate(RegressionEnvironment env, RegressionPath path, int numThreads, int numRepeats) {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtNamedWindowIterate.class));
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtNamedWindowIterateCallable(Integer.toString(i), env, path, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);
    }
}
