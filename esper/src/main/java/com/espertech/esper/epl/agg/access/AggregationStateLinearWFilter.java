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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class AggregationStateLinearWFilter extends AggregationStateLinearImpl {
    private final ExprEvaluator filter;

    public AggregationStateLinearWFilter(int streamId, ExprEvaluator filter) {
        super(streamId);
        this.filter = filter;
    }

    public void clear() {
        events.clear();
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[streamId];
        if (theEvent == null) {
            return;
        }
        Boolean pass = (Boolean) filter.evaluate(eventsPerStream, false, exprEvaluatorContext);
        if (pass != null && pass) {
            events.remove(theEvent);
        }
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[streamId];
        if (theEvent == null) {
            return;
        }
        Boolean pass = (Boolean) filter.evaluate(eventsPerStream, false, exprEvaluatorContext);
        if (pass != null && pass) {
            events.add(theEvent);
        }
    }
}
