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

import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPairForge;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.service.groupall.AggSvcGroupAllForge;
import com.espertech.esper.epl.agg.service.groupby.AggGroupByDesc;
import com.espertech.esper.epl.agg.service.groupby.AggSvcGroupByForge;
import com.espertech.esper.epl.agg.service.groupbylocal.AggSvcLocalGroupByForge;
import com.espertech.esper.epl.agg.service.groupbyrollup.AggSvcGroupByRollupForge;
import com.espertech.esper.epl.agg.service.table.AggSvcGroupByWTableForge;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlanForge;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;

public class AggregationServiceFactoryServiceImpl implements AggregationServiceFactoryService {

    public final static AggregationServiceFactoryService DEFAULT_FACTORY = new AggregationServiceFactoryServiceImpl();

    public AggregationServiceFactoryForge getNullAggregationService() {
        return AggregationServiceNullFactory.AGGREGATION_SERVICE_NULL_FACTORY;
    }

    public AggregationServiceFactoryForge getNoGroup(AggregationRowStateForgeDesc rowStateDesc, boolean join, boolean isUnidirectional, boolean isFireAndForget, boolean isOnSelect) {
        return new AggSvcGroupAllForge(rowStateDesc, join);
    }

    public AggregationServiceFactoryForge getGroupBy(AggGroupByDesc aggGroupByDesc, TimeAbacus timeAbacus, boolean isUnidirectional, boolean isFireAndForget, boolean isOnSelect) {
        return new AggSvcGroupByForge(aggGroupByDesc, timeAbacus);
    }

    public AggregationServiceFactoryForge getGroupLocalGroupBy(boolean hasGroupByClause, boolean join, AggregationLocalGroupByPlanForge localGroupByPlan, boolean isUnidirectional, boolean isFireAndForget, boolean isOnSelect) {
        return new AggSvcLocalGroupByForge(hasGroupByClause, join, localGroupByPlan);
    }

    public AggregationServiceFactoryForge getRollup(ExprNode[] groupByNodes, AggregationGroupByRollupDesc rollupDesc, AggregationRowStateForgeDesc rowStateDesc, boolean join, AggregationGroupByRollupDesc groupByRollupDesc, boolean isUnidirectional, boolean isFireAndForget, boolean isOnSelect) {
        return new AggSvcGroupByRollupForge(rowStateDesc, join, groupByRollupDesc);
    }

    public AggregationServiceFactoryForge getTable(TableService tableService, TableMetadata tableMetadata, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPairForge[] accessorPairs, AggregationAccessorSlotPair[] accessors, boolean join, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgentForge[] agentForges, AggregationAgent[] agents, AggregationGroupByRollupDesc groupByRollupDesc, boolean hasGroupBy) {
        return new AggSvcGroupByWTableForge(tableService, tableMetadata, methodPairs, accessorPairs, accessors, join, targetStates, accessStateExpr, agentForges, agents, groupByRollupDesc, hasGroupBy);
    }
}
