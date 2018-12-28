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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethod;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;
import java.util.NavigableMap;
import java.util.function.Function;

public class AggregationMethodSortedEventsBetweenEval implements AggregationMultiFunctionAggregationMethod {

    private final ExprEvaluator fromKeyEval;
    private final ExprEvaluator fromInclusiveEval;
    private final ExprEvaluator toKeyEval;
    private final ExprEvaluator toInclusiveEval;
    private final Function<NavigableMap<Object, Object>, Object> value;
    private final Function<NavigableMap<Object, Object>, Collection> events;

    public AggregationMethodSortedEventsBetweenEval(ExprEvaluator fromKeyEval, ExprEvaluator fromInclusiveEval, ExprEvaluator toKeyEval, ExprEvaluator toInclusiveEval, Function<NavigableMap<Object, Object>, Object> value, Function<NavigableMap<Object, Object>, Collection> events) {
        this.fromKeyEval = fromKeyEval;
        this.fromInclusiveEval = fromInclusiveEval;
        this.toKeyEval = toKeyEval;
        this.toInclusiveEval = toInclusiveEval;
        this.value = value;
        this.events = events;
    }

    public Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        NavigableMap<Object, Object> submap = getSubmap(aggColNum, row, eventsPerStream, isNewData, exprEvaluatorContext);
        if (submap == null) {
            return null;
        }
        return value.apply(submap);
    }

    public Collection getValueCollectionEvents(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        NavigableMap<Object, Object> submap = getSubmap(aggColNum, row, eventsPerStream, isNewData, exprEvaluatorContext);
        if (submap == null) {
            return null;
        }
        return events.apply(submap);
    }

    public Collection getValueCollectionScalar(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public EventBean getValueEventBean(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    private NavigableMap<Object, Object> getSubmap(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateSorted sorted = (AggregationStateSorted) row.getAccessState(aggColNum);
        Object fromKey = fromKeyEval.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (fromKey == null) {
            return null;
        }
        Boolean fromInclusive = (Boolean) fromInclusiveEval.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (fromInclusive == null) {
            return null;
        }
        Object toKey = toKeyEval.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (toKey == null) {
            return null;
        }
        Boolean toInclusive = (Boolean) toInclusiveEval.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (toInclusive == null) {
            return null;
        }
        return sorted.getSorted().subMap(fromKey, fromInclusive, toKey, toInclusive);
    }
}
