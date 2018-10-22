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
package com.espertech.esper.common.internal.epl.agg.access.countminsketch;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAgent;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchAggState;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

public class AggregationAgentCountMinSketch implements AggregationMultiFunctionAgent {

    private ExprEvaluator stringEval;
    private ExprEvaluator optionalFilterEval;

    public void setStringEval(ExprEvaluator stringEval) {
        this.stringEval = stringEval;
    }

    public void setOptionalFilterEval(ExprEvaluator optionalFilterEval) {
        this.optionalFilterEval = optionalFilterEval;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column) {
        if (optionalFilterEval != null) {
            Boolean pass = (Boolean) optionalFilterEval.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (pass == null || !pass) {
                return;
            }
        }
        Object value = stringEval.evaluate(eventsPerStream, true, exprEvaluatorContext);
        if (value == null) {
            return;
        }
        CountMinSketchAggState state = (CountMinSketchAggState) row.getAccessState(column);
        state.add(value);
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column) {
    }
}
