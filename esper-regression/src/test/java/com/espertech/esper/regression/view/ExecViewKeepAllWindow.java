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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecViewKeepAllWindow implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionIterator(epService);
        runAssertionWindowStats(epService);
    }

    private void runAssertionIterator(EPServiceProvider epService) {
        String epl = "select symbol, price from " + SupportMarketDataBean.class.getName() + "#keepall";
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "ABC", 20);
        sendEvent(epService, "DEF", 100);

        // check iterator results
        Iterator<EventBean> events = statement.iterator();
        EventBean theEvent = events.next();
        assertEquals("ABC", theEvent.get("symbol"));
        assertEquals(20d, theEvent.get("price"));

        theEvent = events.next();
        assertEquals("DEF", theEvent.get("symbol"));
        assertEquals(100d, theEvent.get("price"));
        assertFalse(events.hasNext());

        sendEvent(epService, "EFG", 50);

        // check iterator results
        events = statement.iterator();
        theEvent = events.next();
        assertEquals("ABC", theEvent.get("symbol"));
        assertEquals(20d, theEvent.get("price"));

        theEvent = events.next();
        assertEquals("DEF", theEvent.get("symbol"));
        assertEquals(100d, theEvent.get("price"));

        theEvent = events.next();
        assertEquals("EFG", theEvent.get("symbol"));
        assertEquals(50d, theEvent.get("price"));

        statement.destroy();
    }

    private void runAssertionWindowStats(EPServiceProvider epService) {
        String epl = "select irstream symbol, count(*) as cnt, sum(price) as mysum from " + SupportMarketDataBean.class.getName() +
                "#keepall group by symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        listener.reset();

        sendEvent(epService, "S1", 100);
        String[] fields = new String[]{"symbol", "cnt", "mysum"};
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"S1", 1L, 100d});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"S1", 0L, null});
        listener.reset();

        sendEvent(epService, "S2", 50);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"S2", 1L, 50d});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"S2", 0L, null});
        listener.reset();

        sendEvent(epService, "S1", 5);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"S1", 2L, 105d});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"S1", 1L, 100d});
        listener.reset();

        sendEvent(epService, "S2", -1);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"S2", 2L, 49d});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"S2", 1L, 50d});
        listener.reset();

        statement.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, 0L, "");
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
