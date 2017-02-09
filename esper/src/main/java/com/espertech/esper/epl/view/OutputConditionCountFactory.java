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
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableReader;

public class OutputConditionCountFactory implements OutputConditionFactory {
    protected final long eventRate;
    protected final VariableMetaData variableMetaData;

    /**
     * Constructor.
     *
     * @param eventRate        is the number of old or new events that
     *                         must arrive in order for the condition to be satisfied
     * @param variableMetaData variable metadata
     */
    public OutputConditionCountFactory(int eventRate, VariableMetaData variableMetaData) {
        if ((eventRate < 1) && (variableMetaData == null)) {
            throw new IllegalArgumentException("Limiting output by event count requires an event count of at least 1 or a variable name");
        }
        this.eventRate = eventRate;
        this.variableMetaData = variableMetaData;
    }

    public OutputCondition make(AgentInstanceContext agentInstanceContext, OutputCallback outputCallback) {
        VariableReader variableReader = null;
        if (variableMetaData != null) {
            variableReader = agentInstanceContext.getStatementContext().getVariableService().getReader(variableMetaData.getVariableName(), agentInstanceContext.getAgentInstanceId());
        }
        return new OutputConditionCount(outputCallback, eventRate, variableReader);
    }

    public long getEventRate() {
        return eventRate;
    }

    public VariableMetaData getVariableMetaData() {
        return variableMetaData;
    }
}
