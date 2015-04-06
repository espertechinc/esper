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
    private final ExprEvaluator whenExpressionNode;
    private final AgentInstanceContext agentInstanceContext;
    private final VariableReadWritePackage variableReadWritePackage;

    private ObjectArrayEventBean builtinProperties;
    private EventBean[] eventsPerStream;

    // ongoing builtin properties
    private int totalNewEventsCount;
    private int totalOldEventsCount;
    private int totalNewEventsSum;
    private int totalOldEventsSum;
    private Long lastOutputTimestamp;

    /**
     * Ctor.
     * @param whenExpressionNode the expression to evaluate, returning true when to output
     * @param assignments is the optional then-clause variable assignments, or null or empty if none
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException when validation fails
     */
    public OutputConditionPolledExpression(ExprNode whenExpressionNode, List<OnTriggerSetAssignment> assignments, AgentInstanceContext agentInstanceContext)
            throws ExprValidationException
    {
        this.whenExpressionNode = whenExpressionNode.getExprEvaluator();
        this.agentInstanceContext = agentInstanceContext;
        this.eventsPerStream = new EventBean[1];

        // determine if using properties
        boolean containsBuiltinProperties = false;
        if (containsBuiltinProperties(whenExpressionNode))
        {
            containsBuiltinProperties = true;
        }
        else
        {
            if (assignments != null)
            {
                for (OnTriggerSetAssignment assignment : assignments)
                {
                    if (containsBuiltinProperties(assignment.getExpression()))
                    {
                        containsBuiltinProperties = true;
                    }
                }
            }
        }

        if (containsBuiltinProperties)
        {
            EventType oatype = agentInstanceContext.getStatementContext().getEventAdapterService().createAnonymousObjectArrayType(OutputConditionPolledExpression.class.getName(), OutputConditionExpressionTypeUtil.TYPEINFO);
            builtinProperties = new ObjectArrayEventBean(OutputConditionExpressionTypeUtil.getOAPrototype(), oatype);
            lastOutputTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        }

        if (assignments != null)
        {
            variableReadWritePackage = new VariableReadWritePackage(assignments, agentInstanceContext.getStatementContext().getVariableService(), agentInstanceContext.getStatementContext().getEventAdapterService());
        }
        else
        {
            variableReadWritePackage = null;
        }
    }

    public boolean updateOutputCondition(int newEventsCount, int oldEventsCount)
    {
        this.totalNewEventsCount += newEventsCount;
        this.totalOldEventsCount += oldEventsCount;
        this.totalNewEventsSum += newEventsCount;
        this.totalOldEventsSum += oldEventsCount;

        boolean isOutput = evaluate();
        if (isOutput)
        {
            resetBuiltinProperties();

            // execute assignments
            if (variableReadWritePackage != null)
            {
                if (builtinProperties != null)
                {
                    populateBuiltinProperties();
                    eventsPerStream[0] = builtinProperties;
                }

                try {
                    variableReadWritePackage.writeVariables(agentInstanceContext.getStatementContext().getVariableService(), eventsPerStream, null, agentInstanceContext);
                }
                finally {
                }
            }
        }
        return isOutput;
    }

    private void populateBuiltinProperties() {
        OutputConditionExpressionTypeUtil.populate(builtinProperties.getProperties(), totalNewEventsCount, totalOldEventsCount,
                totalNewEventsSum, totalOldEventsSum, lastOutputTimestamp);
    }

    private boolean evaluate()
    {
        if (builtinProperties != null)
        {
            populateBuiltinProperties();
            eventsPerStream[0] = builtinProperties;
        }

        boolean result = false;
        Boolean output = (Boolean) whenExpressionNode.evaluate(eventsPerStream, true, agentInstanceContext);
        if ((output != null) && (output))
        {
            result = true;
        }

        return result;
    }

    private void resetBuiltinProperties()
    {
        if (builtinProperties  != null)
        {
            totalNewEventsCount = 0;
            totalOldEventsCount = 0;
            lastOutputTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        }
    }

    private boolean containsBuiltinProperties(ExprNode expr)
    {
        ExprNodeIdentifierVisitor propertyVisitor = new ExprNodeIdentifierVisitor(false);
        expr.accept(propertyVisitor);
        return !propertyVisitor.getExprProperties().isEmpty();
    }
}
