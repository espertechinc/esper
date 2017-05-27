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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class ExecQuerytypeRowPerEventDistinct implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public void run(EPServiceProvider epService) throws Exception {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = "select irstream symbol, sum(distinct volume) as volSum " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("volSum"));

        sendEvent(epService, SYMBOL_DELL, 10000);
        assertEvents(listener, SYMBOL_DELL, 10000);

        sendEvent(epService, SYMBOL_DELL, 10000);
        assertEvents(listener, SYMBOL_DELL, 10000);       // still 10k since summing distinct volumes

        sendEvent(epService, SYMBOL_DELL, 20000);
        assertEvents(listener, SYMBOL_DELL, 30000);

        sendEvent(epService, SYMBOL_IBM, 1000);
        assertEvents(listener, SYMBOL_DELL, 31000, SYMBOL_IBM, 31000);

        sendEvent(epService, SYMBOL_IBM, 1000);
        assertEvents(listener, SYMBOL_DELL, 21000, SYMBOL_IBM, 21000);

        sendEvent(epService, SYMBOL_IBM, 1000);
        assertEvents(listener, SYMBOL_DELL, 1000, SYMBOL_IBM, 1000);

        stmt.destroy();
    }

    private void assertEvents(SupportUpdateListener listener, String symbol, long volSum) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEquals(symbol, newData[0].get("symbol"));
        assertEquals(volSum, newData[0].get("volSum"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void assertEvents(SupportUpdateListener listener, String symbolOld, long volSumOld,
                              String symbolNew, long volSumNew) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbolOld, oldData[0].get("symbol"));
        assertEquals(volSumOld, oldData[0].get("volSum"));

        assertEquals(symbolNew, newData[0].get("symbol"));
        assertEquals(volSumNew, newData[0].get("volSum"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void sendEvent(EPServiceProvider epService, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecQuerytypeRowPerEventDistinct.class);
}
