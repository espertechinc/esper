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
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.SendEventCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.fail;

/**
 * Test for multithread-safety of context.
 */
public class MultithreadContextCountSimple implements RegressionExecution {

    private static final Logger log = LoggerFactory.getLogger(MultithreadContextCountSimple.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('ctx') create context HashByUserCtx as coalesce by consistent_hash_crc32(p00) from SupportBean_S0 granularity 10000000", path);
        env.compileDeploy("@name('select') context HashByUserCtx select p01 from SupportBean_S0", path);

        trySendContextCountSimple(env, 4, 5);

        env.undeployAll();
    }

    private static void trySendContextCountSimple(RegressionEnvironment env, int numThreads, int numRepeats) {
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        env.statement("select").addListener(listener);

        List<Object>[] eventsPerThread = new ArrayList[numThreads];
        for (int t = 0; t < numThreads; t++) {
            eventsPerThread[t] = new ArrayList<>();
            for (int i = 0; i < numRepeats; i++) {
                eventsPerThread[t].add(new SupportBean_S0(-1, "E" + i, i + "_" + t));
            }
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadContextCountSimple.class));
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable<Boolean> callable = new SendEventCallable(i, env.runtime(), eventsPerThread[i].iterator());
            future[i] = threadPool.submit(callable);
        }

        SupportCompileDeployUtil.threadSleep(2000);
        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        EventBean[] result = listener.getNewDataListFlattened();
        Set<String> received = new LinkedHashSet<>();
        for (EventBean event : result) {
            String key = (String) event.get("p01");
            if (received.contains(key)) {
                fail("key " + key + " received multiple times");
            }
            received.add(key);
        }

        if (received.size() != numRepeats * numThreads) {
            log.info("Received are " + received.size() + " entries");
            fail();
        }
    }
}
