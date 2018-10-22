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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

public class ExprDotMethodForgeDuck implements ExprDotForge {
    private final String statementName;
    private final ClasspathImportService classpathImportService;
    private final String methodName;
    private final Class[] parameterTypes;
    private final ExprForge[] parameters;

    public ExprDotMethodForgeDuck(String statementName, ClasspathImportService classpathImportService, String methodName, Class[] parameterTypes, ExprForge[] parameters) {
        this.statementName = statementName;
        this.classpathImportService = classpathImportService;
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
        return new ExprDotMethodForgeDuckEval(this, ExprNodeUtilityQuery.getEvaluatorsNoCompile(parameters));
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprDotMethodForgeDuckEval.codegen(this, inner, innerType, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public String getStatementName() {
        return statementName;
    }

    public ClasspathImportService getClasspathImportService() {
        return classpathImportService;
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
