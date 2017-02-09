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
 * Represents the aggregation accessor that provides the result for the "first" and "last" aggregation function with index.
 */
public class AggregationAccessorFirstLastIndexWEval implements AggregationAccessor {
    private final int streamNum;
    private final ExprEvaluator childNode;
    private final EventBean[] eventsPerStream;
    private final ExprEvaluator indexNode;
    private final int constant;
    private final boolean isFirst;

    /**
     * Ctor.
     *
     * @param streamNum stream id
     * @param childNode expression
     * @param indexNode index expression
     * @param constant  constant index
     * @param isFirst   true if returning first, false for returning last
     */
    public AggregationAccessorFirstLastIndexWEval(int streamNum, ExprEvaluator childNode, ExprEvaluator indexNode, int constant, boolean isFirst) {
        this.streamNum = streamNum;
        this.childNode = childNode;
        this.indexNode = indexNode;
        this.eventsPerStream = new EventBean[streamNum + 1];
        this.constant = constant;
        this.isFirst = isFirst;
    }

    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {

        EventBean bean = getBean(state);
        if (bean == null) {
            return null;
        }
        this.eventsPerStream[streamNum] = bean;
        return childNode.evaluate(this.eventsPerStream, true, null);
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean bean = getBean(state);
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
        return getBean(state);
    }

    private EventBean getBean(AggregationState state) {
        EventBean bean;
        int index = constant;
        if (index == -1) {
            Object result = indexNode.evaluate(null, true, null);
            if ((result == null) || (!(result instanceof Integer))) {
                return null;
            }
            index = (Integer) result;
        }
        if (isFirst) {
            bean = ((AggregationStateLinear) state).getFirstNthValue(index);
        } else {
            bean = ((AggregationStateLinear) state).getLastNthValue(index);
        }
        return bean;
    }
}