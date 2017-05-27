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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNull;

/**
 * Test for multithread-safety (or lack thereof) for iterators: iterators fail with concurrent mods as expected behavior
 */
public class ExecMTContextTemporalStartStop implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecMTContextTemporalStartStop.class);

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create context EverySecond as start (*, *, *, *, *, *) end (*, *, *, *, *, *)");
        epService.getEPAdministrator().createEPL("context EverySecond select * from SupportBean");

        TimerRunnable timerRunnable = new TimerRunnable(epService, 0, 24 * 60 * 60 * 1000, 1000);
        Thread timerThread = new Thread(timerRunnable, "timer");

        EventRunnable eventRunnable = new EventRunnable(epService, 1000000);
        Thread eventThread = new Thread(eventRunnable, "event");

        timerThread.start();
        eventThread.start();

        timerThread.join();
        eventThread.join();
        assertNull(eventRunnable.getException());
        assertNull(timerRunnable.getException());
    }

    public static class TimerRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final long start;
        private final long end;
        private final long increment;

        private RuntimeException exception;

        public TimerRunnable(EPServiceProvider epService, long start, long end, long increment) {
            this.epService = epService;
            this.start = start;
            this.end = end;
            this.increment = increment;
        }

        public void run() {
            log.info("Started time drive");
            try {
                long current = start;
                long stepCount = 0;
                long expectedSteps = (end - start) / increment;
                while (current < end) {
                    epService.getEPRuntime().sendEvent(new CurrentTimeEvent(current));
                    current += increment;
                    stepCount++;

                    if (stepCount % 10000 == 0) {
                        log.info("Sending step #" + stepCount + " of " + expectedSteps);
                    }
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }
            log.info("Completed time drive");
        }

        public RuntimeException getException() {
            return exception;
        }
    }

    public static class EventRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final long numEvents;

        private RuntimeException exception;

        public EventRunnable(EPServiceProvider epService, long numEvents) {
            this.epService = epService;
            this.numEvents = numEvents;
        }

        public void run() {
            log.info("Started event send");
            try {
                long count = 0;
                while (count < numEvents) {
                    epService.getEPRuntime().sendEvent(new SupportBean());
                    count++;

                    if (count % 10000 == 0) {
                        log.info("Sending event #" + count);
                    }
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
    }

}
