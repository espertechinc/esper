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
import com.espertech.esper.common.client.util.SafeIterator;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.runtime.client.EPStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTUngroupedAccessReadInotTableWriteIterate implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTUngroupedAccessReadInotTableWriteIterate.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * Proof that multiple threads iterating the same statement
     * can safely access a row that is currently changing.
     */
    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 3, 3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numReadThreads, int numSeconds) throws InterruptedException {
        RegressionPath path = new RegressionPath();
        String eplCreateVariable = "create table vartotal (s0 sum(int), s1 sum(double), s2 sum(long))";
        env.compileDeploy(eplCreateVariable, path);

        String eplInto = "into table vartotal select sum(intPrimitive) as s0, " +
            "sum(doublePrimitive) as s1, sum(longPrimitive) as s2 from SupportBean";
        env.compileDeploy(eplInto, path);
        env.sendEventBean(makeSupportBean("E", 1, 1, 1));

        env.compileDeploy("@name('iterate') select vartotal.s0 as c0, vartotal.s1 as c1, vartotal.s2 as c2 from SupportBean_S0#lastevent", path);
        env.sendEventBean(new SupportBean_S0(0));

        // setup writer
        WriteRunnable writeRunnable = new WriteRunnable(env);
        Thread writeThread = new Thread(writeRunnable, InfraTableMTUngroupedAccessReadInotTableWriteIterate.class.getSimpleName() + "-write");

        // setup readers
        Thread[] readThreads = new Thread[numReadThreads];
        ReadRunnable[] readRunnables = new ReadRunnable[numReadThreads];
        for (int i = 0; i < readThreads.length; i++) {
            readRunnables[i] = new ReadRunnable(env.statement("iterate"));
            readThreads[i] = new Thread(readRunnables[i], InfraTableMTUngroupedAccessReadInotTableWriteIterate.class.getSimpleName() + "-read");
        }

        // start
        for (Thread readThread : readThreads) {
            readThread.start();
        }
        writeThread.start();

        // wait
        Thread.sleep(numSeconds * 1000);

        // shutdown
        writeRunnable.setShutdown(true);
        for (ReadRunnable readRunnable : readRunnables) {
            readRunnable.setShutdown(true);
        }

        // join
        log.info("Waiting for completion");
        writeThread.join();
        for (Thread readThread : readThreads) {
            readThread.join();
        }

        env.undeployAll();

        // assert
        assertNull(writeRunnable.getException());
        assertTrue(writeRunnable.numEvents > 100);
        for (ReadRunnable readRunnable : readRunnables) {
            assertNull(readRunnable.getException());
            assertTrue(readRunnable.numQueries > 100);
        }
    }

    private static SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setDoublePrimitive(doublePrimitive);
        b.setLongPrimitive(longPrimitive);
        return b;
    }

    public static class WriteRunnable implements Runnable {

        private final RegressionEnvironment env;

        private RuntimeException exception;
        private boolean shutdown;
        private int numEvents;

        public WriteRunnable(RegressionEnvironment env) {
            this.env = env;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                while (!shutdown) {
                    env.sendEventBean(makeSupportBean("E", 1, 1, 1));
                    numEvents++;
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

        private final EPStatement iterateStatement;

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;

        public ReadRunnable(EPStatement iterateStatement) {
            this.iterateStatement = iterateStatement;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                while (!shutdown) {
                    SafeIterator<EventBean> iterator = iterateStatement.safeIterator();
                    try {
                        EventBean event = iterator.next();
                        int c0 = (Integer) event.get("c0");
                        assertEquals((double) c0, event.get("c1"));
                        assertEquals((long) c0, event.get("c2"));
                    } finally {
                        iterator.close();
                    }
                    numQueries++;
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
