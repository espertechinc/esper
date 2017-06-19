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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtSharedViewCallable;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety of statements that are very similar that is share the same filter and views.
 * <p>
 * The engine shares locks between statements that share filters and views.
 */
public class ExecMTStmtSharedView implements RegressionExecution {
    private final static String[] SYMBOLS = {"IBM", "MSFT", "GE"};

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        configuration.getEngineDefaults().getViewResources().setShareViews(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        trySend(epService, 4, 500, 100);
    }

    private void trySend(EPServiceProvider epService, int numThreads, int numRepeats, int numStatements) throws Exception {
        // Create same statement X times
        EPStatement[] stmt = new EPStatement[numStatements];
        SupportMTUpdateListener[] listeners = new SupportMTUpdateListener[stmt.length];
        for (int i = 0; i < stmt.length; i++) {
            stmt[i] = epService.getEPAdministrator().createEPL(
                    " select * " +
                            " from " + SupportMarketDataBean.class.getName() + "#groupwin(symbol)#uni(price)");
            listeners[i] = new SupportMTUpdateListener();
            stmt[i].addListener(listeners[i]);
        }

        // Start send threads
        // Each threads sends each symbol with price = 0 to numRepeats
        long startTime = System.currentTimeMillis();
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtSharedViewCallable(numRepeats, epService, SYMBOLS);
            future[i] = threadPool.submit(callable);
        }

        // Shut down
        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);
        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("delta=" + delta + " not less then 5 sec", delta < 5000);   // should take less then 5 seconds even for 100 statements as they need to share resources thread-safely

        // Assert results
        for (SupportMTUpdateListener listener : listeners) {
            assertEquals(numRepeats * numThreads * SYMBOLS.length, listener.getNewDataList().size());
            EventBean[] newDataLast = listener.getNewDataList().get(listener.getNewDataList().size() - 1);
            assertEquals(1, newDataLast.length);
            EventBean result = newDataLast[0];
            assertEquals(numRepeats * numThreads, ((Long) result.get("datapoints")).longValue());
            assertTrue(Arrays.asList(SYMBOLS).contains(result.get("symbol")));
            assertEquals(sumToN(numRepeats) * numThreads, result.get("total"));
            listener.reset();
        }

        for (int i = 0; i < stmt.length; i++) {
            stmt[i].stop();
        }
    }

    private double sumToN(int n) {
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += i;
        }
        return sum;
    }

    private final static Logger log = LoggerFactory.getLogger(ExecMTStmtSharedView.class);
}
