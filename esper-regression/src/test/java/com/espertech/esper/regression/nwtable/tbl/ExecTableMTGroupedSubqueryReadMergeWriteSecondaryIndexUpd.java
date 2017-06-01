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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExecTableMTGroupedSubqueryReadMergeWriteSecondaryIndexUpd implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTGroupedSubqueryReadMergeWriteSecondaryIndexUpd.class);

    /**
     * Primary key is composite: {topgroup, subgroup}. Secondary index on {topgroup}.
     * Single group that always exists is {0,0}. Topgroup is always zero.
     * For a given number of seconds:
     * Single writer merge-inserts such as {0,1}, {0,2} to {0, N} then merge-deletes all rows one by one.
     * Single reader subquery-selects the count all values where subgroup equals 0, should always receive a count of 1 and up.
     */
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(LocalGroupEvent.class);
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryMT(epService, 3);
    }

    private void tryMT(EPServiceProvider epService, int numSeconds) throws Exception {
        String eplCreateVariable = "create table vartotal (topgroup int primary key, subgroup int primary key)";
        epService.getEPAdministrator().createEPL(eplCreateVariable);

        String eplCreateIndex = "create index myindex on vartotal (topgroup)";
        epService.getEPAdministrator().createEPL(eplCreateIndex);

        // insert and delete merge
        String eplMergeInsDel = "on LocalGroupEvent as lge merge vartotal as vt " +
                "where vt.topgroup = lge.topgroup and vt.subgroup = lge.subgroup " +
                "when not matched and lge.op = 'insert' then insert select lge.topgroup as topgroup, lge.subgroup as subgroup " +
                "when matched and lge.op = 'delete' then delete";
        epService.getEPAdministrator().createEPL(eplMergeInsDel);

        // seed with {0, 0} group
        epService.getEPRuntime().sendEvent(new LocalGroupEvent("insert", 0, 0));

        // select/read
        String eplSubselect = "select (select count(*) from vartotal where topgroup=sb.intPrimitive) as c0 " +
                "from SupportBean as sb";
        EPStatement stmtSubselect = epService.getEPAdministrator().createEPL(eplSubselect);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSubselect.addListener(listener);

        WriteRunnable writeRunnable = new WriteRunnable(epService);
        ReadRunnable readRunnable = new ReadRunnable(epService, listener);

        // start
        Thread writeThread = new Thread(writeRunnable);
        Thread readThread = new Thread(readRunnable);
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
        assertTrue(writeRunnable.numLoops > 100);
        assertTrue(readRunnable.numQueries > 100);
        System.out.println("Send " + writeRunnable.numLoops + " and performed " + readRunnable.numQueries + " reads");
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
                    for (int i = 0; i < 10; i++) {
                        epService.getEPRuntime().sendEvent(new LocalGroupEvent("insert", 0, i + 1));
                    }
                    for (int i = 0; i < 10; i++) {
                        epService.getEPRuntime().sendEvent(new LocalGroupEvent("delete", 0, i + 1));
                    }
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

        private int numQueries;
        private RuntimeException exception;
        private boolean shutdown;

        public ReadRunnable(EPServiceProvider epService, SupportUpdateListener listener) {
            this.epService = epService;
            this.listener = listener;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                while (!shutdown) {
                    epService.getEPRuntime().sendEvent(new SupportBean(null, 0));
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

    public static class LocalGroupEvent {
        private final String op;
        private final int topgroup;
        private final int subgroup;

        private LocalGroupEvent(String op, int topgroup, int subgroup) {
            this.op = op;
            this.topgroup = topgroup;
            this.subgroup = subgroup;
        }

        public int getTopgroup() {
            return topgroup;
        }

        public int getSubgroup() {
            return subgroup;
        }

        public String getOp() {
            return op;
        }
    }
}
