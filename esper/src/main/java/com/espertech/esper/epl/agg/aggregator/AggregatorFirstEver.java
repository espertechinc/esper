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
package com.espertech.esper.epl.agg.aggregator;

/**
 * Aggregator for the very first value.
 */
public class AggregatorFirstEver implements AggregationMethod {
    protected boolean isSet;
    protected Object firstValue;

    public void clear() {
        firstValue = null;
        isSet = false;
    }

    public void enter(Object object) {
        if (!isSet) {
            isSet = true;
            firstValue = object;
        }
    }

    public void leave(Object object) {
    }

    public Object getValue() {
        return firstValue;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setSet(boolean set) {
        isSet = set;
    }

    public Object getFirstValue() {
        return firstValue;
    }

    public void setFirstValue(Object firstValue) {
        this.firstValue = firstValue;
    }
}