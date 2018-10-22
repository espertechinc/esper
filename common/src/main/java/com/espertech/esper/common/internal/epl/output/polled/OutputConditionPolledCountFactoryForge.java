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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Output limit condition that is satisfied when either
 * the total number of new events arrived or the total number
 * of old events arrived is greater than a preset value.
 */
public final class OutputConditionPolledCountFactoryForge implements OutputConditionPolledFactoryForge {
    private final int eventRate;
    private final VariableMetaData variableMetaData;

    public OutputConditionPolledCountFactoryForge(int eventRate, VariableMetaData variableMetaData) {
        this.eventRate = eventRate;
        this.variableMetaData = variableMetaData;

        if ((eventRate < 1) && (variableMetaData == null)) {
            throw new IllegalArgumentException("Limiting output by event count requires an event count of at least 1 or a variable name");
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        // resolve variable at init-time via field
        CodegenExpression variableExpression = constantNull();
        if (variableMetaData != null) {
            variableExpression = VariableDeployTimeResolver.makeVariableField(variableMetaData, classScope, this.getClass());
        }

        CodegenMethod method = parent.makeChild(OutputConditionPolledCountFactory.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(OutputConditionPolledCountFactory.class, "factory", newInstance(OutputConditionPolledCountFactory.class))
                .exprDotMethod(ref("factory"), "setEventRate", constant(eventRate))
                .exprDotMethod(ref("factory"), "setVariable", variableExpression)
                .methodReturn(ref("factory"));
        return localMethod(method);
    }
}
