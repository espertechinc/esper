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
package com.espertech.esper.common.internal.epl.resultset.order;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;

/**
 * A factory for row-limit processor instances.
 */
public class RowLimitProcessorFactory {

    private Variable numRowsVariable;
    private Variable offsetVariable;
    private int currentRowLimit;
    private int currentOffset;

    public void setNumRowsVariable(Variable numRowsVariable) {
        this.numRowsVariable = numRowsVariable;
    }

    public void setOffsetVariable(Variable offsetVariable) {
        this.offsetVariable = offsetVariable;
    }

    public void setCurrentRowLimit(int currentRowLimit) {
        this.currentRowLimit = currentRowLimit;
    }

    public void setCurrentOffset(int currentOffset) {
        this.currentOffset = currentOffset;
    }

    public RowLimitProcessor instantiate(AgentInstanceContext agentInstanceContext) {
        VariableReader numRowsVariableReader = null;
        if (numRowsVariable != null) {
            numRowsVariableReader = agentInstanceContext.getVariableManagementService().getReader(numRowsVariable.getDeploymentId(), numRowsVariable.getMetaData().getVariableName(), agentInstanceContext.getAgentInstanceId());
        }

        VariableReader offsetVariableReader = null;
        if (offsetVariable != null) {
            offsetVariableReader = agentInstanceContext.getStatementContext().getVariableManagementService().getReader(offsetVariable.getDeploymentId(), offsetVariable.getMetaData().getVariableName(), agentInstanceContext.getAgentInstanceId());
        }

        return new RowLimitProcessor(numRowsVariableReader, offsetVariableReader, currentRowLimit, currentOffset);
    }
}
