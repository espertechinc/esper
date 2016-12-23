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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;
import junit.framework.TestCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Test for pattern statement parallel execution by threads.
 */
public class TestMTStmtPattern extends TestCase
{
    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(Configuration.EventRepresentation.MAP); // use Map-type events for testing
        engine = EPServiceProviderManager.getDefaultProvider(configuration);
        engine.initialize();
    }

    public void testPattern() throws Exception
    {
        String type = SupportBean.class.getName();

        String pattern = "a=" + type;
        tryPattern(pattern, 4, 20);

        pattern = "a=" + type + " or a=" + type;
        tryPattern(pattern, 2, 20);
    }

    private void tryPattern(String pattern, int numThreads, int numEvents) throws Exception
    {
        Object sendLock = new Object();
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        SendEventWaitCallable[] callables = new SendEventWaitCallable[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            callables[i] = new SendEventWaitCallable(i, engine, sendLock, new GeneratorIterator(numEvents));
            future[i] = threadPool.submit(callables[i]);
        }

        SupportMTUpdateListener listener[] = new SupportMTUpdateListener[numEvents];
        for (int i = 0; i < numEvents; i++)
        {
            EPStatement stmt = engine.getEPAdministrator().createPattern(pattern);
            listener[i] = new SupportMTUpdateListener();
            stmt.addListener(listener[i]);

            synchronized(sendLock)
            {
                sendLock.notifyAll();
            }
        }

        for (SendEventWaitCallable callable : callables)
        {
            callable.setShutdown(true);
        }        
        synchronized(sendLock)
        {
            sendLock.notifyAll();
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numEvents; i++)
        {
            assertTrue(listener[i].assertOneGetNewAndReset().get("a") instanceof SupportBean);
        }
    }
}
