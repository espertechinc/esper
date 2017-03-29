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
 * Aggregator for the very last value.
 */
public class AggregatorLeavingFilter extends AggregatorLeaving {
    @Override
    public void leave(Object parameters) {
        Boolean pass = (Boolean) parameters;
        if (pass != null && pass) {
            super.leave(null);
        }
    }
}