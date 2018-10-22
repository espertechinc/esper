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

public final class SupportMaxAmountEvent {
    private String key;
    private double maxAmount;

    public SupportMaxAmountEvent(String key, double maxAmount) {
        this.key = key;
        this.maxAmount = maxAmount;
    }

    public String getKey() {
        return key;
    }

    public double getMaxAmount() {
        return maxAmount;
    }
}
