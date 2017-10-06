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

public class SubselectEvalStrategyNRExistsDefault implements SubselectEvalStrategyNR {
    private final ExprEvaluator filterEval;
    private final ExprEvaluator havingEval;

    public SubselectEvalStrategyNRExistsDefault(ExprEvaluator filterEval, ExprEvaluator havingEval) {
        this.filterEval = filterEval;
        this.havingEval = havingEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService) {
        if (matchingEvents == null || matchingEvents.size() == 0) {
            return false;
        }
        if (filterEval == null && havingEval == null) {
            return true;
        }

        EventBean[] events = EventBeanUtility.allocatePerStreamShift(eventsPerStream);
        if (havingEval != null) {
            Boolean pass = (Boolean) havingEval.evaluate(events, true, exprEvaluatorContext);
            return (pass != null) && pass;
        } else if (filterEval != null) {
            for (EventBean subselectEvent : matchingEvents) {
                // Prepare filter expression event list
                events[0] = subselectEvent;

                Boolean pass = (Boolean) filterEval.evaluate(events, true, exprEvaluatorContext);
                if ((pass != null) && pass) {
                    return true;
                }
            }
            return false;
        } else {
            throw new IllegalStateException("Both filter and having clause encountered");
        }
    }
}
