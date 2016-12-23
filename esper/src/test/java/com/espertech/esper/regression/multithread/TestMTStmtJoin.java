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
 * Test for multithread-safety for joins.
 */
public class TestMTStmtJoin extends TestCase
{
    private EPServiceProvider engine;

    private final static String EVENT_NAME = SupportBean.class.getName();

    public void setUp()
    {
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        engine = EPServiceProviderManager.getProvider("TestMTStmtJoin", config);
    }

    public void tearDown()
    {
        engine.destroy();
    }

    public void testJoin() throws Exception
    {
        EPStatement stmt = engine.getEPAdministrator().createEPL("select istream * \n" +
                "  from " + EVENT_NAME + "(theString='s0')#length(1000000) as s0,\n" +
                "       " + EVENT_NAME + "(theString='s1')#length(1000000) as s1\n" +
                "where s0.longPrimitive = s1.longPrimitive\n"
                );
        trySendAndReceive(4, stmt, 1000);
        trySendAndReceive(2, stmt, 2000);
    }

    private void trySendAndReceive(int numThreads, EPStatement statement, int numRepeats) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtJoinCallable(i, engine, statement, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue("Failed in " + statement.getText(), (Boolean) future[i].get());
        }
    }
}
