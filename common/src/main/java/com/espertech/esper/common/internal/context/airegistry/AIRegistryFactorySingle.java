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

public class AIRegistryFactorySingle implements AIRegistryFactory {
    public final static AIRegistryFactorySingle INSTANCE = new AIRegistryFactorySingle();

    private AIRegistryFactorySingle() {
    }

    public AIRegistryPriorEvalStrategy makePrior() {
        return new AIRegistryPriorEvalStrategySingle();
    }

    public AIRegistryPreviousGetterStrategy makePrevious() {
        return new AIRegistryPreviousGetterStrategySingle();
    }

    public AIRegistrySubselectLookup makeSubqueryLookup(LookupStrategyDesc lookupStrategyDesc) {
        return new AIRegistrySubselectLookupSingle(lookupStrategyDesc);
    }

    public AIRegistryAggregation makeAggregation() {
        return new AIRegistryAggregationSingle();
    }

    public AIRegistryTableAccess makeTableAccess() {
        return new AIRegistryTableAccessSingle();
    }

    public AIRegistryRowRecogPreviousStrategy makeRowRecogPreviousStrategy() {
        return new AIRegistryRowRecogPreviousStrategySingle();
    }
}
