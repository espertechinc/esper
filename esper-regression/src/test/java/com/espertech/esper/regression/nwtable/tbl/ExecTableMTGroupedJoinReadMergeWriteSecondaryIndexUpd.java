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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExecTableMTGroupedJoinReadMergeWriteSecondaryIndexUpd implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTGroupedJoinReadMergeWriteSecondaryIndexUpd.class);

    private static final int NUM_KEYS = 10;
    private static final int OFFSET_ADDED = 100000000;

    /**
     * Tests concurrent updates on a secondary index also read by a join:
     * create table MyTable (key string primary key, value int)
     * create index MyIndex on MyTable (value)
     * select * from SupportBean_S0, MyTable where intPrimitive = id
     * <p>
     * Prefill MyTable with MyTable={key='A_N', value=N} with N between 0 and NUM_KEYS-1
     * <p>
     * For x seconds:
     * Single reader thread sends SupportBean events, asserts that either one or two rows are found (A_N and maybe B_N)
     * Single writer thread inserts MyTable={key='B_N', value=100000+N} and deletes each row.
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
                "create table MyTable (key1 string primary key, value int);\n" +
                        "create index MyIndex on MyTable (value);\n" +
                        "on SupportBean merge MyTable where theString = key1 when not matched then insert select theString as key1, intPrimitive as value;\n" +
                        "@name('out') select * from SupportBean_S0, MyTable where value = id;\n" +
                        "on SupportBean_S1 delete from MyTable where key1 like 'B%';\n";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        // preload A_n events
        for (int i = 0; i < NUM_KEYS; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("A_" + i, i));
        }

        WriteRunnable writeRunnable = new WriteRunnable(epService);
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

    public static class WriteRunnable implements Runnable {

        private final EPServiceProvider epService;

        private RuntimeException exception;
        private boolean shutdown;
        private int numLoops;

        public WriteRunnable(EPServiceProvider epService) {
            this.epService = epService;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                while (!shutdown) {
                    // write additional B_n events
                    for (int i = 0; i < 10000; i++) {
                        epService.getEPRuntime().sendEvent(new SupportBean("B_" + i, i + OFFSET_ADDED));
                    }
                    // delete B_n events
                    epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
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
                    for (int i = 0; i < NUM_KEYS; i++) {
                        epService.getEPRuntime().sendEvent(new SupportBean_S0(i));
                        EventBean[] events = listener.getAndResetLastNewData();
                        assertTrue(events.length > 0);
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
