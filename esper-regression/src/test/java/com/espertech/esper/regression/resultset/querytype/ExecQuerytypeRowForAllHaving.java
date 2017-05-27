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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecQuerytypeRowForAllHaving implements RegressionExecution {
    private final static String JOIN_KEY = "KEY";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSumOneView(epService);
        runAssertionSumJoin(epService);
        runAssertionAvgGroupWindow(epService);
    }

    private void runAssertionSumOneView(EPServiceProvider epService) {
        String epl = "select irstream sum(longBoxed) as mySum " +
                "from " + SupportBean.class.getName() + "#time(10 seconds) " +
                "having sum(longBoxed) > 10";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssert(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionSumJoin(EPServiceProvider epService) {
        String epl = "select irstream sum(longBoxed) as mySum " +
                "from " + SupportBeanString.class.getName() + "#time(10 seconds) as one, " +
                SupportBean.class.getName() + "#time(10 seconds) as two " +
                "where one.theString = two.theString " +
                "having sum(longBoxed) > 10";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));

        tryAssert(epService, listener, stmt);

        stmt.destroy();
    }

    private void tryAssert(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        // assert select result type
        assertEquals(Long.class, stmt.getEventType().getPropertyType("mySum"));

        sendTimerEvent(epService, 0);
        sendEvent(epService, 10);
        assertFalse(listener.isInvoked());

        sendTimerEvent(epService, 5000);
        sendEvent(epService, 15);
        assertEquals(25L, listener.getAndResetLastNewData()[0].get("mySum"));

        sendTimerEvent(epService, 8000);
        sendEvent(epService, -5);
        assertEquals(20L, listener.getAndResetLastNewData()[0].get("mySum"));
        assertNull(listener.getLastOldData());

        sendTimerEvent(epService, 10000);
        assertEquals(20L, listener.getLastOldData()[0].get("mySum"));
        assertNull(listener.getAndResetLastNewData());
    }

    private void runAssertionAvgGroupWindow(EPServiceProvider epService) {
        //String stmtText = "select istream avg(price) as aprice from "+ SupportMarketDataBean.class.getName()
        //        +"#groupwin(symbol)#length(1) having avg(price) <= 0";
        String stmtText = "select istream avg(price) as aprice from " + SupportMarketDataBean.class.getName()
                + "#unique(symbol) having avg(price) <= 0";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "A", -1);
        assertEquals(-1.0d, listener.getLastNewData()[0].get("aprice"));
        listener.reset();

        sendEvent(epService, "A", 5);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "B", -6);
        assertEquals(-.5d, listener.getLastNewData()[0].get("aprice"));
        listener.reset();

        sendEvent(epService, "C", 2);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "C", 3);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "C", -2);
        assertEquals(-1d, listener.getLastNewData()[0].get("aprice"));
        listener.reset();

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
