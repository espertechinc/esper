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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtNamedWindowPriorityCallable;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of @priority and named windows.
 */
public class ExecMTStmtNamedWindowPriority implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.getEngineDefaults().getExecution().setPrioritized(true);
        configuration.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);
    }

    public void run(EPServiceProvider epService) throws Exception {

        EPStatement stmtWindow = epService.getEPAdministrator().createEPL("create window MyWindow#keepall as (c0 string, c1 string)");
        epService.getEPAdministrator().createEPL("insert into MyWindow select p00 as c0, p01 as c1 from SupportBean_S0");
        epService.getEPAdministrator().createEPL("@Priority(1) on SupportBean_S1 s1 merge MyWindow s0 where s1.p10 = c0 " +
                "when matched then update set c1 = s1.p11");
        epService.getEPAdministrator().createEPL("@Priority(0) on SupportBean_S1 s1 merge MyWindow s0 where s1.p10 = c0 " +
                "when matched then update set c1 = s1.p12");

        trySend(epService, stmtWindow, 4, 1000);
    }

    private void trySend(EPServiceProvider epService, EPStatement stmtWindow, int numThreads, int numRepeats) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtNamedWindowPriorityCallable(i, epService, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        for (int i = 0; i < numThreads; i++) {
            future[i].get();
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        EventBean[] events = EPAssertionUtil.iteratorToArray(stmtWindow.iterator());
        assertEquals(numThreads * numRepeats, events.length);
        for (int i = 0; i < events.length; i++) {
            String valueC1 = (String) events[i].get("c1");
            assertEquals("y", valueC1);
        }
    }
}
