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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTUngroupedAccessWithinRowFAFConsistency implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTUngroupedAccessWithinRowFAFConsistency.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * For a given number of seconds:
     * Single writer updates the group (round-robin) count, sum and avg.
     * A FAF reader thread pulls the value and checks they are consistent.
     */
    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numSeconds) throws InterruptedException {
        RegressionPath path = new RegressionPath();
        String eplCreateVariable = "create table vartotal (cnt count(*), sumint sum(int), avgint avg(int))";
        env.compileDeploy(eplCreateVariable, path);

        String eplInto = "into table vartotal select count(*) as cnt, sum(intPrimitive) as sumint, avg(intPrimitive) as avgint from SupportBean";
        env.compileDeploy(eplInto, path);

        env.compileDeploy("create window MyWindow#lastevent as SupportBean_S0", path);
        env.compileDeploy("insert into MyWindow select * from SupportBean_S0", path);
        env.sendEventBean(new SupportBean_S0(0));

        WriteRunnable writeRunnable = new WriteRunnable(env);
        ReadRunnable readRunnable = new ReadRunnable(env, path);

        // start
        Thread t1 = new Thread(writeRunnable, InfraTableMTUngroupedAccessWithinRowFAFConsistency.class.getSimpleName() + "-write");
        Thread t2 = new Thread(readRunnable, InfraTableMTUngroupedAccessWithinRowFAFConsistency.class.getSimpleName() + "-read");
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

        assertNull(writeRunnable.getException());
        assertNull(readRunnable.getException());
        log.info("Send " + writeRunnable.numEvents + " and performed " + readRunnable.numQueries + " reads");
        assertTrue(writeRunnable.numEvents > 100);
        assertTrue(readRunnable.numQueries > 20);

        env.undeployAll();
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
                    env.sendEventBean(new SupportBean("E1", 2));
                    numEvents++;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        shutdown = true;
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

    public static class ReadRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final RegressionPath path;

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;


        public ReadRunnable(RegressionEnvironment env, RegressionPath path) {
            this.env = env;
            this.path = path;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            // warmup
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

            try {
                String eplSelect = "select vartotal.cnt as c0, vartotal.sumint as c1, vartotal.avgint as c2 from MyWindow";
                EPCompiled compiled = env.compileFAF(eplSelect, path);

                while (!shutdown) {
                    EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(compiled);
                    long count = (Long) result.getArray()[0].get("c0");
                    int sumint = (Integer) result.getArray()[0].get("c1");
                    double avgint = (Double) result.getArray()[0].get("c2");
                    assertEquals(2d, avgint, 0);
                    assertEquals(sumint, count * 2);
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
