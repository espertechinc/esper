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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableReadWritePackage;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryDisallow;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionExpressionFactory implements OutputConditionFactory, StatementReadyCallback {

    private ExprEvaluator whenExpressionNodeEval;
    private ExprEvaluator andWhenTerminatedExpressionNodeEval;
    private boolean isStartConditionOnCreation;
    private boolean isUsingBuiltinProperties;

    private EventType builtinPropertiesEventType;
    private VariableReadWritePackage variableReadWritePackage;
    private VariableReadWritePackage variableReadWritePackageAfterTerminated;
    private Variable[] variables;
    protected int scheduleCallbackId = -1;

    public void setWhenExpressionNodeEval(ExprEvaluator whenExpressionNodeEval) {
        this.whenExpressionNodeEval = whenExpressionNodeEval;
    }

    public void setAndWhenTerminatedExpressionNodeEval(ExprEvaluator andWhenTerminatedExpressionNodeEval) {
        this.andWhenTerminatedExpressionNodeEval = andWhenTerminatedExpressionNodeEval;
    }

    public void setStartConditionOnCreation(boolean startConditionOnCreation) {
        isStartConditionOnCreation = startConditionOnCreation;
    }

    public void setUsingBuiltinProperties(boolean usingBuiltinProperties) {
        isUsingBuiltinProperties = usingBuiltinProperties;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public ExprEvaluator getWhenExpressionNodeEval() {
        return whenExpressionNodeEval;
    }

    public ExprEvaluator getAndWhenTerminatedExpressionNodeEval() {
        return andWhenTerminatedExpressionNodeEval;
    }

    public boolean isStartConditionOnCreation() {
        return isStartConditionOnCreation;
    }

    public EventType getBuiltinPropertiesEventType() {
        return builtinPropertiesEventType;
    }

    public VariableReadWritePackage getVariableReadWritePackage() {
        return variableReadWritePackage;
    }

    public VariableReadWritePackage getVariableReadWritePackageAfterTerminated() {
        return variableReadWritePackageAfterTerminated;
    }

    public void setVariableReadWritePackage(VariableReadWritePackage variableReadWritePackage) {
        this.variableReadWritePackage = variableReadWritePackage;
    }

    public void setVariableReadWritePackageAfterTerminated(VariableReadWritePackage variableReadWritePackageAfterTerminated) {
        this.variableReadWritePackageAfterTerminated = variableReadWritePackageAfterTerminated;
    }

    public Variable[] getVariables() {
        return variables;
    }

    public void setVariables(Variable[] variables) {
        this.variables = variables;
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        if (isUsingBuiltinProperties) {
            builtinPropertiesEventType = OutputConditionExpressionTypeUtil.getBuiltInEventType(statementContext.getModuleName(), new BeanEventTypeFactoryDisallow(statementContext.getEventBeanTypedEventFactory()));
        }
    }

    public OutputCondition instantiateOutputCondition(AgentInstanceContext agentInstanceContext, OutputCallback outputCallback) {
        return new OutputConditionExpression(outputCallback, agentInstanceContext, this);
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }
}
