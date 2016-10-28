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
import com.espertech.esper.support.util.SupportMTUpdateListener;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Test for multithread-safety of insert-into and aggregation per group.
 */
public class TestMTStmtNamedWindowConsume extends TestCase
{
    private EPServiceProvider engine;
    private SupportMTUpdateListener listenerWindow;
    private SupportMTUpdateListener listenerConsumers[];

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getDefaultProvider(configuration);
        engine.initialize();
    }

    protected void tearDown() throws Exception {
        listenerWindow = null;
        listenerConsumers = null;
    }

    public void testNamedWindow() throws Exception
    {
        EPStatement stmtWindow = engine.getEPAdministrator().createEPL(
                "create window MyWindow#keepall() as select theString, longPrimitive from " + SupportBean.class.getName());
        listenerWindow = new SupportMTUpdateListener();
        stmtWindow.addListener(listenerWindow);

        engine.getEPAdministrator().createEPL(
                "insert into MyWindow(theString, longPrimitive) " +
                " select symbol, volume \n" +
                " from " + SupportMarketDataBean.class.getName());

        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " as s0 delete from MyWindow as win where win.theString = s0.id";
        engine.getEPAdministrator().createEPL(stmtTextDelete);

        trySend(4, 1000, 8);
    }

    private void trySend(int numThreads, int numRepeats, int numConsumers) throws Exception
    {
        listenerConsumers = new SupportMTUpdateListener[numConsumers];
        for (int i = 0; i < listenerConsumers.length; i++)
        {
            EPStatement stmtConsumer = engine.getEPAdministrator().createEPL("select theString, longPrimitive from MyWindow");
            listenerConsumers[i] = new SupportMTUpdateListener();
            stmtConsumer.addListener(listenerConsumers[i]);
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtNamedWindowConsumeCallable(Integer.toString(i), engine, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        // compute list of expected
        List<String> expectedIdsList = new ArrayList<String>();
        for (int i = 0; i < numThreads; i++)
        {
            expectedIdsList.addAll((List<String>) future[i].get());
        }
        String[] expectedIds = expectedIdsList.toArray(new String[0]);

        assertEquals(numThreads * numRepeats, listenerWindow.getNewDataList().size());  // old and new each

        // compute list of received
        for (int i = 0; i < listenerConsumers.length; i++)
        {
            EventBean[] newEvents = listenerConsumers[i].getNewDataListFlattened();
            String[] receivedIds = new String[newEvents.length];
            for (int j = 0; j < newEvents.length; j++)
            {
                receivedIds[j] = (String) newEvents[j].get("theString");
            }
            assertEquals(receivedIds.length, expectedIds.length);

            Arrays.sort(receivedIds);
            Arrays.sort(expectedIds);
            Arrays.deepEquals(expectedIds, receivedIds);
        }
    }
}
