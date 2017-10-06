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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.bean.SupportPriceEvent;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecQuerytypeRowForAll implements RegressionExecution {
    private final static String JOIN_KEY = "KEY";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSumOneView(epService);
        runAssertionSumJoin(epService);
        runAssertionAvgPerSym(epService);
        runAssertionSelectStarStdGroupBy(epService);
        runAssertionSelectExprStdGroupBy(epService);
        runAssertionSelectAvgExprStdGroupBy(epService);
        runAssertionSelectAvgStdGroupByUni(epService);
    }

    private void runAssertionSumOneView(EPServiceProvider epService) {
        String epl = "select irstream sum(longBoxed) as mySum " +
                "from " + SupportBean.class.getName() + "#time(10 sec)";

        sendTimerEvent(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssert(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionSumJoin(EPServiceProvider epService) {
        String epl = "select irstream sum(longBoxed) as mySum " +
                "from " + SupportBeanString.class.getName() + "#keepall as one, " +
                SupportBean.class.getName() + "#time(10 sec) as two " +
                "where one.theString = two.theString";

        sendTimerEvent(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));

        tryAssert(epService, stmt, listener);

        stmt.destroy();
    }

    private void tryAssert(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        // assert select result type
        assertEquals(Long.class, stmt.getEventType().getPropertyType("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"mySum"}, new Object[][]{{null}});

        sendTimerEvent(epService, 0);
        sendEvent(epService, 10);
        assertEquals(10L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"mySum"}, new Object[][]{{10L}});

        sendTimerEvent(epService, 5000);
        sendEvent(epService, 15);
        assertEquals(25L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"mySum"}, new Object[][]{{25L}});

        sendTimerEvent(epService, 8000);
        sendEvent(epService, -5);
        assertEquals(20L, listener.getAndResetLastNewData()[0].get("mySum"));
        assertNull(listener.getLastOldData());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"mySum"}, new Object[][]{{20L}});

        sendTimerEvent(epService, 10000);
        assertEquals(20L, listener.getLastOldData()[0].get("mySum"));
        assertEquals(10L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"mySum"}, new Object[][]{{10L}});

        sendTimerEvent(epService, 15000);
        assertEquals(10L, listener.getLastOldData()[0].get("mySum"));
        assertEquals(-5L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"mySum"}, new Object[][]{{-5L}});

        sendTimerEvent(epService, 18000);
        assertEquals(-5L, listener.getLastOldData()[0].get("mySum"));
        assertNull(listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"mySum"}, new Object[][]{{null}});
    }

    private void runAssertionAvgPerSym(EPServiceProvider epService) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream avg(price) as avgp, sym from " + SupportPriceEvent.class.getName() + "#groupwin(sym)#length(2)"
        );
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(1, "A"));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("A", theEvent.get("sym"));
        assertEquals(1.0, theEvent.get("avgp"));

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(2, "B"));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("B", theEvent.get("sym"));
        assertEquals(1.5, theEvent.get("avgp"));

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(9, "A"));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("A", theEvent.get("sym"));
        assertEquals((1 + 2 + 9) / 3.0, theEvent.get("avgp"));

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(18, "B"));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("B", theEvent.get("sym"));
        assertEquals((1 + 2 + 9 + 18) / 4.0, theEvent.get("avgp"));

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(5, "A"));
        theEvent = listener.getLastNewData()[0];
        assertEquals("A", theEvent.get("sym"));
        assertEquals((2 + 9 + 18 + 5) / 4.0, theEvent.get("avgp"));
        theEvent = listener.getLastOldData()[0];
        assertEquals("A", theEvent.get("sym"));
        assertEquals((5 + 2 + 9 + 18) / 4.0, theEvent.get("avgp"));

        stmt.destroy();
    }

    private void runAssertionSelectStarStdGroupBy(EPServiceProvider epService) {
        String stmtText = "select istream * from " + SupportMarketDataBean.class.getName()
                + "#groupwin(symbol)#length(2)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "A", 1);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1.0, listener.getLastNewData()[0].get("price"));
        assertTrue(listener.getLastNewData()[0].getUnderlying() instanceof SupportMarketDataBean);

        statement.destroy();
    }

    private void runAssertionSelectExprStdGroupBy(EPServiceProvider epService) {
        String stmtText = "select istream price from " + SupportMarketDataBean.class.getName()
                + "#groupwin(symbol)#length(2)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "A", 1);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1.0, listener.getLastNewData()[0].get("price"));

        statement.destroy();
    }

    private void runAssertionSelectAvgExprStdGroupBy(EPServiceProvider epService) {
        String stmtText = "select istream avg(price) as aprice from " + SupportMarketDataBean.class.getName()
                + "#groupwin(symbol)#length(2)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "A", 1);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1.0, listener.getLastNewData()[0].get("aprice"));
        sendEvent(epService, "B", 3);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(2.0, listener.getLastNewData()[0].get("aprice"));

        statement.destroy();
    }

    private void runAssertionSelectAvgStdGroupByUni(EPServiceProvider epService) {
        String stmtText = "select istream average as aprice from " + SupportMarketDataBean.class.getName()
                + "#groupwin(symbol)#length(2)#uni(price)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "A", 1);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(1.0, listener.getLastNewData()[0].get("aprice"));
        sendEvent(epService, "B", 3);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(3.0, listener.getLastNewData()[0].get("aprice"));
        sendEvent(epService, "A", 3);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(2.0, listener.getLastNewData()[0].get("aprice"));
        sendEvent(epService, "A", 10);
        sendEvent(epService, "A", 20);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(15.0, listener.getLastNewData()[0].get("aprice"));

        statement.destroy();
    }

    private Object sendEvent(EPServiceProvider epService, String symbol, double price) {
        Object theEvent = new SupportMarketDataBean(symbol, price, null, null);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }

    private void sendEvent(EPServiceProvider epService, long longBoxed, int intBoxed, short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, long longBoxed) {
        sendEvent(epService, longBoxed, 0, (short) 0);
    }

    private void sendTimerEvent(EPServiceProvider epService, long msec) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(msec));
    }
}
