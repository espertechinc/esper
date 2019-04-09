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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

/**
 * Interface for use by aggregate expression nodes representing aggregate functions such as 'sum' or 'avg' to use
 * to obtain the current value for the function at time of expression evaluation.
 */
public interface AggregationResultFuture {
    Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    Object getGroupKey(int agentInstanceId);

    Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext);

    Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    AggregationRow getAggregationRow(int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    AggregationService getContextPartitionAggregationService(int agentInstanceId);

    /**
     * Set the current aggregation state row - for use when evaluation nodes are asked to evaluate.
     * @param groupKey        - single key identifying the row of aggregation states
     * @param agentInstanceId context partition id
     * @param rollupLevel     rollup level
     */
    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel);
}
