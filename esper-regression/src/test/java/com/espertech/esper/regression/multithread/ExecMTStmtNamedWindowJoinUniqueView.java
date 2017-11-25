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
package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;

public class ExecMTStmtNamedWindowJoinUniqueView implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(MyEventA.class);
        configuration.addEventType(MyEventB.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String epl =
                "create window A#unique(key) as MyEventA;\n" +
                        "create window B#unique(key) as MyEventB;\n" +
                        "insert into A select * from MyEventA;\n" +
                        "insert into B select * from MyEventB;\n" +
                        "\n" +
                        "@Name('stmt') select sum(A.data) as aTotal,sum(B.data) as bTotal " +
                        "from A unidirectional, B where A.key = B.key;\n";
        EPDeploymentAdmin deployment = epService.getEPAdministrator().getDeploymentAdmin();
        deployment.parseDeploy(epl);

        ExecutorService es = Executors.newFixedThreadPool(10);
        List<MyRunnable> runnables = new ArrayList<MyRunnable>();
        for (int i = 0; i < 6; i++) {
            runnables.add(new MyRunnable(epService.getEPRuntime()));
        }

        for (MyRunnable toRun : runnables) {
            es.submit(toRun);
        }
        Thread.sleep(2000);
        for (MyRunnable toRun : runnables) {
            toRun.setShutdown(true);
        }

        es.shutdown();
        es.awaitTermination(20, TimeUnit.SECONDS);

        for (MyRunnable runnable : runnables) {
            assertNull(runnable.getException());
        }
    }

    public static class MyRunnable implements Runnable {
        private final EPRuntime runtime;

        private Exception exception;
        private boolean shutdown;

        public MyRunnable(EPRuntime runtime) {
            this.runtime = runtime;
        }

        public void run() {
            try {
                for (int i = 0; i < 1000; i++) {
                    runtime.sendEvent(new MyEventA("key1", (int) (Math.random() * 1000000)));
                    runtime.sendEvent(new MyEventA("key2", (int) (Math.random() * 1000000)));
                    runtime.sendEvent(new MyEventB("key1", (int) (Math.random() * 1000000)));
                    runtime.sendEvent(new MyEventB("key2", (int) (Math.random() * 1000000)));

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


