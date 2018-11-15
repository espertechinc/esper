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
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtJoinCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.concurrent.*;

/**
 * Test for multithread-safety for joins.
 */
public class MultithreadStmtJoin implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') select istream * \n" +
            "  from SupportBean(theString='s0')#length(1000000) as s0,\n" +
            "       SupportBean(theString='s1')#length(1000000) as s1\n" +
            "where s0.longPrimitive = s1.longPrimitive\n"
        );
        trySendAndReceive(env, 4, env.statement("s0"), 1000);
        trySendAndReceive(env, 2, env.statement("s0"), 2000);
        env.undeployAll();
    }

    private static void trySendAndReceive(RegressionEnvironment env, int numThreads, EPStatement statement, int numRepeats) {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtJoin.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtJoinCallable(i, env.runtime(), statement, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);
    }
}
