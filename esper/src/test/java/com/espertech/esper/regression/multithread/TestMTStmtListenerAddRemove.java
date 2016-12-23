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

import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;

import java.util.concurrent.*;

/**
 * Test for multithread-safety for adding and removing listener.
 */
public class TestMTStmtListenerAddRemove extends TestCase
{
    private EPServiceProvider engine;

    private final static String EVENT_NAME = SupportMarketDataBean.class.getName();

    public void setUp()
    {
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setListenerDispatchTimeout(Long.MAX_VALUE);
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        engine = EPServiceProviderManager.getProvider("TestMTStmtListenerAddRemove", config);
    }

    public void tearDown()
    {
        engine.initialize();
    }

    public void testPatterns() throws Exception
    {
        EPStatement stmt = engine.getEPAdministrator().createPattern("every a=" + EVENT_NAME + "(symbol='IBM')");
        int numThreads = 2;
        tryStatementListenerAddRemove(numThreads, stmt, false, 10000);
    }

    public void testEPL() throws Exception
    {
        EPStatement stmt = engine.getEPAdministrator().createEPL("select * from " + EVENT_NAME + " (symbol='IBM', feed='RT')");
        int numThreads = 2;
        tryStatementListenerAddRemove(numThreads, stmt, true, 10000);
    }

    private void tryStatementListenerAddRemove(int numThreads, EPStatement statement, boolean isEPL, int numRepeats) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtListenerAddRemoveCallable(engine, statement, isEPL, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue("Failed stmt=" + statement.getText(), (Boolean) future[i].get());
        }
    }
}
