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
package com.espertech.esper.supportregression.bean;

import java.io.Serializable;

public class SupportMarketDataBean implements Serializable {
    private String symbol;
    private String id;
    private double price;
    private Long volume;
    private String feed;

    public SupportMarketDataBean(String symbol, double price, Long volume, String feed) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.feed = feed;
    }

    public SupportMarketDataBean(String symbol, String id, double price) {
        this.symbol = symbol;
        this.id = id;
        this.price = price;
    }

    public double getPriceTimesVolume(double factor) {
        return price * volume * factor;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public Long getVolume() {
        return volume;
    }

    public String getFeed() {
        return feed;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return "SupportMarketDataBean " +
                "symbol=" + symbol +
                " price=" + price +
                " volume=" + volume +
                " feed=" + feed;
    }
}
