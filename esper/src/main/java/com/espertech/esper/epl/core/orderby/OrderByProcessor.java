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
package com.espertech.esper.epl.core.orderby;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.rollup.GroupByRollupKey;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.List;

/**
 * A processor for ordering output events according to the order specified in the order-by clause.
 */
public interface OrderByProcessor {

    /**
     * Sort the output events. If the order-by processor needs group-by
     * keys to evaluate the expressions in the order-by clause, these will
     * be computed from the generating events.
     *
     * @param outgoingEvents       - the events to be sorted
     * @param generatingEvents     - the events that generated the output events (each event has a corresponding array of generating events per different event streams)
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param exprEvaluatorContext context for expression evalauation
     * @param aggregationService   aggregation svc
     * @return an array containing the output events in sorted order
     */
    public EventBean[] sortPlain(EventBean[] outgoingEvents, EventBean[][] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService);

    /**
     * Sort the output events, using the provided group-by keys for
     * evaluating grouped aggregation functions, and avoiding the cost of
     * recomputing the keys.
     *
     * @param outgoingEvents       - the events to sort
     * @param generatingEvents     - the events that generated the output events (each event has a corresponding array of generating events per different event streams)
     * @param groupByKeys          - the keys to use for determining the group-by group of output events
     * @param isNewData            - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param exprEvaluatorContext context for expression evaluation
     * @param aggregationService   aggregation svc
     * @return an array containing the output events in sorted order
     */
    public EventBean[] sortWGroupKeys(EventBean[] outgoingEvents, EventBean[][] generatingEvents, Object[] groupByKeys, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService);

    /**
     * Sort the output events, using the provided group-by keys for
     * evaluating grouped aggregation functions, and avoiding the cost of
     * recomputing the keys.
     *
     * @param outgoingEvents       - the events to sort
     * @param currentGenerators     - the events that generated the output events (each event has a corresponding array of generating events per different event streams)
     * @param newData - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param agentInstanceContext context for expression evaluation
     * @param aggregationService   aggregation svc
     * @return an array containing the output events in sorted order
     */
    public EventBean[] sortRollup(EventBean[] outgoingEvents, List<GroupByRollupKey> currentGenerators, boolean newData, AgentInstanceContext agentInstanceContext, AggregationService aggregationService);

    /**
     * Returns the sort key for a given row.
     *
     * @param eventsPerStream      is the row consisting of one event per stream
     * @param isNewData            is true for new data
     * @param exprEvaluatorContext context for expression evalauation
     * @return sort key
     */
    public Object getSortKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Returns the sort key for a given row for rollup.
     *
     * @param eventsPerStream      is the row consisting of one event per stream
     * @param isNewData            is true for new data
     * @param exprEvaluatorContext context for expression evalauation
     * @param level rollup level
     * @return sort key
     */
    public Object getSortKeyRollup(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, AggregationGroupByRollupLevel level);

    /**
     * Sort a given array of outgoing events using the sort keys returning a sorted outgoing event array.
     *
     * @param outgoingEvents       is the events to sort
     * @param orderKeys            is the keys to sort by
     * @param exprEvaluatorContext context for expression evalauation
     * @return sorted events
     */
    public EventBean[] sortWOrderKeys(EventBean[] outgoingEvents, Object[] orderKeys, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Sort two keys and events
     * @param first first
     * @param sortKeyFirst sort key first
     * @param second second
     * @param sortKeySecond sort key seconds
     * @return sorted
     */
    public EventBean[] sortTwoKeys(EventBean first, Object sortKeyFirst, EventBean second, Object sortKeySecond);
}
