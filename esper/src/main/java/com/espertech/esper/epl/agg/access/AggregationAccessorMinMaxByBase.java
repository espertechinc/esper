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
import java.util.Collections;

/**
 * Represents the aggregation accessor that provides the result for the "maxBy" aggregation function.
 */
public abstract class AggregationAccessorMinMaxByBase implements AggregationAccessor {
    private final boolean max;

    protected AggregationAccessorMinMaxByBase(boolean max) {
        this.max = max;
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean bean = getEnumerableEvent(state, eventsPerStream, isNewData, context);
        if (bean == null) {
            return null;
        }
        return Collections.singletonList(bean);
    }

    public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (max) {
            return ((AggregationStateSorted) state).getLastValue();
        } else {
            return ((AggregationStateSorted) state).getFirstValue();
        }
    }
}