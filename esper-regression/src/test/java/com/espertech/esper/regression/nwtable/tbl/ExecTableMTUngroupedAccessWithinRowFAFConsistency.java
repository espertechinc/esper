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
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExecTableMTUngroupedAccessWithinRowFAFConsistency implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTUngroupedAccessWithinRowFAFConsistency.class);

    /**
     * For a given number of seconds:
     * Single writer updates the group (round-robin) count, sum and avg.
     * A FAF reader thread pulls the value and checks they are consistent.
     */
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryMT(epService, 2);
    }

    private void tryMT(EPServiceProvider epService, int numSeconds) throws Exception {
        String eplCreateVariable = "create table vartotal (cnt count(*), sumint sum(int), avgint avg(int))";
        epService.getEPAdministrator().createEPL(eplCreateVariable);

        String eplInto = "into table vartotal select count(*) as cnt, sum(intPrimitive) as sumint, avg(intPrimitive) as avgint from SupportBean";
        epService.getEPAdministrator().createEPL(eplInto);

        epService.getEPAdministrator().createEPL("create window MyWindow#lastevent as SupportBean_S0");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean_S0");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));

        WriteRunnable writeRunnable = new WriteRunnable(epService);
        ReadRunnable readRunnable = new ReadRunnable(epService);

        // start
        Thread t1 = new Thread(writeRunnable);
        Thread t2 = new Thread(readRunnable);
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
        assertTrue(writeRunnable.numEvents > 100);
        assertTrue(readRunnable.numQueries > 100);
        System.out.println("Send " + writeRunnable.numEvents + " and performed " + readRunnable.numQueries + " reads");

        epService.getEPAdministrator().destroyAllStatements();
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
                while (!shutdown) {
                    epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
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

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;

        public ReadRunnable(EPServiceProvider epService) {
            this.epService = epService;
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

                while (!shutdown) {
                    EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(eplSelect);
                    long count = (Long) result.getArray()[0].get("c0");
                    int sumint = (Integer) result.getArray()[0].get("c1");
                    double avgint = (Double) result.getArray()[0].get("c2");
                    assertEquals(2d, avgint);
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
