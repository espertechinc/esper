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

public class LimitAlert {
    private final StockTick tick;
    private final PriceLimit priceLimit;
    private final double reference;
    private final double lower;
    private final double upper;

    public LimitAlert(StockTick tick, PriceLimit priceLimit, double reference, double lower, double upper) {
        this.tick = tick;
        this.priceLimit = priceLimit;
        this.reference = reference;
        this.lower = lower;
        this.upper = upper;
    }

    public StockTick getTick() {
        return tick;
    }

    public PriceLimit getPriceLimit() {
        return priceLimit;
    }

    public double getReference() {
        return reference;
    }

    public double getLower() {
        return lower;
    }

    public double getUpper() {
        return upper;
    }
}
