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

public final class OutputConditionPolledTime implements OutputConditionPolled {
    private final OutputConditionPolledTimeFactory factory;
    private final AgentInstanceContext context;
    private final OutputConditionPolledTimeState state;

    public OutputConditionPolledTime(OutputConditionPolledTimeFactory factory, AgentInstanceContext context, OutputConditionPolledTimeState state) {
        this.factory = factory;
        this.context = context;
        this.state = state;
    }

    public OutputConditionPolledState getState() {
        return state;
    }

    public boolean updateOutputCondition(int newEventsCount, int oldEventsCount) {
        // If we pull the interval from a variable, then we may need to reschedule
        long msecIntervalSize = factory.getTimePeriod().nonconstEvaluator().deltaUseEngineTime(null, context, context.getTimeProvider());

        long current = context.getTimeProvider().getTime();
        if (state.getLastUpdate() == null || current - state.getLastUpdate() >= msecIntervalSize) {
            state.setLastUpdate(current);
            return true;
        }
        return false;
    }
}