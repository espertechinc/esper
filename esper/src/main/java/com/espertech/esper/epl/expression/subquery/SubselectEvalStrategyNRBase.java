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
package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventBeanUtility;

import java.util.Collection;

public abstract class SubselectEvalStrategyNRBase implements SubselectEvalStrategyNR {
    protected final ExprEvaluator valueEval;
    protected final ExprEvaluator selectEval;
    private final boolean resultWhenNoMatchingEvents;

    protected abstract Object evaluateInternal(Object leftResult, EventBean[] events, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService);

    public SubselectEvalStrategyNRBase(ExprEvaluator valueEval, ExprEvaluator selectEval, boolean resultWhenNoMatchingEvents) {
        this.valueEval = valueEval;
        this.selectEval = selectEval;
        this.resultWhenNoMatchingEvents = resultWhenNoMatchingEvents;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService) {
        if (matchingEvents == null || matchingEvents.size() == 0) {
            return resultWhenNoMatchingEvents;
        }

        Object leftResult = valueEval.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        EventBean[] events = EventBeanUtility.allocatePerStreamShift(eventsPerStream);
        return evaluateInternal(leftResult, events, isNewData, matchingEvents, exprEvaluatorContext, aggregationService);
    }

}
