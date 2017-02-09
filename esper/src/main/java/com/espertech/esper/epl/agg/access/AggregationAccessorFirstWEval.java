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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;
import java.util.Collections;

/**
 * Represents the aggregation accessor that provides the result for the "first" aggregation function without index.
 */
public class AggregationAccessorFirstWEval implements AggregationAccessor {
    private final int streamNum;
    private final ExprEvaluator childNode;
    private final EventBean[] eventsPerStream;

    /**
     * Ctor.
     *
     * @param streamNum stream id
     * @param childNode expression
     */
    public AggregationAccessorFirstWEval(int streamNum, ExprEvaluator childNode) {
        this.streamNum = streamNum;
        this.childNode = childNode;
        this.eventsPerStream = new EventBean[streamNum + 1];
    }

    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean bean = ((AggregationStateLinear) state).getFirstValue();
        if (bean == null) {
            return null;
        }
        this.eventsPerStream[streamNum] = bean;
        return childNode.evaluate(this.eventsPerStream, true, null);
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean bean = ((AggregationStateLinear) state).getFirstValue();
        if (bean == null) {
            return null;
        }
        return Collections.singletonList(bean);
    }

    public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object value = getValue(state, eventsPerStream, isNewData, exprEvaluatorContext);
        if (value == null) {
            return null;
        }
        return Collections.singletonList(value);
    }

    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return ((AggregationStateLinear) state).getFirstValue();
    }
}