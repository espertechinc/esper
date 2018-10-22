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
import com.espertech.esper.common.internal.util.StopCallback;

/**
 * Service for maintaining aggregation state. Processes events entering (a window, a join etc,) and
 * events leaving. Answers questions about current aggregation state for a given row.
 */
public interface AggregationService extends AggregationResultFuture, StopCallback {
    /**
     * Apply events as entering a window (new events).
     *
     * @param eventsPerStream        - events for each stream entering window
     * @param optionalGroupKeyPerRow - can be null if grouping without keys is desired, else the keys
     *                               or array of keys to use for grouping, each distinct key value results in a new row of aggregation state.
     * @param exprEvaluatorContext   context for expression evaluatiom
     */
    public void applyEnter(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Apply events as leaving a window (old events).
     *
     * @param eventsPerStream        - events for each stream entering window
     * @param optionalGroupKeyPerRow - can be null if grouping without keys is desired, else the keys
     *                               or array of keys to use for grouping, each distinct key value results in a new row of aggregation state.
     * @param exprEvaluatorContext   context for expression evaluatiom
     */
    public void applyLeave(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Clear current aggregation state.
     *
     * @param exprEvaluatorContext context
     */
    public void clearResults(ExprEvaluatorContext exprEvaluatorContext);

    public void setRemovedCallback(AggregationRowRemovedCallback callback);

    public void accept(AggregationServiceVisitor visitor);

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor);

    public boolean isGrouped();
}
