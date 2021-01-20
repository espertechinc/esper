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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportTradeEvent implements Serializable {
    private static final long serialVersionUID = -7440180168559834726L;
    private int id;
    private String userId;
    private String ccypair;
    private String direction;
    private int amount;

    public SupportTradeEvent(int id, String userId, String ccypair, String direction) {
        this.id = id;
        this.userId = userId;
        this.ccypair = ccypair;
        this.direction = direction;
    }

    public SupportTradeEvent(int id, String userId, int amount) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getCcypair() {
        return ccypair;
    }

    public String getDirection() {
        return direction;
    }

    public int getAmount() {
        return amount;
    }

    public String toString() {
        return "id=" + id +
            " userId=" + userId +
            " ccypair=" + ccypair +
            " direction=" + direction;
    }
}
