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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAgent;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

public class SupportReferenceCountedMapAgent implements AggregationMultiFunctionAgent {
    private final ExprEvaluator evaluator;

    public SupportReferenceCountedMapAgent(ExprEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column) {
        apply(true, eventsPerStream, exprEvaluatorContext, row, column);
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column) {
        apply(false, eventsPerStream, exprEvaluatorContext, row, column);
    }

    private void apply(boolean enter, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column) {
        SupportReferenceCountedMapState state = (SupportReferenceCountedMapState) row.getAccessState(column);
        Object value = evaluator.evaluate(eventsPerStream, enter, exprEvaluatorContext);
        if (enter) {
            state.enter(value);
        } else {
            state.leave(value);
        }
    }
}
