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
package com.espertech.esper.epl.view;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;

public final class OutputConditionPolledTimeFactory implements OutputConditionPolledFactory {
    private final ExprTimePeriod timePeriod;

    public OutputConditionPolledTimeFactory(ExprTimePeriod timePeriod, StatementContext statementContext) {
        this.timePeriod = timePeriod;
        double numSeconds = timePeriod.evaluateAsSeconds(null, true, new ExprEvaluatorContextStatement(statementContext, false));
        if ((numSeconds < 0.001) && (!timePeriod.hasVariable())) {
            throw new IllegalArgumentException("Output condition by time requires a interval size of at least 1 msec or a variable");
        }
    }

    public OutputConditionPolled makeNew(AgentInstanceContext agentInstanceContext) {
        return new OutputConditionPolledTime(this, agentInstanceContext, new OutputConditionPolledTimeState(null));
    }

    public OutputConditionPolled makeFromState(AgentInstanceContext agentInstanceContext, OutputConditionPolledState state) {
        OutputConditionPolledTimeState timeState = (OutputConditionPolledTimeState) state;
        return new OutputConditionPolledTime(this, agentInstanceContext, timeState);
    }

    public ExprTimePeriod getTimePeriod() {
        return timePeriod;
    }
}