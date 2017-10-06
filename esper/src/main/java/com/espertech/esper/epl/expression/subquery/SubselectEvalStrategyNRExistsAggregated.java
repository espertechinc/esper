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

public class SubselectEvalStrategyNRExistsAggregated implements SubselectEvalStrategyNR {
    private final ExprEvaluator havingEval;

    public SubselectEvalStrategyNRExistsAggregated(ExprEvaluator havingEval) {
        this.havingEval = havingEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService) {
        EventBean[] events = EventBeanUtility.allocatePerStreamShift(eventsPerStream);
        Boolean pass = (Boolean) havingEval.evaluate(events, true, exprEvaluatorContext);
        return pass != null && pass;
    }
}
