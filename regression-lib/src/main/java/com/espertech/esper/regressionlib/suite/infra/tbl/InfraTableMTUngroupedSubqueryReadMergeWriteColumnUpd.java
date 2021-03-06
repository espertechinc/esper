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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTUngroupedSubqueryReadMergeWriteColumnUpd implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTUngroupedSubqueryReadMergeWriteColumnUpd.class);

    @Override
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED, RegressionFlag.MULTITHREADED);
    }

    /**
     * Tests column-consistency for subquery:
     * create table MyTable(p0 string, p1 string, ..., p4 string)   (5 props)
     * Insert row single: MyTable={p0="1", p1="1", p2="1", p3="1", p4="1"}
     * <p>
     * A writer-thread uses an on-merge statement to update the p0 to p4 columns from "1" to "2", then "2" to "1"
     * A reader-thread uses a subquery checking ("p1="1" and p2="1" and p3="1" and p4="1")
     */
    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numSeconds) throws InterruptedException {
        String epl =
            "create table MyTable (p0 string, p1 string, p2 string, p3 string, p4 string);\n" +
                "on SupportBean merge MyTable " +
                "  when not matched then insert select '1' as p0, '1' as p1, '1' as p2, '1' as p3, '1' as p4;\n" +
                "on SupportBean_S0 merge MyTable " +
                "  when matched then update set p0=p00, p1=p00, p2=p00, p3=p00, p4=p00;\n" +
                "@name('out') select " +
                "(select p0 from MyTable where " +
                "  (p0='1' and p1='1' and p2='1' and p3='1' and p4='1') " +
                "  or (p0='2' and p1='2' and p2='2' and p3='2' and p4='2')) as c0 " +
                "from SupportBean_S1;\n";
        env.compileDeploy(epl);

        // preload
        env.sendEventBean(new SupportBean());

        InfraTableMTUngroupedJoinColumnConsistency.UpdateWriteRunnable writeRunnable = new InfraTableMTUngroupedJoinColumnConsistency.UpdateWriteRunnable(env);
        ReadRunnable readRunnable = new ReadRunnable(env);

        // start
        Thread threadWrite = new Thread(writeRunnable, InfraTableMTUngroupedSubqueryReadMergeWriteColumnUpd.class.getSimpleName() + "-write");
        Thread threadRead = new Thread(readRunnable, InfraTableMTUngroupedSubqueryReadMergeWriteColumnUpd.class.getSimpleName() + "-read");
        threadWrite.start();
        threadRead.start();

        // wait
        Thread.sleep(numSeconds * 1000);

        // shutdown
        writeRunnable.setShutdown(true);
        readRunnable.setShutdown(true);

        // join
        log.info("Waiting for completion");
        threadWrite.join();
        threadRead.join();

        env.undeployAll();
        assertNull(writeRunnable.getException());
        assertNull(readRunnable.getException());
        System.out.println("Write loops " + writeRunnable.getNumLoops() + " and performed " + readRunnable.numQueries + " reads");
        assertTrue(writeRunnable.getNumLoops() > 1);
        assertTrue(readRunnable.numQueries > 100);
    }

    public static class ReadRunnable implements Runnable {

        private final RegressionEnvironment env;
        private final SupportListener listener;

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;

        public ReadRunnable(RegressionEnvironment env) {
            this.env = env;
            env.addListener("out");
            listener = env.listener("out");
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                while (!shutdown) {
                    env.sendEventBean(new SupportBean_S1(0, null));
                    Object value = listener.assertOneGetNewAndReset().get("c0");
                    if (value == null) {
                        throw new IllegalStateException("Value is null");
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
