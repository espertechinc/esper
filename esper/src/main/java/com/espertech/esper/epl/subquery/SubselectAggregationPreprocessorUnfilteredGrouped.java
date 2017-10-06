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
package com.espertech.esper.epl.subquery;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

public class SubselectAggregationPreprocessorUnfilteredGrouped extends SubselectAggregationPreprocessorBase {

    public SubselectAggregationPreprocessorUnfilteredGrouped(AggregationService aggregationService, ExprEvaluator filterEval, ExprEvaluator[] groupKeys) {
        super(aggregationService, filterEval, groupKeys);
    }

    public void evaluate(EventBean[] eventsPerStream, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext) {

        aggregationService.clearResults(exprEvaluatorContext);
        if (matchingEvents == null) {
            return;
        }
        EventBean[] events = new EventBean[eventsPerStream.length + 1];
        System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);

        for (EventBean subselectEvent : matchingEvents) {
            events[0] = subselectEvent;
            Object groupKey = generateGroupKey(events, true, exprEvaluatorContext);
            aggregationService.applyEnter(events, groupKey, exprEvaluatorContext);
        }
    }
}
