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
package com.espertech.esper.core.context.stmt;

public class StatementAIResourceRegistry {

    private AIRegistryAggregation agentInstanceAggregationService;
    private AIRegistryExpr agentInstanceExprService;

    public StatementAIResourceRegistry(AIRegistryAggregation agentInstanceAggregationService, AIRegistryExpr agentInstanceExprService) {
        this.agentInstanceAggregationService = agentInstanceAggregationService;
        this.agentInstanceExprService = agentInstanceExprService;
    }

    public AIRegistryExpr getAgentInstanceExprService() {
        return agentInstanceExprService;
    }

    public AIRegistryAggregation getAgentInstanceAggregationService() {
        return agentInstanceAggregationService;
    }

    public void deassign(int agentInstanceId) {
        agentInstanceAggregationService.deassignService(agentInstanceId);
        agentInstanceExprService.deassignService(agentInstanceId);
    }
}
