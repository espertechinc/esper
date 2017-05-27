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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class SupportAggMFStateCollScalar implements AggregationState {

    private EventBean event;

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        event = eventsPerStream[0];
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // ever semantics
    }

    public void clear() {
        event = null;
    }

    public int size() {
        return event == null ? 0 : 1;
    }

    public EventBean getEvent() {
        return event;
    }
}
