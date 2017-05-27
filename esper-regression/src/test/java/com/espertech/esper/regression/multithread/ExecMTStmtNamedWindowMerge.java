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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtNamedWindowMergeCallable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety and named window updates.
 */
public class ExecMTStmtNamedWindowMerge implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        trySend(epService, 3, 100);
        epService.getEPAdministrator().destroyAllStatements();
        trySend(epService, 2, 1000);
    }

    private void trySend(EPServiceProvider epService, int numThreads, int numEventsPerThread) throws Exception {
        // setup statements
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean sb " +
                "merge MyWindow nw where nw.theString = sb.theString " +
                " when not matched then insert select * " +
                " when matched then update set intPrimitive = nw.intPrimitive + 1");

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new StmtNamedWindowMergeCallable(epService, numEventsPerThread));
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        // total up result
        for (int i = 0; i < numThreads; i++) {
            Boolean result = future[i].get();
            assertTrue(result);
        }

        // compare
        EventBean[] rows = epService.getEPRuntime().executeQuery("select * from MyWindow").getArray();
        assertEquals(numEventsPerThread, rows.length);
        for (EventBean row : rows) {
            assertEquals(numThreads - 1, row.get("intPrimitive"));
        }
        //long deltaTime = endTime - startTime;
        //System.out.println("Totals updated: " + totalUpdates + "  Delta cumu: " + deltaCumulative + "  Delta pooled: " + deltaTime);
    }
}
