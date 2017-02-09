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

import com.espertech.esper.example.stockticker.eventbean.PriceLimit;
import com.espertech.esper.example.stockticker.eventbean.StockTick;
import junit.framework.TestCase;

import java.util.List;

public class TestStockTickerGenerator extends TestCase implements StockTickerRegressionConstants {
    public final static int NUM_STOCK_NAMES = 1000;

    public void testFlow() {
        StockTickerEventGenerator generator = new StockTickerEventGenerator();

        PriceLimit[] limitBeans = generator.makeLimits("junit",
                NUM_STOCK_NAMES, PRICE_LIMIT_PCT_LOWER_LIMIT, PRICE_LIMIT_PCT_UPPER_LIMIT);

        assertTrue(limitBeans.length == NUM_STOCK_NAMES);
        assertTrue(limitBeans[0].getUserId().equals("junit"));
        for (int i = 0; i < limitBeans.length; i++) {
            assertTrue(limitBeans[i].getLimitPct() >= PRICE_LIMIT_PCT_LOWER_LIMIT);
            assertTrue(limitBeans[i].getLimitPct() <= PRICE_LIMIT_PCT_UPPER_LIMIT);
        }

        StockTick[] initialPrices = generator.makeInitialPriceStockTicks(limitBeans,
                PRICE_LOWER_LIMIT, PRICE_LOWER_LIMIT);

        assertTrue(initialPrices.length == NUM_STOCK_NAMES);
        for (int i = 0; i < initialPrices.length; i++) {
            assertTrue(initialPrices[i].getPrice() >= PRICE_LOWER_LIMIT);
            assertTrue(initialPrices[i].getPrice() <= PRICE_UPPER_LIMIT);
        }

        for (int i = 0; i < 100000; i++) {
            StockTick tick = generator.makeStockTick(limitBeans[0], initialPrices[0]);

            double initialPrice = initialPrices[0].getPrice();
            double range = initialPrice * limitBeans[0].getLimitPct() / 100;

            assertTrue(tick.getPrice() > (initialPrice - range) - 1);
            assertTrue(tick.getPrice() < (initialPrice + range) + 1);
        }
    }

    public void testMakeStream() throws Exception {
        StockTickerEventGenerator generator = new StockTickerEventGenerator();

        final int NUM_EVENTS = 1000;

        List stream = generator.makeEventStream(NUM_EVENTS, 1000, NUM_STOCK_NAMES, 25, 30, 46, 54, true);

        assertTrue(stream.size() == (NUM_STOCK_NAMES * 2 + NUM_EVENTS));
    }
}
