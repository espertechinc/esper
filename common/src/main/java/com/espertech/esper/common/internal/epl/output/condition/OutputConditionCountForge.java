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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableDeployTimeResolver;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class OutputConditionCountForge implements OutputConditionFactoryForge {
    protected final int eventRate;
    protected final VariableMetaData variableMetaData;

    public OutputConditionCountForge(int eventRate, VariableMetaData variableMetaData) {
        if ((eventRate < 1) && (variableMetaData == null)) {
            throw new IllegalArgumentException("Limiting output by event count requires an event count of at least 1 or a variable name");
        }
        this.eventRate = eventRate;
        this.variableMetaData = variableMetaData;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        // resolve variable at init-time via field
        CodegenExpression variableExpression = constantNull();
        if (variableMetaData != null) {
            variableExpression = VariableDeployTimeResolver.makeVariableField(variableMetaData, classScope, this.getClass());
        }

        CodegenMethod method = parent.makeChild(OutputConditionFactory.class, this.getClass(), classScope);
        method.getBlock()
                .methodReturn(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETRESULTSETPROCESSORHELPERFACTORY)
                        .add("makeOutputConditionCount", constant(eventRate), variableExpression));
        return localMethod(method);
    }

    public void collectSchedules(List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders) {
    }
}
