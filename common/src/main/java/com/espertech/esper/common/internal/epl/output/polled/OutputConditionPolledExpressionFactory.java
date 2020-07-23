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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionExpressionTypeUtil;
import com.espertech.esper.common.internal.epl.variable.core.VariableReadWritePackage;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryDisallow;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionPolledExpressionFactory implements OutputConditionPolledFactory {
    public final static EPTypeClass EPTYPE = new EPTypeClass(OutputConditionPolledExpressionFactory.class);

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

    public OutputConditionPolled makeFromState(ExprEvaluatorContext exprEvaluatorContext, OutputConditionPolledState state) {
        ObjectArrayEventBean builtinProperties = null;
        if (isUsingBuiltinProperties) {
            initType(exprEvaluatorContext);
            builtinProperties = new ObjectArrayEventBean(OutputConditionExpressionTypeUtil.getOAPrototype(), builtinPropertiesEventType);
        }
        OutputConditionPolledExpressionState expressionState = (OutputConditionPolledExpressionState) state;
        return new OutputConditionPolledExpression(this, expressionState, exprEvaluatorContext, builtinProperties);
    }

    public OutputConditionPolled makeNew(ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayEventBean builtinProperties = null;
        Long lastOutputTimestamp = null;
        if (isUsingBuiltinProperties) {
            initType(exprEvaluatorContext);
            builtinProperties = new ObjectArrayEventBean(OutputConditionExpressionTypeUtil.getOAPrototype(), builtinPropertiesEventType);
            lastOutputTimestamp = exprEvaluatorContext.getTimeProvider().getTime();
        }
        OutputConditionPolledExpressionState state = new OutputConditionPolledExpressionState(0, 0, 0, 0, lastOutputTimestamp);
        return new OutputConditionPolledExpression(this, state, exprEvaluatorContext, builtinProperties);
    }

    public ExprEvaluator getWhenExpression() {
        return whenExpression;
    }

    public VariableReadWritePackage getVariableReadWritePackage() {
        return variableReadWritePackage;
    }

    private void initType(ExprEvaluatorContext exprEvaluatorContext) {
        if (builtinPropertiesEventType == null) {
            builtinPropertiesEventType = OutputConditionExpressionTypeUtil.getBuiltInEventType(exprEvaluatorContext.getModuleName(), new BeanEventTypeFactoryDisallow(exprEvaluatorContext.getEventBeanTypedEventFactory()));
        }
    }
}
