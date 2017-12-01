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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;

public class ExprDotMethodForgeDuck implements ExprDotForge {
    private final String statementName;
    private final EngineImportService engineImportService;
    private final String methodName;
    private final Class[] parameterTypes;
    private final ExprForge[] parameters;

    public ExprDotMethodForgeDuck(String statementName, EngineImportService engineImportService, String methodName, Class[] parameterTypes, ExprForge[] parameters) {
        this.statementName = statementName;
        this.engineImportService = engineImportService;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }

    public EPType getTypeInfo() {
        return EPTypeHelper.singleValue(Object.class);
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitMethod(methodName);
    }

    public ExprDotEval getDotEvaluator() {
        return new ExprDotMethodForgeDuckEval(this, ExprNodeUtilityCore.getEvaluatorsNoCompile(parameters));
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprDotMethodForgeDuckEval.codegen(this, inner, innerType, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public String getStatementName() {
        return statementName;
    }

    public EngineImportService getEngineImportService() {
        return engineImportService;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public ExprForge[] getParameters() {
        return parameters;
    }
}
