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
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.GeneratorIterator;
import com.espertech.esper.regressionlib.support.multithread.SendEventWaitCallable;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Test for pattern statement parallel execution by threads.
 */
public class MultithreadStmtPattern implements RegressionExecutionWithConfigure {
    public void configure(Configuration configuration) {
    }

    @Override
    public boolean haWithCOnly() {
        return true;
    }

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {

        String pattern = "a=SupportBean";
        tryPattern(env, pattern, 4, 20);

        pattern = "a=SupportBean or a=SupportBean";
        tryPattern(env, pattern, 2, 20);
    }

    private static void tryPattern(RegressionEnvironment env, String pattern, int numThreads, int numEvents) {
        Object sendLock = new Object();
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtPattern.class));
        Future[] future = new Future[numThreads];
        SendEventWaitCallable[] callables = new SendEventWaitCallable[numThreads];
        for (int i = 0; i < numThreads; i++) {
            callables[i] = new SendEventWaitCallable(i, env.runtime(), sendLock, new GeneratorIterator(numEvents));
            future[i] = threadPool.submit(callables[i]);
        }

        SupportMTUpdateListener[] listener = new SupportMTUpdateListener[numEvents];
        String epl = "select * from pattern[" + pattern + "]";
        EPCompiled compiled = env.compile(epl);
        for (int i = 0; i < numEvents; i++) {
            String stmtName = "p" + i;
            env.deploy(compiled, new DeploymentOptions().setStatementNameRuntime(ctx -> stmtName));
            EPStatement stmt = env.statement(stmtName);
            listener[i] = new SupportMTUpdateListener();
            stmt.addListener(listener[i]);
        }

        synchronized (sendLock) {
            sendLock.notifyAll();
        }

        SupportCompileDeployUtil.threadSleep(100);
        for (SendEventWaitCallable callable : callables) {
            callable.setShutdown(true);
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);

        for (int i = 0; i < numEvents; i++) {
            EventBean event = listener[i].assertOneGetNewAndReset();
            Object a = event.get("a");
            assertTrue(a instanceof SupportBean);
        }

        env.undeployAll();
    }
}
