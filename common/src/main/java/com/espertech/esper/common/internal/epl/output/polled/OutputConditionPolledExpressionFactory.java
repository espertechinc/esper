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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionExpressionTypeUtil;
import com.espertech.esper.common.internal.epl.variable.core.VariableReadWritePackage;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryDisallow;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionPolledExpressionFactory implements OutputConditionPolledFactory {
    private ExprEvaluator whenExpression;
    private VariableReadWritePackage variableReadWritePackage;
    private boolean isUsingBuiltinProperties;
    private EventType builtinPropertiesEventType;

    public void setWhenExpression(ExprEvaluator whenExpression) {
        this.whenExpression = whenExpression;
    }

    public void setVariableReadWritePackage(VariableReadWritePackage variableReadWritePackage) {
        this.variableReadWritePackage = variableReadWritePackage;
    }

    public void setUsingBuiltinProperties(boolean usingBuiltinProperties) {
        isUsingBuiltinProperties = usingBuiltinProperties;
    }

    public OutputConditionPolled makeFromState(AgentInstanceContext agentInstanceContext, OutputConditionPolledState state) {
        ObjectArrayEventBean builtinProperties = null;
        if (isUsingBuiltinProperties) {
            initType(agentInstanceContext);
            builtinProperties = new ObjectArrayEventBean(OutputConditionExpressionTypeUtil.getOAPrototype(), builtinPropertiesEventType);
        }
        OutputConditionPolledExpressionState expressionState = (OutputConditionPolledExpressionState) state;
        return new OutputConditionPolledExpression(this, expressionState, agentInstanceContext, builtinProperties);
    }

    public OutputConditionPolled makeNew(AgentInstanceContext agentInstanceContext) {
        ObjectArrayEventBean builtinProperties = null;
        Long lastOutputTimestamp = null;
        if (isUsingBuiltinProperties) {
            initType(agentInstanceContext);
            builtinProperties = new ObjectArrayEventBean(OutputConditionExpressionTypeUtil.getOAPrototype(), builtinPropertiesEventType);
            lastOutputTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        }
        OutputConditionPolledExpressionState state = new OutputConditionPolledExpressionState(0, 0, 0, 0, lastOutputTimestamp);
        return new OutputConditionPolledExpression(this, state, agentInstanceContext, builtinProperties);
    }

    public ExprEvaluator getWhenExpression() {
        return whenExpression;
    }

    public VariableReadWritePackage getVariableReadWritePackage() {
        return variableReadWritePackage;
    }

    private void initType(AgentInstanceContext agentInstanceContext) {
        if (builtinPropertiesEventType == null) {
            builtinPropertiesEventType = OutputConditionExpressionTypeUtil.getBuiltInEventType(agentInstanceContext.getModuleName(), new BeanEventTypeFactoryDisallow(agentInstanceContext.getEventBeanTypedEventFactory()));
        }
    }
}
