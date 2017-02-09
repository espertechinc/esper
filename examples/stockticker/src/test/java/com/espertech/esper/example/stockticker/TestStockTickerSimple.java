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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.example.stockticker.eventbean.LimitAlert;
import com.espertech.esper.example.stockticker.eventbean.PriceLimit;
import com.espertech.esper.example.stockticker.eventbean.StockTick;
import com.espertech.esper.example.stockticker.monitor.StockTickerMonitor;
import com.espertech.esper.example.stockticker.monitor.StockTickerResultListener;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestStockTickerSimple extends TestCase {
    private StockTickerResultListener listener;
    private EPServiceProvider epService;

    protected void setUp() throws Exception {
        listener = new StockTickerResultListener();

        Configuration configuration = new Configuration();
        configuration.addEventType("PriceLimit", PriceLimit.class.getName());
        configuration.addEventType("StockTick", StockTick.class.getName());

        epService = EPServiceProviderManager.getProvider("TestStockTickerSimple", configuration);

        // To reduce logging noise and get max performance
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
    }

    public void testStockTicker() throws Exception {
        log.info(".testStockTicker");

        new StockTickerMonitor(epService, listener);

        performEventFlowTest();
        performBoundaryTest();
    }

    public void performEventFlowTest() {
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

        log.debug(".testEvents");
        listener.clearMatched();

        // Set a limit
        sendEvent(new PriceLimit(USER_ID_ONE, STOCK_NAME, LIMIT_PERCENT));
        assertTrue(listener.getSize() == 0);

        // First stock ticker sets the initial price
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE));

        // Go within the limit, expect no response
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_LOW));
        assertTrue(listener.getSize() == 0);

        // Go outside the limit, expect an event
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_LOW));
        sleep(500);
        assertTrue(listener.getSize() == 1);
        listener.clearMatched();

        // Go within the limit, expect no response
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_HIGH));
        assertTrue(listener.getSize() == 0);

        // Go outside the limit, expect an event
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_HIGH));
        sleep(500);
        assertTrue(listener.getSize() == 1);
        LimitAlert alert = (LimitAlert) listener.getMatchEvents().get(0);
        listener.clearMatched();
        assertTrue(alert.getInitialPrice() == STOCK_PRICE);
        assertTrue(alert.getPriceLimit().getUserId().equals(USER_ID_ONE));
        assertTrue(alert.getPriceLimit().getStockSymbol().equals(STOCK_NAME));
        assertTrue(alert.getPriceLimit().getLimitPct() == LIMIT_PERCENT);
        assertTrue(alert.getTick().getStockSymbol().equals(STOCK_NAME));
        assertTrue(alert.getTick().getPrice() == STOCK_PRICE_OUTSIDE_LIMIT_HIGH);

        // Set a new limit for the same stock
        // With the new limit none of these should fire
        sendEvent(new PriceLimit(USER_ID_ONE, STOCK_NAME, LIMIT_PERCENT_LARGE));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_LOW));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_LOW));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_HIGH));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_HIGH));
        sleep(500);
        assertTrue(listener.getSize() == 0);

        // Set a smaller limit for another couple of users
        sendEvent(new PriceLimit(USER_ID_TWO, STOCK_NAME, LIMIT_PERCENT));
        sendEvent(new PriceLimit(USER_ID_THREE, STOCK_NAME, LIMIT_PERCENT_LARGE));

        // Set limit back to original limit, send same prices, expect exactly 2 event
        sendEvent(new PriceLimit(USER_ID_ONE, STOCK_NAME, LIMIT_PERCENT));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_LOW));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_LOW));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_WITHIN_LIMIT_HIGH));
        sendEvent(new StockTick(STOCK_NAME, STOCK_PRICE_OUTSIDE_LIMIT_HIGH));
        sleep(500);

        log.info(".performEventFlowTest listSize=" + listener.getSize());
        assertTrue(listener.getSize() == 4);
    }

    public void performBoundaryTest() {
        final String STOCK_NAME = "BOUNDARY_TEST";

        listener.clearMatched();
        sendEvent(new PriceLimit("junit", STOCK_NAME, 25.0));
        sendEvent(new StockTick(STOCK_NAME, 46.0));
        sendEvent(new StockTick(STOCK_NAME, 46.0 - 11.5));
        sendEvent(new StockTick(STOCK_NAME, 46.0 + 11.5));
        sleep(500);
        assertTrue(listener.getSize() == 0);

        sendEvent(new StockTick(STOCK_NAME, 46.0 - 11.5001));
        sendEvent(new StockTick(STOCK_NAME, 46.0 + 11.5001));
        sleep(500);
        assertTrue(listener.getSize() == 2);
    }

    private void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            log.error("Interrupted: {}", e.getMessage(), e);
        }
    }

    private void sendEvent(Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private static final Logger log = LoggerFactory.getLogger(TestStockTickerSimple.class);
}
