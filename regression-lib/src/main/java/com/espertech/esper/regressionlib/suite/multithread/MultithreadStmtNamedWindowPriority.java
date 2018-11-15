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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtNamedWindowPriorityCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of @priority and named windows.
 */
public class MultithreadStmtNamedWindowPriority implements RegressionExecutionWithConfigure {
    public void configure(Configuration configuration) {
        configuration.getCommon().addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.getCommon().addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.getRuntime().getExecution().setPrioritized(true);
        configuration.getRuntime().getThreading().setInsertIntoDispatchPreserveOrder(false);
    }

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('window') create window MyWindow#keepall as (c0 string, c1 string)", path);
        env.compileDeploy("insert into MyWindow select p00 as c0, p01 as c1 from SupportBean_S0", path);
        env.compileDeploy("@Priority(1) on SupportBean_S1 s1 merge MyWindow s0 where s1.p10 = c0 " +
            "when matched then update set c1 = s1.p11", path);
        env.compileDeploy("@Priority(0) on SupportBean_S1 s1 merge MyWindow s0 where s1.p10 = c0 " +
            "when matched then update set c1 = s1.p12", path);

        trySend(env, env.statement("window"), 4, 1000);

        env.undeployAll();
    }

    private static void trySend(RegressionEnvironment env, EPStatement stmtWindow, int numThreads, int numRepeats) {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtNamedWindowPriority.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtNamedWindowPriorityCallable(i, env.runtime(), numRepeats);
            future[i] = threadPool.submit(callable);
        }

        SupportCompileDeployUtil.assertFutures(future);

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);

        EventBean[] events = EPAssertionUtil.iteratorToArray(stmtWindow.iterator());
        assertEquals(numThreads * numRepeats, events.length);
        for (int i = 0; i < events.length; i++) {
            String valueC1 = (String) events[i].get("c1");
            assertEquals("y", valueC1);
        }
    }
}
