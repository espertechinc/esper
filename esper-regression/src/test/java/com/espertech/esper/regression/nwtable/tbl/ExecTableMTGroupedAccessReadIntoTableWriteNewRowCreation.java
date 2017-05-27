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

import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecTableMTGroupedAccessReadIntoTableWriteNewRowCreation implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTGroupedAccessReadIntoTableWriteNewRowCreation.class);

    /**
     * Table:
     * create table varTotal (key string primary key, total sum(int));
     * <p>
     * For a given number of events
     * - Single writer expands the group-key space by sending additional keys.
     * - Single reader against a last-inserted group gets the non-zero-value.
     */
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryMT(epService, 10000);
    }

    private void tryMT(EPServiceProvider epService, int numEvents) throws Exception {
        String epl =
                "create table varTotal (key string primary key, total sum(int));\n" +
                        "into table varTotal select theString, sum(intPrimitive) as total from SupportBean group by theString;\n" +
                        "@Name('listen') select varTotal[p00].total as c0 from SupportBean_S0;\n";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPRuntime().sendEvent(new SupportBean("A", 10));

        LinkedBlockingDeque<String> queueCreated = new LinkedBlockingDeque<String>();
        WriteRunnable writeRunnable = new WriteRunnable(epService, numEvents, queueCreated);
        ReadRunnable readRunnable = new ReadRunnable(epService, numEvents, queueCreated);

        // start
        Thread t1 = new Thread(writeRunnable);
        Thread t2 = new Thread(readRunnable);
        t1.start();
        t2.start();

        // join
        log.info("Waiting for completion");
        t1.join();
        t2.join();

        assertNull(writeRunnable.getException());
        assertNull(readRunnable.getException());
    }

    public static class WriteRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final int numEvents;
        private final LinkedBlockingDeque<String> queueCreated;
        private RuntimeException exception;

        public WriteRunnable(EPServiceProvider epService, int numEvents, LinkedBlockingDeque<String> queueCreated) {
            this.epService = epService;
            this.numEvents = numEvents;
            this.queueCreated = queueCreated;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                for (int i = 0; i < numEvents; i++) {
                    String key = "E" + i;
                    epService.getEPRuntime().sendEvent(new SupportBean(key, 10));
                    queueCreated.add(key);
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
        private final int numEvents;
        private final LinkedBlockingDeque<String> queueCreated;
        private RuntimeException exception;

        public ReadRunnable(EPServiceProvider epService, int numEvents, LinkedBlockingDeque<String> queueCreated) {
            this.epService = epService;
            this.numEvents = numEvents;
            this.queueCreated = queueCreated;
        }

        public void run() {
            log.info("Started event send for read");
            SupportUpdateListener listener = new SupportUpdateListener();
            epService.getEPAdministrator().getStatement("listen").addListener(listener);

            try {
                String currentEventId = "A";
                for (int i = 0; i < numEvents; i++) {
                    if (!queueCreated.isEmpty()) {
                        currentEventId = queueCreated.removeFirst();
                    }
                    epService.getEPRuntime().sendEvent(new SupportBean_S0(0, currentEventId));
                    int value = (Integer) listener.assertOneGetNewAndReset().get("c0");
                    assertEquals(10, value);
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
