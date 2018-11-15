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
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtSubqueryCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of a lookup statement.
 */
public class MultithreadStmtSubquery implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        trySend(env, 4, 10000);
        trySend(env, 3, 10000);
        trySend(env, 2, 10000);
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numRepeats) {
        env.compileDeploy("@name('s0') select (select id from SupportBean_S0#length(1000000) where id = s1.id) as value from SupportBean_S1 as s1");
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        env.statement("s0").addListener(listener);

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtSubquery.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtSubqueryCallable(i, env.runtime(), numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        // Assert results
        int totalExpected = numThreads * numRepeats;

        // assert new data
        EventBean[] resultNewData = listener.getNewDataListFlattened();
        assertEquals(totalExpected, resultNewData.length);

        Set<Integer> values = new HashSet<Integer>();
        for (EventBean theEvent : resultNewData) {
            values.add((Integer) theEvent.get("value"));
        }
        assertEquals("Unexpected duplicates", totalExpected, values.size());

        listener.reset();
        env.undeployAll();
    }
}
