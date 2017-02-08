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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceGrouped;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByWTableRollupMultiKeyImpl extends AggSvcGroupByWTableBase
{
    private final AggregationGroupByRollupDesc groupByRollupDesc;

    public AggSvcGroupByWTableRollupMultiKeyImpl(TableMetadata tableMetadata, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessors, boolean join, TableStateInstanceGrouped tableStateInstance, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents, AggregationGroupByRollupDesc groupByRollupDesc) {
        super(tableMetadata, methodPairs, accessors, join, tableStateInstance, targetStates, accessStateExpr, agents);
        this.groupByRollupDesc = groupByRollupDesc;
    }

    public void applyEnterInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (int i = 0; i < groupKeyPerLevel.length; i++) {
            AggregationGroupByRollupLevel level = groupByRollupDesc.getLevels()[i];
            Object groupByKey = level.computeMultiKey(groupKeyPerLevel[i], tableMetadata.getKeyTypes().length);
            applyEnterGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    public void applyLeaveInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (int i = 0; i < groupKeyPerLevel.length; i++) {
            AggregationGroupByRollupLevel level = groupByRollupDesc.getLevels()[i];
            Object groupByKey = level.computeMultiKey(groupKeyPerLevel[i], tableMetadata.getKeyTypes().length);
            applyLeaveGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    @Override
    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel)
    {
        MultiKeyUntyped key = rollupLevel.computeMultiKey(groupByKey, tableMetadata.getKeyTypes().length);
        ObjectArrayBackedEventBean bean = tableStateInstance.getRowForGroupKey(key);

        if (bean != null) {
            AggregationRowPair row = (AggregationRowPair) bean.getProperties()[0];
            currentAggregatorMethods = row.getMethods();
            currentAggregatorStates = row.getStates();
        }
        else {
            currentAggregatorMethods = null;
        }

        this.currentGroupKey = key;
    }
}
