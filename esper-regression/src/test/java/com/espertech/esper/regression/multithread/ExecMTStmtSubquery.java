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
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtSubqueryCallable;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety of a lookup statement.
 */
public class ExecMTStmtSubquery implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        trySend(epService, 4, 10000);
        trySend(epService, 3, 10000);
        trySend(epService, 2, 10000);
    }

    private void trySend(EPServiceProvider epService, int numThreads, int numRepeats) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select (select id from S0#length(1000000) where id = s1.id) as value from S1 as s1");

        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        stmt.addListener(listener);

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtSubqueryCallable(i, epService, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        // Assert results
        int totalExpected = numThreads * numRepeats;

        // assert new data
        EventBean[] resultNewData = listener.getNewDataListFlattened();
        assertEquals(totalExpected, resultNewData.length);

        Set<Integer> values = new HashSet<Integer>();
        for (EventBean theEvent : resultNewData) {
            values.add((Integer) theEvent.get("value"));
        }
        assertEquals("Unexpected duplicates", totalExpected, values.size());

        listener.reset();
        stmt.stop();
    }
}
