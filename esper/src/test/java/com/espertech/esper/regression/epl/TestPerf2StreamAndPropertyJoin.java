/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPerf2StreamAndPropertyJoin extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        updateListener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        updateListener = null;
    }

    public void testPerfRemoveStream() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("myStaticEvaluator", MyStaticEval.class.getName(), "myStaticEvaluator");

        MyStaticEval.setCountCalled(0);
        MyStaticEval.setWaitTimeMSec(0);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String joinStatement = "select * from SupportBean.win:time(1) as sb, " +
                " SupportBean_S0.win:keepall() as s0 " +
                " where myStaticEvaluator(sb.theString, s0.p00)";
        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "x"));
        assertEquals(0, MyStaticEval.getCountCalled());

        epService.getEPRuntime().sendEvent(new SupportBean("y", 10));
        assertEquals(1, MyStaticEval.getCountCalled());
        assertTrue(updateListener.isInvoked());

        // this would be observed as hanging if there was remove-stream evaluation
        MyStaticEval.setWaitTimeMSec(10000000);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(100000));
    }

    public void testPerf2Properties()
    {
        String methodName = ".testPerformanceJoinNoResults";

        String joinStatement = "select * from " +
                SupportMarketDataBean.class.getName() + ".win:length(1000000)," +
                SupportBean.class.getName() + ".win:length(1000000)" +
            " where symbol=theString and volume=longBoxed";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        // Send events for each stream
        log.info(methodName + " Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
        {
            sendEvent(makeMarketEvent("IBM_" + i, 1));
            sendEvent(makeSupportEvent("CSCO_" + i, 2));
        }
        log.info(methodName + " Done preloading");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stay at 250, belwo 500ms
        assertTrue((endTime - startTime) < 500);
    }

    public void testPerf3Properties()
    {
        String methodName = ".testPerformanceJoinNoResults";

        String joinStatement = "select * from " +
                SupportMarketDataBean.class.getName() + "().win:length(1000000)," +
                SupportBean.class.getName() + ".win:length(1000000)" +
            " where symbol=theString and volume=longBoxed and doublePrimitive=price";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        // Send events for each stream
        log.info(methodName + " Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
        {
            sendEvent(makeMarketEvent("IBM_" + i, 1));
            sendEvent(makeSupportEvent("CSCO_" + i, 2));
        }
        log.info(methodName + " Done preloading");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stay at 250, belwo 500ms
        assertTrue((endTime - startTime) < 500);
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private Object makeSupportEvent(String id, long longBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(id);
        bean.setLongBoxed(longBoxed);
        return bean;
    }

    private Object makeMarketEvent(String id, long volume)
    {
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
            }
            catch (InterruptedException ex) {
                return false;
            }
            return true;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TestPerf2StreamAndPropertyJoin.class);
}
