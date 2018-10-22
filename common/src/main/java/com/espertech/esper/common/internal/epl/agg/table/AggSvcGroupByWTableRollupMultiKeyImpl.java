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
package com.espertech.esper.common.internal.epl.agg.table;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAgent;
import com.espertech.esper.common.internal.collection.HashableMultiKey;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupDesc;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupLevel;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableColumnMethodPairEval;
import com.espertech.esper.common.internal.epl.table.core.TableInstanceGrouped;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByWTableRollupMultiKeyImpl extends AggSvcGroupByWTableBase {

    private final AggregationGroupByRollupDesc groupByRollupDesc;
    private final int numKeys;

    public AggSvcGroupByWTableRollupMultiKeyImpl(TableInstanceGrouped tableInstance, TableColumnMethodPairEval[] methodPairs, AggregationMultiFunctionAgent[] accessAgents, int[] accessColumnsZeroOffset, AggregationGroupByRollupDesc groupByRollupDesc) {
        super(tableInstance, methodPairs, accessAgents, accessColumnsZeroOffset);
        this.groupByRollupDesc = groupByRollupDesc;
        numKeys = tableInstance.getTable().getMetaData().getKeyTypes().length;
    }

    public void applyEnterInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (int i = 0; i < groupKeyPerLevel.length; i++) {
            AggregationGroupByRollupLevel level = groupByRollupDesc.getLevels()[i];
            Object groupByKey = level.computeMultiKey(groupKeyPerLevel[i], numKeys);
            applyEnterGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    public void applyLeaveInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (int i = 0; i < groupKeyPerLevel.length; i++) {
            AggregationGroupByRollupLevel level = groupByRollupDesc.getLevels()[i];
            Object groupByKey = level.computeMultiKey(groupKeyPerLevel[i], numKeys);
            applyLeaveGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    @Override
    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        HashableMultiKey key = rollupLevel.computeMultiKey(groupByKey, numKeys);
        ObjectArrayBackedEventBean bean = tableInstance.getRowForGroupKey(key);

        if (bean != null) {
            currentAggregationRow = (AggregationRow) bean.getProperties()[0];
        } else {
            currentAggregationRow = null;
        }

        this.currentGroupKey = key;
    }
}
