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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecJoin2StreamSimplePerformance implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPerformanceJoinNoResults(epService);
        runAssertionJoinPerformanceStreamA(epService);
        runAssertionJoinPerformanceStreamB(epService);
    }

    private void runAssertionPerformanceJoinNoResults(EPServiceProvider epService) {
        setupStatement(epService);
        String methodName = ".testPerformanceJoinNoResults";

        // Send events for each stream
        log.info(methodName + " Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            sendEvent(epService, makeMarketEvent("IBM_" + i));
            sendEvent(epService, makeSupportEvent("CSCO_" + i));
        }
        log.info(methodName + " Done preloading");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stay below 50 ms
        assertTrue((endTime - startTime) < 500);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionJoinPerformanceStreamA(EPServiceProvider epService) {
        SupportUpdateListener updateListener = setupStatement(epService);
        String methodName = ".testJoinPerformanceStreamA";

        // Send 100k events
        log.info(methodName + " Preloading events");
        for (int i = 0; i < 50000; i++) {
            sendEvent(epService, makeMarketEvent("IBM_" + i));
        }
        log.info(methodName + " Done preloading");

        long startTime = System.currentTimeMillis();
        sendEvent(epService, makeSupportEvent("IBM_10"));
        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        assertEquals(1, updateListener.getLastNewData().length);
        // Stay below 50 ms
        assertTrue((endTime - startTime) < 50);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionJoinPerformanceStreamB(EPServiceProvider epService) {
        String methodName = ".testJoinPerformanceStreamB";
        SupportUpdateListener updateListener = setupStatement(epService);

        // Send 100k events
        log.info(methodName + " Preloading events");
        for (int i = 0; i < 50000; i++) {
            sendEvent(epService, makeSupportEvent("IBM_" + i));
        }
        log.info(methodName + " Done preloading");

        long startTime = System.currentTimeMillis();

        updateListener.reset();
        sendEvent(epService, makeMarketEvent("IBM_" + 10));

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        assertEquals(1, updateListener.getLastNewData().length);
        // Stay below 50 ms
        assertTrue((endTime - startTime) < 25);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private Object makeSupportEvent(String id) {
        SupportBean bean = new SupportBean();
        bean.setTheString(id);
        return bean;
    }

    private Object makeMarketEvent(String id) {
        return new SupportMarketDataBean(id, 0, (long) 0, "");
    }

    private SupportUpdateListener setupStatement(EPServiceProvider epService) {
        SupportUpdateListener updateListener = new SupportUpdateListener();

        String epl = "select * from " +
                SupportMarketDataBean.class.getName() + "#length(1000000)," +
                SupportBean.class.getName() + "#length(1000000)" +
                " where symbol=theString";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(updateListener);
        return updateListener;
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoin2StreamSimplePerformance.class);
}
