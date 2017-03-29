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
package com.espertech.esper.epl.approx;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class CountMinSketchAggAgentAddFilter extends CountMinSketchAggAgentAdd {

    private final ExprEvaluator filter;

    public CountMinSketchAggAgentAddFilter(ExprEvaluator stringEvaluator, ExprEvaluator filter) {
        super(stringEvaluator);
        this.filter = filter;
    }

    @Override
    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationState aggregationState) {
        Boolean pass = (Boolean) filter.evaluate(eventsPerStream, true, exprEvaluatorContext);
        if (pass != null && pass) {
            Object value = stringEvaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
            CountMinSketchAggState state = (CountMinSketchAggState) aggregationState;
            state.add(value);
        }
    }
}
