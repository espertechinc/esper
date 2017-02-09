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
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;

public class AggregationMultiFunctionAnalysisResult {
    private final AggregationAccessorSlotPair[] accessorPairs;
    private final AggregationStateFactory[] stateFactories;

    public AggregationMultiFunctionAnalysisResult(AggregationAccessorSlotPair[] accessorPairs, AggregationStateFactory[] stateFactories) {
        this.accessorPairs = accessorPairs;
        this.stateFactories = stateFactories;
    }

    public AggregationAccessorSlotPair[] getAccessorPairs() {
        return accessorPairs;
    }

    public AggregationStateFactory[] getStateFactories() {
        return stateFactories;
    }
}
