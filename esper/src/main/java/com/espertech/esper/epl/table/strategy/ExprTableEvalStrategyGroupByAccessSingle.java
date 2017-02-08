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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

public class ExprTableEvalStrategyGroupByAccessSingle extends ExprTableEvalStrategyGroupByAccessBase {

    private final ExprEvaluator groupExpr;

    public ExprTableEvalStrategyGroupByAccessSingle(TableAndLockProviderGrouped provider, AggregationAccessorSlotPair pair, ExprEvaluator groupExpr) {
        super(provider, pair);
        this.groupExpr = groupExpr;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object group = groupExpr.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        return evaluateInternal(group, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object group = groupExpr.evaluate(eventsPerStream, isNewData, context);
        return evaluateGetROCollectionEventsInternal(group, eventsPerStream, isNewData, context);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object group = groupExpr.evaluate(eventsPerStream, isNewData, context);
        return evaluateGetEventBeanInternal(group, eventsPerStream, isNewData, context);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object group = groupExpr.evaluate(eventsPerStream, isNewData, context);
        return evaluateGetROCollectionScalarInternal(group, eventsPerStream, isNewData, context);
    }
}
