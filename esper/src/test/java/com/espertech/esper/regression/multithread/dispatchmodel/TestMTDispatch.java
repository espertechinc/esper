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

package com.espertech.esper.regression.multithread.dispatchmodel;

import junit.framework.TestCase;
import com.espertech.esper.dispatch.DispatchService;
import com.espertech.esper.dispatch.DispatchServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Arrays;
import java.util.concurrent.*;

/**
 * A model for testing multithreaded dispatches to listeners.
 * <p>
 * Each thread in a loop:
 *   Next producer invoke
 *      Producer generates next integer
 *      Producer sends int[] {num, 0}
 */
public class TestMTDispatch extends TestCase
{
    private static final Logger log = LoggerFactory.getLogger(TestMTDispatch.class);

    public void testSceneOne() throws Exception
    {
        DispatchService dispatchService = new DispatchServiceImpl();
        DispatchListenerImpl listener = new DispatchListenerImpl();
        //UpdateDispatchViewModel updateDispatchView = new UpdateDispatchViewNonConcurrentModel(dispatchService, listener);
        UpdateDispatchViewOrderEnforcingModel updateDispatchView = new UpdateDispatchViewOrderEnforcingModel(dispatchService, listener);

        int numThreads = 2;
        int numActions = 10000;
        int ratioDoubleAdd = 5;
        // generates {(1,0),(2,0), (3,0)}

        trySend(numThreads, numActions, ratioDoubleAdd, updateDispatchView, dispatchService);

        // assert size
        List<int[][]> result = listener.getReceived();
        assertEquals(numActions * numThreads, result.size());

        // analyze result
        int[] nominals = new int[result.size()];
        for (int i = 0; i < result.size(); i++)
        {
            int[][] entry = result.get(i);
            //System.out.println("entry=" + print(entry));
            
            nominals[i] = entry[0][0];
            assertEquals("Order not correct: #" + i, (i + 1), nominals[i]);

            // Assert last digits and nominals, i.e. (1, 0) (1, 1), (1, 2)
            for (int j = 0; j < entry.length; j++)
            {
                assertEquals(nominals[i], entry[j][0]);
                assertEquals(j, entry[j][1]);
            }
        }
    }

    private void trySend(int numThreads, int numCount, int ratioDoubleAdd, UpdateDispatchViewModel updateDispatchView, DispatchService dispatchService) throws Exception
    {
        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        DispatchCallable callables[] = new DispatchCallable[numThreads];
        DispatchProducer producer = new DispatchProducer(updateDispatchView);
        for (int i = 0; i < numThreads; i++)
        {
            callables[i] = new DispatchCallable(producer, i, numCount, ratioDoubleAdd, updateDispatchView, dispatchService);
            future[i] = threadPool.submit(callables[i]);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue((Boolean) future[i].get());
        }
    }

    private String print(int[][] input)
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < input.length; i++)
        {
            buf.append(Arrays.toString(input[i]));
            buf.append(",");
        }
        return buf.toString();
    }

    public static class DispatchCallable implements Callable
    {
        private static final Logger log = LoggerFactory.getLogger(DispatchCallable.class);
        
        private DispatchProducer sharedProducer;
        private final int threadNum;
        private final int numActions;
        private final int ratioDoubleAdd;
        private final UpdateDispatchViewModel updateDispatchView;
        private final DispatchService dispatchService;

        public DispatchCallable(DispatchProducer sharedProducer, int threadNum, int numActions, int ratioDoubleAdd, UpdateDispatchViewModel updateDispatchView, DispatchService dispatchService)
        {
            this.sharedProducer = sharedProducer;
            this.threadNum = threadNum;
            this.numActions = numActions;
            this.ratioDoubleAdd = ratioDoubleAdd;
            this.updateDispatchView = updateDispatchView;
            this.dispatchService = dispatchService;
        }

        public Object call() throws Exception
        {
            log.info(".call Thread " + Thread.currentThread().getId() + " starting");
            for (int i = 0; i < numActions; i++)
            {
                if (i % 10000 == 1)
                {
                    log.info(".call Thread " + Thread.currentThread().getId() + " at " + i);
                }
                
                int nominal = sharedProducer.next();
                if (i % ratioDoubleAdd == 1)
                {
                    updateDispatchView.add(new int[] {nominal, 1});
                }
                dispatchService.dispatch();
            }
            log.info(".call Thread " + Thread.currentThread().getId() + " done");
            return true;
        }
    }
}
