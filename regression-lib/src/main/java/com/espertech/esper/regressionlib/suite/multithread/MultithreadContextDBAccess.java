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
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.SendEventCallable;
import com.espertech.esper.regressionlib.support.util.SupportDatabaseService;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of context with database access.
 */
public class MultithreadContextDBAccess implements RegressionExecutionWithConfigure {
    public void configure(Configuration configuration) {
        configuration.getCommon().addEventType(SupportBean.class);
        configuration.getCommon().getLogging().setEnableJDBC(true);
        configuration.getRuntime().getThreading().setListenerDispatchPreserveOrder(false);

        ConfigurationCommonDBRef configDB = new ConfigurationCommonDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.RETAIN);
        configuration.getCommon().addDatabaseReference("MyDB", configDB);
    }

    public boolean haWithCOnly() {
        return true;
    }

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create context CtxEachString partition by theString from SupportBean", path);
        env.compileDeploy("@Name('select') context CtxEachString " +
            "select * from SupportBean, " +
            "  sql:MyDB ['select mycol3 from mytesttable_large where ${theString} = mycol1']", path);

        // up to 10 threads, up to 1000 combinations (1 to 1000)
        tryThreadSafetyHistoricalJoin(env, 8, 20);

        env.undeployAll();
    }

    private static void tryThreadSafetyHistoricalJoin(RegressionEnvironment env, int numThreads, int numRepeats) {
        MyListener listener = new MyListener();
        env.statement("select").addListener(listener);

        List<Object>[] events = new ArrayList[numThreads];
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            events[threadNum] = new ArrayList<Object>();
            for (int eventNum = 0; eventNum < numRepeats; eventNum++) {
                // range: 1 to 1000
                int partition = eventNum + 1;
                events[threadNum].add(new SupportBean(new Integer(partition).toString(), 0));
            }
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadContextDBAccess.class));
        Future[] futures = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new SendEventCallable(i, env.runtime(), events[i].iterator());
            futures[i] = threadPool.submit(callable);
        }

        SupportCompileDeployUtil.assertFutures(futures);

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);

        assertEquals(numRepeats * numThreads, listener.getCount());
    }

    public static class MyListener implements UpdateListener {
        private int count;

        public synchronized void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            if (newEvents.length > 1) {
                assertEquals(1, newEvents.length);
            }
            count += 1;
        }

        public int getCount() {
            return count;
        }
    }
}
