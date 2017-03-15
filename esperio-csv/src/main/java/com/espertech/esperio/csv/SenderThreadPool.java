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
package com.espertech.esperio.csv;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of Sender to send to esper engine using a thread pool
 *
 * @author Jerry Shea
 */
public class SenderThreadPool extends DirectSender {

    private ExecutorService executorService;

    /**
     * Ctor.
     *
     * @param threadPoolSize size of pool
     */
    public SenderThreadPool(int threadPoolSize) {
        executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Ctor.
     *
     * @param executorService threadpool to use
     */
    public SenderThreadPool(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Send an event.
     *
     * @param beanToSend event to send
     */
    public void sendEvent(final Object beanToSend) {
        executorService.execute(new Runnable() {
            public void run() {
                runtime.sendEvent(beanToSend);
            }
        });
    }

    /**
     * Send an event.
     *
     * @param mapToSend     event to send
     * @param eventTypeName name of event type
     */
    public void sendEvent(final Map mapToSend, final String eventTypeName) {
        executorService.execute(new Runnable() {
            public void run() {
                runtime.sendEvent(mapToSend, eventTypeName);
            }
        });
    }
}
