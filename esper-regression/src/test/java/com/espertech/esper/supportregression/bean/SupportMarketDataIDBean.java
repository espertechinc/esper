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

public class SupportMarketDataIDBean implements Serializable {
    private String symbol;
    private String id;
    private double price;

    public SupportMarketDataIDBean(String symbol, String id, double price) {
        this.symbol = symbol;
        this.id = id;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public String getId() {
        return id;
    }
}
