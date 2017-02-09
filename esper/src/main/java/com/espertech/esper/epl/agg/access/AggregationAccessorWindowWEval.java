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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the aggregation accessor that provides the result for the "window" aggregation function.
 */
public class AggregationAccessorWindowWEval implements AggregationAccessor {
    private final int streamNum;
    private final ExprEvaluator childNode;
    private final EventBean[] eventsPerStream;
    private final Class componentType;

    /**
     * Ctor.
     *
     * @param streamNum     stream id
     * @param childNode     expression
     * @param componentType type
     */
    public AggregationAccessorWindowWEval(int streamNum, ExprEvaluator childNode, Class componentType) {
        this.streamNum = streamNum;
        this.childNode = childNode;
        this.eventsPerStream = new EventBean[streamNum + 1];
        this.componentType = componentType;
    }

    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateLinear linear = (AggregationStateLinear) state;
        if (linear.size() == 0) {
            return null;
        }
        Object array = Array.newInstance(componentType, linear.size());
        Iterator<EventBean> it = linear.iterator();
        int count = 0;
        for (; it.hasNext(); ) {
            EventBean bean = it.next();
            this.eventsPerStream[streamNum] = bean;
            Object value = childNode.evaluate(this.eventsPerStream, true, null);
            Array.set(array, count++, value);
        }

        return array;
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateLinear linear = (AggregationStateLinear) state;
        if (linear.size() == 0) {
            return null;
        }
        return linear.collectionReadOnly();
    }

    public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateLinear linear = (AggregationStateLinear) state;
        if (linear.size() == 0) {
            return null;
        }
        List<Object> values = new ArrayList<Object>(linear.size());
        Iterator<EventBean> it = linear.iterator();
        for (; it.hasNext(); ) {
            EventBean bean = it.next();
            this.eventsPerStream[streamNum] = bean;
            Object value = childNode.evaluate(this.eventsPerStream, true, null);
            values.add(value);
        }

        return values;
    }

    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }
}