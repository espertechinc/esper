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
package com.espertech.esper.example.stockticker.monitor;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.example.stockticker.eventbean.PriceLimit;
import com.espertech.esper.example.stockticker.eventbean.StockTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockTickerMonitor {
    private final EPServiceProvider epService;
    private final StockTickerResultListener stockTickerResultListener;

    private PriceLimit limit = null;

    private EPStatement newLimitListener = null;
    private EPStatement initialPriceListener = null;
    private EPStatement lowPriceListener = null;
    private EPStatement highPriceListener = null;

    public StockTickerMonitor(EPServiceProvider epService, final StockTickerResultListener stockTickerResultListener) {
        this.epService = epService;
        this.stockTickerResultListener = stockTickerResultListener;

        // Listen to all limits to be set
        String expressionText = "every pricelimit=PriceLimit()";
        EPStatement factory = epService.getEPAdministrator().createPattern(expressionText);

        factory.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                PriceLimit limitBean = (PriceLimit) newEvents[0].get("pricelimit");

                if (log.isDebugEnabled()) {
                    log.debug(".update Received new limit, user=" + limitBean.getUserId() +
                            "  stock=" + limitBean.getStockSymbol() +
                            "  pct=" + limitBean.getLimitPct());
                }

                new StockTickerMonitor(StockTickerMonitor.this.epService, limitBean, stockTickerResultListener);
            }
        });
    }

    public StockTickerMonitor(EPServiceProvider epService, PriceLimit limit, final StockTickerResultListener stockTickerResultListener) {
        this.epService = epService;
        this.limit = limit;
        this.stockTickerResultListener = stockTickerResultListener;

        String expressionText = "every pricelimit=PriceLimit" +
                "(userId='" + limit.getUserId() + "'," +
                "stockSymbol='" + limit.getStockSymbol() + "')";
        newLimitListener = epService.getEPAdministrator().createPattern(expressionText);

        newLimitListener.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                if (log.isDebugEnabled()) {
                    log.debug(".update Received an override limit, stopping listeners");
                }

                die();
            }
        });

        expressionText = "tick=StockTick(stockSymbol='" + limit.getStockSymbol() + "')";
        initialPriceListener = epService.getEPAdministrator().createPattern(expressionText);

        initialPriceListener.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                StockTick tick = (StockTick) newEvents[0].get("tick");
                PriceLimit limit = StockTickerMonitor.this.limit;

                initialPriceListener = null;

                double limitPct = limit.getLimitPct();
                double upperLimit = tick.getPrice() * (1.0 + (limitPct / 100.0));
                double lowerLimit = tick.getPrice() * (1.0 - (limitPct / 100.0));

                if (log.isDebugEnabled()) {
                    log.debug(".update Received initial tick, stock=" + tick.getStockSymbol() +
                            "  price=" + tick.getPrice() +
                            "  limit.limitPct=" + limitPct +
                            "  lowerLimit=" + lowerLimit +
                            "  upperLimit=" + upperLimit);
                }

                StockTickerAlertListener listener = new StockTickerAlertListener(StockTickerMonitor.this.epService, limit, tick, stockTickerResultListener);

                String expressionText = "every tick=StockTick" +
                        "(stockSymbol='" + limit.getStockSymbol() + "', price < " + lowerLimit + ")";
                lowPriceListener = StockTickerMonitor.this.epService.getEPAdministrator().createPattern(expressionText);
                lowPriceListener.addListener(listener);

                expressionText = "every tick=StockTick" +
                        "(stockSymbol='" + limit.getStockSymbol() + "', price > " + upperLimit + ")";
                highPriceListener = StockTickerMonitor.this.epService.getEPAdministrator().createPattern(expressionText);
                highPriceListener.addListener(listener);
            }
        });
    }

    private void die() {
        if (newLimitListener != null) newLimitListener.removeAllListeners();
        if (initialPriceListener != null) initialPriceListener.removeAllListeners();
        if (lowPriceListener != null) lowPriceListener.removeAllListeners();
        if (highPriceListener != null) highPriceListener.removeAllListeners();
    }

    private static final Logger log = LoggerFactory.getLogger(StockTickerMonitor.class);
}
