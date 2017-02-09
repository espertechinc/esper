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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.example.stockticker.eventbean.LimitAlert;
import com.espertech.esper.example.stockticker.eventbean.PriceLimit;
import com.espertech.esper.example.stockticker.eventbean.StockTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockTickerAlertListener implements UpdateListener {
    private final EPServiceProvider epService;
    private final PriceLimit limit;
    private final StockTick initialPriceTick;
    private final StockTickerResultListener stockTickerResultListener;

    public StockTickerAlertListener(EPServiceProvider epService, PriceLimit limit, StockTick initialPriceTick, StockTickerResultListener stockTickerResultListener) {
        this.epService = epService;
        this.limit = limit;
        this.initialPriceTick = initialPriceTick;
        this.stockTickerResultListener = stockTickerResultListener;
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        Object theEvent = newEvents[0].get("tick");
        StockTick tick = (StockTick) theEvent;

        log.debug(".update Alert for stock=" + tick.getStockSymbol() +
                "  price=" + tick.getPrice() +
                "  initialPriceTick=" + initialPriceTick.getPrice() +
                "  limt=" + limit.getLimitPct());

        LimitAlert alert = new LimitAlert(tick, limit, initialPriceTick.getPrice());
        stockTickerResultListener.emitted(alert);
    }

    private static final Logger log = LoggerFactory.getLogger(StockTickerAlertListener.class);
}


