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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.GeneratorIterator;
import com.espertech.esper.supportregression.multithread.SendEventCallable;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety of a time window -based statement.
 */
public class ExecMTStmtTimeWindow implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                " select irstream intPrimitive, theString as key " +
                        " from " + SupportBean.class.getName() + "#time(1 sec)");

        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        stmt.addListener(listener);

        trySend(epService, listener, 10, 5000);
        trySend(epService, listener, 6, 2000);
        trySend(epService, listener, 2, 10000);
        trySend(epService, listener, 3, 5000);
        trySend(epService, listener, 5, 2500);
    }

    private void trySend(EPServiceProvider epService, SupportMTUpdateListener listener, int numThreads, int numRepeats) throws Exception {
        // set time to 0
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new SendEventCallable(i, epService, new GeneratorIterator(numRepeats));
            future[i] = threadPool.submit(callable);
        }

        // Advance time window every 100 milliseconds for 1 second
        for (int i = 0; i < 10; i++) {
            epService.getEPRuntime().sendEvent(new CurrentTimeEvent(i * 1000));
            Thread.sleep(100);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        // set time to a large value
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000000000L));

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

    private HashMap<Integer, List<String>> sortPerIntKey(EventBean[] result) {
        HashMap<Integer, List<String>> results = new LinkedHashMap<Integer, List<String>>();
        for (EventBean theEvent : result) {
            int count = (Integer) theEvent.get("intPrimitive");
            String key = (String) theEvent.get("key");

            List<String> entries = results.get(count);
            if (entries == null) {
                entries = new LinkedList<String>();
                results.put(count, entries);
            }
            entries.add(key);
        }
        return results;
    }

    // Each integer value must be there with 2 entries of the same value
    private void assertResult(int numRepeats, int numThreads, HashMap<Integer, List<String>> results) {
        for (int i = 0; i < numRepeats; i++) {
            List<String> values = results.get(i);
            assertEquals(numThreads, values.size());
            for (String value : values) {
                assertEquals(Integer.toString(i), value);
            }
        }
    }
}
