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

import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.bean.SupportBean;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Test for multithread-safety (or lack thereof) for iterators: iterators fail with concurrent mods as expected behavior
 */
public class TestMTUpdate extends TestCase
{
    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getProvider("TestMTUpdate", config);
    }

    public void tearDown()
    {
        engine.initialize();
    }

    public void testUpdateCreateDelete() throws Exception
    {
        EPStatement stmt = engine.getEPAdministrator().createEPL("select theString from " + SupportBean.class.getName());

        final List<String> strings = Collections.synchronizedList(new ArrayList<String>());
        stmt.addListener(new UpdateListener()
        {
            public void update(EventBean[] newEvents, EventBean[] oldEvents)
            {
                strings.add((String) newEvents[0].get("theString"));
            }
        });

        trySend(2, 50000);

        boolean found = false;
        for (String value : strings)
        {
            if (value.equals("a"))
            {
                found = true;
            }
        }
        assertTrue(found);

        engine.destroy();
    }

    private void trySend(int numThreads, int numRepeats) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtUpdateSendCallable(i, engine, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        for (int i = 0; i < 50; i++)
        {
            EPStatement stmtUpd = engine.getEPAdministrator().createEPL("update istream " + SupportBean.class.getName() + " set theString='a'");
            Thread.sleep(10);
            stmtUpd.destroy();
        }

        threadPool.shutdown();
        threadPool.awaitTermination(5, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue((Boolean) future[i].get());
        }
    }
}
