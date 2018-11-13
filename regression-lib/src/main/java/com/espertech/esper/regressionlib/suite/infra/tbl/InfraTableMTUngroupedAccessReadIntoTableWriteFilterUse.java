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
public class InfraTableMTUngroupedAccessReadIntoTableWriteFilterUse implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTUngroupedAccessReadIntoTableWriteFilterUse.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * For a given number of seconds:
     * Single writer updates a total sum, continuously adding 1 and subtracting 1.
     * Two statements are set up, one listens to "0" and the other to "1"
     * Single reader sends event and that event must be received by any one of the listeners.
     */
    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numSeconds) throws InterruptedException {
        RegressionPath path = new RegressionPath();
        String eplCreateVariable = "create table vartotal (total sum(int))";
        env.compileDeploy(eplCreateVariable, path);

        String eplInto = "into table vartotal select sum(intPrimitive) as total from SupportBean";
        env.compileDeploy(eplInto, path);

        env.compileDeploy("@name('s0') select * from SupportBean_S0(1 = vartotal.total)", path).addListener("s0");

        env.compileDeploy("@name('s1') select * from SupportBean_S0(0 = vartotal.total)", path).addListener("s1");

        WriteRunnable writeRunnable = new WriteRunnable(env);
        ReadRunnable readRunnable = new ReadRunnable(env, env.listener("s0"), env.listener("s1"));

        // start
        Thread t1 = new Thread(writeRunnable, InfraTableMTUngroupedAccessReadIntoTableWriteFilterUse.class.getSimpleName() + "-write");
        Thread t2 = new Thread(readRunnable, InfraTableMTUngroupedAccessReadIntoTableWriteFilterUse.class.getSimpleName() + "-read");
        t1.start();
        t2.start();

        // wait
        Thread.sleep(numSeconds * 1000);

        // shutdown
        writeRunnable.setShutdown(true);
        readRunnable.setShutdown(true);

        // join
        log.info("Waiting for completion");
        t1.join();
        t2.join();

        env.undeployAll();
        assertNull(writeRunnable.getException());
        assertNull(readRunnable.getException());
        assertTrue(writeRunnable.numEvents > 100);
        assertTrue(readRunnable.numQueries > 100);
        System.out.println("Send " + writeRunnable.numEvents + " and performed " + readRunnable.numQueries + " reads");
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
                    env.sendEventBean(new SupportBean("E", 1));
                    env.sendEventBean(new SupportBean("E", -1));
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

        private final RegressionEnvironment env;
        private final SupportListener listenerZero;
        private final SupportListener listenerOne;

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;

        public ReadRunnable(RegressionEnvironment env, SupportListener listenerZero, SupportListener listenerOne) {
            this.env = env;
            this.listenerZero = listenerZero;
            this.listenerOne = listenerOne;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                while (!shutdown) {
                    env.sendEventBean(new SupportBean_S0(0));
                    listenerZero.reset();
                    listenerOne.reset();
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
