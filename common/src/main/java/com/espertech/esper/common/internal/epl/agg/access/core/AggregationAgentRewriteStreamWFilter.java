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
package com.espertech.esper.common.internal.epl.agg.access.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAgent;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

public class AggregationAgentRewriteStreamWFilter implements AggregationMultiFunctionAgent {

    private final int streamNum;
    private final ExprEvaluator filterEval;

    public AggregationAgentRewriteStreamWFilter(int streamNum, ExprEvaluator filterEval) {
        this.streamNum = streamNum;
        this.filterEval = filterEval;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column) {
        Boolean pass = (Boolean) filterEval.evaluate(eventsPerStream, true, exprEvaluatorContext);
        if (pass != null && pass) {
            EventBean[] rewrite = new EventBean[]{eventsPerStream[streamNum]};
            row.enterAccess(column, rewrite, exprEvaluatorContext);
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column) {
        Boolean pass = (Boolean) filterEval.evaluate(eventsPerStream, false, exprEvaluatorContext);
        if (pass != null && pass) {
            EventBean[] rewrite = new EventBean[]{eventsPerStream[streamNum]};
            row.leaveAccess(column, rewrite, exprEvaluatorContext);
        }
    }
}
