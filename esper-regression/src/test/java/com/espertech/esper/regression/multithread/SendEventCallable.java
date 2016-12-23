/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.EPServiceProvider;

import java.util.Iterator;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendEventCallable implements Callable
{
    private final int threadNum;
    private final EPServiceProvider engine;
    private final Iterator<Object> events;

    public SendEventCallable(int threadNum, EPServiceProvider engine, Iterator<Object> events)
    {
        this.threadNum = threadNum;
        this.engine = engine;
        this.events = events;
    }

    public Object call() throws Exception
    {
        log.debug(".call Thread " + Thread.currentThread().getId() + " starting");
        try
        {
            while (events.hasNext())
            {
                Object event = events.next();
                engine.getEPRuntime().sendEvent(event);
            }
        }
        catch (RuntimeException ex)
        {
            log.error("Error in thread " + threadNum, ex);
            return false;
        }
        log.debug(".call Thread " + Thread.currentThread().getId() + " done");
        return true;
    }

    private static final Logger log = LoggerFactory.getLogger(SendEventCallable.class);            
}
