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

import com.espertech.esper.client.EPException;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

public class ExprTimePeriodEvalDeltaConstFactoryMsec implements ExprTimePeriodEvalDeltaConstFactory {
    private final ExprEvaluator secondsEvaluator;
    private final TimeAbacus timeAbacus;

    public ExprTimePeriodEvalDeltaConstFactoryMsec(ExprEvaluator secondsEvaluator, TimeAbacus timeAbacus) {
        this.secondsEvaluator = secondsEvaluator;
        this.timeAbacus = timeAbacus;
    }

    public ExprTimePeriodEvalDeltaConst make(String validateMsgName, String validateMsgValue, AgentInstanceContext agentInstanceContext) {
        Number time = (Number) secondsEvaluator.evaluate(null, true, agentInstanceContext);
        if (!ExprTimePeriodUtil.validateTime(time, agentInstanceContext.getStatementContext().getTimeAbacus())) {
            throw new EPException(ExprTimePeriodUtil.getTimeInvalidMsg(validateMsgName, validateMsgValue, time));
        }
        return new ExprTimePeriodEvalDeltaConstGivenDelta(timeAbacus.deltaForSecondsNumber(time));
    }
}
