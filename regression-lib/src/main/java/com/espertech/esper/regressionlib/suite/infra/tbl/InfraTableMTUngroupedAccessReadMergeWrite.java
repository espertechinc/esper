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
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTUngroupedAccessReadMergeWrite implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTUngroupedAccessReadMergeWrite.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * For a given number of seconds:
     * Multiple writer threads each update their thread-id into a shared ungrouped row with plain props,
     * and a single reader thread reads the row and asserts that the values is the same for all cols.
     */
    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 2, 3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numSeconds, int numWriteThreads) throws InterruptedException {
        RegressionPath path = new RegressionPath();
        String eplCreateVariable = "create table varagg (c0 int, c1 int, c2 int, c3 int, c4 int, c5 int)";
        env.compileDeploy(eplCreateVariable, path);

        String eplMerge = "on SupportBean_S0 merge varagg " +
            "when not matched then insert select -1 as c0, -1 as c1, -1 as c2, -1 as c3, -1 as c4, -1 as c5 " +
            "when matched then update set c0=id, c1=id, c2=id, c3=id, c4=id, c5=id";
        env.compileDeploy(eplMerge, path);

        String eplQuery = "@name('s0') select varagg.c0 as c0, varagg.c1 as c1, varagg.c2 as c2," +
            "varagg.c3 as c3, varagg.c4 as c4, varagg.c5 as c5 from SupportBean_S1";
        env.compileDeploy(eplQuery, path).addListener("s0");

        Thread[] writeThreads = new Thread[numWriteThreads];
        WriteRunnable[] writeRunnables = new WriteRunnable[numWriteThreads];
        for (int i = 0; i < writeThreads.length; i++) {
            writeRunnables[i] = new WriteRunnable(env, i);
            writeThreads[i] = new Thread(writeRunnables[i], InfraTableMTUngroupedAccessReadMergeWrite.class.getSimpleName() + "-write");
            writeThreads[i].start();
        }

        ReadRunnable readRunnable = new ReadRunnable(env, env.listener("s0"));
        Thread readThread = new Thread(readRunnable, InfraTableMTUngroupedAccessReadMergeWrite.class.getSimpleName() + "-read");
        readThread.start();

        Thread.sleep(numSeconds * 1000);

        // join
        log.info("Waiting for completion");
        for (int i = 0; i < writeThreads.length; i++) {
            writeRunnables[i].setShutdown(true);
            writeThreads[i].join();
            assertNull(writeRunnables[i].getException());
        }
        readRunnable.setShutdown(true);
        readThread.join();

        env.undeployAll();
        assertNull(readRunnable.getException());
    }

    public static class WriteRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final int threadNum;

        private boolean shutdown;
        private RuntimeException exception;

        public WriteRunnable(RegressionEnvironment env, int threadNum) {
            this.env = env;
            this.threadNum = threadNum;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                while (!shutdown) {
                    env.sendEventBean(new SupportBean_S0(threadNum));
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send for write");
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public RuntimeException getException() {
            return exception;
        }
    }

    public static class ReadRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final SupportListener listener;

        private RuntimeException exception;
        private boolean shutdown;

        public ReadRunnable(RegressionEnvironment env, SupportListener listener) {
            this.env = env;
            this.listener = listener;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                while (!shutdown) {
                    String[] fields = "c1,c2,c3,c4,c5".split(",");
                    env.sendEventBean(new SupportBean_S1(0));
                    EventBean event = listener.assertOneGetNewAndReset();
                    Object valueOne = event.get("c0");
                    for (String field : fields) {
                        assertEquals(valueOne, event.get(field));
                    }
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
