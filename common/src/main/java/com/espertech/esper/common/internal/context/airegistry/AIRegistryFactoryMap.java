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
package com.espertech.esper.common.internal.context.airegistry;

import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;

public class AIRegistryFactoryMap implements AIRegistryFactory {
    public final static AIRegistryFactoryMap INSTANCE = new AIRegistryFactoryMap();

    private AIRegistryFactoryMap() {
    }

    public AIRegistryPriorEvalStrategy makePrior() {
        return new AIRegistryPriorEvalStrategyMap();
    }

    public AIRegistryPreviousGetterStrategy makePrevious() {
        return new AIRegistryPreviousGetterStrategyMap();
    }

    public AIRegistrySubselectLookup makeSubqueryLookup(LookupStrategyDesc lookupStrategyDesc) {
        return new AIRegistrySubselectLookupMap(lookupStrategyDesc);
    }

    public AIRegistryAggregation makeAggregation() {
        return new AIRegistryAggregationMap();
    }

    public AIRegistryTableAccess makeTableAccess() {
        return new AIRegistryTableAccessMap();
    }

    public AIRegistryRowRecogPreviousStrategy makeRowRecogPreviousStrategy() {
        return new AIRegistryRowRecogPreviousStrategyMap();
    }
}
