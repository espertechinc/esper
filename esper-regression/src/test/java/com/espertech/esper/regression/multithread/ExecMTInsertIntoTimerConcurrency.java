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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.NoActionUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ExecMTInsertIntoTimerConcurrency implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecMTInsertIntoTimerConcurrency.class);
    private AtomicLong idCounter;
    private ExecutorService executorService;
    private EPRuntime epRuntime;
    private EPAdministrator epAdministrator;
    private NoActionUpdateListener noActionUpdateListener;

    public void run(EPServiceProvider epService) throws Exception {
        runAssertion();
    }

    private void runAssertion() throws Exception {
        idCounter = new AtomicLong(0);
        executorService = Executors.newCachedThreadPool();
        noActionUpdateListener = new NoActionUpdateListener();

        Configuration epConfig = new Configuration();
        epConfig.addEventType(SupportBean.class);
        epConfig.getEngineDefaults().getThreading().setInsertIntoDispatchLocking(ConfigurationEngineDefaults.Threading.Locking.SUSPEND);

        final EPServiceProvider epServiceProvider = EPServiceProviderManager.getProvider(this.getClass().getSimpleName(), epConfig);
        epServiceProvider.initialize();

        epAdministrator = epServiceProvider.getEPAdministrator();
        epRuntime = epServiceProvider.getEPRuntime();

        epAdministrator.startAllStatements();

        String epl = "insert into Stream1 select count(*) as cnt from SupportBean#time(7 sec)";
        createEPL(epl, noActionUpdateListener);
        epl = epl + " output every 10 seconds";
        createEPL(epl, noActionUpdateListener);

        SendEventRunnable sendTickEventRunnable = new SendEventRunnable(10000);
        start(sendTickEventRunnable, 4);

        // Adjust here for long-running test
        Thread.sleep(3000);
        sendTickEventRunnable.setShutdown(true);

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        epServiceProvider.destroy();
    }

    private void createEPL(String epl, UpdateListener updateListener) {
        EPStatement statement = epAdministrator.createEPL(epl);
        statement.addListener(updateListener);
    }

    private <T> void start(Callable<T> task, int numInstances) {
        for (int i = 0; i < numInstances; i++) {
            start(task);
        }
    }

    private <T> Future<T> start(Callable<T> task) {
        Future<T> future = executorService.submit(task);
        return future;
    }

    private void sendEvent() {
        long id = idCounter.getAndIncrement();
        SupportBean theEvent = new SupportBean();
        theEvent.setLongPrimitive(id);
        epRuntime.sendEvent(theEvent);
    }

    class SendEventRunnable implements Callable<Object> {
        private int maxSent;
        private boolean shutdown;

        public SendEventRunnable(int maxSent) {
            this.maxSent = maxSent;
        }

        public Object call() throws Exception {
            int count = 0;
            while (true) {
                sendEvent();
                Thread.sleep(1);
                count++;

                if (count % 1000 == 0) {
                    log.info("Thread " + Thread.currentThread().getId() + " send " + count + " events");
                }

                if (count > maxSent) {
                    break;
                }

                if (shutdown) {
                    break;
                }
            }

            return null;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }
    }
}
