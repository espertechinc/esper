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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class ExprTimePeriodEvalDeltaNonConstMsec implements ExprTimePeriodEvalDeltaNonConst {
    private final ExprTimePeriodImpl exprTimePeriod;

    public ExprTimePeriodEvalDeltaNonConstMsec(ExprTimePeriodImpl exprTimePeriod) {
        this.exprTimePeriod = exprTimePeriod;
    }

    public long deltaAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        double d = exprTimePeriod.evaluateAsSeconds(eventsPerStream, isNewData, context);
        return exprTimePeriod.getTimeAbacus().deltaForSecondsDouble(d);
    }

    public long deltaSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return deltaAdd(currentTime, eventsPerStream, isNewData, context);
    }

    public long deltaUseEngineTime(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext) {
        return deltaAdd(0, eventsPerStream, true, agentInstanceContext);
    }

    public ExprTimePeriodEvalDeltaResult deltaAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        long msec = deltaAdd(current, eventsPerStream, isNewData, context);
        return new ExprTimePeriodEvalDeltaResult(ExprTimePeriodEvalDeltaConstGivenDelta.deltaAddWReference(current, reference, msec), reference);
    }
}
