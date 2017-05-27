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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecMTContextSegmented implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecMTContextSegmented.class);

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String[] choices = "A,B,C,D".split(",");
        trySend(epService, 4, 1000, choices);
    }

    private void trySend(EPServiceProvider epService, int numThreads, int numEvents, String[] choices) throws Exception {
        if (numEvents < choices.length) {
            throw new IllegalArgumentException("Number of events must at least match number of choices");
        }

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create variable boolean myvar = false");
        epService.getEPAdministrator().createEPL("create context SegmentedByString as partition by theString from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context SegmentedByString select theString, count(*) - 1 as cnt from SupportBean output snapshot when myvar = true");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // preload - since concurrently sending same-category events an event can be dropped
        for (int i = 0; i < choices.length; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean(choices[i], 0));
        }

        EventRunnable[] runnables = new EventRunnable[numThreads];
        for (int i = 0; i < runnables.length; i++) {
            runnables[i] = new EventRunnable(epService, numEvents, choices);
        }

        // start
        Thread[] threads = new Thread[runnables.length];
        for (int i = 0; i < runnables.length; i++) {
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }

        // join
        log.info("Waiting for completion");
        for (int i = 0; i < runnables.length; i++) {
            threads[i].join();
        }

        Map<String, Long> totals = new HashMap<String, Long>();
        for (String choice : choices) {
            totals.put(choice, 0L);
        }

        // verify
        int sum = 0;
        for (int i = 0; i < runnables.length; i++) {
            assertNull(runnables[i].getException());
            for (Map.Entry<String, Integer> entry : runnables[i].getTotals().entrySet()) {
                Long current = totals.get(entry.getKey());
                current += entry.getValue();
                sum += entry.getValue();
                totals.put(entry.getKey(), current);
                //System.out.println("Thread " + i + " key " + entry.getKey() + " count " + entry.getValue());
            }
        }

        assertEquals(numThreads * numEvents, sum);

        epService.getEPRuntime().setVariableValue("myvar", true);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        EventBean[] result = listener.getLastNewData();
        assertEquals(choices.length, result.length);
        for (EventBean item : result) {
            String theString = (String) item.get("theString");
            Long count = (Long) item.get("cnt");
            //System.out.println("String " + string + " count " + count);
            assertEquals(count, totals.get(theString));
        }
    }

    public static class EventRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final int numEvents;
        private final String[] choices;
        private final Map<String, Integer> totals = new HashMap<String, Integer>();

        private RuntimeException exception;

        public EventRunnable(EPServiceProvider epService, int numEvents, String[] choices) {
            this.epService = epService;
            this.numEvents = numEvents;
            this.choices = choices;
        }

        public void run() {
            log.info("Started event send");

            try {
                for (int i = 0; i < numEvents; i++) {
                    String chosen = choices[i % choices.length];
                    epService.getEPRuntime().sendEvent(new SupportBean(chosen, 1));

                    Integer current = totals.get(chosen);
                    if (current == null) {
                        current = 0;
                    }
                    current += 1;
                    totals.put(chosen, current);
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send");
        }

        public RuntimeException getException() {
            return exception;
        }

        public Map<String, Integer> getTotals() {
            return totals;
        }
    }
}
