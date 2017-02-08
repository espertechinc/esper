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
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;

import java.util.concurrent.*;
import java.util.*;

/**
 * Test for multithread-safety of insert-into and aggregation per group.
 */
public class TestMTStmtNamedWindowDelete extends TestCase
{
    private EPServiceProvider engine;
    private SupportMTUpdateListener listenerWindow;
    private SupportMTUpdateListener listenerConsumer;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getDefaultProvider(configuration);
        engine.initialize();
    }

    protected void tearDown() throws Exception {
        listenerWindow = null;
        listenerConsumer = null;
    }

    public void testNamedWindow() throws Exception
    {
        EPStatement stmtWindow = engine.getEPAdministrator().createEPL(
                "create window MyWindow#keepall as select theString, longPrimitive from " + SupportBean.class.getName());
        listenerWindow = new SupportMTUpdateListener();
        stmtWindow.addListener(listenerWindow);

        engine.getEPAdministrator().createEPL(
                "insert into MyWindow(theString, longPrimitive) " +
                " select symbol, volume \n" +
                " from " + SupportMarketDataBean.class.getName());

        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " as s0 delete from MyWindow as win where win.theString = s0.id";
        engine.getEPAdministrator().createEPL(stmtTextDelete);

        EPStatement stmtConsumer = engine.getEPAdministrator().createEPL("select irstream theString, longPrimitive from MyWindow");
        listenerConsumer = new SupportMTUpdateListener();
        stmtConsumer.addListener(listenerConsumer);

        trySend(4, 1000);
    }

    private void trySend(int numThreads, int numRepeats) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtNamedWindowDeleteCallable(Integer.toString(i), engine, numRepeats);
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

        assertEquals(2 * numThreads * numRepeats, listenerWindow.getNewDataList().size());  // old and new each
        assertEquals(2 * numThreads * numRepeats, listenerConsumer.getNewDataList().size());  // old and new each

        // compute list of received
        EventBean[] newEvents = listenerWindow.getNewDataListFlattened();
        String[] receivedIds = new String[newEvents.length];
        for (int i = 0; i < newEvents.length; i++)
        {
            receivedIds[i] = (String) newEvents[i].get("theString");
        }
        assertEquals(receivedIds.length, expectedIds.length);

        Arrays.sort(receivedIds);
        Arrays.sort(expectedIds);
        Arrays.deepEquals(expectedIds, receivedIds);
    }
}
