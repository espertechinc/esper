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
package com.espertech.esper.example.marketdatafeed;

public class MarketDataEvent {

    private String symbol;
    private FeedEnum feed;

    public MarketDataEvent(String symbol, FeedEnum feed) {
        this.symbol = symbol;
        this.feed = feed;
    }

    public String getSymbol() {
        return symbol;
    }

    public FeedEnum getFeed() {
        return feed;
    }
}
