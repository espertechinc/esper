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
import com.espertech.esper.common.internal.rettype.EPType;

import java.util.Collection;

/**
 * Aggregation method that operates on aggregation multi-function state such as provided by a multi-function aggregation (standalone or table column).
 */
public interface AggregationMultiFunctionAggregationMethod {
    /**
     * Returns the plain value
     *
     * @param aggColNum            column number
     * @param row                  aggregation row
     * @param eventsPerStream      events
     * @param isNewData            new-data flag
     * @param exprEvaluatorContext evaluation context
     * @return value
     */
    Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Return a collection of events or null when not available.
     * The {@link EPType} returned by the handler indicates whether the compiler allows operations on events.
     *
     * @param aggColNum            column number
     * @param row                  aggregation row
     * @param eventsPerStream      events
     * @param isNewData            new-data flag
     * @param exprEvaluatorContext evaluation context
     * @return collection of {@link EventBean}
     */
    Collection getValueCollectionEvents(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Return a collection of values or null when not available.
     * The {@link EPType} returned by the handler indicates whether the compiler allows operations on events.
     *
     * @param aggColNum            column number
     * @param row                  aggregation row
     * @param eventsPerStream      events
     * @param isNewData            new-data flag
     * @param exprEvaluatorContext evaluation context
     * @return collection of values
     */
    Collection getValueCollectionScalar(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Returns a single event or null when not available.
     * The {@link EPType} returned by the handler indicates whether the compiler allows operations on events.
     *
     * @param aggColNum            column number
     * @param row                  aggregation row
     * @param eventsPerStream      events
     * @param isNewData            new-data flag
     * @param exprEvaluatorContext evaluation context
     * @return event
     */
    EventBean getValueEventBean(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);
}
