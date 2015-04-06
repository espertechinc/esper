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

package com.espertech.esper.multithread;

import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.support.util.SupportMTUpdateListener;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.client.EventBean;

import java.util.concurrent.*;
import java.util.*;

/**
 * Test for multithread-safety of a time window -based statement.
 */
public class TestMTStmtTimeWindow extends TestCase
{
    private EPServiceProvider engine;
    private SupportMTUpdateListener listener;

    public void setUp()
    {
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        engine = EPServiceProviderManager.getProvider("TestMTStmtTimeWindow", config);
    }

    public void tearDown()
    {
        listener = null;
        engine.destroy();
    }

    public void testTimeWin() throws Exception
    {
        EPStatement stmt = engine.getEPAdministrator().createEPL(
                " select irstream intPrimitive, theString as key " +
                " from " + SupportBean.class.getName() + ".win:time(1 sec)");

        listener = new SupportMTUpdateListener();
        stmt.addListener(listener);

        trySend(10, 5000);
        trySend(6, 2000);
        trySend(2, 10000);
        trySend(3, 5000);
        trySend(5, 2500);
    }

    private void trySend(int numThreads, int numRepeats) throws Exception
    {
        // set time to 0
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new SendEventCallable(i, engine, new GeneratorIterator(numRepeats));
            future[i] = threadPool.submit(callable);
        }

        // Advance time window every 100 milliseconds for 1 second
        for (int i = 0; i < 10; i++)
        {
            engine.getEPRuntime().sendEvent(new CurrentTimeEvent(i * 1000));
            Thread.sleep(100);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue((Boolean) future[i].get());
        }

        // set time to a large value
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(10000000000L));

        // Assert results
        int totalExpected = numThreads * numRepeats;

        // assert new data
        EventBean[] resultNewData = listener.getNewDataListFlattened();
        assertEquals(totalExpected, resultNewData.length);
        HashMap<Integer, List<String>> resultsNewData = sortPerIntKey(resultNewData);
        assertResult(numRepeats, numThreads, resultsNewData);

        // assert old data
        EventBean[] resultOldData = listener.getOldDataListFlattened();
        assertEquals(totalExpected, resultOldData.length);
        HashMap<Integer, List<String>> resultsOldData = sortPerIntKey(resultOldData);
        assertResult(numRepeats, numThreads, resultsOldData);

        listener.reset();
    }

    private HashMap<Integer, List<String>> sortPerIntKey(EventBean[] result)
    {
        HashMap<Integer, List<String>> results = new LinkedHashMap<Integer, List<String>>();
        for (EventBean theEvent : result)
        {
            int count = (Integer) theEvent.get("intPrimitive");
            String key = (String) theEvent.get("key");

            List<String> entries = results.get(count);
            if (entries == null)
            {
                entries = new LinkedList<String>();
                results.put(count, entries);
            }
            entries.add(key);
        }
        return results;
    }

    // Each integer value must be there with 2 entries of the same value
    private void assertResult(int numRepeats, int numThreads, HashMap<Integer, List<String>> results)
    {
        for (int i = 0; i < numRepeats; i++)
        {
            List<String> values = results.get(i);
            assertEquals(numThreads, values.size());
            for (String value : values)
            {
                assertEquals(Integer.toString(i), value);
            }
        }
    }
}
