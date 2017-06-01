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
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExecTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd.class);

    /**
     * Primary key is composite: {topgroup, subgroup}. Secondary index on {topgroup}.
     * For a given number of seconds:
     * Single writer inserts such as {0,1}, {0,2} to {0, N}, each event a new subgroup and topgroup always 0.
     * Single reader tries to count all values where subgroup equals 0, should always receive a count of 1 and increasing.
     */
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExecution().setFairlock(true);
        configuration.addEventType(LocalGroupEvent.class);
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryMT(epService, 3);
    }

    private void tryMT(EPServiceProvider epService, int numSeconds) throws Exception {
        String eplCreateVariable = "create table vartotal (topgroup int primary key, subgroup int primary key, thecnt count(*))";
        epService.getEPAdministrator().createEPL(eplCreateVariable);

        String eplCreateIndex = "create index myindex on vartotal (topgroup)";
        epService.getEPAdministrator().createEPL(eplCreateIndex);

        // populate
        String eplInto = "into table vartotal select count(*) as thecnt from LocalGroupEvent#length(100) group by topgroup, subgroup";
        epService.getEPAdministrator().createEPL(eplInto);

        // delete empty groups
        String eplDelete = "on SupportBean_S0 merge vartotal when matched and thecnt = 0 then delete";
        epService.getEPAdministrator().createEPL(eplDelete);

        // seed with {0, 0} group
        epService.getEPRuntime().sendEvent(new LocalGroupEvent(0, 0));

        // select/read
        String eplMergeSelect = "on SupportBean merge vartotal as vt " +
                "where vt.topgroup = intPrimitive and vt.thecnt > 0 " +
                "when matched then insert into MyOutputStream select *";
        epService.getEPAdministrator().createEPL(eplMergeSelect);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from MyOutputStream").addListener(listener);

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
        assertTrue(writeRunnable.numEvents > 100);
        assertTrue(readRunnable.numQueries > 100);
        System.out.println("Send " + writeRunnable.numEvents + " and performed " + readRunnable.numQueries + " reads");
    }

    public static class WriteRunnable implements Runnable {

        private final EPServiceProvider epService;

        private RuntimeException exception;
        private boolean shutdown;
        private int numEvents;

        public WriteRunnable(EPServiceProvider epService) {
            this.epService = epService;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                int subgroup = 1;
                while (!shutdown) {
                    epService.getEPRuntime().sendEvent(new LocalGroupEvent(0, subgroup));
                    subgroup++;

                    // send delete event
                    if (subgroup % 100 == 0) {
                        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
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

    public static class LocalGroupEvent {
        private final int topgroup;
        private final int subgroup;

        private LocalGroupEvent(int topgroup, int subgroup) {
            this.topgroup = topgroup;
            this.subgroup = subgroup;
        }

        public int getTopgroup() {
            return topgroup;
        }

        public int getSubgroup() {
            return subgroup;
        }
    }
}
