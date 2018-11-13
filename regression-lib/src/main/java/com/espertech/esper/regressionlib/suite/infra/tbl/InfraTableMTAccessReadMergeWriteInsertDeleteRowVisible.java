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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
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
public class InfraTableMTAccessReadMergeWriteInsertDeleteRowVisible implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTAccessReadMergeWriteInsertDeleteRowVisible.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * Table:
     * create table MyTable(key string primary key, p0 int, p1 int, p2, int, p3 int, p4 int)
     * <p>
     * For a given number of seconds:
     * - Single writer uses merge in a loop:
     * - inserts MyTable={key='K1', p0=1, p1=1, p2=1, p3=1, p4=1}
     * - deletes the row
     * - Single reader outputs p0 to p4 using "MyTable['K1'].px"
     * Row should either exist with all values found or not exist.
     */

    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 1, true);
            tryMT(env, 1, false);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numSeconds, boolean grouped) throws InterruptedException {
        RegressionPath path = new RegressionPath();
        String eplCreateTable = "create table MyTable (key string " + (grouped ? "primary key" : "") +
            ", p0 int, p1 int, p2 int, p3 int, p4 int, p5 int)";
        env.compileDeploy(eplCreateTable, path);

        String eplSelect = grouped ?
            "@name('s0') select MyTable['K1'].p0 as c0, MyTable['K1'].p1 as c1, MyTable['K1'].p2 as c2, " +
                "MyTable['K1'].p3 as c3, MyTable['K1'].p4 as c4, MyTable['K1'].p5 as c5 from SupportBean_S0"
            :
            "@name('s0') select MyTable.p0 as c0, MyTable.p1 as c1, MyTable.p2 as c2, " +
                "MyTable.p3 as c3, MyTable.p4 as c4, MyTable.p5 as c5 from SupportBean_S0";
        env.compileDeploy(eplSelect, path).addListener("s0");

        String eplMerge = "on SupportBean merge MyTable " +
            "when not matched then insert select 'K1' as key, 1 as p0, 1 as p1, 1 as p2, 1 as p3, 1 as p4, 1 as p5 " +
            "when matched then delete";
        env.compileDeploy(eplMerge, path);

        WriteRunnable writeRunnable = new WriteRunnable(env);
        ReadRunnable readRunnable = new ReadRunnable(env, env.listener("s0"));

        // start
        Thread t1 = new Thread(writeRunnable, InfraTableMTAccessReadMergeWriteInsertDeleteRowVisible.class.getSimpleName() + "-write");
        Thread t2 = new Thread(readRunnable, InfraTableMTAccessReadMergeWriteInsertDeleteRowVisible.class.getSimpleName() + "read");
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
        assertTrue(writeRunnable.numEvents > 100);
        assertNull(readRunnable.getException());
        assertTrue(readRunnable.numQueries > 100);
        assertTrue(readRunnable.getNotFoundCount() > 2);
        assertTrue(readRunnable.getFoundCount() > 2);
        System.out.println("Send " + writeRunnable.numEvents + " and performed " + readRunnable.numQueries +
            " reads (found " + readRunnable.getFoundCount() + ") (not found " + readRunnable.getNotFoundCount() + ")");

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
                    env.sendEventBean(new SupportBean(null, 0));
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
        private final SupportListener listener;

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;
        private int foundCount;
        private int notFoundCount;

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
                String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
                Object[] expected = new Object[]{1, 1, 1, 1, 1, 1};
                while (!shutdown) {
                    env.sendEventBean(new SupportBean_S0(0));
                    EventBean event = listener.assertOneGetNewAndReset();
                    if (event.get("c0") == null) {
                        notFoundCount++;
                    } else {
                        foundCount++;
                        EPAssertionUtil.assertProps(event, fields, expected);
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

        public int getFoundCount() {
            return foundCount;
        }

        public int getNotFoundCount() {
            return notFoundCount;
        }
    }
}
