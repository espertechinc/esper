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
package com.espertech.esper.example.cycledetect;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAccessor;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAccessorFactory;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAccessorFactoryContext;

public class CycleDetectorAggregationAccessorDetectFactory implements AggregationMultiFunctionAccessorFactory {
    public AggregationMultiFunctionAccessor newAccessor(AggregationMultiFunctionAccessorFactoryContext ctx) {
        return new CycleDetectorAggregationAccessorDetect();
    }
}
