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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.SendEventCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety of context with database access.
 */
public class ExecMTContextListenerDispatch implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecMTContextListenerDispatch.class);

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL("create context CtxEachString partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("@Name('select') context CtxEachString select * from SupportBean");

        tryPerformanceDispatch(epService, 8, 100);
    }

    private void tryPerformanceDispatch(EPServiceProvider epService, int numThreads, int numRepeats) throws Exception {
        MyListener listener = new MyListener();
        epService.getEPAdministrator().getStatement("select").addListener(listener);

        List<Object>[] events = new ArrayList[numThreads];
        int eventId = 0;
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            events[threadNum] = new ArrayList<Object>();
            for (int eventNum = 0; eventNum < numRepeats; eventNum++) {
                // range: 1 to 1000
                int partition = (int) (Math.random() * 50);
                eventId++;
                events[threadNum].add(new SupportBean(new Integer(partition).toString(), eventId));
            }
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] futures = new Future[numThreads];
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            Callable callable = new SendEventCallable(i, epService, events[i].iterator());
            futures[i] = threadPool.submit(callable);
        }
        for (Future future : futures) {
            assertEquals(true, future.get());
        }
        long delta = System.currentTimeMillis() - startTime;

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        // print those events not received
        for (List<Object> eventList : events) {
            for (Object event : eventList) {
                if (!listener.getBeans().contains(event)) {
                    log.info("Expected event was not received, event " + event);
                }
            }
        }

        assertEquals(numRepeats * numThreads, listener.getBeans().size());
        assertTrue("delta=" + delta, delta < 500);
    }

    public class MyListener implements UpdateListener {
        private List<SupportBean> beans = new ArrayList<SupportBean>();

        public synchronized void update(EventBean[] newEvents, EventBean[] oldEvents) {
            if (newEvents.length > 1) {
                assertEquals(1, newEvents.length);
            }
            beans.add((SupportBean) newEvents[0].getUnderlying());
        }

        public List<SupportBean> getBeans() {
            return beans;
        }
    }
}
