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

import com.espertech.esper.schedule.TimeProvider;

public class AggregatorRateEverFilter extends AggregatorRateEver {

    public AggregatorRateEverFilter(long interval, long oneSecondTime, TimeProvider timeProvider) {
        super(interval, oneSecondTime, timeProvider);
    }

    public void enter(Object object) {
        Boolean pass = (Boolean) object;
        if (pass != null && pass) {
            super.enter(object);
        }
    }
}