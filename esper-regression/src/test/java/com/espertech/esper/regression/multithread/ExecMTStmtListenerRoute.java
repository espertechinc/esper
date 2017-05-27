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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtListenerCreateStmtCallable;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for update listeners that route events.
 */
public class ExecMTStmtListenerRoute implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        tryListener(epService, 4, 500);
    }

    private void tryListener(EPServiceProvider epService, int numThreads, int numRoutes) throws Exception {
        EPStatement stmtTrigger = epService.getEPAdministrator().createEPL(
                " select * from " + SupportBean.class.getName());

        EPStatement stmtListen = epService.getEPAdministrator().createEPL(
                " select * from " + SupportMarketDataBean.class.getName());
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        stmtListen.addListener(listener);

        // Set of events routed by each listener
        Set<SupportMarketDataBean> routed = Collections.synchronizedSet(new HashSet<SupportMarketDataBean>());

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtListenerCreateStmtCallable(i, epService, stmtTrigger, numRoutes, routed);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        // assert
        EventBean[] results = listener.getNewDataListFlattened();
        assertTrue(results.length >= numThreads * numRoutes);

        for (SupportMarketDataBean routedEvent : routed) {
            boolean found = false;
            for (int i = 0; i < results.length; i++) {
                if (results[i].getUnderlying() == routedEvent) {
                    found = true;
                }
            }
            assertTrue(found);
        }
    }
}
