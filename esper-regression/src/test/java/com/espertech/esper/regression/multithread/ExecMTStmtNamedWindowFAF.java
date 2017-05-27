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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtNamedWindowQueryCallable;

import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety of named windows and fire-and-forget queries.
 */
public class ExecMTStmtNamedWindowFAF implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL(
                "create window MyWindow#keepall as select theString, longPrimitive from " + SupportBean.class.getName());

        epService.getEPAdministrator().createEPL(
                "insert into MyWindow(theString, longPrimitive) " +
                        " select symbol, volume \n" +
                        " from " + SupportMarketDataBean.class.getName());

        tryIterate(epService, 2, 500);
    }

    private void tryIterate(EPServiceProvider epService, int numThreads, int numRepeats) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtNamedWindowQueryCallable(Integer.toString(i), epService, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        Thread.sleep(100);
        for (int i = 0; i < numThreads; i++) {
            assertTrue(future[i].get(10, TimeUnit.SECONDS));
        }
    }
}
