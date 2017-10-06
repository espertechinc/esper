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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

/**
 * Aggregation result future for use with match recognize.
 */
public interface AggregationServiceMatchRecognize extends AggregationResultFuture {
    /**
     * Enter a single event row consisting of one or more events per stream (each stream representing a variable).
     *
     * @param eventsPerStream      events per stream
     * @param streamId             variable number that is the base
     * @param exprEvaluatorContext context for expression evaluatiom
     */
    public void applyEnter(EventBean[] eventsPerStream, int streamId, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Clear current aggregation state.
     */
    public void clearResults();
}