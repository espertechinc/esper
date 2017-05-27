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
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtNamedWindowSubqueryLookupCallable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety and named window subqueries and direct index-based lookup.
 */
public class ExecMTStmtNamedWindowSubqueryLookup implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(EventUnderlyingType.MAP); // use Map-type events for testing
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        trySend(epService, 3, 10000);
    }

    private void trySend(EPServiceProvider epService, int numThreads, int numEventsPerThread) throws Exception {
        // setup statements
        epService.getEPAdministrator().createEPL("create schema MyUpdateEvent as (key string, intupd int)");
        epService.getEPAdministrator().createEPL("create schema MySchema as (theString string, intval int)");
        EPStatement namedWindow = epService.getEPAdministrator().createEPL("create window MyWindow#keepall as MySchema");
        epService.getEPAdministrator().createEPL("on MyUpdateEvent mue merge MyWindow mw " +
                "where mw.theString = mue.key " +
                "when not matched then insert select key as theString, intupd as intval " +
                "when matched then delete");
        EPStatement targetStatement = epService.getEPAdministrator().createEPL("select (select intval from MyWindow mw where mw.theString = sb.theString) as val from SupportBean sb");

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new StmtNamedWindowSubqueryLookupCallable(i, epService, numEventsPerThread, targetStatement));
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        // total up result
        for (int i = 0; i < numThreads; i++) {
            Boolean result = future[i].get();
            assertTrue(result);
        }

        EventBean[] events = EPAssertionUtil.iteratorToArray(namedWindow.iterator());
        assertEquals(0, events.length);
    }
}
