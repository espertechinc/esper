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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

/**
 * Accessor for access aggregation functions.
 */
public interface AggregationAccessor {
    /**
     * Return the aggregation state value either as a scalar value or any other object.
     * <p>
     * For enumeration over scalar values or objects return an array or collection of scalar or object values.
     * </p>
     * <p>
     * Use the #getEnumerableEvents method to return a collection of events.
     * </p>
     * <p>
     * Use the #getEnumerableEvent to return a single events.
     * </p>
     *
     * @param state                aggregation state, downcast as needed
     * @param eventsPerStream      events
     * @param isNewData            new-data indicator
     * @param exprEvaluatorContext eval context
     * @return return value
     */
    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Return the aggregation state value consisting of a collection of events.
     *
     * @param state                aggregation state, downcast as needed
     * @param eventsPerStream      events
     * @param isNewData            new-data indicator
     * @param exprEvaluatorContext eval context
     * @return return collection of events or null or empty collection
     */
    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Return the aggregation state value consisting of a single event.
     *
     * @param state                aggregation state, downcast as needed
     * @param eventsPerStream      events
     * @param isNewData            new-data indicator
     * @param exprEvaluatorContext eval context
     * @return return event or null
     */
    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Return the aggregation state value consisting of a collection of scalar values.
     *
     * @param state                aggregation state, downcast as needed
     * @param eventsPerStream      events
     * @param isNewData            new-data indicator
     * @param exprEvaluatorContext eval context
     * @return return collection of scalar or null or empty collection
     */
    public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);
}
