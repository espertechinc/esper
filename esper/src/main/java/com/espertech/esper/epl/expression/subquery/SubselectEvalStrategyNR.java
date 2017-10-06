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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

/**
 * Strategy for evaluation of a subselect.
 */
public interface SubselectEvalStrategyNR {
    /**
     * Evaluate.
     *
     * @param eventsPerStream      events per stream
     * @param isNewData            true for new data
     * @param matchingEvents       prefiltered events
     * @param exprEvaluatorContext expression evaluation context
     * @param aggregationService aggregation service or null if none
     * @return eval result
     */
    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService);
}
