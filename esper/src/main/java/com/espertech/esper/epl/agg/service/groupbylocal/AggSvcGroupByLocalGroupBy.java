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
package com.espertech.esper.epl.agg.service.groupbylocal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggSvcGroupByUtil;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.service.common.AggregationMethodPairRow;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByColumn;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByLevel;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlan;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByLocalGroupBy extends AggSvcGroupLocalGroupByBase {
    private AggregationMethod[] currentAggregatorMethods;
    private AggregationState[] currentAggregatorStates;

    public AggSvcGroupByLocalGroupBy(boolean isJoin, AggregationLocalGroupByPlan localGroupByPlan) {
        super(isJoin, localGroupByPlan);
    }

    protected Object computeGroupKey(AggregationLocalGroupByLevel level, Object groupKey, ExprEvaluator[] partitionEval, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (level.isDefaultLevel()) {
            return groupKey;
        }
        return AggSvcGroupAllLocalGroupBy.computeGroupKey(partitionEval, eventsPerStream, true, exprEvaluatorContext);
    }

    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        if (!localGroupByPlan.getAllLevels()[0].isDefaultLevel()) {
            return;
        }
        AggregationMethodPairRow row = aggregatorsPerLevelAndGroup[0].get(groupByKey);

        if (row != null) {
            currentAggregatorMethods = row.getMethods();
            currentAggregatorStates = row.getStates();
        } else {
            currentAggregatorMethods = null;
        }

        if (currentAggregatorMethods == null) {
            currentAggregatorMethods = AggSvcGroupByUtil.newAggregators(localGroupByPlan.getAllLevels()[0].getMethodFactories());
            currentAggregatorStates = AggSvcGroupByUtil.newAccesses(agentInstanceId, isJoin, localGroupByPlan.getAllLevels()[0].getStateFactories(), groupByKey, null);
        }
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationLocalGroupByColumn col = localGroupByPlan.getColumns()[column];
        if (col.isDefaultGroupLevel()) {
            if (col.isMethodAgg()) {
                return currentAggregatorMethods[col.getMethodOffset()].getValue();
            }
            return col.getPair().getAccessor().getValue(currentAggregatorStates[col.getPair().getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
        if (col.getPartitionEvaluators().length == 0) {
            if (col.isMethodAgg()) {
                return aggregatorsTopLevel[col.getMethodOffset()].getValue();
            }
            return col.getPair().getAccessor().getValue(statesTopLevel[col.getPair().getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
        Object groupByKey = AggSvcGroupAllLocalGroupBy.computeGroupKey(col.getPartitionEvaluators(), eventsPerStream, true, exprEvaluatorContext);
        AggregationMethodPairRow row = aggregatorsPerLevelAndGroup[col.getLevelNum()].get(groupByKey);
        if (col.isMethodAgg()) {
            return row.getMethods()[col.getMethodOffset()].getValue();
        }
        return col.getPair().getAccessor().getValue(row.getStates()[col.getPair().getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
    }
}
