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
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import java.util.Collection;

public abstract class ExprTableEvalStrategyGroupByAccessBase extends ExprTableEvalStrategyGroupByBase implements ExprTableAccessEvalStrategy {

    private final AggregationAccessorSlotPair pair;

    protected ExprTableEvalStrategyGroupByAccessBase(TableAndLockProviderGrouped provider, AggregationAccessorSlotPair pair) {
        super(provider);
        this.pair = pair;
    }

    protected Object evaluateInternal(Object group, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(group, context);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.evalAccessorGetValue(ExprTableEvalStrategyUtil.getRow(row), pair, eventsPerStream, isNewData, context);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw new IllegalStateException("Not typable");
    }

    protected Collection<EventBean> evaluateGetROCollectionEventsInternal(Object group, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(group, context);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.evalGetROCollectionEvents(ExprTableEvalStrategyUtil.getRow(row), pair, eventsPerStream, isNewData, context);
    }

    protected EventBean evaluateGetEventBeanInternal(Object group, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(group, context);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.evalGetEventBean(ExprTableEvalStrategyUtil.getRow(row), pair, eventsPerStream, isNewData, context);
    }

    protected Collection evaluateGetROCollectionScalarInternal(Object group, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(group, context);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.evalGetROCollectionScalar(ExprTableEvalStrategyUtil.getRow(row), pair, eventsPerStream, isNewData, context);
    }
}
