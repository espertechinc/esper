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
package com.espertech.esper.common.internal.epl.historical.method.poll;

import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;

import java.lang.reflect.Method;

public class MethodTargetStrategyVariableFactory implements MethodTargetStrategyFactory, StatementReadyCallback {
    private Variable variable;
    private String methodName;
    private Class[] methodParameters;
    protected Method method;
    protected MethodTargetStrategyStaticMethodInvokeType invokeType;

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        this.method = MethodTargetStrategyStaticMethod.resolveMethod(variable.getMetaData().getType(), methodName, methodParameters);
        this.invokeType = MethodTargetStrategyStaticMethodInvokeType.getInvokeType(method);
    }

    public MethodTargetStrategy make(AgentInstanceContext agentInstanceContext) {
        VariableReader reader = agentInstanceContext.getVariableManagementService().getReader(variable.getDeploymentId(), variable.getMetaData().getVariableName(), agentInstanceContext.getAgentInstanceId());
        return new MethodTargetStrategyVariable(this, reader);
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setMethodParameters(Class[] methodParameters) {
        this.methodParameters = methodParameters;
    }
}
