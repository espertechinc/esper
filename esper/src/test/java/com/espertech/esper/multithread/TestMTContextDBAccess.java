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
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportDatabaseService;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * Test for multithread-safety of context with database access.
 */
public class TestMTContextDBAccess extends TestCase
{
    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getLogging().setEnableJDBC(true);
        configuration.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(false);

        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configuration.addDatabaseReference("MyDB", configDB);

        engine = EPServiceProviderManager.getDefaultProvider(configuration);
        engine.initialize();
    }

    public void testThreadSafetyHistoricalJoin() throws Exception
    {
        engine.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        engine.getEPAdministrator().createEPL("create context CtxEachString partition by theString from SupportBean");
        engine.getEPAdministrator().createEPL("@Name('select') context CtxEachString " +
                "select * from SupportBean, " +
                "  sql:MyDB ['select mycol3 from mytesttable_large where ${theString} = mycol1']");

        // up to 10 threads, up to 1000 combinations (1 to 1000)
        tryThreadSafetyHistoricalJoin(8, 20);
    }

    private void tryThreadSafetyHistoricalJoin(int numThreads, int numRepeats) throws Exception
    {
        MyListener listener = new MyListener();
        engine.getEPAdministrator().getStatement("select").addListener(listener);

        List<Object>[] events = new ArrayList[numThreads];
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            events[threadNum] = new ArrayList<Object>();
            for (int eventNum = 0; eventNum < numRepeats; eventNum++) {
                // range: 1 to 1000
                int partition = eventNum + 1;
                events[threadNum].add(new SupportBean(new Integer(partition).toString(), 0));
            }
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future futures[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new SendEventCallable(i, engine, events[i].iterator());
            futures[i] = threadPool.submit(callable);
        }

        for (Future future : futures) {
            assertEquals(true, future.get());
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(numRepeats * numThreads, listener.getCount());
    }

    public class MyListener implements UpdateListener {
        private int count;

        public synchronized void update(EventBean[] newEvents, EventBean[] oldEvents) {
            if (newEvents.length > 1) {
                assertEquals(1, newEvents.length);
            }
            count += 1;
        }

        public int getCount() {
            return count;
        }
    }
}
