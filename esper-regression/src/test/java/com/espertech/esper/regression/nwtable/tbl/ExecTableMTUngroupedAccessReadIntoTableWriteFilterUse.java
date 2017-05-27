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

public class ExecTableMTUngroupedAccessReadIntoTableWriteFilterUse implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTUngroupedAccessReadIntoTableWriteFilterUse.class);

    /**
     * For a given number of seconds:
     * Single writer updates a total sum, continuously adding 1 and subtracting 1.
     * Two statements are set up, one listens to "0" and the other to "1"
     * Single reader sends event and that event must be received by any one of the listeners.
     */
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryMT(epService, 3);
    }

    private void tryMT(EPServiceProvider epService, int numSeconds) throws Exception {
        String eplCreateVariable = "create table vartotal (total sum(int))";
        epService.getEPAdministrator().createEPL(eplCreateVariable);

        String eplInto = "into table vartotal select sum(intPrimitive) as total from SupportBean";
        epService.getEPAdministrator().createEPL(eplInto);

        SupportUpdateListener listenerZero = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from SupportBean_S0(1 = vartotal.total)").addListener(listenerZero);

        SupportUpdateListener listenerOne = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from SupportBean_S0(0 = vartotal.total)").addListener(listenerOne);

        WriteRunnable writeRunnable = new WriteRunnable(epService);
        ReadRunnable readRunnable = new ReadRunnable(epService, listenerZero, listenerOne);

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
                    epService.getEPRuntime().sendEvent(new SupportBean("E", 1));
                    epService.getEPRuntime().sendEvent(new SupportBean("E", -1));
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
        private final SupportUpdateListener listenerZero;
        private final SupportUpdateListener listenerOne;

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;

        public ReadRunnable(EPServiceProvider epService, SupportUpdateListener listenerZero, SupportUpdateListener listenerOne) {
            this.epService = epService;
            this.listenerZero = listenerZero;
            this.listenerOne = listenerOne;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                while (!shutdown) {
                    epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
                    listenerZero.reset();
                    listenerOne.reset();
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
