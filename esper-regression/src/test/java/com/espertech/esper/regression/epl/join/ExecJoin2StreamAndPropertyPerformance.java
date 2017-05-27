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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecJoin2StreamAndPropertyPerformance implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPerfRemoveStream(epService);
        runAssertionPerf2Properties(epService);
        runAssertionPerf3Properties(epService);
    }

    private void runAssertionPerfRemoveStream(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("myStaticEvaluator", MyStaticEval.class.getName(), "myStaticEvaluator");

        MyStaticEval.setCountCalled(0);
        MyStaticEval.setWaitTimeMSec(0);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String epl = "select * from SupportBean#time(1) as sb, " +
                " SupportBean_S0#keepall as s0 " +
                " where myStaticEvaluator(sb.theString, s0.p00)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "x"));
        assertEquals(0, MyStaticEval.getCountCalled());

        epService.getEPRuntime().sendEvent(new SupportBean("y", 10));
        assertEquals(1, MyStaticEval.getCountCalled());
        assertTrue(updateListener.isInvoked());

        // this would be observed as hanging if there was remove-stream evaluation
        MyStaticEval.setWaitTimeMSec(10000000);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(100000));

        stmt.destroy();
    }

    private void runAssertionPerf2Properties(EPServiceProvider epService) {
        String methodName = ".testPerformanceJoinNoResults";

        String epl = "select * from " +
                SupportMarketDataBean.class.getName() + "#length(1000000)," +
                SupportBean.class.getName() + "#length(1000000)" +
                " where symbol=theString and volume=longBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        // Send events for each stream
        log.info(methodName + " Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            sendEvent(epService, makeMarketEvent("IBM_" + i, 1));
            sendEvent(epService, makeSupportEvent("CSCO_" + i, 2));
        }
        log.info(methodName + " Done preloading");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stay at 250, belwo 500ms
        assertTrue((endTime - startTime) < 500);
        stmt.destroy();
    }

    private void runAssertionPerf3Properties(EPServiceProvider epService) {
        String methodName = ".testPerformanceJoinNoResults";

        String epl = "select * from " +
                SupportMarketDataBean.class.getName() + "()#length(1000000)," +
                SupportBean.class.getName() + "#length(1000000)" +
                " where symbol=theString and volume=longBoxed and doublePrimitive=price";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        // Send events for each stream
        log.info(methodName + " Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            sendEvent(epService, makeMarketEvent("IBM_" + i, 1));
            sendEvent(epService, makeSupportEvent("CSCO_" + i, 2));
        }
        log.info(methodName + " Done preloading");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stay at 250, belwo 500ms
        assertTrue((endTime - startTime) < 500);
        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private Object makeSupportEvent(String id, long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(id);
        bean.setLongBoxed(longBoxed);
        return bean;
    }

    private Object makeMarketEvent(String id, long volume) {
        return new SupportMarketDataBean(id, 0, (long) volume, "");
    }

    public static class MyStaticEval {
        private static int countCalled = 0;
        private static long waitTimeMSec;

        public static int getCountCalled() {
            return countCalled;
        }

        public static void setCountCalled(int countCalled) {
            MyStaticEval.countCalled = countCalled;
        }

        public static long getWaitTimeMSec() {
            return waitTimeMSec;
        }

        public static void setWaitTimeMSec(long waitTimeMSec) {
            MyStaticEval.waitTimeMSec = waitTimeMSec;
        }

        public static boolean myStaticEvaluator(String a, String b) {
            try {
                Thread.sleep(waitTimeMSec);
                countCalled++;
            } catch (InterruptedException ex) {
                return false;
            }
            return true;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoin2StreamAndPropertyPerformance.class);
}
