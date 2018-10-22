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
package com.espertech.esper.common.client.hook.aggmultifunc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

/**
 * Agents change mult-function aggregation state.
 */
public interface AggregationMultiFunctionAgent {
    /**
     * Enter-into (add to) an aggregation
     *
     * @param eventsPerStream      events
     * @param exprEvaluatorContext evaluation context
     * @param row                  aggregation row
     * @param column               column assigned to the aggregation state
     */
    void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column);

    /**
     * Leave-from (remove from) an aggregation
     *
     * @param eventsPerStream      events
     * @param exprEvaluatorContext evaluation context
     * @param row                  aggregation row
     * @param column               column assigned to the aggregation state
     */
    void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column);
}
