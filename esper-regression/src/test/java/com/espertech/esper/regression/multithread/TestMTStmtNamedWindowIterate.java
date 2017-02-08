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

import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

import java.util.concurrent.*;

/**
 * Test for multithread-safety of insert-into and aggregation per group.
 */
public class TestMTStmtNamedWindowIterate extends TestCase
{
    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getDefaultProvider(configuration);
        engine.initialize();

        engine.getEPAdministrator().createEPL(
                "create window MyWindow#groupwin(theString)#keepall as select theString, longPrimitive from " + SupportBean.class.getName());

        engine.getEPAdministrator().createEPL(
                "insert into MyWindow(theString, longPrimitive) " +
                " select symbol, volume \n" +
                " from " + SupportMarketDataBean.class.getName());
    }

    public void test4Threads() throws Exception
    {
        tryIterate(4, 250);
    }

    public void test2Threads() throws Exception
    {
        tryIterate(2, 500);
    }

    private void tryIterate(int numThreads, int numRepeats) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future<Boolean> future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtNamedWindowIterateCallable(Integer.toString(i), engine, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue(future[i].get());
        }
    }
}
