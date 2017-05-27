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
package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for multithread-safety for a simple aggregation case using count(*).
 */
public class ExecMTStmtFilterSubquery implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecMTStmtFilterSubquery.class);

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        tryNamedWindowFilterSubquery(epService);
        tryStreamFilterSubquery(epService);
    }

    private void tryNamedWindowFilterSubquery(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean_S0");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean_S0");

        String epl = "select * from pattern[SupportBean_S0 -> SupportBean(not exists (select * from MyWindow mw where mw.p00 = 'E'))]";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        Thread insertThread = new Thread(new InsertRunnable(epService, 1000));
        Thread filterThread = new Thread(new FilterRunnable(epService, 1000));

        log.info("Starting threads");
        insertThread.start();
        filterThread.start();

        log.info("Waiting for join");
        insertThread.join();
        filterThread.join();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryStreamFilterSubquery(EPServiceProvider engine) throws Exception {
        String epl = "select * from SupportBean(not exists (select * from SupportBean_S0#keepall mw where mw.p00 = 'E'))";
        engine.getEPAdministrator().createEPL(epl);

        Thread insertThread = new Thread(new InsertRunnable(engine, 1000));
        Thread filterThread = new Thread(new FilterRunnable(engine, 1000));

        log.info("Starting threads");
        insertThread.start();
        filterThread.start();

        log.info("Waiting for join");
        insertThread.join();
        filterThread.join();

        engine.getEPAdministrator().destroyAllStatements();
    }

    public static class InsertRunnable implements Runnable {
        private final EPServiceProvider engine;
        private final int numInserts;

        public InsertRunnable(EPServiceProvider engine, int numInserts) {
            this.engine = engine;
            this.numInserts = numInserts;
        }

        public void run() {
            log.info("Starting insert thread");
            for (int i = 0; i < numInserts; i++) {
                engine.getEPRuntime().sendEvent(new SupportBean_S0(i, "E"));
            }
            log.info("Completed insert thread, " + numInserts + " inserted");
        }
    }

    public static class FilterRunnable implements Runnable {
        private final EPServiceProvider engine;
        private final int numEvents;

        public FilterRunnable(EPServiceProvider engine, int numEvents) {
            this.engine = engine;
            this.numEvents = numEvents;
        }

        public void run() {
            log.info("Starting filter thread");
            for (int i = 0; i < numEvents; i++) {
                engine.getEPRuntime().sendEvent(new SupportBean("G" + i, i));
            }
            log.info("Completed filter thread, " + numEvents + " completed");
        }
    }
}
