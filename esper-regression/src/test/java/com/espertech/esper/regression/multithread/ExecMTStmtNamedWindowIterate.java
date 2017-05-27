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
import com.espertech.esper.supportregression.multithread.StmtNamedWindowIterateCallable;

import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety of insert-into and aggregation per group.
 */
public class ExecMTStmtNamedWindowIterate implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        setupStmts(epService);
        tryIterate(epService, 4, 250);
        epService.getEPAdministrator().destroyAllStatements();

        setupStmts(epService);
        tryIterate(epService, 2, 500);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void setupStmts(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL(
                "create window MyWindow#groupwin(theString)#keepall as select theString, longPrimitive from " + SupportBean.class.getName());

        epService.getEPAdministrator().createEPL(
                "insert into MyWindow(theString, longPrimitive) " +
                        " select symbol, volume \n" +
                        " from " + SupportMarketDataBean.class.getName());
    }

    private void tryIterate(EPServiceProvider epService, int numThreads, int numRepeats) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtNamedWindowIterateCallable(Integer.toString(i), epService, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue(future[i].get());
        }
    }
}
