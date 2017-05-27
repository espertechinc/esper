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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecTableMTUngroupedAccessReadMergeWrite implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTUngroupedAccessReadMergeWrite.class);

    /**
     * For a given number of seconds:
     * Multiple writer threads each update their thread-id into a shared ungrouped row with plain props,
     * and a single reader thread reads the row and asserts that the values is the same for all cols.
     */
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
        configuration.addEventType(SupportBean_S1.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryMT(epService, 2, 3);
    }

    private void tryMT(EPServiceProvider epService, int numSeconds, int numWriteThreads) throws Exception {
        String eplCreateVariable = "create table varagg (c0 int, c1 int, c2 int, c3 int, c4 int, c5 int)";
        epService.getEPAdministrator().createEPL(eplCreateVariable);

        String eplMerge = "on SupportBean_S0 merge varagg " +
                "when not matched then insert select -1 as c0, -1 as c1, -1 as c2, -1 as c3, -1 as c4, -1 as c5 " +
                "when matched then update set c0=id, c1=id, c2=id, c3=id, c4=id, c5=id";
        epService.getEPAdministrator().createEPL(eplMerge);

        SupportUpdateListener listener = new SupportUpdateListener();
        String eplQuery = "select varagg.c0 as c0, varagg.c1 as c1, varagg.c2 as c2," +
                "varagg.c3 as c3, varagg.c4 as c4, varagg.c5 as c5 from SupportBean_S1";
        epService.getEPAdministrator().createEPL(eplQuery).addListener(listener);

        Thread[] writeThreads = new Thread[numWriteThreads];
        WriteRunnable[] writeRunnables = new WriteRunnable[numWriteThreads];
        for (int i = 0; i < writeThreads.length; i++) {
            writeRunnables[i] = new WriteRunnable(epService, i);
            writeThreads[i] = new Thread(writeRunnables[i]);
            writeThreads[i].start();
        }

        ReadRunnable readRunnable = new ReadRunnable(epService, listener);
        Thread readThread = new Thread(readRunnable);
        readThread.start();

        Thread.sleep(numSeconds * 1000);

        // join
        log.info("Waiting for completion");
        for (int i = 0; i < writeThreads.length; i++) {
            writeRunnables[i].setShutdown(true);
            writeThreads[i].join();
            assertNull(writeRunnables[i].getException());
        }
        readRunnable.setShutdown(true);
        readThread.join();
        assertNull(readRunnable.getException());
    }

    public static class WriteRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final int threadNum;

        private boolean shutdown;
        private RuntimeException exception;

        public WriteRunnable(EPServiceProvider epService, int threadNum) {
            this.epService = epService;
            this.threadNum = threadNum;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                while (!shutdown) {
                    epService.getEPRuntime().sendEvent(new SupportBean_S0(threadNum));
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send for write");
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public RuntimeException getException() {
            return exception;
        }
    }

    public static class ReadRunnable implements Runnable {

        private final EPServiceProvider engine;
        private final SupportUpdateListener listener;

        private RuntimeException exception;
        private boolean shutdown;

        public ReadRunnable(EPServiceProvider engine, SupportUpdateListener listener) {
            this.engine = engine;
            this.listener = listener;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                while (!shutdown) {
                    String[] fields = "c1,c2,c3,c4,c5".split(",");
                    engine.getEPRuntime().sendEvent(new SupportBean_S1(0));
                    EventBean event = listener.assertOneGetNewAndReset();
                    Object valueOne = event.get("c0");
                    for (String field : fields) {
                        assertEquals(valueOne, event.get(field));
                    }
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
