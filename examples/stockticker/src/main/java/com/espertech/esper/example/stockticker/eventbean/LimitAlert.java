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
package com.espertech.esper.example.stockticker.eventbean;

public class LimitAlert {
    private StockTick tick;
    private PriceLimit limit;
    double initialPrice;

    public LimitAlert(StockTick tick, PriceLimit limit, double initialPrice) {
        this.tick = tick;
        this.limit = limit;
        this.initialPrice = initialPrice;
    }

    public StockTick getTick() {
        return tick;
    }

    public PriceLimit getPriceLimit() {
        return limit;
    }

    public double getInitialPrice() {
        return initialPrice;
    }

    public String toString() {
        return tick.toString() +
                "  " + limit.toString() +
                "  initialPrice=" + initialPrice;
    }

}
