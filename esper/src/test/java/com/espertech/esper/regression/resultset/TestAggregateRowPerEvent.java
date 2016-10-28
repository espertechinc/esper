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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import junit.framework.TestCase;

public class TestAggregateRowPerEvent extends TestCase
{
    private final static String JOIN_KEY = "KEY";

    private EPServiceProvider epService;
    private SupportUpdateListener testListener;
    private int eventCount;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        eventCount = 0;
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testAggregatedSelectTriggerEvent() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        String epl = "select window(s0.*) as rows, sb " +
                "from SupportBean#keepall() as sb, SupportBean_S0#keepall() as s0 " +
                "where sb.theString = s0.p00";
        epService.getEPAdministrator().createEPL(epl).addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "K1", "V1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "K1", "V2"));

        // test SB-direction
        SupportBean b1 = new SupportBean("K1", 0);
        epService.getEPRuntime().sendEvent(b1);
        EventBean events[] = testListener.getAndResetLastNewData();
        assertEquals(2, events.length);
        for (EventBean event : events) {
            assertEquals(b1, event.get("sb"));
            assertEquals(2, ((SupportBean_S0[]) event.get("rows")).length);
        }

        // test S0-direction
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "K1", "V3"));
        EventBean event = testListener.assertOneGetNewAndReset();
        assertEquals(b1, event.get("sb"));
        assertEquals(3, ((SupportBean_S0[]) event.get("rows")).length);
    }

    public void testAggregatedSelectUnaggregatedHaving() {
        // ESPER-571
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String epl = "select max(intPrimitive) as val from SupportBean#time(1) having max(intPrimitive) > intBoxed";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(testListener);

        sendEvent("E1", 10, 1);
        assertEquals(10, testListener.assertOneGetNewAndReset().get("val"));

        sendEvent("E2", 10, 11);
        assertFalse(testListener.isInvoked());

        sendEvent("E3", 15, 11);
        assertEquals(15, testListener.assertOneGetNewAndReset().get("val"));

        sendEvent("E4", 20, 11);
        assertEquals(20, testListener.assertOneGetNewAndReset().get("val"));

        sendEvent("E5", 25, 25);
        assertFalse(testListener.isInvoked());
    }

    public void testSumOneView()
    {
        String viewExpr = "select irstream longPrimitive, sum(longBoxed) as mySum " +
                          "from " + SupportBean.class.getName() + "#length(3)";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        runAssert(selectTestView);
    }

    public void testSumJoin()
    {
        String viewExpr = "select irstream longPrimitive, sum(longBoxed) as mySum " +
                          "from " + SupportBeanString.class.getName() + "#length(3) as one, " +
                                    SupportBean.class.getName() + "#length(3) as two " +
                          "where one.theString = two.theString";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));

        runAssert(selectTestView);
    }

    public void testSumAvgWithWhere()
    {
        String viewExpr = "select 'IBM stats' as title, volume, avg(volume) as myAvg, sum(volume) as mySum " +
                          "from " + SupportMarketDataBean.class.getName() + "#length(3)" +
                          "where symbol='IBM'";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        sendMarketDataEvent("GE", 10L);
        assertFalse(testListener.isInvoked());

        sendMarketDataEvent("IBM", 20L);
        assertPostedNew(20d, 20L);

        sendMarketDataEvent("XXX", 10000L);
        assertFalse(testListener.isInvoked());

        sendMarketDataEvent("IBM", 30L);
        assertPostedNew(25d, 50L);
    }

    private void assertPostedNew(Double newAvg, Long newSum)
    {
        EventBean[] oldData = testListener.getLastOldData();
        EventBean[] newData = testListener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEquals("IBM stats", newData[0].get("title"));
        assertEquals(newAvg, newData[0].get("myAvg"));
        assertEquals(newSum, newData[0].get("mySum"));

        testListener.reset();
    }

    private void runAssert(EPStatement selectTestView)
    {
        String[] fields = new String[] {"longPrimitive", "mySum"};

        // assert select result type
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, null);

        sendEvent(10);
        assertEquals(10L, testListener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{1L, 10L}});

        sendEvent(15);
        assertEquals(25L, testListener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{1L, 25L}, {2L, 25L}});

        sendEvent(-5);
        assertEquals(20L, testListener.getAndResetLastNewData()[0].get("mySum"));
        assertNull(testListener.getLastOldData());
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{1L, 20L}, {2L, 20L}, {3L, 20L}});

        sendEvent(-2);
        assertEquals(8L, testListener.getLastOldData()[0].get("mySum"));
        assertEquals(8L, testListener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{4L, 8L}, {2L, 8L}, {3L, 8L}});

        sendEvent(100);
        assertEquals(93L, testListener.getLastOldData()[0].get("mySum"));
        assertEquals(93L, testListener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{4L, 93L}, {5L, 93L}, {3L, 93L}});

        sendEvent(1000);
        assertEquals(1098L, testListener.getLastOldData()[0].get("mySum"));
        assertEquals(1098L, testListener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{4L, 1098L}, {5L, 1098L}, {6L, 1098L}});
    }

    private void sendEvent(long longBoxed, int intBoxed, short shortBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        bean.setLongPrimitive(++eventCount);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketDataEvent(String symbol, Long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(long longBoxed)
    {
        sendEvent(longBoxed, 0, (short)0);
    }

    private void sendEvent(String theString, int intPrimitive, int intBoxed) {
        SupportBean theEvent = new SupportBean(theString, intPrimitive);
        theEvent.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private static final Logger log = LoggerFactory.getLogger(TestAggregateRowPerEvent.class);
}
