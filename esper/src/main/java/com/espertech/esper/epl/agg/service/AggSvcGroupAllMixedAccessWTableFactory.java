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

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceUngrouped;

/**
 * Implementation for handling aggregation without any grouping (no group-by).
 */
public class AggSvcGroupAllMixedAccessWTableFactory implements AggregationServiceFactory {
    protected final AggregationAccessorSlotPair[] accessors;
    protected final boolean isJoin;
    private final TableColumnMethodPair[] methodPairs;
    private final String tableName;
    private final int[] targetStates;
    private final ExprNode[] accessStateExpr;
    private final AggregationAgent[] agents;

    public AggSvcGroupAllMixedAccessWTableFactory(AggregationAccessorSlotPair[] accessors, boolean join, TableColumnMethodPair[] methodPairs, String tableName, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents) {
        this.accessors = accessors;
        isJoin = join;
        this.methodPairs = methodPairs;
        this.tableName = tableName;
        this.targetStates = targetStates;
        this.accessStateExpr = accessStateExpr;
        this.agents = agents;
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, EngineImportService engineImportService, boolean isSubquery, Integer subqueryNumber) {
        TableStateInstanceUngrouped tableState = (TableStateInstanceUngrouped) agentInstanceContext.getStatementContext().getTableService().getState(tableName, agentInstanceContext.getAgentInstanceId());
        return new AggSvcGroupAllMixedAccessWTableImpl(tableState, methodPairs,
                accessors, targetStates, accessStateExpr, agents);
    }
}