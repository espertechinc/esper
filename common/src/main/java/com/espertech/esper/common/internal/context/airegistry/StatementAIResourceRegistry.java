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

import java.util.Map;

public class StatementAIResourceRegistry {

    private final AIRegistryAggregation agentInstanceAggregationService;
    private final AIRegistryPriorEvalStrategy[] agentInstancePriorEvalStrategies;
    private final Map<Integer, AIRegistrySubqueryEntry> agentInstanceSubselects;
    private final Map<Integer, AIRegistryTableAccess> agentInstanceTableAccesses;
    private final AIRegistryPreviousGetterStrategy[] agentInstancePreviousGetterStrategies;
    private final AIRegistryRowRecogPreviousStrategy agentInstanceRowRecogPreviousStrategy;

    public StatementAIResourceRegistry(AIRegistryAggregation agentInstanceAggregationService, AIRegistryPriorEvalStrategy[] agentInstancePriorEvalStrategies, Map<Integer, AIRegistrySubqueryEntry> agentInstanceSubselects, Map<Integer, AIRegistryTableAccess> agentInstanceTableAccesses, AIRegistryPreviousGetterStrategy[] agentInstancePreviousGetterStrategies, AIRegistryRowRecogPreviousStrategy agentInstanceRowRecogPreviousStrategy) {
        this.agentInstanceAggregationService = agentInstanceAggregationService;
        this.agentInstancePriorEvalStrategies = agentInstancePriorEvalStrategies;
        this.agentInstanceSubselects = agentInstanceSubselects;
        this.agentInstanceTableAccesses = agentInstanceTableAccesses;
        this.agentInstancePreviousGetterStrategies = agentInstancePreviousGetterStrategies;
        this.agentInstanceRowRecogPreviousStrategy = agentInstanceRowRecogPreviousStrategy;
    }

    public AIRegistryAggregation getAgentInstanceAggregationService() {
        return agentInstanceAggregationService;
    }

    public AIRegistryPriorEvalStrategy[] getAgentInstancePriorEvalStrategies() {
        return agentInstancePriorEvalStrategies;
    }

    public AIRegistryPreviousGetterStrategy[] getAgentInstancePreviousGetterStrategies() {
        return agentInstancePreviousGetterStrategies;
    }

    public Map<Integer, AIRegistrySubqueryEntry> getAgentInstanceSubselects() {
        return agentInstanceSubselects;
    }

    public Map<Integer, AIRegistryTableAccess> getAgentInstanceTableAccesses() {
        return agentInstanceTableAccesses;
    }

    public AIRegistryRowRecogPreviousStrategy getAgentInstanceRowRecogPreviousStrategy() {
        return agentInstanceRowRecogPreviousStrategy;
    }

    public void deassign(int agentInstanceId) {
        agentInstanceAggregationService.deassignService(agentInstanceId);
        if (agentInstancePriorEvalStrategies != null) {
            for (AIRegistryPriorEvalStrategy prior : agentInstancePriorEvalStrategies) {
                prior.deassignService(agentInstanceId);
            }
        }
        if (agentInstanceSubselects != null) {
            for (Map.Entry<Integer, AIRegistrySubqueryEntry> entry : agentInstanceSubselects.entrySet()) {
                entry.getValue().deassignService(agentInstanceId);
            }
        }
        if (agentInstancePreviousGetterStrategies != null) {
            for (AIRegistryPreviousGetterStrategy previous : agentInstancePreviousGetterStrategies) {
                previous.deassignService(agentInstanceId);
            }
        }
        if (agentInstanceTableAccesses != null) {
            for (Map.Entry<Integer, AIRegistryTableAccess> entry : agentInstanceTableAccesses.entrySet()) {
                entry.getValue().deassignService(agentInstanceId);
            }
        }
    }
}
