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

public class Order implements Serializable {
    private String orderId;
    private OrderItem[] items;

    public Order(String orderId, OrderItem[] items) {
        this.items = items;
        this.orderId = orderId;
    }

    public OrderItem[] getItems() {
        return items;
    }

    public String getOrderId() {
        return orderId;
    }
}
