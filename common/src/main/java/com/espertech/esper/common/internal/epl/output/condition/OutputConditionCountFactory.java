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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;

public class OutputConditionCountFactory implements OutputConditionFactory {
    protected final long eventRate;
    protected final Variable variable;

    /**
     * Constructor.
     *
     * @param eventRate is the number of old or new events that
     *                  must arrive in order for the condition to be satisfied
     * @param variable  varianle
     */
    public OutputConditionCountFactory(int eventRate, Variable variable) {
        if ((eventRate < 1) && (variable == null)) {
            throw new IllegalArgumentException("Limiting output by event count requires an event count of at least 1 or a variable name");
        }
        this.eventRate = eventRate;
        this.variable = variable;
    }

    public OutputCondition instantiateOutputCondition(AgentInstanceContext agentInstanceContext, OutputCallback outputCallback) {
        VariableReader variableReader = null;
        if (variable != null) {
            variableReader = agentInstanceContext.getStatementContext().getVariableManagementService().getReader(variable.getDeploymentId(), variable.getMetaData().getVariableName(), agentInstanceContext.getAgentInstanceId());
        }
        return new OutputConditionCount(outputCallback, eventRate, variableReader);
    }

    public long getEventRate() {
        return eventRate;
    }

    public Object getVariable() {
        return variable;
    }
}
