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
package com.espertech.esper.common.internal.context.aifactory.createvariable;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StatementAgentInstanceFactoryCreateVariableForge {

    private final String variableName;
    private final ExprForge optionalInitialValue;
    private final String resultSetProcessorProviderClassName;

    public StatementAgentInstanceFactoryCreateVariableForge(String variableName, ExprForge optionalInitialValue, String resultSetProcessorProviderClassName) {
        this.variableName = variableName;
        this.optionalInitialValue = optionalInitialValue;
        this.resultSetProcessorProviderClassName = resultSetProcessorProviderClassName;
    }

    public CodegenMethod initializeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(StatementAgentInstanceFactoryCreateVariable.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(StatementAgentInstanceFactoryCreateVariable.class, "saiff", newInstance(StatementAgentInstanceFactoryCreateVariable.class))
                .exprDotMethod(ref("saiff"), "setVariableName", constant(variableName))
                .exprDotMethod(ref("saiff"), "setResultSetProcessorFactoryProvider", CodegenExpressionBuilder.newInstance(resultSetProcessorProviderClassName, symbols.getAddInitSvc(method)));
        if (optionalInitialValue != null) {
            method.getBlock()
                    .exprDotMethod(ref("saiff"), "setVariableInitialValueExpr", ExprNodeUtilityCodegen.codegenEvaluator(optionalInitialValue, method, this.getClass(), classScope))
                    .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add("addReadyCallback", ref("saiff")));
        }
        method.getBlock().methodReturn(ref("saiff"));
        return method;
    }
}
