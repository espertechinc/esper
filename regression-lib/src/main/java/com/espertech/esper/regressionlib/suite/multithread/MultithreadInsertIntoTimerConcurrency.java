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
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.Locking;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.util.NoActionUpdateListener;
import com.espertech.esper.runtime.client.EPEventService;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class MultithreadInsertIntoTimerConcurrency {
    private static final Logger log = LoggerFactory.getLogger(MultithreadInsertIntoTimerConcurrency.class);
    private AtomicLong idCounter;
    private ExecutorService executorService;
    private EPEventService epRuntime;
    private NoActionUpdateListener noActionUpdateListener;

    public void run(Configuration configuration) {
        idCounter = new AtomicLong(0);
        executorService = Executors.newCachedThreadPool();
        noActionUpdateListener = new NoActionUpdateListener();

        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
        configuration.getCommon().addEventType(SupportBean.class);
        configuration.getRuntime().getThreading().setInsertIntoDispatchLocking(Locking.SUSPEND);

        final EPRuntime runtime = EPRuntimeProvider.getRuntime(this.getClass().getSimpleName(), configuration);
        runtime.initialize();
        epRuntime = runtime.getEventService();

        RegressionPath path = new RegressionPath();
        String epl = "insert into Stream1 select count(*) as cnt from SupportBean#time(7 sec)";
        EPCompiled compiled = SupportCompileDeployUtil.compile(epl, configuration, path);
        path.add(compiled);
        SupportCompileDeployUtil.deploy(compiled, runtime);

        epl = epl + " output every 10 seconds";
        compiled = SupportCompileDeployUtil.compile(epl, configuration, path);
        SupportCompileDeployUtil.deployAddListener(compiled, "insert", noActionUpdateListener, runtime);

        SendEventRunnable sendTickEventRunnable = new SendEventRunnable(10000);
        start(sendTickEventRunnable, 4);

        // Adjust here for long-running test
        SupportCompileDeployUtil.threadSleep(3000);
        sendTickEventRunnable.setShutdown(true);

        executorService.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(executorService, 1, TimeUnit.SECONDS);
        runtime.destroy();
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
        epRuntime.sendEventBean(theEvent, "SupportBean");
    }

    class SendEventRunnable implements Callable<Object> {
        private int maxSent;
        private boolean shutdown;

        public SendEventRunnable(int maxSent) {
            this.maxSent = maxSent;
        }

        public Object call() {
            int count = 0;
            while (true) {
                sendEvent();
                SupportCompileDeployUtil.threadSleep(1);
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
