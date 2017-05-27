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

import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.SendEventCallable;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of context with database access.
 */
public class ExecMTContextDBAccess implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableJDBC(true);
        configuration.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(false);

        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configuration.addDatabaseReference("MyDB", configDB);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL("create context CtxEachString partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("@Name('select') context CtxEachString " +
                "select * from SupportBean, " +
                "  sql:MyDB ['select mycol3 from mytesttable_large where ${theString} = mycol1']");

        // up to 10 threads, up to 1000 combinations (1 to 1000)
        tryThreadSafetyHistoricalJoin(epService, 8, 20);
    }

    private void tryThreadSafetyHistoricalJoin(EPServiceProvider epService, int numThreads, int numRepeats) throws Exception {
        MyListener listener = new MyListener();
        epService.getEPAdministrator().getStatement("select").addListener(listener);

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
        Future[] futures = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new SendEventCallable(i, epService, events[i].iterator());
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
