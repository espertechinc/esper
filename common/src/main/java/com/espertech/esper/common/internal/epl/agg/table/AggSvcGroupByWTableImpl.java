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
public class AggSvcGroupByWTableImpl extends AggSvcGroupByWTableBase {
    public AggSvcGroupByWTableImpl(TableInstanceGrouped table, TableColumnMethodPairEval[] methodPairs, AggregationMultiFunctionAgent[] accessAgents, int[] accessColumnsZeroOffset) {
        super(table, methodPairs, accessAgents, accessColumnsZeroOffset);
    }

    public void applyEnterInternal(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        applyEnterGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
    }

    public void applyLeaveInternal(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        applyLeaveGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
    }
}
