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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.SendEventCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety of context with database access.
 */
public class MultithreadContextPartitioned implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(MultithreadContextPartitioned.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create context CtxEachString partition by theString from SupportBean", path);
        env.compileDeploy("@Name('select') context CtxEachString select * from SupportBean", path);

        tryPerformanceDispatch(env, 8, 100);

        env.undeployAll();
    }

    private static void tryPerformanceDispatch(RegressionEnvironment env, int numThreads, int numRepeats) {
        MyListener listener = new MyListener();
        env.statement("select").addListener(listener);

        List<Object>[] events = new ArrayList[numThreads];
        int eventId = 0;
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            events[threadNum] = new ArrayList<Object>();
            for (int eventNum = 0; eventNum < numRepeats; eventNum++) {
                // range: 1 to 1000
                int partition = (int) (Math.random() * 50);
                eventId++;
                events[threadNum].add(new SupportBean(new Integer(partition).toString(), eventId));
            }
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadContextPartitioned.class));
        Future[] futures = new Future[numThreads];
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            Callable callable = new SendEventCallable(i, env.runtime(), events[i].iterator());
            futures[i] = threadPool.submit(callable);
        }
        SupportCompileDeployUtil.assertFutures(futures);
        long delta = System.currentTimeMillis() - startTime;

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);

        // print those events not received
        for (List<Object> eventList : events) {
            for (Object event : eventList) {
                if (!listener.getBeans().contains(event)) {
                    log.info("Expected event was not received, event " + event);
                }
            }
        }

        assertEquals(numRepeats * numThreads, listener.getBeans().size());
        assertTrue("delta=" + delta, delta < 500);
    }

    public static class MyListener implements UpdateListener {
        private List<SupportBean> beans = new ArrayList<SupportBean>();

        public synchronized void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            if (newEvents.length > 1) {
                assertEquals(1, newEvents.length);
            }
            beans.add((SupportBean) newEvents[0].getUnderlying());
        }

        public List<SupportBean> getBeans() {
            return beans;
        }
    }
}
