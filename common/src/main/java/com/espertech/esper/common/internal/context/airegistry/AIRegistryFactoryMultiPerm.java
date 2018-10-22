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

public class AIRegistryFactoryMultiPerm implements AIRegistryFactory {
    public final static AIRegistryFactoryMultiPerm INSTANCE = new AIRegistryFactoryMultiPerm();

    private AIRegistryFactoryMultiPerm() {
    }

    public AIRegistryPriorEvalStrategy makePrior() {
        return new AIRegistryPriorEvalStrategyMultiPerm();
    }

    public AIRegistryPreviousGetterStrategy makePrevious() {
        return new AIRegistryPreviousGetterStrategyMultiPerm();
    }

    public AIRegistrySubselectLookup makeSubqueryLookup(LookupStrategyDesc lookupStrategyDesc) {
        return new AIRegistrySubselectLookupMultiPerm(lookupStrategyDesc);
    }

    public AIRegistryAggregation makeAggregation() {
        return new AIRegistryAggregationMultiPerm();
    }

    public AIRegistryTableAccess makeTableAccess() {
        return new AIRegistryTableAccessMultiPerm();
    }

    public AIRegistryRowRecogPreviousStrategy makeRowRecogPreviousStrategy() {
        return new AIRegistryRowRecogPreviousStrategyMultiPerm();
    }
}
