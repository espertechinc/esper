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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class StockTickerAlertListener implements UpdateListener {

    private List<Object> matchEvents = Collections.synchronizedList(new LinkedList<Object>());

    public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
        for (int i = 0; i < newEvents.length; i++) {
            EventBean event = newEvents[i];

            StockTick tick = (StockTick) event.get("tick");
            PriceLimit priceLimit = (PriceLimit) event.get("priceLimit");
            double lowerLimit = (double) event.get("observed.lowerLimit");
            double upperLimit = (double) event.get("observed.upperLimit");
            double reference = (double) event.get("observed.reference");

            log.debug(".update Alert for stock=" + tick.getSymbol() +
                "  price=" + tick.getPrice() +
                "  reference=" + reference +
                "  lower=" + lowerLimit +
                "  upper=" + upperLimit);

            LimitAlert alert = new LimitAlert(tick, priceLimit, reference, lowerLimit, upperLimit);
            matchEvents.add(alert);
        }
    }

    public int getSize() {
        return matchEvents.size();
    }

    public List getMatchEvents() {
        return matchEvents;
    }

    public void clearMatched() {
        matchEvents.clear();
    }

    private static final Logger log = LoggerFactory.getLogger(StockTickerAlertListener.class);
}


