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
package com.espertech.esper.supportregression.bean.bookexample;

import java.io.Serializable;

public class OrderItem implements Serializable {
    private String itemId;
    private String productId;
    private int amount;
    private double price;

    public OrderItem(String itemId, String productId, int amount, double price) {
        this.itemId = itemId;
        this.amount = amount;
        this.productId = productId;
        this.price = price;
    }

    public String getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }

    public String getProductId() {
        return productId;
    }

    public double getPrice() {
        return price;
    }
}
