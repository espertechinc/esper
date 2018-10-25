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
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.Locking;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.GeneratorIterator;
import com.espertech.esper.regressionlib.support.multithread.SendEventCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety and deterministic behavior when using insert-into.
 */
public class MultithreadDeterminismListener {
    public void run(Configuration configuration) {
        trySend(4, 10000, true, Locking.SUSPEND, configuration);
        trySend(4, 10000, true, Locking.SPIN, configuration);
    }

    public void manualTestOrderedDeliveryFail() {
        /**
         * Commented out as this is a manual test -- it should fail since the disable preserve order.
         */
        // trySend(3, 1000, false, null, configuration);
    }

    private static void trySend(int numThreads, int numEvents, boolean isPreserveOrder, Locking locking, Configuration configuration) {
        configuration.getRuntime().getThreading().setListenerDispatchPreserveOrder(isPreserveOrder);
        configuration.getRuntime().getThreading().setListenerDispatchLocking(locking);
        configuration.getCommon().addEventType(SupportBean.class);

        EPRuntime runtime = EPRuntimeProvider.getRuntime(MultithreadDeterminismListener.class.getSimpleName(), configuration);
        runtime.initialize();

        // setup statements
        EPDeployment deployed = SupportCompileDeployUtil.compileDeploy("@name('s0') select count(*) as cnt from SupportBean", runtime, configuration);
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        deployed.getStatements()[0].addListener(listener);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadDeterminismListener.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventCallable(i, runtime, new GeneratorIterator(numEvents)));
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        EventBean[] events = listener.getNewDataListFlattened();
        long[] result = new long[events.length];
        for (int i = 0; i < events.length; i++) {
            result[i] = (Long) events[i].get("cnt");
        }
        //log.info(".trySend result=" + Arrays.toString(result));

        // assert result
        assertEquals(numEvents * numThreads, events.length);
        for (int i = 0; i < numEvents * numThreads; i++) {
            assertEquals(result[i], (long) i + 1);
        }

        runtime.destroy();
    }
}
