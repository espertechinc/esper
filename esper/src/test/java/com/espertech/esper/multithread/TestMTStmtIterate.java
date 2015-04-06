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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.concurrent.*;

/**
 * Test for multithread-safety (or lack thereof) for iterators: iterators fail with concurrent mods as expected behavior
 */
public class TestMTStmtIterate extends TestCase
{
    private EPServiceProvider engine;

    public void tearDown()
    {
        engine.destroy();
    }

    public void testIteratorSingleStmt() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getProvider("TestMTStmtIterate", config);

        EPStatement stmt[] = new EPStatement[] {engine.getEPAdministrator().createEPL(
                " select theString from " + SupportBean.class.getName() + ".win:time(5 min)")};

        trySend(2, 10, stmt);
    }

    public void testIteratorMultiStmtNoViewShare() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setShareViews(false);
        engine = EPServiceProviderManager.getProvider("TestMTStmtIterate", config);

        EPStatement stmt[] = new EPStatement[3];
        for (int i = 0; i < stmt.length; i++)
        {
            String name = "Stmt_" + i;
            String stmtText = "@Name('" + name + "') select theString from " + SupportBean.class.getName() + ".win:time(5 min)";
            stmt[i] = engine.getEPAdministrator().createEPL(stmtText);
        }

        trySend(4, 10, stmt);
    }

    public void testIteratorMultiStmtViewShare() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setShareViews(true);
        engine = EPServiceProviderManager.getProvider("TestMTStmtIterate", config);

        EPStatement stmt[] = new EPStatement[3];
        for (int i = 0; i < stmt.length; i++)
        {
            String name = "Stmt_" + i;
            String stmtText = "@Name('" + name + "') select theString from " + SupportBean.class.getName() + ".win:time(5 min)";
            stmt[i] = engine.getEPAdministrator().createEPL(stmtText);
        }

        trySend(4, 10, stmt);
    }

    private void trySend(int numThreads, int numRepeats, EPStatement stmt[]) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtIterateCallable(i, engine, stmt, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(5, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue((Boolean) future[i].get());
        }
    }
}
