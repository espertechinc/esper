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
import com.espertech.esper.supportregression.bean.SupportBean;

import java.util.concurrent.*;

/**
 * Test for update listeners that create and stop statements.
 *
 */
public class TestMTStmtListenerCreateStmt extends TestCase
{
    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        engine = EPServiceProviderManager.getProvider("TestMTStmtListenerCreateStmt", config);
    }

    public void tearDown()
    {
        engine.destroy();
    }

    public void testListenerCreateStmt() throws Exception
    {
        EPStatement stmt = engine.getEPAdministrator().createEPL(
                " select * from " + SupportBean.class.getName());

        tryListener(2, 100, stmt);
    }

    private void tryListener(int numThreads, int numRepeats, EPStatement stmt) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtListenerRouteCallable(i, engine, stmt, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue((Boolean) future[i].get());
        }
    }
}
