package com.espertech.esper.regression.multithread;/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

import com.espertech.esper.client.*;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestMTStmtNamedWindowJoinUniqueView extends TestCase {

    private EPServiceProvider service;

    public void setUp()  throws Exception {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType(MyEventA.class);
        configuration.addEventType(MyEventB.class);
        service = EPServiceProviderManager.getDefaultProvider(configuration);
        service.initialize();

        String epl =
                "create window A#unique(key) as MyEventA;\n" +
                "create window B#unique(key) as MyEventB;\n" +
                "insert into A select * from MyEventA;\n" +
                "insert into B select * from MyEventB;\n" +
                "\n" +
                "@Name('stmt') select sum(A.data) as aTotal,sum(B.data) as bTotal " +
                "from A unidirectional, B where A.key = B.key;\n";
        EPDeploymentAdmin deployment = service.getEPAdministrator().getDeploymentAdmin();
        deployment.parseDeploy(epl);
    }

    public void testJoin() throws Exception
    {
        ExecutorService es = Executors.newFixedThreadPool(10);
        List<MyRunnable> runnables = new ArrayList<MyRunnable>();
        for (int i = 0; i < 6; i++) {
            runnables.add(new MyRunnable(service.getEPRuntime()));
        }

        for(Runnable toRun : runnables) {
            es.submit(toRun);
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
                }
            }
            catch (Exception ex) {
                this.exception = ex;
            }
        }

        public Exception getException() {
            return exception;
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


