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
package com.espertech.esper.epl.table.strategy;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.service.common.AggregationRowPair;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import java.util.Collection;

public class ExprTableEvalStrategyUngroupedAccess extends ExprTableEvalStrategyUngroupedBase implements ExprTableAccessEvalStrategy {

    private final int slot;
    private final AggregationAccessor accessor;

    public ExprTableEvalStrategyUngroupedAccess(TableAndLockProviderUngrouped provider, int slot, AggregationAccessor accessor) {
        super(provider);
        this.slot = slot;
        this.accessor = accessor;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean event = lockTableReadAndGet(context);
        if (event == null) {
            return null;
        }
        AggregationState aggregationState = getAndLock(event, context);
        return accessor.getValue(aggregationState, eventsPerStream, isNewData, context);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw new IllegalStateException("Not typable");
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean event = lockTableReadAndGet(context);
        if (event == null) {
            return null;
        }
        AggregationState aggregationState = getAndLock(event, context);
        return accessor.getEnumerableEvents(aggregationState, eventsPerStream, isNewData, context);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean event = lockTableReadAndGet(context);
        if (event == null) {
            return null;
        }
        AggregationState aggregationState = getAndLock(event, context);
        return accessor.getEnumerableEvent(aggregationState, eventsPerStream, isNewData, context);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean event = lockTableReadAndGet(context);
        if (event == null) {
            return null;
        }
        AggregationState aggregationState = getAndLock(event, context);
        return accessor.getEnumerableScalar(aggregationState, eventsPerStream, isNewData, context);
    }

    private AggregationState getAndLock(ObjectArrayBackedEventBean event, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        return row.getStates()[slot];
    }
}
