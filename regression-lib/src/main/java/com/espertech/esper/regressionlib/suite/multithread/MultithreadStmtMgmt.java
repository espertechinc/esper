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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtMgmtCallable;
import com.espertech.esper.regressionlib.support.multithread.StmtMgmtCallablePair;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;

import java.util.concurrent.*;

/**
 * Test for multithread-safety for creating and stopping various statements.
 */
public class MultithreadStmtMgmt implements RegressionExecution {
    private final static String EVENT_NAME = SupportMarketDataBean.class.getSimpleName();
    private final static String[] EPL = new String[]{
        "select * from " + EVENT_NAME + " where symbol = 'IBM'",
        "select * from " + EVENT_NAME + " (symbol = 'IBM')",
        "select * from " + EVENT_NAME + " (price>1)",
        "select * from " + EVENT_NAME + " (feed='RT')",
        "select * from " + EVENT_NAME + " (symbol='IBM', price>1, feed='RT')",
        "select * from " + EVENT_NAME + " (price>1, feed='RT')",
        "select * from " + EVENT_NAME + " (symbol='IBM', feed='RT')",
        "select * from " + EVENT_NAME + " (symbol='IBM', feed='RT') where price between 0 and 1000",
        "select * from " + EVENT_NAME + " (symbol='IBM') where price between 0 and 1000 and feed='RT'",
        "select * from " + EVENT_NAME + " (symbol='IBM') where 'a'='a'",
        "select a.* from pattern[every a=" + EVENT_NAME + "(symbol='IBM')]",
        "select a.* from pattern[every a=" + EVENT_NAME + "(symbol='IBM', price < 1000)]",
        "select a.* from pattern[every a=" + EVENT_NAME + "(feed='RT', price < 1000)]",
        "select a.* from pattern[every a=" + EVENT_NAME + "(symbol='IBM', feed='RT')]"};

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        StmtMgmtCallablePair[] eplAndStmt = new StmtMgmtCallablePair[EPL.length];
        for (int i = 0; i < EPL.length; i++) {
            EPCompiled compiled = env.compile(EPL[i]);
            eplAndStmt[i] = new StmtMgmtCallablePair(EPL[i], compiled);
        }

        runAssertionPatterns(env, eplAndStmt);
        runAssertionEachStatementAlone(env, eplAndStmt);
        runAssertionStatementsMixed(env, eplAndStmt);
        runAssertionStatementsAll(env, eplAndStmt);
    }

    private static void runAssertionPatterns(RegressionEnvironment env, StmtMgmtCallablePair[] eplAndStmt) {
        int numThreads = 3;
        StmtMgmtCallablePair[] statements;

        statements = new StmtMgmtCallablePair[]{eplAndStmt[10]};
        tryStatementCreateSendAndStop(env, numThreads, statements, 10);
        env.undeployAll();

        statements = new StmtMgmtCallablePair[]{eplAndStmt[10], eplAndStmt[11]};
        tryStatementCreateSendAndStop(env, numThreads, statements, 10);
        env.undeployAll();

        statements = new StmtMgmtCallablePair[]{eplAndStmt[10], eplAndStmt[11], eplAndStmt[12]};
        tryStatementCreateSendAndStop(env, numThreads, statements, 10);
        env.undeployAll();

        statements = new StmtMgmtCallablePair[]{eplAndStmt[10], eplAndStmt[11], eplAndStmt[12], eplAndStmt[13]};
        tryStatementCreateSendAndStop(env, numThreads, statements, 10);
        env.undeployAll();
    }

    private static void runAssertionEachStatementAlone(RegressionEnvironment env, StmtMgmtCallablePair[] eplAndStmt) {
        int numThreads = 4;
        for (int i = 0; i < eplAndStmt.length; i++) {
            StmtMgmtCallablePair[] statements = new StmtMgmtCallablePair[]{eplAndStmt[i]};
            tryStatementCreateSendAndStop(env, numThreads, statements, 10);
            env.undeployAll();
        }
    }

    private static void runAssertionStatementsMixed(RegressionEnvironment env, StmtMgmtCallablePair[] eplAndStmt) {
        int numThreads = 2;
        StmtMgmtCallablePair[] statements = new StmtMgmtCallablePair[]{eplAndStmt[1], eplAndStmt[4], eplAndStmt[6], eplAndStmt[7], eplAndStmt[8]};
        tryStatementCreateSendAndStop(env, numThreads, statements, 10);
        env.undeployAll();

        statements = new StmtMgmtCallablePair[]{eplAndStmt[1], eplAndStmt[7], eplAndStmt[8], eplAndStmt[11], eplAndStmt[12]};
        tryStatementCreateSendAndStop(env, numThreads, statements, 10);
        env.undeployAll();
    }

    private static void runAssertionStatementsAll(RegressionEnvironment env, StmtMgmtCallablePair[] eplAndStmt) {
        int numThreads = 3;
        tryStatementCreateSendAndStop(env, numThreads, eplAndStmt, 10);
        env.undeployAll();
    }

    private static void tryStatementCreateSendAndStop(RegressionEnvironment env, int numThreads, StmtMgmtCallablePair[] statements, int numRepeats) {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtMgmt.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtMgmtCallable(env.runtime(), statements, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);

        StringBuilder statementDigest = new StringBuilder();
        for (int i = 0; i < statements.length; i++) {
            statementDigest.append(statements[i].getEpl());
        }
        SupportCompileDeployUtil.assertFutures(future);
    }
}
