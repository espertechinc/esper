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
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtListenerAddRemoveCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.concurrent.*;

/**
 * Test for multithread-safety for adding and removing listener.
 */
public class MultithreadStmtListenerAddRemove implements RegressionExecutionWithConfigure {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void configure(Configuration configuration) {
        configuration.getRuntime().getThreading().setListenerDispatchTimeout(Long.MAX_VALUE);
        configuration.getCommon().addEventType(SupportMarketDataBean.class);
    }

    public void run(RegressionEnvironment env) {
        int numThreads = 2;

        env.compileDeploy("@name('s0') select * from pattern[every a=SupportMarketDataBean(symbol='IBM')]");
        tryStatementListenerAddRemove(env, numThreads, env.statement("s0"), false, 10000);
        env.undeployModuleContaining("s0");

        env.compileDeploy("@name('s0') select * from SupportMarketDataBean(symbol='IBM', feed='RT')");
        tryStatementListenerAddRemove(env, numThreads, env.statement("s0"), true, 10000);
        env.undeployModuleContaining("s0");
    }

    private static void tryStatementListenerAddRemove(RegressionEnvironment env, int numThreads, EPStatement statement, boolean isEPL, int numRepeats) {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtListenerAddRemove.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtListenerAddRemoveCallable(env.runtime(), statement, isEPL, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);
    }
}
