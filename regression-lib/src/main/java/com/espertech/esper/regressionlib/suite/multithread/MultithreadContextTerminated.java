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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.threadJoin;
import static org.junit.Assert.assertNull;

public class MultithreadContextTerminated implements RegressionExecutionWithConfigure {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void configure(Configuration configuration) {
        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
        configuration.getCommon().addEventType(StartContextEvent.class);
        configuration.getCommon().addEventType(PayloadEvent.class);
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        String eplStatement = "create context StartThenTwoSeconds start StartContextEvent end after 2 seconds";
        env.compileDeploy(eplStatement, path);

        String aggStatement = "@name('select') context StartThenTwoSeconds " +
            "select account, count(*) as totalCount " +
            "from PayloadEvent " +
            "group by account " +
            "output snapshot when terminated";
        env.compileDeploy(aggStatement, path);
        env.statement("select").addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                // no action, still listening to make sure select-clause evaluates
            }
        });

        // start context
        env.sendEventBean(new StartContextEvent());

        // start threads
        List<Thread> threads = new ArrayList<Thread>();
        List<MyRunnable> runnables = new ArrayList<MyRunnable>();
        for (int i = 0; i < 8; i++) {
            MyRunnable myRunnable = new MyRunnable(env.runtime());
            runnables.add(myRunnable);
            Thread thread = new Thread(myRunnable, this.getClass().getSimpleName() + "-Thread" + i);
            thread.start();
            threads.add(thread);
        }

        // join
        for (Thread thread : threads) {
            threadJoin(thread);
        }

        // assert
        for (MyRunnable runnable : runnables) {
            assertNull(runnable.exception);
        }

        env.undeployAll();
    }

    public class StartContextEvent {
    }

    public class PayloadEvent {
        private final String account;

        public PayloadEvent(String account) {
            this.account = account;
        }

        public String getAccount() {
            return account;
        }
    }

    public class MyRunnable implements Runnable {
        private final EPRuntime runtime;

        private Exception exception;

        public MyRunnable(EPRuntime runtime) {
            this.runtime = runtime;
        }

        public void run() {
            try {
                for (int i = 0; i < 2000000; i++) {
                    PayloadEvent payloadEvent = new PayloadEvent("A1");
                    runtime.getEventService().sendEventBean(payloadEvent, "PayloadEvent");
                }
            } catch (Exception ex) {
                this.exception = ex;
            }
        }
    }
}