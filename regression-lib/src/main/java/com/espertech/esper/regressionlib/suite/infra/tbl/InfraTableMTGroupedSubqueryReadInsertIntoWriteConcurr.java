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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTGroupedSubqueryReadInsertIntoWriteConcurr implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTGroupedSubqueryReadInsertIntoWriteConcurr.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * Primary key is single: {id}
     * For a given number of seconds:
     * Single writer insert-into such as {0} to {N}.
     * Single reader subquery-selects the count all rows.
     */
    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 3);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numSeconds) throws InterruptedException {
        RegressionPath path = new RegressionPath();
        String eplCreateVariable = "create table MyTable (pkey string primary key)";
        env.compileDeploy(eplCreateVariable, path);

        String eplInsertInto = "insert into MyTable select theString as pkey from SupportBean";
        env.compileDeploy(eplInsertInto, path);

        // seed with count 1
        env.sendEventBean(new SupportBean("E0", 0));

        // select/read
        String eplSubselect = "@name('s0') select (select count(*) from MyTable) as c0 from SupportBean_S0";
        env.compileDeploy(eplSubselect, path).addListener("s0");

        WriteRunnable writeRunnable = new WriteRunnable(env);
        ReadRunnable readRunnable = new ReadRunnable(env, env.listener("s0"));

        // start
        Thread writeThread = new Thread(writeRunnable, InfraTableMTGroupedSubqueryReadInsertIntoWriteConcurr.class.getSimpleName() + "-write");
        Thread readThread = new Thread(readRunnable, InfraTableMTGroupedSubqueryReadInsertIntoWriteConcurr.class.getSimpleName() + "-read");
        writeThread.start();
        readThread.start();

        // wait
        Thread.sleep(numSeconds * 1000);

        // shutdown
        writeRunnable.setShutdown(true);
        readRunnable.setShutdown(true);

        // join
        log.info("Waiting for completion");
        writeThread.join();
        readThread.join();

        env.undeployAll();

        assertNull(writeRunnable.getException());
        assertNull(readRunnable.getException());
        assertTrue(writeRunnable.numLoops > 100);
        assertTrue(readRunnable.numQueries > 100);
        System.out.println("Send " + writeRunnable.numLoops + " and performed " + readRunnable.numQueries + " reads");
    }

    public static class WriteRunnable implements Runnable {

        private final RegressionEnvironment env;

        private RuntimeException exception;
        private boolean shutdown;
        private int numLoops;

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
                    env.sendEventBean(new SupportBean("E" + numLoops + 1, 0));
                    numLoops++;
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
        private final SupportListener listener;

        private int numQueries;
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
                    env.sendEventBean(new SupportBean_S0(0));
                    Object value = listener.assertOneGetNewAndReset().get("c0");
                    assertTrue((Long) value >= 1);
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

        public int getNumQueries() {
            return numQueries;
        }
    }
}
