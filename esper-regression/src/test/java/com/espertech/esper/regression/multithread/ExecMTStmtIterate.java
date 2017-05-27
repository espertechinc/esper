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
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtIterateCallable;

import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety (or lack thereof) for iterators: iterators fail with concurrent mods as expected behavior
 */
public class ExecMTStmtIterate implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionIteratorSingleStmt(epService);
        runAssertionIteratorMultiStmtNoViewShare();
        runAssertionIteratorMultiStmtViewShare();
    }

    private void runAssertionIteratorSingleStmt(EPServiceProvider epService) throws Exception {
        EPStatement[] stmt = new EPStatement[]{epService.getEPAdministrator().createEPL(
                " select theString from " + SupportBean.class.getName() + "#time(5 min)")};

        trySend(epService, 2, 10, stmt);
    }

    private void runAssertionIteratorMultiStmtNoViewShare() throws Exception {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setShareViews(false);
        EPServiceProvider engine = EPServiceProviderManager.getProvider(ExecMTStmtIterate.class.getSimpleName(), config);

        EPStatement[] stmt = new EPStatement[3];
        for (int i = 0; i < stmt.length; i++) {
            String name = "Stmt_" + i;
            String stmtText = "@Name('" + name + "') select theString from " + SupportBean.class.getName() + "#time(5 min)";
            stmt[i] = engine.getEPAdministrator().createEPL(stmtText);
        }

        trySend(engine, 4, 10, stmt);

        engine.destroy();
    }

    private void runAssertionIteratorMultiStmtViewShare() throws Exception {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setShareViews(true);
        EPServiceProvider engine = EPServiceProviderManager.getProvider(ExecMTStmtIterate.class.getSimpleName(), config);

        EPStatement[] stmt = new EPStatement[3];
        for (int i = 0; i < stmt.length; i++) {
            String name = "Stmt_" + i;
            String stmtText = "@Name('" + name + "') select theString from " + SupportBean.class.getName() + "#time(5 min)";
            stmt[i] = engine.getEPAdministrator().createEPL(stmtText);
        }

        trySend(engine, 4, 10, stmt);

        engine.destroy();
    }

    private void trySend(EPServiceProvider epService, int numThreads, int numRepeats, EPStatement[] stmt) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtIterateCallable(i, epService, stmt, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(5, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }
    }
}
