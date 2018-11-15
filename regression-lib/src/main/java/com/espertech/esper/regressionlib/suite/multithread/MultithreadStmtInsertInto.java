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
import com.espertech.esper.regressionlib.support.multithread.StmtInsertIntoCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety of insert-into and aggregation per group.
 */
public class MultithreadStmtInsertInto implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy(
            "insert into XStream " +
                " select theString as key, count(*) as mycount\n" +
                " from SupportBean#time(5 min)" +
                " group by theString", path);
        env.compileDeploy(
            "insert into XStream " +
                " select symbol as key, count(*) as mycount\n" +
                " from SupportMarketDataBean#time(5 min)" +
                " group by symbol", path);

        env.compileDeploy("@name('s0') select key, mycount from XStream", path);
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        env.statement("s0").addListener(listener);

        trySend(env, listener, 10, 5000);
        trySend(env, listener, 4, 10000);

        env.undeployAll();
    }

    private static void trySend(RegressionEnvironment env, SupportMTUpdateListener listener, int numThreads, int numRepeats) {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtInsertInto.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtInsertIntoCallable(Integer.toString(i), env.runtime(), numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        // Assert results
        int totalExpected = numThreads * numRepeats * 2;
        EventBean[] result = listener.getNewDataListFlattened();
        assertEquals(totalExpected, result.length);
        HashMap<Long, Set<String>> results = new LinkedHashMap<Long, Set<String>>();
        for (EventBean theEvent : result) {
            long count = (Long) theEvent.get("mycount");
            String key = (String) theEvent.get("key");

            Set<String> entries = results.get(count);
            if (entries == null) {
                entries = new HashSet<String>();
                results.put(count, entries);
            }
            entries.add(key);
        }

        assertEquals(numRepeats, results.size());
        for (Set<String> value : results.values()) {
            assertEquals(2 * numThreads, value.size());
            for (int i = 0; i < numThreads; i++) {
                assertTrue(value.contains("E1_" + i));
                assertTrue(value.contains("E2_" + i));
            }
        }

        listener.reset();
    }
}
