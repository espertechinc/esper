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

public class AggregationAccessorFirstLastIndexNoEval implements AggregationAccessor {
    private final ExprEvaluator indexNode;
    private final int constant;
    private final boolean isFirst;

    public AggregationAccessorFirstLastIndexNoEval(ExprEvaluator indexNode, int constant, boolean first) {
        this.indexNode = indexNode;
        this.constant = constant;
        isFirst = first;
    }

    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean bean = getBean(state);
        if (bean == null) {
            return null;
        }
        return bean.getUnderlying();
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