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
import com.espertech.esper.epl.table.mgmt.TableMetadata;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

/**
 * Represents the aggregation accessor that provides the result for the "maxBy" aggregation function.
 */
public class AggregationAccessorSortedTable implements AggregationAccessor {
    private final boolean max;
    private final Class componentType;
    private final TableMetadata tableMetadata;

    public AggregationAccessorSortedTable(boolean max, Class componentType, TableMetadata tableMetadata) {
        this.max = max;
        this.componentType = componentType;
        this.tableMetadata = tableMetadata;
    }

    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateSorted sorted = (AggregationStateSorted) state;
        if (sorted.size() == 0) {
            return null;
        }
        Object array = Array.newInstance(componentType, sorted.size());

        Iterator<EventBean> it;
        if (max) {
            it = sorted.getReverseIterator();
        } else {
            it = sorted.iterator();
        }

        int count = 0;
        for (; it.hasNext(); ) {
            EventBean bean = it.next();
            Object und = tableMetadata.getEventToPublic().convertToUnd(bean, eventsPerStream, isNewData, exprEvaluatorContext);
            Array.set(array, count++, und);
        }
        return array;
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return ((AggregationStateSorted) state).collectionReadOnly();
    }

    public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }
}