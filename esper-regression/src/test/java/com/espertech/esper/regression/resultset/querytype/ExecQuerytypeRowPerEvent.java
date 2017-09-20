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
package com.espertech.esper.regression.resultset.querytype;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ExecQuerytypeRowPerEvent implements RegressionExecution {
    private final static String JOIN_KEY = "KEY";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSumOneView(epService);
        runAssertionSumJoin(epService);
        runAssertionAggregatedSelectTriggerEvent(epService);
        runAssertionAggregatedSelectUnaggregatedHaving(epService);
        runAssertionSumAvgWithWhere(epService);
    }

    private void runAssertionAggregatedSelectTriggerEvent(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        String epl = "select window(s0.*) as rows, sb " +
                "from SupportBean#keepall as sb, SupportBean_S0#keepall as s0 " +
                "where sb.theString = s0.p00";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "K1", "V1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "K1", "V2"));

        // test SB-direction
        SupportBean b1 = new SupportBean("K1", 0);
        epService.getEPRuntime().sendEvent(b1);
        EventBean[] events = listener.getAndResetLastNewData();
        assertEquals(2, events.length);
        for (EventBean event : events) {
            assertEquals(b1, event.get("sb"));
            assertEquals(2, ((SupportBean_S0[]) event.get("rows")).length);
        }

        // test S0-direction
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "K1", "V3"));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEquals(b1, event.get("sb"));
        assertEquals(3, ((SupportBean_S0[]) event.get("rows")).length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAggregatedSelectUnaggregatedHaving(EPServiceProvider epService) {
        // ESPER-571
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String epl = "select max(intPrimitive) as val from SupportBean#time(1) having max(intPrimitive) > intBoxed";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 10, 1);
        assertEquals(10, listener.assertOneGetNewAndReset().get("val"));

        sendEvent(epService, "E2", 10, 11);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E3", 15, 11);
        assertEquals(15, listener.assertOneGetNewAndReset().get("val"));

        sendEvent(epService, "E4", 20, 11);
        assertEquals(20, listener.assertOneGetNewAndReset().get("val"));

        sendEvent(epService, "E5", 25, 25);
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionSumOneView(EPServiceProvider epService) {
        String epl = "select irstream longPrimitive, sum(longBoxed) as mySum " +
                "from " + SupportBean.class.getName() + "#length(3)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssert(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionSumJoin(EPServiceProvider epService) {
        String epl = "select irstream longPrimitive, sum(longBoxed) as mySum " +
                "from " + SupportBeanString.class.getName() + "#length(3) as one, " +
                SupportBean.class.getName() + "#length(3) as two " +
                "where one.theString = two.theString";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));

        tryAssert(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionSumAvgWithWhere(EPServiceProvider epService) {
        String epl = "select 'IBM stats' as title, volume, avg(volume) as myAvg, sum(volume) as mySum " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3)" +
                "where symbol='IBM'";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMarketDataEvent(epService, "GE", 10L);
        assertFalse(listener.isInvoked());

        sendMarketDataEvent(epService, "IBM", 20L);
        assertPostedNew(listener, 20d, 20L);

        sendMarketDataEvent(epService, "XXX", 10000L);
        assertFalse(listener.isInvoked());

        sendMarketDataEvent(epService, "IBM", 30L);
        assertPostedNew(listener, 25d, 50L);

        stmt.destroy();
    }

    private void assertPostedNew(SupportUpdateListener listener, Double newAvg, Long newSum) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEquals("IBM stats", newData[0].get("title"));
        assertEquals(newAvg, newData[0].get("myAvg"));
        assertEquals(newSum, newData[0].get("mySum"));

        listener.reset();
    }

    private void tryAssert(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        String[] fields = new String[]{"longPrimitive", "mySum"};

        // assert select result type
        assertEquals(Long.class, stmt.getEventType().getPropertyType("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        AtomicInteger eventCount = new AtomicInteger();

        sendEvent(epService, eventCount, 10);
        assertEquals(10L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1L, 10L}});

        sendEvent(epService, eventCount, 15);
        assertEquals(25L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1L, 25L}, {2L, 25L}});

        sendEvent(epService, eventCount, -5);
        assertEquals(20L, listener.getAndResetLastNewData()[0].get("mySum"));
        assertNull(listener.getLastOldData());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1L, 20L}, {2L, 20L}, {3L, 20L}});

        sendEvent(epService, eventCount, -2);
        assertEquals(8L, listener.getLastOldData()[0].get("mySum"));
        assertEquals(8L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{4L, 8L}, {2L, 8L}, {3L, 8L}});

        sendEvent(epService, eventCount, 100);
        assertEquals(93L, listener.getLastOldData()[0].get("mySum"));
        assertEquals(93L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{4L, 93L}, {5L, 93L}, {3L, 93L}});

        sendEvent(epService, eventCount, 1000);
        assertEquals(1098L, listener.getLastOldData()[0].get("mySum"));
        assertEquals(1098L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{4L, 1098L}, {5L, 1098L}, {6L, 1098L}});
    }

    private void sendEvent(EPServiceProvider epService, long longBoxed, int intBoxed, short shortBoxed, AtomicInteger eventCount) {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        bean.setLongPrimitive(eventCount.incrementAndGet());
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketDataEvent(EPServiceProvider epService, String symbol, Long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, AtomicInteger eventCount, long longBoxed) {
        sendEvent(epService, longBoxed, 0, (short) 0, eventCount);
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive, int intBoxed) {
        SupportBean theEvent = new SupportBean(theString, intPrimitive);
        theEvent.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecQuerytypeRowPerEvent.class);
}
