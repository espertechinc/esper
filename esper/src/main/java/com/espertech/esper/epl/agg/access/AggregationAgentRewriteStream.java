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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class AggregationAgentRewriteStream implements AggregationAgent {

    private final int streamNum;

    public AggregationAgentRewriteStream(int streamNum) {
        this.streamNum = streamNum;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationState aggregationState) {
        EventBean[] rewrite = new EventBean[]{eventsPerStream[streamNum]};
        aggregationState.applyEnter(rewrite, exprEvaluatorContext);
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationState aggregationState) {
        EventBean[] rewrite = new EventBean[]{eventsPerStream[streamNum]};
        aggregationState.applyLeave(rewrite, exprEvaluatorContext);
    }
}
