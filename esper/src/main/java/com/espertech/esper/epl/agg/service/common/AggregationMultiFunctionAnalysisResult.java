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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPairForge;

public class AggregationMultiFunctionAnalysisResult {
    private final AggregationAccessorSlotPairForge[] accessorPairsForge;
    private final AggregationStateFactoryForge[] stateFactoryForges;

    public AggregationMultiFunctionAnalysisResult(AggregationAccessorSlotPairForge[] accessorPairsForge, AggregationStateFactoryForge[] stateFactoryForges) {
        this.accessorPairsForge = accessorPairsForge;
        this.stateFactoryForges = stateFactoryForges;
    }

    public AggregationAccessorSlotPairForge[] getAccessorPairsForge() {
        return accessorPairsForge;
    }

    public AggregationStateFactoryForge[] getStateFactoryForges() {
        return stateFactoryForges;
    }
}
