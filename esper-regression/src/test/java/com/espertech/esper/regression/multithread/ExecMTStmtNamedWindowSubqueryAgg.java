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
import com.espertech.esper.supportregression.multithread.MyIntListAggregationFactory;
import com.espertech.esper.supportregression.multithread.StmtNamedWindowSubqueryAggCallable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety and named window subqueries and aggregation.
 */
public class ExecMTStmtNamedWindowSubqueryAgg implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addPlugInAggregationFunctionFactory("intListAgg", MyIntListAggregationFactory.class.getName());
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(EventUnderlyingType.MAP); // use Map-type events for testing
    }

    public void run(EPServiceProvider epService) throws Exception {
        trySend(epService, 3, 1000, false);
        epService.getEPAdministrator().destroyAllStatements();
        trySend(epService, 3, 1000, true);
    }

    private void trySend(EPServiceProvider epService, int numThreads, int numEventsPerThread, boolean indexShare) throws Exception {
        // setup statements
        epService.getEPAdministrator().createEPL("create schema UpdateEvent as (uekey string, ueint int)");
        epService.getEPAdministrator().createEPL("create schema WindowSchema as (wskey string, wsint int)");

        String createEpl = "create window MyWindow#keepall as WindowSchema";
        if (indexShare) {
            createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
        }
        EPStatement namedWindow = epService.getEPAdministrator().createEPL(createEpl);

        epService.getEPAdministrator().createEPL("create index ABC on MyWindow(wskey)");
        epService.getEPAdministrator().createEPL("on UpdateEvent mue merge MyWindow mw " +
                "where uekey = wskey and ueint = wsint " +
                "when not matched then insert select uekey as wskey, ueint as wsint " +
                "when matched then delete");
        // note: here all threads use the same string key to insert/delete and different values for the int
        EPStatement targetStatement = epService.getEPAdministrator().createEPL("select (select intListAgg(wsint) from MyWindow mw where wskey = sb.theString) as val from SupportBean sb");

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new StmtNamedWindowSubqueryAggCallable(i, epService, numEventsPerThread, targetStatement));
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
