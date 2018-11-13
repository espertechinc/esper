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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTGroupedAccessReadIntoTableWriteNewRowCreation implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTGroupedAccessReadIntoTableWriteNewRowCreation.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * Table:
     * create table varTotal (key string primary key, total sum(int));
     * <p>
     * For a given number of events
     * - Single writer expands the group-key space by sending additional keys.
     * - Single reader against a last-inserted group gets the non-zero-value.
     */

    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numEvents) throws InterruptedException {
        String epl =
            "create table varTotal (key string primary key, total sum(int));\n" +
                "into table varTotal select theString, sum(intPrimitive) as total from SupportBean group by theString;\n" +
                "@Name('s0') select varTotal[p00].total as c0 from SupportBean_S0;\n";
        env.compileDeploy(epl).addListener("s0");
        env.sendEventBean(new SupportBean("A", 10));

        LinkedBlockingDeque<String> queueCreated = new LinkedBlockingDeque<String>();
        WriteRunnable writeRunnable = new WriteRunnable(env, numEvents, queueCreated);
        ReadRunnable readRunnable = new ReadRunnable(env, numEvents, queueCreated);

        // start
        Thread t1 = new Thread(writeRunnable, InfraTableMTGroupedAccessReadIntoTableWriteNewRowCreation.class.getSimpleName() + "-write");
        Thread t2 = new Thread(readRunnable, InfraTableMTGroupedAccessReadIntoTableWriteNewRowCreation.class.getSimpleName() + "-read");
        t1.start();
        t2.start();

        // join
        log.info("Waiting for completion");
        t1.join();
        t2.join();

        env.undeployAll();
        assertNull(writeRunnable.getException());
        assertNull(readRunnable.getException());
    }

    public static class WriteRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final int numEvents;
        private final LinkedBlockingDeque<String> queueCreated;
        private RuntimeException exception;

        public WriteRunnable(RegressionEnvironment env, int numEvents, LinkedBlockingDeque<String> queueCreated) {
            this.env = env;
            this.numEvents = numEvents;
            this.queueCreated = queueCreated;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                for (int i = 0; i < numEvents; i++) {
                    String key = "E" + i;
                    env.sendEventBean(new SupportBean(key, 10));
                    queueCreated.add(key);
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send for write");
        }

        public RuntimeException getException() {
            return exception;
        }
    }

    public static class ReadRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final int numEvents;
        private final LinkedBlockingDeque<String> queueCreated;
        private RuntimeException exception;

        public ReadRunnable(RegressionEnvironment env, int numEvents, LinkedBlockingDeque<String> queueCreated) {
            this.env = env;
            this.numEvents = numEvents;
            this.queueCreated = queueCreated;
        }

        public void run() {
            log.info("Started event send for read");
            try {
                SupportListener listener = env.listener("s0");
                String currentEventId = "A";

                for (int i = 0; i < numEvents; i++) {
                    if (!queueCreated.isEmpty()) {
                        currentEventId = queueCreated.removeFirst();
                    }
                    env.sendEventBean(new SupportBean_S0(0, currentEventId));
                    int value = (Integer) listener.assertOneGetNewAndReset().get("c0");
                    assertEquals(10, value);
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send for read");
        }

        public RuntimeException getException() {
            return exception;
        }
    }
}
