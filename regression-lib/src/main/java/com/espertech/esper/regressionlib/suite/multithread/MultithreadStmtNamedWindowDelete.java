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
import com.espertech.esper.regressionlib.support.multithread.StmtNamedWindowDeleteCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of insert-into and aggregation per group.
 */
public class MultithreadStmtNamedWindowDelete implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('window') create window MyWindow#keepall as select theString, longPrimitive from SupportBean", path);
        SupportMTUpdateListener listenerWindow = new SupportMTUpdateListener();
        env.statement("window").addListener(listenerWindow);

        env.compileDeploy("insert into MyWindow(theString, longPrimitive) select symbol, volume from SupportMarketDataBean", path);
        env.compileDeploy("on SupportBean_A as s0 delete from MyWindow as win where win.theString = s0.id", path);

        env.compileDeploy("@name('consumer') select irstream theString, longPrimitive from MyWindow", path);
        SupportMTUpdateListener listenerConsumer = new SupportMTUpdateListener();
        env.statement("consumer").addListener(listenerConsumer);

        trySend(env, listenerWindow, listenerConsumer, 4, 1000);

        env.undeployAll();
    }

    private static void trySend(RegressionEnvironment env, SupportMTUpdateListener listenerWindow, SupportMTUpdateListener listenerConsumer, int numThreads, int numRepeats) {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtNamedWindowDelete.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtNamedWindowDeleteCallable(Integer.toString(i), env.runtime(), numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);

        // compute list of expected
        List<String> expectedIdsList = new ArrayList<String>();
        for (int i = 0; i < numThreads; i++) {
            try {
                expectedIdsList.addAll((List<String>) future[i].get());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        String[] expectedIds = expectedIdsList.toArray(new String[0]);

        Assert.assertEquals(2 * numThreads * numRepeats, listenerWindow.getNewDataList().size());  // old and new each
        Assert.assertEquals(2 * numThreads * numRepeats, listenerConsumer.getNewDataList().size());  // old and new each

        // compute list of received
        EventBean[] newEvents = listenerWindow.getNewDataListFlattened();
        String[] receivedIds = new String[newEvents.length];
        for (int i = 0; i < newEvents.length; i++) {
            receivedIds[i] = (String) newEvents[i].get("theString");
        }
        assertEquals(receivedIds.length, expectedIds.length);

        Arrays.sort(receivedIds);
        Arrays.sort(expectedIds);
        Arrays.deepEquals(expectedIds, receivedIds);
    }
}
