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
import com.espertech.esper.regressionlib.support.multithread.StmtNamedWindowMergeCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import org.junit.Assert;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety and named window updates.
 */
public class MultithreadStmtNamedWindowMerge implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        trySend(env, 3, 100);
        trySend(env, 2, 1000);
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numEventsPerThread) {
        // setup statements
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyWindow#keepall as select * from SupportBean", path);
        env.compileDeploy("on SupportBean sb " +
            "merge MyWindow nw where nw.theString = sb.theString " +
            " when not matched then insert select * " +
            " when matched then update set intPrimitive = nw.intPrimitive + 1", path);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtNamedWindowMerge.class));
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new StmtNamedWindowMergeCallable(env.runtime(), numEventsPerThread));
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        // compare
        EventBean[] rows = env.compileExecuteFAF("select * from MyWindow", path).getArray();
        assertEquals(numEventsPerThread, rows.length);
        for (EventBean row : rows) {
            Assert.assertEquals(numThreads - 1, row.get("intPrimitive"));
        }
        //long deltaTime = endTime - startTime;
        //System.out.println("Totals updated: " + totalUpdates + "  Delta cumu: " + deltaCumulative + "  Delta pooled: " + deltaTime);
        env.undeployAll();
    }
}
