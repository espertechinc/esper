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
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class AggregationMethodSortedSubmapEval implements AggregationMultiFunctionAggregationMethod {

    private final ExprEvaluator fromKeyEval;
    private final ExprEvaluator fromInclusiveEval;
    private final ExprEvaluator toKeyEval;
    private final ExprEvaluator toInclusiveEval;
    private final Class underlyingClass;

    public AggregationMethodSortedSubmapEval(ExprEvaluator fromKeyEval, ExprEvaluator fromInclusiveEval, ExprEvaluator toKeyEval, ExprEvaluator toInclusiveEval, Class underlyingClass) {
        this.fromKeyEval = fromKeyEval;
        this.fromInclusiveEval = fromInclusiveEval;
        this.toKeyEval = toKeyEval;
        this.toInclusiveEval = toInclusiveEval;
        this.underlyingClass = underlyingClass;
    }

    public Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
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
        TreeMap<Object, Object> mapOfArrays = new TreeMap<>(sorted.getSorted().comparator());
        NavigableMap<Object, Object> submap = sorted.getSorted().subMap(fromKey, fromInclusive, toKey, toInclusive);
        for (Map.Entry<Object, Object> entry : submap.entrySet()) {
            mapOfArrays.put(entry.getKey(), AggregatorAccessSortedImpl.checkedPayloadGetUnderlyingArray(entry.getValue(), underlyingClass));
        }
        return mapOfArrays;
    }

    public Collection getValueCollectionEvents(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public Collection getValueCollectionScalar(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public EventBean getValueEventBean(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }
}
