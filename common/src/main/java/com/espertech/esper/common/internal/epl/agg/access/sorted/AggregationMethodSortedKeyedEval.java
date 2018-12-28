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
import java.util.TreeMap;
import java.util.function.BiFunction;

public class AggregationMethodSortedKeyedEval implements AggregationMultiFunctionAggregationMethod {

    private final ExprEvaluator keyEval;
    private final BiFunction<TreeMap<Object, Object>, Object, Object> value;
    private final BiFunction<TreeMap<Object, Object>, Object, EventBean> event;
    private final BiFunction<TreeMap<Object, Object>, Object, Collection> events;

    public AggregationMethodSortedKeyedEval(ExprEvaluator keyEval, BiFunction<TreeMap<Object, Object>, Object, Object> value, BiFunction<TreeMap<Object, Object>, Object, EventBean> event, BiFunction<TreeMap<Object, Object>, Object, Collection> events) {
        this.keyEval = keyEval;
        this.value = value;
        this.event = event;
        this.events = events;
    }

    public Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateSorted sorted = (AggregationStateSorted) row.getAccessState(aggColNum);
        Object key = keyEval.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (key == null) {
            return null;
        }
        return value.apply(sorted.getSorted(), key);
    }

    public Collection getValueCollectionEvents(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateSorted sorted = (AggregationStateSorted) row.getAccessState(aggColNum);
        Object key = keyEval.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (key == null) {
            return null;
        }
        return events.apply(sorted.getSorted(), key);
    }

    public Collection getValueCollectionScalar(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public EventBean getValueEventBean(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateSorted sorted = (AggregationStateSorted) row.getAccessState(aggColNum);
        Object key = keyEval.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (key == null) {
            return null;
        }
        return event.apply(sorted.getSorted(), key);
    }
}
