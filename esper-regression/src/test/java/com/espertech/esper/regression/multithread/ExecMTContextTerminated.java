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

import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNull;

public class ExecMTContextTerminated implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
        configuration.addEventType(StartContextEvent.class);
        configuration.addEventType(PayloadEvent.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String eplStatement = "create context StartThenTwoSeconds start StartContextEvent end after 2 seconds";
        epService.getEPAdministrator().createEPL(eplStatement);

        String aggStatement = "@name('select') context StartThenTwoSeconds " +
                "select account, count(*) as totalCount " +
                "from PayloadEvent " +
                "group by account " +
                "output snapshot when terminated";
        EPStatement epAggStatement = epService.getEPAdministrator().createEPL(aggStatement);
        epAggStatement.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                // no action, still listening to make sure select-clause evaluates
            }
        });

        // start context
        epService.getEPRuntime().sendEvent(new StartContextEvent());

        // start threads
        List<Thread> threads = new ArrayList<Thread>();
        List<MyRunnable> runnables = new ArrayList<MyRunnable>();
        for (int i = 0; i < 8; i++) {
            MyRunnable myRunnable = new MyRunnable(epService);
            runnables.add(myRunnable);
            Thread thread = new Thread(myRunnable, "Thread" + i);
            thread.start();
            threads.add(thread);
        }

        // join
        for (Thread thread : threads) {
            thread.join();
        }

        // assert
        for (MyRunnable runnable : runnables) {
            assertNull(runnable.exception);
        }
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
        private final EPServiceProvider engine;

        private Exception exception;

        public MyRunnable(EPServiceProvider engine) {
            this.engine = engine;
        }

        public void run() {
            try {
                for (int i = 0; i < 2000000; i++) {
                    PayloadEvent payloadEvent = new PayloadEvent("A1");
                    engine.getEPRuntime().sendEvent(payloadEvent);
                }
            } catch (Exception ex) {
                this.exception = ex;
            }
        }
    }
}