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
package com.espertech.esper.common.internal.epl.agg.groupby;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class AggSvcGroupByReclaimAgedEvalFuncFactoryVariableForge implements AggSvcGroupByReclaimAgedEvalFuncFactoryForge {
    private final VariableMetaData variableMetaData;

    public AggSvcGroupByReclaimAgedEvalFuncFactoryVariableForge(VariableMetaData variableMetaData) {
        this.variableMetaData = variableMetaData;
    }

    public CodegenExpressionField make(CodegenClassScope classScope) {
        CodegenExpression resolve = VariableDeployTimeResolver.makeResolveVariable(variableMetaData, EPStatementInitServices.REF);
        return classScope.addFieldUnshared(true, AggSvcGroupByReclaimAgedEvalFuncFactoryVariable.class,
                newInstance(AggSvcGroupByReclaimAgedEvalFuncFactoryVariable.class, resolve));
    }
}
