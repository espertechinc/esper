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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Test for multithread-safety of insert-into and aggregation per group.
 */
public class TestMTStmtInsertInto extends TestCase
{
    private EPServiceProvider engine;
    private SupportMTUpdateListener listener;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getDefaultProvider(configuration);
        engine.initialize();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testInsertInto() throws Exception
    {
        engine.getEPAdministrator().createEPL(
                "insert into XStream " +
                " select theString as key, count(*) as mycount\n" +
                " from " + SupportBean.class.getName() + "#time(5 min)" +
                " group by theString"
                );
        engine.getEPAdministrator().createEPL(
                "insert into XStream " +
                " select symbol as key, count(*) as mycount\n" +
                " from " + SupportMarketDataBean.class.getName() + "#time(5 min)" +
                " group by symbol"
                );
        
        EPStatement stmtConsolidated = engine.getEPAdministrator().createEPL("select key, mycount from XStream");
        listener = new SupportMTUpdateListener();
        stmtConsolidated.addListener(listener);

        trySend(10, 5000);
        trySend(4, 10000);
    }

    private void trySend(int numThreads, int numRepeats) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtInsertIntoCallable(Integer.toString(i), engine, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue((Boolean) future[i].get());
        }

        // Assert results
        int totalExpected = numThreads * numRepeats * 2;
        EventBean[] result = listener.getNewDataListFlattened();
        assertEquals(totalExpected, result.length);
        HashMap<Long, Set<String>> results = new LinkedHashMap<Long, Set<String>>();
        for (EventBean theEvent : result)
        {
            long count = (Long) theEvent.get("mycount");
            String key = (String) theEvent.get("key");

            Set<String> entries = results.get(count);
            if (entries == null)
            {
                entries = new HashSet<String>();
                results.put(count, entries);
            }
            entries.add(key);
        }

        assertEquals(numRepeats, results.size());
        for (Set<String> value : results.values())
        {
            assertEquals(2 * numThreads, value.size());
            for (int i = 0; i < numThreads; i++)
            {
                assertTrue(value.contains("E1_" + i));
                assertTrue(value.contains("E2_" + i));
            }            
        }

        listener.reset();
    }
}
