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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestMTContextInitatedTerminatedWithNowParallel extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        listener = null;
    }

    public void testStartNowCountReliably() throws InterruptedException {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create context MyCtx start @now end after 1 second");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx select count(*) as cnt from SupportBean output last when terminated");
        stmt.addListener(listener);

        AtomicBoolean latch = new AtomicBoolean(true);
        // With 0-sleep or 1-sleep the counts start to drop because the event is chasing the context partition.
        Thread t = new Thread(new MyTimeAdvancingRunnable(epService, latch, 10, -1));
        t.start();

        int numEvents = 10000;
        for (int i = 0; i < numEvents; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean());
        }
        latch.set(false);
        t.join();
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(Integer.MAX_VALUE));

        long total = 0;
        EventBean[] deliveries = listener.getNewDataListFlattened();
        for (EventBean event : deliveries) {
            long count = (Long) event.get("cnt");
            total += count;
        }
        assertEquals(numEvents, total);
    }

    public static class MyTimeAdvancingRunnable implements Runnable {
        private final EPServiceProvider epService;
        private final AtomicBoolean latch;
        private final long threadSleepTime;
        private final long maxNumAdvances;

        public MyTimeAdvancingRunnable(EPServiceProvider epService, AtomicBoolean latch, long threadSleepTime, long maxNumAdvances) {
            this.epService = epService;
            this.latch = latch;
            this.threadSleepTime = threadSleepTime;
            this.maxNumAdvances = maxNumAdvances;
        }

        public void run() {
            long time = 1000;
            long numAdvances = 0;
            try {
                while(latch.get() && (maxNumAdvances == -1 || numAdvances < maxNumAdvances)) {
                    epService.getEPRuntime().sendEvent(new CurrentTimeEvent(time));
                    numAdvances++;
                    time += 1000;
                    try {
                        Thread.sleep(threadSleepTime);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
            catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
    }
}
