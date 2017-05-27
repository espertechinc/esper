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
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtListenerAddRemoveCallable;

import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety for adding and removing listener.
 */
public class ExecMTStmtListenerAddRemove implements RegressionExecution {
    private final static String EVENT_NAME = SupportMarketDataBean.class.getName();

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setListenerDispatchTimeout(Long.MAX_VALUE);
    }

    public void run(EPServiceProvider epService) throws Exception {
        int numThreads = 2;

        EPStatement stmt = epService.getEPAdministrator().createPattern("every a=" + EVENT_NAME + "(symbol='IBM')");
        tryStatementListenerAddRemove(epService, numThreads, stmt, false, 10000);
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select * from " + EVENT_NAME + " (symbol='IBM', feed='RT')");
        tryStatementListenerAddRemove(epService, numThreads, stmt, true, 10000);
        stmt.destroy();
    }

    private void tryStatementListenerAddRemove(EPServiceProvider epService, int numThreads, EPStatement statement, boolean isEPL, int numRepeats) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtListenerAddRemoveCallable(epService, statement, isEPL, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue("Failed stmt=" + statement.getText(), (Boolean) future[i].get());
        }
    }
}
