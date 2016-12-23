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

package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Test for multithread-safety and named window subqueries and aggregation.
 */
public class TestMTStmtNamedWindowSubqueryAgg extends TestCase
{
    private static final Logger log = LoggerFactory.getLogger(TestMTStmtNamedWindowSubqueryAgg.class);

    private EPServiceProvider engine;

    public void tearDown()
    {
        engine.initialize();
    }

    public void testConcurrentSubqueryIndexNoShare() throws Exception
    {
        trySend(3, 1000, false);
    }

    public void testConcurrentSubqueryIndexShare() throws Exception
    {
        trySend(3, 1000, true);
    }

    private void trySend(int numThreads, int numEventsPerThread, boolean indexShare) throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.addPlugInAggregationFunctionFactory("intListAgg", MyIntListAggregationFactory.class.getName());
        config.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(Configuration.EventRepresentation.MAP); // use Map-type events for testing
        engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();

        // setup statements
        engine.getEPAdministrator().createEPL("create schema UpdateEvent as (uekey string, ueint int)");
        engine.getEPAdministrator().createEPL("create schema WindowSchema as (wskey string, wsint int)");

        String createEpl = "create window MyWindow#keepall as WindowSchema";
        if (indexShare) {
            createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
        }
        EPStatement namedWindow = engine.getEPAdministrator().createEPL(createEpl);
        
        engine.getEPAdministrator().createEPL("create index ABC on MyWindow(wskey)");
        engine.getEPAdministrator().createEPL("on UpdateEvent mue merge MyWindow mw " +
                "where uekey = wskey and ueint = wsint " +
                "when not matched then insert select uekey as wskey, ueint as wsint " +
                "when matched then delete");
        // note: here all threads use the same string key to insert/delete and different values for the int
        EPStatement targetStatement = engine.getEPAdministrator().createEPL("select (select intListAgg(wsint) from MyWindow mw where wskey = sb.theString) as val from SupportBean sb");

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future<Boolean> future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            future[i] = threadPool.submit(new StmtNamedWindowSubqueryAggCallable(i, engine, numEventsPerThread, targetStatement));
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        // total up result
        for (int i = 0; i < numThreads; i++)
        {
            Boolean result = future[i].get();
            assertTrue(result);
        }
        
        EventBean[] events = EPAssertionUtil.iteratorToArray(namedWindow.iterator());
        assertEquals(0, events.length);
    }
}
