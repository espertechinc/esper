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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtListenerRouteCallable;

import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for update listeners that create and stop statements.
 */
public class ExecMTStmtListenerCreateStmt implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                " select * from " + SupportBean.class.getName());

        tryListener(epService, 2, 100, stmt);
    }

    private void tryListener(EPServiceProvider epService, int numThreads, int numRepeats, EPStatement stmt) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtListenerRouteCallable(i, epService, stmt, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }
    }
}
