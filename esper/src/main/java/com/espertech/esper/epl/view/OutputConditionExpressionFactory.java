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

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeVariableVisitor;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.variable.VariableReadWritePackage;
import com.espertech.esper.event.EventAdapterService;

import java.util.List;
import java.util.Set;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionExpressionFactory implements OutputConditionFactory {
    private final ExprEvaluator whenExpressionNodeEval;
    private final ExprEvaluator andWhenTerminatedExpressionNodeEval;
    private final VariableReadWritePackage variableReadWritePackage;
    private final VariableReadWritePackage variableReadWritePackageAfterTerminated;
    private final Set<String> variableNames;
    protected final boolean isStartConditionOnCreation;

    private EventType builtinPropertiesEventType;

    public OutputConditionExpressionFactory(ExprNode whenExpressionNode, List<OnTriggerSetAssignment> assignments, final StatementContext statementContext, ExprNode andWhenTerminatedExpr, List<OnTriggerSetAssignment> afterTerminateAssignments, boolean isStartConditionOnCreation)
            throws ExprValidationException {
        this.whenExpressionNodeEval = ExprNodeCompiler.allocateEvaluator(whenExpressionNode.getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
        this.andWhenTerminatedExpressionNodeEval = andWhenTerminatedExpr != null ? ExprNodeCompiler.allocateEvaluator(andWhenTerminatedExpr.getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName()) : null;
        this.isStartConditionOnCreation = isStartConditionOnCreation;

        // determine if using variables
        ExprNodeVariableVisitor variableVisitor = new ExprNodeVariableVisitor(statementContext.getVariableService());
        whenExpressionNode.accept(variableVisitor);
        variableNames = variableVisitor.getVariableNames();

        // determine if using properties
        boolean containsBuiltinProperties = containsBuiltinProperties(whenExpressionNode);
        if (!containsBuiltinProperties && assignments != null) {
            for (OnTriggerSetAssignment assignment : assignments) {
                if (containsBuiltinProperties(assignment.getExpression())) {
                    containsBuiltinProperties = true;
                }
            }
        }
        if (!containsBuiltinProperties && andWhenTerminatedExpressionNodeEval != null) {
            containsBuiltinProperties = containsBuiltinProperties(andWhenTerminatedExpr);
        }
        if (!containsBuiltinProperties && afterTerminateAssignments != null) {
            for (OnTriggerSetAssignment assignment : afterTerminateAssignments) {
                if (containsBuiltinProperties(assignment.getExpression())) {
                    containsBuiltinProperties = true;
                }
            }
        }

        if (containsBuiltinProperties) {
            builtinPropertiesEventType = getBuiltInEventType(statementContext.getEventAdapterService());
        }

        if (assignments != null) {
            variableReadWritePackage = new VariableReadWritePackage(assignments, statementContext.getVariableService(), statementContext.getEventAdapterService(), statementContext.getStatementName());
        } else {
            variableReadWritePackage = null;
        }

        if (afterTerminateAssignments != null) {
            variableReadWritePackageAfterTerminated = new VariableReadWritePackage(afterTerminateAssignments, statementContext.getVariableService(), statementContext.getEventAdapterService(), statementContext.getStatementName());
        } else {
            variableReadWritePackageAfterTerminated = null;
        }
    }

    public OutputCondition make(AgentInstanceContext agentInstanceContext, OutputCallback outputCallback) {
        return new OutputConditionExpression(outputCallback, agentInstanceContext, this, isStartConditionOnCreation);
    }

    public ExprEvaluator getWhenExpressionNodeEval() {
        return whenExpressionNodeEval;
    }

    public ExprEvaluator getAndWhenTerminatedExpressionNodeEval() {
        return andWhenTerminatedExpressionNodeEval;
    }

    public VariableReadWritePackage getVariableReadWritePackage() {
        return variableReadWritePackage;
    }

    public VariableReadWritePackage getVariableReadWritePackageAfterTerminated() {
        return variableReadWritePackageAfterTerminated;
    }

    public EventType getBuiltinPropertiesEventType() {
        return builtinPropertiesEventType;
    }

    public Set<String> getVariableNames() {
        return variableNames;
    }

    /**
     * Build the event type for built-in properties.
     *
     * @param eventAdapterService event adapters
     * @return event type
     */
    public static EventType getBuiltInEventType(EventAdapterService eventAdapterService) {
        return eventAdapterService.createAnonymousObjectArrayType(OutputConditionExpressionFactory.class.getName(), OutputConditionExpressionTypeUtil.TYPEINFO);
    }

    private boolean containsBuiltinProperties(ExprNode expr) {
        ExprNodeIdentifierVisitor propertyVisitor = new ExprNodeIdentifierVisitor(false);
        expr.accept(propertyVisitor);
        return !propertyVisitor.getExprProperties().isEmpty();
    }
}
