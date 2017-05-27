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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.GeneratorIterator;
import com.espertech.esper.supportregression.multithread.SendEventWaitCallable;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Test for pattern statement parallel execution by threads.
 */
public class ExecMTStmtPattern implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String type = SupportBean.class.getName();

        String pattern = "a=" + type;
        tryPattern(epService, pattern, 4, 20);

        pattern = "a=" + type + " or a=" + type;
        tryPattern(epService, pattern, 2, 20);
    }

    private void tryPattern(EPServiceProvider epService, String pattern, int numThreads, int numEvents) throws Exception {
        Object sendLock = new Object();
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        SendEventWaitCallable[] callables = new SendEventWaitCallable[numThreads];
        for (int i = 0; i < numThreads; i++) {
            callables[i] = new SendEventWaitCallable(i, epService, sendLock, new GeneratorIterator(numEvents));
            future[i] = threadPool.submit(callables[i]);
        }

        SupportMTUpdateListener[] listener = new SupportMTUpdateListener[numEvents];
        for (int i = 0; i < numEvents; i++) {
            EPStatement stmt = epService.getEPAdministrator().createPattern(pattern);
            listener[i] = new SupportMTUpdateListener();
            stmt.addListener(listener[i]);

            synchronized (sendLock) {
                sendLock.notifyAll();
            }
        }

        for (SendEventWaitCallable callable : callables) {
            callable.setShutdown(true);
        }
        synchronized (sendLock) {
            sendLock.notifyAll();
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numEvents; i++) {
            assertTrue(listener[i].assertOneGetNewAndReset().get("a") instanceof SupportBean);
        }
    }
}
