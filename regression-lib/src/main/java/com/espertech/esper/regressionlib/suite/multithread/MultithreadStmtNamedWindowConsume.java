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
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtNamedWindowConsumeCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPStatement;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of insert-into and aggregation per group.
 */
public class MultithreadStmtNamedWindowConsume implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('window') create window MyWindow#keepall as select theString, longPrimitive from SupportBean", path);
        SupportMTUpdateListener listenerWindow = new SupportMTUpdateListener();
        env.statement("window").addListener(listenerWindow);

        env.compileDeploy("insert into MyWindow(theString, longPrimitive) " +
            " select symbol, volume \n" +
            " from SupportMarketDataBean", path);

        String stmtTextDelete = "on SupportBean_A as s0 delete from MyWindow as win where win.theString = s0.id";
        env.compileDeploy(stmtTextDelete, path);

        trySend(env, path, listenerWindow, 4, 1000, 8);

        env.undeployAll();
    }

    private static void trySend(RegressionEnvironment env, RegressionPath path, SupportMTUpdateListener listenerWindow, int numThreads, int numRepeats, int numConsumers) {
        SupportMTUpdateListener[] listenerConsumers = new SupportMTUpdateListener[numConsumers];
        EPCompiled compiled = env.compile("select theString, longPrimitive from MyWindow", path);
        for (int i = 0; i < listenerConsumers.length; i++) {
            String stmtName = "c" + i;
            try {
                env.deployment().deploy(compiled, new DeploymentOptions().setStatementNameRuntime(ctx -> stmtName));
            } catch (EPDeployException e) {
                throw new RuntimeException(e);
            }
            EPStatement stmtConsumer = env.statement(stmtName);
            listenerConsumers[i] = new SupportMTUpdateListener();
            stmtConsumer.addListener(listenerConsumers[i]);
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtNamedWindowConsume.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtNamedWindowConsumeCallable(Integer.toString(i), env.runtime(), numRepeats);
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
                throw new RuntimeException();
            }
        }
        String[] expectedIds = expectedIdsList.toArray(new String[0]);

        Assert.assertEquals(numThreads * numRepeats, listenerWindow.getNewDataList().size());  // old and new each

        // compute list of received
        for (int i = 0; i < listenerConsumers.length; i++) {
            EventBean[] newEvents = listenerConsumers[i].getNewDataListFlattened();
            String[] receivedIds = new String[newEvents.length];
            for (int j = 0; j < newEvents.length; j++) {
                receivedIds[j] = (String) newEvents[j].get("theString");
            }
            assertEquals(receivedIds.length, expectedIds.length);

            Arrays.sort(receivedIds);
            Arrays.sort(expectedIds);
            Arrays.deepEquals(expectedIds, receivedIds);
        }
    }
}

