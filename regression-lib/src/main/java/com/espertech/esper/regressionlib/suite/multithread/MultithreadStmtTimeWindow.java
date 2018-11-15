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
import com.espertech.esper.regressionlib.support.multithread.GeneratorIterator;
import com.espertech.esper.regressionlib.support.multithread.SendEventCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of a time window -based statement.
 */
public class MultithreadStmtTimeWindow implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        trySend(env, 10, 5000);
        trySend(env, 6, 2000);
        trySend(env, 2, 10000);
        trySend(env, 3, 5000);
        trySend(env, 5, 2500);
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numRepeats) {
        // set time to 0
        env.advanceTime(0);

        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        env.compileDeploy("@name('s0') select irstream intPrimitive, theString as key from SupportBean#time(1 sec)");
        env.statement("s0").addListener(listener);

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtTimeWindow.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new SendEventCallable(i, env.runtime(), new GeneratorIterator(numRepeats));
            future[i] = threadPool.submit(callable);
        }

        // Advance time window every 100 milliseconds for 1 second
        for (int i = 0; i < 10; i++) {
            env.advanceTime(i * 1000);
            SupportCompileDeployUtil.threadSleep(100);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        // set time to a large value
        env.advanceTime(10000000000L);

        // Assert results
        int totalExpected = numThreads * numRepeats;

        // assert new data
        EventBean[] resultNewData = listener.getNewDataListFlattened();
        assertEquals(totalExpected, resultNewData.length);
        HashMap<Integer, List<String>> resultsNewData = sortPerIntKey(resultNewData);
        assertResult(numRepeats, numThreads, resultsNewData);

        // assert old data
        EventBean[] resultOldData = listener.getOldDataListFlattened();
        assertEquals(totalExpected, resultOldData.length);
        HashMap<Integer, List<String>> resultsOldData = sortPerIntKey(resultOldData);
        assertResult(numRepeats, numThreads, resultsOldData);

        env.undeployAll();
    }

    private static HashMap<Integer, List<String>> sortPerIntKey(EventBean[] result) {
        HashMap<Integer, List<String>> results = new LinkedHashMap<Integer, List<String>>();
        for (EventBean theEvent : result) {
            int count = (Integer) theEvent.get("intPrimitive");
            String key = (String) theEvent.get("key");

            List<String> entries = results.get(count);
            if (entries == null) {
                entries = new LinkedList<String>();
                results.put(count, entries);
            }
            entries.add(key);
        }
        return results;
    }

    // Each integer value must be there with 2 entries of the same value
    private static void assertResult(int numRepeats, int numThreads, HashMap<Integer, List<String>> results) {
        for (int i = 0; i < numRepeats; i++) {
            List<String> values = results.get(i);
            assertEquals(numThreads, values.size());
            for (String value : values) {
                assertEquals(Integer.toString(i), value);
            }
        }
    }
}
