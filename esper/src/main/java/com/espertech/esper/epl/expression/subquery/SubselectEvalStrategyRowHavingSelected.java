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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventBeanUtility;

import java.util.Collection;

public class SubselectEvalStrategyRowHavingSelected implements SubselectEvalStrategyRow {

    public Object evaluate(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprSubselectRowNode parent) {
        EventBean[] eventsZeroBased = EventBeanUtility.allocatePerStreamShift(eventsPerStream);
        Boolean pass = (Boolean) parent.havingExpr.evaluate(eventsZeroBased, newData, exprEvaluatorContext);
        if ((pass == null) || (!pass)) {
            return null;
        }

        Object result;
        if (parent.selectClauseEvaluator.length == 1) {
            result = parent.selectClauseEvaluator[0].evaluate(eventsZeroBased, true, exprEvaluatorContext);
        } else {
            // we are returning a Map here, not object-array, preferring the self-describing structure
            result = parent.evaluateRow(eventsZeroBased, true, exprEvaluatorContext);
        }

        return result;
    }

    // Filter and Select
    public Collection<EventBean> evaluateGetCollEvents(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context, ExprSubselectRowNode parent) {
        return null;
    }

    // Filter and Select
    public Collection evaluateGetCollScalar(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context, ExprSubselectRowNode parent) {
        return null;
    }

    // Filter and Select
    public Object[] typableEvaluate(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprSubselectRowNode parent) {
        return null;
    }

    public Object[][] typableEvaluateMultirow(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprSubselectRowNode parent) {
        return null;
    }

    // Filter and Select
    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context, ExprSubselectRowNode parent) {
        return null;
    }
}
