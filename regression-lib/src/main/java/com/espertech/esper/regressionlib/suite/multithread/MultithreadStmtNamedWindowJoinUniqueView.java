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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPEventService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;

public class MultithreadStmtNamedWindowJoinUniqueView implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        String epl = "create window A#unique(key) as MyEventA;\n" +
            "create window B#unique(key) as MyEventB;\n" +
            "insert into A select * from MyEventA;\n" +
            "insert into B select * from MyEventB;\n" +
            "\n" +
            "@Name('stmt') select sum(A.data) as aTotal,sum(B.data) as bTotal " +
            "from A unidirectional, B where A.key = B.key;\n";
        env.compileDeploy(epl);

        ExecutorService es = Executors.newFixedThreadPool(10, new SupportThreadFactory(MultithreadStmtNamedWindowJoinUniqueView.class));
        List<MyRunnable> runnables = new ArrayList<MyRunnable>();
        for (int i = 0; i < 6; i++) {
            runnables.add(new MyRunnable(env.eventService()));
        }

        for (MyRunnable toRun : runnables) {
            es.submit(toRun);
        }
        SupportCompileDeployUtil.threadSleep(2000);
        for (MyRunnable toRun : runnables) {
            toRun.setShutdown(true);
        }

        es.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(es, 20, TimeUnit.SECONDS);

        for (MyRunnable runnable : runnables) {
            assertNull(runnable.getException());
        }
        env.undeployAll();
    }

    public static class MyRunnable implements Runnable {
        private final EPEventService runtime;

        private Exception exception;
        private boolean shutdown;

        public MyRunnable(EPEventService runtime) {
            this.runtime = runtime;
        }

        public void run() {
            try {
                for (int i = 0; i < 1000; i++) {
                    runtime.sendEventBean(new MyEventA("key1", (int) (Math.random() * 1000000)), "MyEventA");
                    runtime.sendEventBean(new MyEventA("key2", (int) (Math.random() * 1000000)), "MyEventA");
                    runtime.sendEventBean(new MyEventB("key1", (int) (Math.random() * 1000000)), "MyEventB");
                    runtime.sendEventBean(new MyEventB("key2", (int) (Math.random() * 1000000)), "MyEventB");

                    if (shutdown) {
                        break;
                    }
                }
            } catch (Exception ex) {
                this.exception = ex;
            }
        }

        public Exception getException() {
            return exception;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }
    }

    public static class MyEventA {
        private final String key;
        private final int data;

        public MyEventA(String key, int data) {
            this.key = key;
            this.data = data;
        }

        public String getKey() {
            return key;
        }

        public int getData() {
            return data;
        }
    }

    public static class MyEventB {
        private final String key;
        private final int data;

        public MyEventB(String key, int data) {
            this.key = key;
            this.data = data;
        }

        public String getKey() {
            return key;
        }

        public int getData() {
            return data;
        }
    }
}


