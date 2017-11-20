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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

public class ExecMTContextPartitionedWTerm implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.getEngineDefaults().getExecution().setPrioritized(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create context MyCtx partition by theString from SupportBean terminated after 1 second");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx select count(*) as cnt from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
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
                while (latch.get() && (maxNumAdvances == -1 || numAdvances < maxNumAdvances)) {
                    epService.getEPRuntime().sendEvent(new CurrentTimeEvent(time));
                    numAdvances++;
                    time += 1000;
                    try {
                        Thread.sleep(threadSleepTime);
                    } catch (InterruptedException e) {
                    }
                }
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
    }
}
