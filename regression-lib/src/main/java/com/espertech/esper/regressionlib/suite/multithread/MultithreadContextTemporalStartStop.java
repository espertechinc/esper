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
package com.espertech.esper.regressionlib.suite.multithread;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.threadJoin;
import static org.junit.Assert.assertNull;

/**
 * Test for multithread-safety (or lack thereof) for iterators: iterators fail with concurrent mods as expected behavior
 */
public class MultithreadContextTemporalStartStop implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(MultithreadContextTemporalStartStop.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create context EverySecond as start (*, *, *, *, *, *) end (*, *, *, *, *, *)", path);
        env.compileDeploy("context EverySecond select * from SupportBean", path);

        TimerRunnable timerRunnable = new TimerRunnable(env, 0, 24 * 60 * 60 * 1000, 1000);
        Thread timerThread = new Thread(timerRunnable, this.getClass().getSimpleName() + "-timer");

        EventRunnable eventRunnable = new EventRunnable(env, 1000000);
        Thread eventThread = new Thread(eventRunnable, this.getClass().getSimpleName() + "event");

        timerThread.start();
        eventThread.start();

        threadJoin(timerThread);
        threadJoin(eventThread);
        assertNull(eventRunnable.getException());
        assertNull(timerRunnable.getException());

        env.undeployAll();
    }

    public static class TimerRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final long start;
        private final long end;
        private final long increment;

        private RuntimeException exception;

        public TimerRunnable(RegressionEnvironment env, long start, long end, long increment) {
            this.env = env;
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
                    env.advanceTime(current);
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

        private final RegressionEnvironment env;
        private final long numEvents;

        private RuntimeException exception;

        public EventRunnable(RegressionEnvironment env, long numEvents) {
            this.env = env;
            this.numEvents = numEvents;
        }

        public void run() {
            log.info("Started event send");
            try {
                long count = 0;
                while (count < numEvents) {
                    env.sendEventBean(new SupportBean());
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
