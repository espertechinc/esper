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
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.Support10ColEvent;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTGroupedAccessReadIntoTableWriteAggColConsistency implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTGroupedAccessReadIntoTableWriteAggColConsistency.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * Table:
     * create table vartotal (key string primary key, tc0 sum(int), tc1 sum(int) ... tc9 sum(int))
     * <p>
     * Seed the table with a number of groups, no new ones are added or deleted during the test.
     * For a given number of seconds and a given number of groups:
     * - Single writer updates a group (round-robin), each group associates with 10 columns .
     * - N readers pull a group's columns, round-robin, check that all 10 values are consistent.
     * - The 10 values are sum-int totals that are expected to all have the same value.
     */

    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 10, 3);
        } catch (InterruptedException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numGroups, int numSeconds) throws InterruptedException {
        RegressionPath path = new RegressionPath();
        String eplCreateVariable = "create table vartotal (key string primary key, " + CollectionUtil.toString(getDeclareCols()) + ")";
        env.compileDeploy(eplCreateVariable, path);

        String eplInto = "into table vartotal select " + CollectionUtil.toString(getIntoCols()) + " from Support10ColEvent group by groupKey";
        env.compileDeploy(eplInto, path);

        // initialize groups
        String[] groups = new String[numGroups];
        for (int i = 0; i < numGroups; i++) {
            groups[i] = "G" + i;
            env.sendEventBean(new Support10ColEvent(groups[i], 0));
        }

        WriteRunnable writeRunnable = new WriteRunnable(env, groups);
        ReadRunnable readRunnable = new ReadRunnable(env, path, groups);

        // start
        Thread t1 = new Thread(writeRunnable, InfraTableMTGroupedAccessReadIntoTableWriteAggColConsistency.class.getSimpleName() + "write");
        Thread t2 = new Thread(readRunnable, InfraTableMTGroupedAccessReadIntoTableWriteAggColConsistency.class.getSimpleName() + "read");
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

    private static Collection<String> getDeclareCols() {
        List<String> cols = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {  // 10 columns, not configurable
            cols.add("tc" + i + " sum(int)");
        }
        return cols;
    }

    private static Collection<String> getIntoCols() {
        List<String> cols = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {  // 10 columns, not configurable
            cols.add("sum(c" + i + ") as tc" + i);
        }
        return cols;
    }

    public static class WriteRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final String[] groups;

        private RuntimeException exception;
        private boolean shutdown;
        private int numEvents;

        public WriteRunnable(RegressionEnvironment env, String[] groups) {
            this.env = env;
            this.groups = groups;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                while (!shutdown) {
                    int groupNum = numEvents % groups.length;
                    env.sendEventBean(new Support10ColEvent(groups[groupNum], numEvents));
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
        private final RegressionPath path;
        private final String[] groups;

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;

        public ReadRunnable(RegressionEnvironment env, RegressionPath path, String[] groups) {
            this.env = env;
            this.path = path;
            this.groups = groups;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                String eplSelect = "@name('s0') select vartotal[theString] as out from SupportBean";
                env.compileDeploy(eplSelect, path).addListener("s0");
                SupportListener listener = env.listener("s0");

                while (!shutdown) {
                    int groupNum = numQueries % groups.length;
                    env.sendEventBean(new SupportBean(groups[groupNum], 0));
                    EventBean event = listener.assertOneGetNewAndReset();
                    assertEvent((Map) event.get("out"));
                    numQueries++;
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send for read");
        }

        private static void assertEvent(Map info) {
            Object tc0 = info.get("tc0");
            for (int i = 1; i < 10; i++) {
                assertEquals(tc0, info.get("tc" + i));
            }
        }

        public RuntimeException getException() {
            return exception;
        }
    }

}
