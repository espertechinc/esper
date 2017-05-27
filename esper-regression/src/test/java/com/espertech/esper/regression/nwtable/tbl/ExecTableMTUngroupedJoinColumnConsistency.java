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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExecTableMTUngroupedJoinColumnConsistency implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTUngroupedJoinColumnConsistency.class);

    /**
     * Tests column-consistency for joins:
     * create table MyTable(p0 string, p1 string, ..., p4 string)   (5 props)
     * Insert row single: MyTable={p0="1", p1="1", p2="1", p3="1", p4="1"}
     * <p>
     * A writer-thread uses an on-merge statement to update the p0 to p4 columns from "1" to "2", then "2" to "1"
     * A reader-thread uses a join checking ("p1="1" and p2="1" and p3="1" and p4="1")
     */
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
        configuration.addEventType(SupportBean_S1.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryMT(epService, 2);
    }

    private void tryMT(EPServiceProvider epService, int numSeconds) throws Exception {
        String epl =
                "create table MyTable (p0 string, p1 string, p2 string, p3 string, p4 string);\n" +
                        "on SupportBean merge MyTable " +
                        "  when not matched then insert select '1' as p0, '1' as p1, '1' as p2, '1' as p3, '1' as p4;\n" +
                        "on SupportBean_S0 merge MyTable " +
                        "  when matched then update set p0=p00, p1=p00, p2=p00, p3=p00, p4=p00;\n" +
                        "@name('out') select p0 from SupportBean_S1 unidirectional, MyTable where " +
                        "(p0='1' and p1='1' and p2='1' and p3='1' and p4='1')" +
                        " or (p0='2' and p1='2' and p2='2' and p3='2' and p4='2')" +
                        ";\n";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        // preload
        epService.getEPRuntime().sendEvent(new SupportBean());

        UpdateWriteRunnable writeRunnable = new UpdateWriteRunnable(epService);
        ReadRunnable readRunnable = new ReadRunnable(epService);

        // start
        Thread threadWrite = new Thread(writeRunnable);
        Thread threadRead = new Thread(readRunnable);
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

        assertNull(writeRunnable.getException());
        assertNull(readRunnable.getException());
        System.out.println("Write loops " + writeRunnable.numLoops + " and performed " + readRunnable.numQueries + " reads");
        assertTrue(writeRunnable.numLoops > 1);
        assertTrue(readRunnable.numQueries > 100);
    }

    public static class UpdateWriteRunnable implements Runnable {

        private final EPServiceProvider epService;

        private RuntimeException exception;
        private boolean shutdown;
        private int numLoops;

        public UpdateWriteRunnable(EPServiceProvider epService) {
            this.epService = epService;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                while (!shutdown) {
                    // update to "2"
                    epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "2"));

                    // update to "1"
                    epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "1"));

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

        public int getNumLoops() {
            return numLoops;
        }
    }

    public static class ReadRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final SupportUpdateListener listener;

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;

        public ReadRunnable(EPServiceProvider epService) {
            this.epService = epService;
            listener = new SupportUpdateListener();
            epService.getEPAdministrator().getStatement("out").addListener(listener);
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                while (!shutdown) {
                    epService.getEPRuntime().sendEvent(new SupportBean_S1(0, null));
                    if (!listener.isInvoked()) {
                        throw new IllegalStateException("Failed to receive an event");
                    }
                    listener.reset();
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
