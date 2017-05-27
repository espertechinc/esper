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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecViewTimeWin implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionWinTimeSum(epService);
        runAssertionWinTimeSumGroupBy(epService);
        runAssertionWinTimeSumSingle(epService);
    }

    private void runAssertionWinTimeSum(EPServiceProvider epService) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String sumTimeExpr = "select symbol, volume, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() + "#time(30)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(sumTimeExpr);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        tryAssertion(epService, testListener, stmt);

        stmt.destroy();
    }

    private void runAssertionWinTimeSumGroupBy(EPServiceProvider epService) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String sumTimeUniExpr = "select symbol, volume, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() +
                "#time(30) group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(sumTimeUniExpr);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        tryGroupByAssertions(epService, testListener, stmt);

        stmt.destroy();
    }

    private void runAssertionWinTimeSumSingle(EPServiceProvider epService) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String sumTimeUniExpr = "select symbol, volume, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() +
                "(symbol = 'IBM')#time(30)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(sumTimeUniExpr);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        trySingleAssertion(epService, testListener, stmt);

        stmt.destroy();
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener testListener, EPStatement stmt) {
        assertSelectResultType(stmt);

        CurrentTimeEvent currentTime = new CurrentTimeEvent(0);
        epService.getEPRuntime().sendEvent(currentTime);

        sendEvent(epService, SYMBOL_DELL, 10000, 51);
        assertEvents(testListener, SYMBOL_DELL, 10000, 51, false);

        sendEvent(epService, SYMBOL_IBM, 20000, 52);
        assertEvents(testListener, SYMBOL_IBM, 20000, 103, false);

        sendEvent(epService, SYMBOL_DELL, 40000, 45);
        assertEvents(testListener, SYMBOL_DELL, 40000, 148, false);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));

        //These events are out of the window and new sums are generated

        sendEvent(epService, SYMBOL_IBM, 30000, 70);
        assertEvents(testListener, SYMBOL_IBM, 30000, 70, false);

        sendEvent(epService, SYMBOL_DELL, 10000, 20);
        assertEvents(testListener, SYMBOL_DELL, 10000, 90, false);
    }

    private void tryGroupByAssertions(EPServiceProvider epService, SupportUpdateListener testListener, EPStatement stmt) {
        assertSelectResultType(stmt);

        CurrentTimeEvent currentTime = new CurrentTimeEvent(0);
        epService.getEPRuntime().sendEvent(currentTime);

        sendEvent(epService, SYMBOL_DELL, 10000, 51);
        assertEvents(testListener, SYMBOL_DELL, 10000, 51, false);

        sendEvent(epService, SYMBOL_IBM, 30000, 70);
        assertEvents(testListener, SYMBOL_IBM, 30000, 70, false);

        sendEvent(epService, SYMBOL_DELL, 20000, 52);
        assertEvents(testListener, SYMBOL_DELL, 20000, 103, false);

        sendEvent(epService, SYMBOL_IBM, 30000, 70);
        assertEvents(testListener, SYMBOL_IBM, 30000, 140, false);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));

        //These events are out of the window and new sums are generated
        sendEvent(epService, SYMBOL_DELL, 10000, 90);
        assertEvents(testListener, SYMBOL_DELL, 10000, 90, false);

        sendEvent(epService, SYMBOL_IBM, 30000, 120);
        assertEvents(testListener, SYMBOL_IBM, 30000, 120, false);

        sendEvent(epService, SYMBOL_DELL, 20000, 90);
        assertEvents(testListener, SYMBOL_DELL, 20000, 180, false);

        sendEvent(epService, SYMBOL_IBM, 30000, 120);
        assertEvents(testListener, SYMBOL_IBM, 30000, 240, false);
    }

    private void trySingleAssertion(EPServiceProvider epService, SupportUpdateListener testListener, EPStatement stmt) {
        assertSelectResultType(stmt);

        CurrentTimeEvent currentTime = new CurrentTimeEvent(0);
        epService.getEPRuntime().sendEvent(currentTime);

        sendEvent(epService, SYMBOL_IBM, 20000, 52);
        assertEvents(testListener, SYMBOL_IBM, 20000, 52, false);

        sendEvent(epService, SYMBOL_IBM, 20000, 100);
        assertEvents(testListener, SYMBOL_IBM, 20000, 152, false);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));

        //These events are out of the window and new sums are generated
        sendEvent(epService, SYMBOL_IBM, 20000, 252);
        assertEvents(testListener, SYMBOL_IBM, 20000, 252, false);

        sendEvent(epService, SYMBOL_IBM, 20000, 100);
        assertEvents(testListener, SYMBOL_IBM, 20000, 352, false);
    }

    private void assertEvents(SupportUpdateListener testListener, String symbol, long volume, double sum, boolean unique) {
        EventBean[] oldData = testListener.getLastOldData();
        EventBean[] newData = testListener.getLastNewData();

        if (!unique)
            assertNull(oldData);

        assertEquals(1, newData.length);

        assertEquals(symbol, newData[0].get("symbol"));
        assertEquals(volume, newData[0].get("volume"));
        assertEquals(sum, newData[0].get("mySum"));

        testListener.reset();
        assertFalse(testListener.isInvoked());
    }

    private void assertSelectResultType(EPStatement stmt) {
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("volume"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));
    }

    private void sendEvent(EPServiceProvider epService, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }
}
