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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTUngroupedIntoTableWriteMultiWriterAgg implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTUngroupedIntoTableWriteMultiWriterAgg.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * For a given number of seconds:
     * Configurable number of into-writers update a shared aggregation.
     * At the end of the test we read and assert.
     */
    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 3, 10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numThreads, int numEvents) throws InterruptedException {
        RegressionPath path = new RegressionPath();
        String eplCreateVariable = "create table varagg (theEvents window(*) @type(SupportBean))";
        env.compileDeploy(eplCreateVariable, path);

        Thread[] threads = new Thread[numThreads];
        WriteRunnable[] runnables = new WriteRunnable[numThreads];
        for (int i = 0; i < threads.length; i++) {
            runnables[i] = new WriteRunnable(env, path, numEvents, i);
            threads[i] = new Thread(runnables[i], InfraTableMTUngroupedIntoTableWriteMultiWriterAgg.class.getSimpleName() + "-write");
            threads[i].start();
        }

        // join
        log.info("Waiting for completion");
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
            assertNull(runnables[i].getException());
        }

        // verify
        env.compileDeploy("@name('s0') select varagg.theEvents as c0 from SupportBean_S0", path).addListener("s0");
        env.sendEventBean(new SupportBean_S0(0));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        SupportBean[] window = (SupportBean[]) event.get("c0");
        assertEquals(numThreads * 3, window.length);

        env.undeployAll();
    }

    public static class WriteRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final RegressionPath path;
        private final int numEvents;
        private final int threadNum;

        private RuntimeException exception;

        public WriteRunnable(RegressionEnvironment env, RegressionPath path, int numEvents, int threadNum) {
            this.env = env;
            this.path = path;
            this.numEvents = numEvents;
            this.threadNum = threadNum;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                String eplInto = "into table varagg select window(*) as theEvents from SupportBean(theString='E" + threadNum + "')#length(3)";
                env.compileDeploy(eplInto, path);

                for (int i = 0; i < numEvents; i++) {
                    env.sendEventBean(new SupportBean("E" + threadNum, i));
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
}
