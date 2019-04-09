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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableColumnMethodPairEval;
import com.espertech.esper.common.internal.epl.table.core.TableInstanceGrouped;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByWTableRollupSingleKeyImpl extends AggSvcGroupByWTableBase {
    public AggSvcGroupByWTableRollupSingleKeyImpl(TableInstanceGrouped tableInstance, TableColumnMethodPairEval[] methodPairs, AggregationMultiFunctionAgent[] accessAgents, int[] accessColumnsZeroOffset) {
        super(tableInstance, methodPairs, accessAgents, accessColumnsZeroOffset);
    }

    public void applyEnterInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (Object groupByKey : groupKeyPerLevel) {
            applyEnterTableKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    public void applyLeaveInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (Object groupByKey : groupKeyPerLevel) {
            applyLeaveTableKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }
}
