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
import com.espertech.esper.regressionlib.support.multithread.StmtIterateCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.concurrent.*;

/**
 * Test for multithread-safety (or lack thereof) for iterators: iterators fail with concurrent mods as expected behavior
 */
public class MultithreadStmtIterate implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        runAssertionIteratorSingleStmt(env);
        runAssertionIteratorMultiStmtNoViewShare(env);
    }

    private static void runAssertionIteratorSingleStmt(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') select theString from SupportBean#time(5 min)");
        EPStatement[] stmt = new EPStatement[]{env.statement("s0")};
        trySend(env, 2, 10, stmt);
        env.undeployAll();
    }

    private static void runAssertionIteratorMultiStmtNoViewShare(RegressionEnvironment env) {
        EPStatement[] stmt = new EPStatement[3];
        for (int i = 0; i < stmt.length; i++) {
            String name = "Stmt_" + i;
            String stmtText = "@Name('" + name + "') select theString from SupportBean#time(5 min)";
            env.compileDeploy(stmtText);
            stmt[i] = env.statement(name);
        }

        trySend(env, 4, 10, stmt);

        env.undeployAll();
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numRepeats, EPStatement[] stmt) {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtIterate.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtIterateCallable(i, env.runtime(), stmt, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 5, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);
    }
}
