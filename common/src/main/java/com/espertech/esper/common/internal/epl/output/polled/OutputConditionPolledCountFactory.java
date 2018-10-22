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
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;

/**
 * Output limit condition that is satisfied when either
 * the total number of new events arrived or the total number
 * of old events arrived is greater than a preset value.
 */
public final class OutputConditionPolledCountFactory implements OutputConditionPolledFactory {
    private int eventRate;
    private Variable variable;

    public void setEventRate(int eventRate) {
        this.eventRate = eventRate;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public OutputConditionPolled makeNew(AgentInstanceContext agentInstanceContext) {
        OutputConditionPolledCountState state = new OutputConditionPolledCountState(eventRate, eventRate, eventRate, true);
        return new OutputConditionPolledCount(state, getVariableReader(agentInstanceContext));
    }

    public OutputConditionPolled makeFromState(AgentInstanceContext agentInstanceContext, OutputConditionPolledState state) {
        return new OutputConditionPolledCount((OutputConditionPolledCountState) state, getVariableReader(agentInstanceContext));
    }

    private VariableReader getVariableReader(AgentInstanceContext agentInstanceContext) {
        if (variable == null) {
            return null;
        }
        return agentInstanceContext.getVariableManagementService().getReader(variable.getDeploymentId(), variable.getMetaData().getVariableName(), agentInstanceContext.getAgentInstanceId());
    }
}
