/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class ExprTimePeriodEvalDeltaNonConstMsec implements ExprTimePeriodEvalDeltaNonConst
{
    private final ExprTimePeriodImpl exprTimePeriod;

    public ExprTimePeriodEvalDeltaNonConstMsec(ExprTimePeriodImpl exprTimePeriod) {
        this.exprTimePeriod = exprTimePeriod;
    }

    public long deltaMillisecondsAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        double d = exprTimePeriod.evaluateAsSeconds(eventsPerStream, isNewData, context);
        return Math.round(d * 1000d);
    }

    public long deltaMillisecondsSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return deltaMillisecondsAdd(currentTime, eventsPerStream, isNewData, context);
    }

    public long deltaMillisecondsUseEngineTime(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext) {
        return deltaMillisecondsAdd(0, eventsPerStream, true, agentInstanceContext);
    }

    public ExprTimePeriodEvalDeltaResult deltaMillisecondsAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        long msec = deltaMillisecondsAdd(current, eventsPerStream, isNewData, context);
        return new ExprTimePeriodEvalDeltaResult(ExprTimePeriodEvalDeltaConstMsec.deltaMillisecondsAddWReference(current, reference, msec), reference);
    }
}
