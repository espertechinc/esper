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
package com.espertech.esper.example.stockticker;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.example.stockticker.StockTickerEPLUtil.*;

public class TestStockTickerSimple extends TestCase {
    private StockTickerAlertListener listener;
    private EPRuntime runtime;

    protected void setUp() {
        Configuration configuration = getConfiguration();
        EPCompiled compiled = compileEPL(configuration);

        runtime = EPRuntimeProvider.getRuntime("TestStockTickerSimple", configuration);
        runtime.getEventService().clockExternal();

        deploy(runtime, compiled);

        listener = new StockTickerAlertListener();
        runtime.getDeploymentService().getStatement("stockticker", "alert").addListener(listener);
    }

    public void testStockTicker() {
        performEventFlowTest();
        performBoundaryTest();
    }

    private void performEventFlowTest() {
        final String STOCK_NAME = "IBM.N";
        final double STOCK_PRICE = 50;
        final double LIMIT_PERCENT = 10;
        final double LIMIT_PERCENT_LARGE = 20;
        final String USER_ID_ONE = "junit";
        final String USER_ID_TWO = "jack";
        final String USER_ID_THREE = "anna";

        final double STOCK_PRICE_WITHIN_LIMIT_LOW = 46.0;
        final double STOCK_PRICE_OUTSIDE_LIMIT_LOW = 44.9;
        final double STOCK_PRICE_WITHIN_LIMIT_HIGH = 51.0;
        final double STOCK_PRICE_OUTSIDE_LIMIT_HIGH = 55.01;

        listener.clearMatched();

        // Set a limit
        sendEvent(new PriceLimit(USER_ID_ONE, STOCK_NAME, LIMIT_PERCENT));
        assertEquals(0, listener.getSize());

        // First stock ticker sets the initial price
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE));

        // Go within the limit, expect no response
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_LOW));
        assertEquals(0, listener.getSize());

        // Go outside the limit, expect an event
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_LOW));
        assertEquals(1, listener.getSize());
        listener.clearMatched();

        // Go within the limit, expect no response
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_HIGH));
        assertEquals(0, listener.getSize());

        // Go outside the limit, expect an event
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_HIGH));
        assertEquals(1, listener.getSize());
        LimitAlert alert = (LimitAlert) listener.getMatchEvents().get(0);
        listener.clearMatched();
        assertEquals(STOCK_PRICE, alert.getReference());
        assertEquals(USER_ID_ONE, alert.getPriceLimit().getUserId());
        assertEquals(STOCK_NAME, alert.getPriceLimit().getSymbol());
        assertEquals(LIMIT_PERCENT, alert.getPriceLimit().getLimitPct());
        assertEquals(STOCK_NAME, alert.getTick().getSymbol());
        assertEquals(STOCK_PRICE_OUTSIDE_LIMIT_HIGH, alert.getTick().getPrice());

        // Set a new limit for the same stock
        // With the new limit none of these should fire
        sendEvent(new PriceLimit(USER_ID_ONE, STOCK_NAME, LIMIT_PERCENT_LARGE));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_LOW));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_LOW));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_HIGH));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_HIGH));
        assertEquals(0, listener.getSize());

        // Set a smaller limit for another couple of users
        sendEvent(new PriceLimit(USER_ID_TWO, STOCK_NAME, LIMIT_PERCENT));
        sendEvent(new PriceLimit(USER_ID_THREE, STOCK_NAME, LIMIT_PERCENT_LARGE));

        // Set limit back to original limit, send same prices, expect exactly 2 event
        sendEvent(new PriceLimit(USER_ID_ONE, STOCK_NAME, LIMIT_PERCENT));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_LOW));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_LOW));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_HIGH));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_HIGH));

        assertEquals(4, listener.getSize());
    }

    private void performBoundaryTest() {
        final String STOCK_NAME = "BOUNDARY_TEST";

        listener.clearMatched();
        sendEvent(new PriceLimit("junit", STOCK_NAME, 25.0));
        sendEvent(new StockTick(STOCK_NAME, 46.0));
        sendEvent(new StockTick(STOCK_NAME, 46.0 - 11.5));
        sendEvent(new StockTick(STOCK_NAME, 46.0 + 11.5));
        assertEquals(0, listener.getSize());

        sendEvent(new StockTick(STOCK_NAME, 46.0 - 11.5001));
        sendEvent(new StockTick(STOCK_NAME, 46.0 + 11.5001));
        assertEquals(2, listener.getSize());
    }

    private void sendEvent(Object theEvent) {
        runtime.getEventService().sendEventBean(theEvent, theEvent.getClass().getSimpleName());
    }

    private static final Logger log = LoggerFactory.getLogger(TestStockTickerSimple.class);
}
