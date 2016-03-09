/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierVisitor;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.variable.VariableReadWritePackage;
import com.espertech.esper.event.arr.ObjectArrayEventBean;

import java.util.List;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionPolledExpression implements OutputConditionPolled
{
    private final OutputConditionPolledExpressionFactory factory;
    private final OutputConditionPolledExpressionState state;
    private final AgentInstanceContext agentInstanceContext;

    private ObjectArrayEventBean builtinProperties;
    private EventBean[] eventsPerStream = new EventBean[1];

    public OutputConditionPolledExpression(OutputConditionPolledExpressionFactory factory, OutputConditionPolledExpressionState state, AgentInstanceContext agentInstanceContext, ObjectArrayEventBean builtinProperties) {
        this.factory = factory;
        this.state = state;
        this.builtinProperties = builtinProperties;
        this.agentInstanceContext = agentInstanceContext;
    }

    public OutputConditionPolledState getState() {
        return state;
    }

    public boolean updateOutputCondition(int newEventsCount, int oldEventsCount)
    {
        state.setTotalNewEventsCount(state.getTotalNewEventsCount() + newEventsCount);
        state.setTotalOldEventsCount(state.getTotalOldEventsCount() + oldEventsCount);
        state.setTotalNewEventsSum(state.getTotalNewEventsSum() + newEventsCount);
        state.setTotalOldEventsSum(state.getTotalOldEventsCount() + oldEventsCount);

        boolean isOutput = evaluate();
        if (isOutput) {
            resetBuiltinProperties();

            // execute assignments
            if (factory.getVariableReadWritePackage() != null) {
                if (builtinProperties != null) {
                    populateBuiltinProperties();
                    eventsPerStream[0] = builtinProperties;
                }

                try {
                    factory.getVariableReadWritePackage().writeVariables(agentInstanceContext.getStatementContext().getVariableService(), eventsPerStream, null, agentInstanceContext);
                }
                finally {
                }
            }
        }
        return isOutput;
    }

    private void populateBuiltinProperties() {
        OutputConditionExpressionTypeUtil.populate(builtinProperties.getProperties(), state.getTotalNewEventsCount(),
                state.getTotalOldEventsCount(), state.getTotalNewEventsSum(),
                state.getTotalOldEventsSum(), state.getLastOutputTimestamp());
    }

    private boolean evaluate()
    {
        if (builtinProperties != null) {
            populateBuiltinProperties();
            eventsPerStream[0] = builtinProperties;
        }

        boolean result = false;
        Boolean output = (Boolean) factory.getWhenExpressionNode().evaluate(eventsPerStream, true, agentInstanceContext);
        if ((output != null) && (output)) {
            result = true;
        }

        return result;
    }

    private void resetBuiltinProperties()
    {
        if (builtinProperties  != null)
        {
            state.setTotalNewEventsCount(0);
            state.setTotalOldEventsCount(0);
            state.setLastOutputTimestamp(agentInstanceContext.getStatementContext().getSchedulingService().getTime());
        }
    }
}
