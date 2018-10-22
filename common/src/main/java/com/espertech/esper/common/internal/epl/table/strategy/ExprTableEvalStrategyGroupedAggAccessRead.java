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
package com.espertech.esper.common.internal.epl.table.strategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.util.Collection;

public class ExprTableEvalStrategyGroupedAggAccessRead extends ExprTableEvalStrategyGroupedBase {

    public ExprTableEvalStrategyGroupedAggAccessRead(TableAndLockProviderGrouped provider, ExprTableEvalStrategyFactory factory) {
        super(provider, factory);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationRow aggs = getRow(eventsPerStream, isNewData, exprEvaluatorContext);
        return aggs == null ? null : factory.getAccessAggReader().getValue(factory.getAggColumnNum(), aggs, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationRow aggs = getRow(eventsPerStream, isNewData, context);
        return aggs == null ? null : factory.getAccessAggReader().getValueCollectionEvents(factory.getAggColumnNum(), aggs, eventsPerStream, isNewData, context);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationRow aggs = getRow(eventsPerStream, isNewData, context);
        return aggs == null ? null : factory.getAccessAggReader().getValueEventBean(factory.getAggColumnNum(), aggs, eventsPerStream, isNewData, context);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationRow aggs = getRow(eventsPerStream, isNewData, context);
        return aggs == null ? null : factory.getAccessAggReader().getValueCollectionScalar(factory.getAggColumnNum(), aggs, eventsPerStream, isNewData, context);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    private AggregationRow getRow(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object groupKey = factory.getGroupKeyEval().evaluate(eventsPerStream, isNewData, context);
        ObjectArrayBackedEventBean row = lockTableReadAndGet(groupKey, context);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.getRow(row);
    }
}
