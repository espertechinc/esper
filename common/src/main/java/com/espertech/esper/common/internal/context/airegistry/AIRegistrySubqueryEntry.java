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

import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

public class AIRegistrySubqueryEntry {
    private final AIRegistrySubselectLookup lookupStrategies;
    private final AIRegistryAggregation aggregationServices;
    private final AIRegistryPriorEvalStrategy priorEvalStrategies;
    private final AIRegistryPreviousGetterStrategy previousGetterStrategies;

    public AIRegistrySubqueryEntry(AIRegistrySubselectLookup lookupStrategies, AIRegistryAggregation aggregationServices, AIRegistryPriorEvalStrategy priorEvalStrategies, AIRegistryPreviousGetterStrategy previousGetterStrategies) {
        this.lookupStrategies = lookupStrategies;
        this.aggregationServices = aggregationServices;
        this.priorEvalStrategies = priorEvalStrategies;
        this.previousGetterStrategies = previousGetterStrategies;
    }

    public AIRegistrySubselectLookup getLookupStrategies() {
        return lookupStrategies;
    }

    public AIRegistryAggregation getAggregationServices() {
        return aggregationServices;
    }

    public AIRegistryPriorEvalStrategy getPriorEvalStrategies() {
        return priorEvalStrategies;
    }

    public AIRegistryPreviousGetterStrategy getPreviousGetterStrategies() {
        return previousGetterStrategies;
    }

    public void deassignService(int agentInstanceId) {
        lookupStrategies.deassignService(agentInstanceId);
        if (aggregationServices != null) {
            aggregationServices.deassignService(agentInstanceId);
        }
        if (priorEvalStrategies != null) {
            priorEvalStrategies.deassignService(agentInstanceId);
        }
        if (previousGetterStrategies != null) {
            previousGetterStrategies.deassignService(agentInstanceId);
        }
    }

    public void assign(int agentInstanceId, SubordTableLookupStrategy lookupStrategy, AggregationService aggregationService,
                       PriorEvalStrategy priorEvalStrategy, PreviousGetterStrategy previousGetterStrategy) {
        lookupStrategies.assignService(agentInstanceId, lookupStrategy);
        if (aggregationServices != null) {
            aggregationServices.assignService(agentInstanceId, aggregationService);
        }
        if (priorEvalStrategies != null) {
            priorEvalStrategies.assignService(agentInstanceId, priorEvalStrategy);
        }
        if (previousGetterStrategies != null) {
            previousGetterStrategies.assignService(agentInstanceId, previousGetterStrategy);
        }
    }
}
