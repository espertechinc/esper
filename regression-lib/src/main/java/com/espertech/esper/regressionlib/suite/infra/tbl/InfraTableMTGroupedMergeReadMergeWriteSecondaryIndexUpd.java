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
import com.espertech.esper.regressionlib.support.bean.SupportTopGroupSubGroupEvent;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * Primary key is composite: {topgroup, subgroup}. Secondary index on {topgroup}.
     * For a given number of seconds:
     * Single writer inserts such as {0,1}, {0,2} to {0, N}, each event a new subgroup and topgroup always 0.
     * Single reader tries to count all values where subgroup equals 0, should always receive a count of 1 and increasing.
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
        String eplCreateVariable = "create table vartotal (topgroup int primary key, subgroup int primary key, thecnt count(*))";
        env.compileDeploy(eplCreateVariable, path);

        String eplCreateIndex = "create index myindex on vartotal (topgroup)";
        env.compileDeploy(eplCreateIndex, path);

        // populate
        String eplInto = "into table vartotal select count(*) as thecnt from SupportTopGroupSubGroupEvent#length(100) group by topgroup, subgroup";
        env.compileDeploy(eplInto, path);

        // delete empty groups
        String eplDelete = "on SupportBean_S0 merge vartotal when matched and thecnt = 0 then delete";
        env.compileDeploy(eplDelete, path);

        // seed with {0, 0} group
        env.sendEventBean(new SupportTopGroupSubGroupEvent(0, 0));

        // select/read
        String eplMergeSelect = "on SupportBean merge vartotal as vt " +
            "where vt.topgroup = intPrimitive and vt.thecnt > 0 " +
            "when matched then insert into MyOutputStream select *";
        env.compileDeploy(eplMergeSelect, path);
        env.compileDeploy("@name('s0') select * from MyOutputStream", path).addListener("s0");
        SupportListener listener = env.listener("s0");

        WriteRunnable writeRunnable = new WriteRunnable(env);
        ReadRunnable readRunnable = new ReadRunnable(env, listener);

        // start
        Thread writeThread = new Thread(writeRunnable, InfraTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd.class.getSimpleName() + "-write");
        Thread readThread = new Thread(readRunnable, InfraTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd.class.getSimpleName() + "-read");
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

        assertNull(writeRunnable.getException());
        assertNull(readRunnable.getException());
        assertTrue(writeRunnable.numEvents > 100);
        assertTrue(readRunnable.numQueries > 100);
        System.out.println("Send " + writeRunnable.numEvents + " and performed " + readRunnable.numQueries + " reads");

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
                int subgroup = 1;
                while (!shutdown) {
                    env.sendEventBean(new SupportTopGroupSubGroupEvent(0, subgroup));
                    subgroup++;

                    // send delete event
                    if (subgroup % 100 == 0) {
                        env.sendEventBean(new SupportBean_S0(0));
                    }
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
                    env.sendEventBean(new SupportBean(null, 0));
                    int len = listener.getNewDataList().size();
                    // Comment me in: System.out.println("Number of events found: " + len);
                    listener.reset();
                    assertTrue(len >= 1);
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
