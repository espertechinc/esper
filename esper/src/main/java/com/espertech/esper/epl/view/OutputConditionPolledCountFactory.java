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
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.variable.VariableReader;

/**
 * Output limit condition that is satisfied when either
 * the total number of new events arrived or the total number
 * of old events arrived is greater than a preset value.
 */
public final class OutputConditionPolledCountFactory implements OutputConditionPolledFactory {
    private final int eventRate;
    private final StatementContext statementContext;
    private final String variableName;

    /**
     * Constructor.
     *
     * @param eventRate        is the number of old or new events that
     *                         must arrive in order for the condition to be satisfied
     * @param variableName     variable
     * @param statementContext context
     */
    public OutputConditionPolledCountFactory(int eventRate, StatementContext statementContext, String variableName) {
        if ((eventRate < 1) && (variableName == null)) {
            throw new IllegalArgumentException("Limiting output by event count requires an event count of at least 1 or a variable name");
        }
        this.eventRate = eventRate;
        this.statementContext = statementContext;
        this.variableName = variableName;
    }

    public OutputConditionPolled makeNew(AgentInstanceContext agentInstanceContext) {
        OutputConditionPolledCountState state = new OutputConditionPolledCountState(eventRate, eventRate, eventRate, true);
        return new OutputConditionPolledCount(this, state, getVariableReader(agentInstanceContext));
    }

    public OutputConditionPolled makeFromState(AgentInstanceContext agentInstanceContext, OutputConditionPolledState state) {
        return new OutputConditionPolledCount(this, (OutputConditionPolledCountState) state, getVariableReader(agentInstanceContext));
    }

    private VariableReader getVariableReader(AgentInstanceContext agentInstanceContext) {
        if (variableName == null) {
            return null;
        }
        return statementContext.getVariableService().getReader(variableName, agentInstanceContext.getAgentInstanceId());
    }
}
