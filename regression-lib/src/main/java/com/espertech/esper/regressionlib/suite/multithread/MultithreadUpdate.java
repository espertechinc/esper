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
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtUpdateSendCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety (or lack thereof) for iterators: iterators fail with concurrent mods as expected behavior
 */
public class MultithreadUpdate implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') select theString from SupportBean");

        final List<String> strings = Collections.synchronizedList(new ArrayList<String>());
        env.statement("s0").addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                strings.add((String) newEvents[0].get("theString"));
            }
        });

        trySend(env, 2, 50000);

        boolean found = false;
        for (String value : strings) {
            if (value.equals("a")) {
                found = true;
            }
        }
        assertTrue(found);

        env.undeployAll();
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numRepeats) {
        EPCompiled compiled = env.compile("@name('upd') update istream SupportBean set theString='a'");

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadUpdate.class));
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtUpdateSendCallable(i, env.runtime(), numRepeats);
            future[i] = threadPool.submit(callable);
        }

        for (int i = 0; i < 50; i++) {
            env.deploy(compiled);
            SupportCompileDeployUtil.threadSleep(10);
            env.undeployModuleContaining("upd");
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 5, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);
    }
}
