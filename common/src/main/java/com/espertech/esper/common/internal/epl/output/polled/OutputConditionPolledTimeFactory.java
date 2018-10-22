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
package com.espertech.esper.common.internal.epl.output.polled;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;

public final class OutputConditionPolledTimeFactory implements OutputConditionPolledFactory {
    protected final TimePeriodCompute timePeriodCompute;

    public OutputConditionPolledTimeFactory(TimePeriodCompute timePeriodCompute) {
        this.timePeriodCompute = timePeriodCompute;
    }

    public OutputConditionPolled makeNew(AgentInstanceContext agentInstanceContext) {
        return new OutputConditionPolledTime(this, agentInstanceContext, new OutputConditionPolledTimeState(null));
    }

    public OutputConditionPolled makeFromState(AgentInstanceContext agentInstanceContext, OutputConditionPolledState state) {
        OutputConditionPolledTimeState timeState = (OutputConditionPolledTimeState) state;
        return new OutputConditionPolledTime(this, agentInstanceContext, timeState);
    }
}