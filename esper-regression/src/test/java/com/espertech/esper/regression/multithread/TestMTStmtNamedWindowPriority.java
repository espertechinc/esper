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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.concurrent.*;

/**
 * Test for multithread-safety of @priority and named windows.
 */
public class TestMTStmtNamedWindowPriority extends TestCase
{
    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.getEngineDefaults().getExecution().setPrioritized(true);
        configuration.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);
        engine = EPServiceProviderManager.getDefaultProvider(configuration);
        engine.initialize();
    }

    public void testPriority() throws Exception
    {
        EPStatement stmtWindow = engine.getEPAdministrator().createEPL("create window MyWindow#keepall as (c0 string, c1 string)");
        engine.getEPAdministrator().createEPL("insert into MyWindow select p00 as c0, p01 as c1 from SupportBean_S0");
        engine.getEPAdministrator().createEPL("@Priority(1) on SupportBean_S1 s1 merge MyWindow s0 where s1.p10 = c0 " +
                "when matched then update set c1 = s1.p11");
        engine.getEPAdministrator().createEPL("@Priority(0) on SupportBean_S1 s1 merge MyWindow s0 where s1.p10 = c0 " +
                "when matched then update set c1 = s1.p12");

        trySend(stmtWindow, 4, 1000);
    }

    private void trySend(EPStatement stmtWindow, int numThreads, int numRepeats) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtNamedWindowPriorityCallable(i, engine, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        for (int i = 0; i < numThreads; i++)
        {
            future[i].get();
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        EventBean[] events = EPAssertionUtil.iteratorToArray(stmtWindow.iterator());
        assertEquals(numThreads * numRepeats, events.length);
        for (int i = 0; i < events.length; i++) {
            String valueC1 = (String) events[i].get("c1");
            assertEquals("y", valueC1);
        }
    }
}
