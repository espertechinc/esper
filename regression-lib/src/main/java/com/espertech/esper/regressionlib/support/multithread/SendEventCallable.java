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
package com.espertech.esper.regressionlib.support.multithread;

import com.espertech.esper.runtime.client.EPRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.Callable;

public class SendEventCallable implements Callable<Boolean> {
    private final int threadNum;
    private final EPRuntime runtime;
    private final Iterator<Object> events;

    public SendEventCallable(int threadNum, EPRuntime runtime, Iterator<Object> events) {
        this.threadNum = threadNum;
        this.runtime = runtime;
        this.events = events;
    }

    public Boolean call() throws Exception {
        log.info(".call Thread " + Thread.currentThread().getId() + " starting");
        try {
            while (events.hasNext()) {
                Object event = events.next();
                runtime.getEventService().sendEventBean(event, event.getClass().getSimpleName());
            }
        } catch (RuntimeException ex) {
            log.error("Error in thread " + threadNum, ex);
            return false;
        }
        log.info(".call Thread " + Thread.currentThread().getId() + " done");
        return true;
    }

    private static final Logger log = LoggerFactory.getLogger(SendEventCallable.class);
}
