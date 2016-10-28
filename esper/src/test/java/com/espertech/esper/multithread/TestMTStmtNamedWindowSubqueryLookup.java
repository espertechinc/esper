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
 * Test for multithread-safety and named window subqueries and direct index-based lookup.
 */
public class TestMTStmtNamedWindowSubqueryLookup extends TestCase
{
    private static final Logger log = LoggerFactory.getLogger(TestMTStmtNamedWindowSubqueryLookup.class);

    private EPServiceProvider engine;

    public void tearDown()
    {
        engine.initialize();
    }

    public void testConcurrentSubquery() throws Exception
    {
        trySend(3, 10000);
    }

    private void trySend(int numThreads, int numEventsPerThread) throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(Configuration.EventRepresentation.MAP); // use Map-type events for testing
        config.addEventType("SupportBean", SupportBean.class);
        engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();

        // setup statements
        engine.getEPAdministrator().createEPL("create schema MyUpdateEvent as (key string, intupd int)");
        engine.getEPAdministrator().createEPL("create schema MySchema as (theString string, intval int)");
        EPStatement namedWindow = engine.getEPAdministrator().createEPL("create window MyWindow#keepall() as MySchema");
        engine.getEPAdministrator().createEPL("on MyUpdateEvent mue merge MyWindow mw " +
                "where mw.theString = mue.key " +
                "when not matched then insert select key as theString, intupd as intval " +
                "when matched then delete");
        EPStatement targetStatement = engine.getEPAdministrator().createEPL("select (select intval from MyWindow mw where mw.theString = sb.theString) as val from SupportBean sb");

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future<Boolean> future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            future[i] = threadPool.submit(new StmtNamedWindowSubqueryLookupCallable(i, engine, numEventsPerThread, targetStatement));
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
