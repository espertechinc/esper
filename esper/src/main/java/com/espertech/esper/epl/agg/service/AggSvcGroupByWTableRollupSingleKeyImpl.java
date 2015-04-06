/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceGroupBy;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByWTableRollupSingleKeyImpl extends AggSvcGroupByWTableBase
{
    public AggSvcGroupByWTableRollupSingleKeyImpl(TableMetadata tableMetadata, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessors, boolean join, TableStateInstanceGroupBy tableStateInstance, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents) {
        super(tableMetadata, methodPairs, accessors, join, tableStateInstance, targetStates, accessStateExpr, agents);
    }

    public void applyEnterInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (Object groupByKey : groupKeyPerLevel) {
            applyEnterGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    public void applyLeaveInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (Object groupByKey : groupKeyPerLevel) {
            applyLeaveGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }
}
