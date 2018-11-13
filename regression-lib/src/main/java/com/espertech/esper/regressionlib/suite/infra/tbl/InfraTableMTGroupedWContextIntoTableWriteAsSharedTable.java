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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTGroupedWContextIntoTableWriteAsSharedTable implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTGroupedWContextIntoTableWriteAsSharedTable.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * Multiple writers share a key space that they aggregate into.
     * Writer utilize a hash partition context.
     * After all writers are done validate the space.
     */
    public void run(RegressionEnvironment env) {
        // with T, N, G:  Each of T threads loops N times and sends for each loop G events for each group.
        // for a total of T*N*G events being processed, and G aggregations retained in a shared variable.
        // Group is the innermost loop.
        try {
            tryMT(env, 8, 1000, 64);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numThreads, int numLoops, int numGroups) throws InterruptedException {
        String eplDeclare =
            "create table varTotal (key string primary key, total sum(int));\n" +
                "create context ByStringHash\n" +
                "  coalesce by consistent_hash_crc32(theString) from SupportBean granularity 16 preallocate\n;" +
                "context ByStringHash into table varTotal select theString, sum(intPrimitive) as total from SupportBean group by theString;\n";
        String eplAssert = "select varTotal[p00].total as c0 from SupportBean_S0";

        runAndAssert(env, eplDeclare, eplAssert, numThreads, numLoops, numGroups);
    }

    public static void runAndAssert(RegressionEnvironment env, String eplDeclare, String eplAssert, int numThreads, int numLoops, int numGroups) throws InterruptedException {
        RegressionPath path = new RegressionPath();
        env.compileDeploy(eplDeclare, path);

        // setup readers
        Thread[] writeThreads = new Thread[numThreads];
        WriteRunnable[] writeRunnables = new WriteRunnable[numThreads];
        for (int i = 0; i < writeThreads.length; i++) {
            writeRunnables[i] = new WriteRunnable(env, numLoops, numGroups);
            writeThreads[i] = new Thread(writeRunnables[i], InfraTableMTGroupedWContextIntoTableWriteAsSharedTable.class.getSimpleName() + "-write");
        }

        // start
        for (Thread writeThread : writeThreads) {
            writeThread.start();
        }

        // join
        log.info("Waiting for completion");
        for (Thread writeThread : writeThreads) {
            writeThread.join();
        }

        // assert
        for (WriteRunnable writeRunnable : writeRunnables) {
            assertNull(writeRunnable.getException());
        }

        // each group should total up to "numLoops*numThreads"
        env.compileDeploy("@name('s0') " + eplAssert, path).addListener("s0");
        SupportListener listener = env.listener("s0");

        Integer expected = numLoops * numThreads;
        for (int i = 0; i < numGroups; i++) {
            env.sendEventBean(new SupportBean_S0(0, "G" + i));
            assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
        }

        env.undeployAll();
    }

    public static class WriteRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final int numGroups;
        private final int numLoops;

        private RuntimeException exception;

        public WriteRunnable(RegressionEnvironment env, int numLoops, int numGroups) {
            this.env = env;
            this.numGroups = numGroups;
            this.numLoops = numLoops;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                for (int i = 0; i < numLoops; i++) {
                    for (int j = 0; j < numGroups; j++) {
                        env.sendEventBean(new SupportBean("G" + j, 1));
                    }
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
